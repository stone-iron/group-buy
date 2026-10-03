package com.livegroupbuy.backend.payment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.livegroupbuy.backend.purchase.ProductInventory;
import com.livegroupbuy.backend.purchase.ProductInventoryRepository;
import com.livegroupbuy.backend.purchase.PurchaseQueueResponse;
import com.livegroupbuy.backend.purchase.PurchaseQueueService;
import com.livegroupbuy.backend.purchase.PurchaseStatus;
import com.livegroupbuy.backend.purchase.PurchaseTicket;
import com.livegroupbuy.backend.purchase.PurchaseTicketRepository;

@Service
public class TossPaymentService {

    private final TossPaymentOrderRepository paymentRepository;
    private final PurchaseTicketRepository ticketRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final PurchaseQueueService purchaseQueueService;
    private final RestClient tossClient;
    private final String secretKey;

    public TossPaymentService(TossPaymentOrderRepository paymentRepository,
                              PurchaseTicketRepository ticketRepository,
                              ProductInventoryRepository inventoryRepository,
                              PurchaseQueueService purchaseQueueService,
                              @Value("${payment.toss.base-url:https://api.tosspayments.com}") String baseUrl,
                              @Value("${payment.toss.secret-key:}") String secretKey) {
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseQueueService = purchaseQueueService;
        this.tossClient = RestClient.create(baseUrl);
        this.secretKey = secretKey;
    }

    @Transactional
    public TossPaymentPrepareResponse prepare(TossPaymentPrepareRequest request) {
        requireSecretKey();
        PurchaseTicket ticket = ticketRepository.findLockedById(request.ticketId())
                .orElseThrow(() -> new IllegalArgumentException("구매 대기열 요청을 찾을 수 없습니다."));
        if (!ticket.getMemberId().equals(request.memberId())) {
            throw new IllegalArgumentException("본인의 구매 요청만 결제할 수 있습니다.");
        }
        if (ticket.getStatus() != PurchaseStatus.RESERVED) {
            throw new IllegalStateException("결제 가능한 재고 예약 상태가 아닙니다.");
        }
        if (ticket.getExpiresAt() == null || !ticket.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("결제 시간이 만료되었습니다. 대기열 상태를 다시 확인해 주세요.");
        }

        TossPaymentOrder existing = paymentRepository.findByTicketId(ticket.getId()).orElse(null);
        if (existing != null) {
            existing.updateShippingInformation(
                    request.recipientName(), request.phone(), request.postalCode(),
                    request.address(), request.addressDetail(), request.deliveryMessage()
            );
            return TossPaymentPrepareResponse.from(existing);
        }

        ProductInventory inventory = inventoryRepository.findById(ticket.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));
        long amount = Math.multiplyExact((long) inventory.getUnitPrice(), ticket.getQuantity());
        String orderName = inventory.getProductTitle() + (ticket.getQuantity() > 1 ? " " + ticket.getQuantity() + "개" : "");
        if (orderName.length() > 100) orderName = orderName.substring(0, 100);

        TossPaymentOrder order = paymentRepository.save(new TossPaymentOrder(
                "order_" + UUID.randomUUID().toString().replace("-", ""),
                ticket.getId(), ticket.getMemberId(), amount, orderName,
                "customer_" + UUID.randomUUID().toString().replace("-", ""),
                request.recipientName(), request.phone(), request.postalCode(),
                request.address(), request.addressDetail(), request.deliveryMessage()
        ));
        return TossPaymentPrepareResponse.from(order);
    }

    @Transactional
    public PurchaseQueueResponse confirm(TossPaymentConfirmRequest request) {
        requireSecretKey();
        TossPaymentOrder order = paymentRepository.findLockedByOrderId(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("결제 주문 정보를 찾을 수 없습니다."));
        if (!order.getTicketId().equals(request.ticketId())) {
            throw new IllegalArgumentException("결제 주문과 대기열 요청이 일치하지 않습니다.");
        }
        if (order.getAmount() != request.amount()) {
            throw new IllegalArgumentException("결제 금액이 서버 주문 금액과 일치하지 않습니다.");
        }
        if (order.getStatus() == TossPaymentStatus.APPROVED) {
            return purchaseQueueService.status(order.getTicketId());
        }

        PurchaseTicket ticket = ticketRepository.findLockedById(order.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("구매 대기열 요청을 찾을 수 없습니다."));
        if (ticket.getStatus() != PurchaseStatus.RESERVED
                || ticket.getExpiresAt() == null
                || !ticket.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("재고 예약 시간이 만료되어 결제를 승인할 수 없습니다.");
        }

        TossConfirmApiResponse approved;
        try {
            approved = tossClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", request.paymentKey(),
                            "orderId", request.orderId(),
                            "amount", order.getAmount()
                    ))
                    .retrieve()
                    .body(TossConfirmApiResponse.class);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("토스 결제 승인에 실패했습니다. 결제 상태를 확인한 뒤 다시 시도해 주세요.");
        }

        if (approved == null
                || !order.getOrderId().equals(approved.orderId())
                || order.getAmount() != approved.totalAmount()
                || !"DONE".equals(approved.status())) {
            throw new IllegalStateException("토스 결제 승인 결과를 검증하지 못했습니다.");
        }

        PurchaseQueueResponse result = purchaseQueueService.confirmPaidReservation(
                order.getTicketId(), order.getMemberId()
        );
        if (result.status() != PurchaseStatus.CONFIRMED) {
            throw new IllegalStateException("결제는 승인됐지만 구매 재고를 확정하지 못했습니다. 관리자에게 문의해 주세요.");
        }
        order.approve(approved.paymentKey());
        return result;
    }

    private void requireSecretKey() {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("토스페이먼츠 시크릿 키가 설정되지 않았습니다.");
        }
    }
}
