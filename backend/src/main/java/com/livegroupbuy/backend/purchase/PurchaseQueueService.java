package com.livegroupbuy.backend.purchase;

import java.util.List;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseQueueService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseQueueService.class);

    private static final List<PurchaseStatus> ACTIVE_STATUSES =
            List.of(PurchaseStatus.WAITING, PurchaseStatus.PROCESSING);
    private static final List<PurchaseStatus> MEMBER_LIMIT_STATUSES =
            List.of(PurchaseStatus.WAITING, PurchaseStatus.PROCESSING, PurchaseStatus.RESERVED, PurchaseStatus.CONFIRMED);
    private static final List<PurchaseStatus> MEMBER_ACTIVE_STATUSES =
            List.of(PurchaseStatus.WAITING, PurchaseStatus.PROCESSING, PurchaseStatus.RESERVED);

    private final PurchaseTicketRepository ticketRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final PurchasePenaltyService penaltyService;
    private final long reservationSeconds;
    private final int maxPerMember;

    public PurchaseQueueService(PurchaseTicketRepository ticketRepository,
                                ProductInventoryRepository inventoryRepository,
                                PurchasePenaltyService penaltyService,
                                @Value("${purchase.queue.reservation-seconds:300}") long reservationSeconds,
                                @Value("${purchase.queue.max-per-member:3}") int maxPerMember) {
        this.ticketRepository = ticketRepository;
        this.inventoryRepository = inventoryRepository;
        this.penaltyService = penaltyService;
        this.reservationSeconds = reservationSeconds;
        this.maxPerMember = maxPerMember;
    }

    @Transactional
    public PurchaseQueueResponse enqueue(PurchaseQueueRequest request) {
        penaltyService.assertParticipationAllowed(request.memberId());
        PurchaseTicket existing = ticketRepository.findByRequestKey(request.requestKey()).orElse(null);
        if (existing != null) return response(existing);

        inventoryRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("구매할 수 없는 상품입니다."));

        PurchaseTicket activeTicket = ticketRepository
                .findFirstByProductIdAndMemberIdAndStatusInOrderByIdDesc(
                        request.productId(), request.memberId(), MEMBER_ACTIVE_STATUSES
                )
                .orElse(null);
        if (activeTicket != null) return response(activeTicket);

        long alreadyRequested = ticketRepository.sumQuantityByProductAndMemberAndStatuses(
                request.productId(), request.memberId(), MEMBER_LIMIT_STATUSES
        );
        if (alreadyRequested + request.quantity() > maxPerMember) {
            int availableForMember = Math.max(0, maxPerMember - (int) alreadyRequested);
            throw new IllegalArgumentException("이 상품은 1인당 최대 " + maxPerMember
                    + "개까지 구매할 수 있습니다. 추가 구매 가능 수량은 " + availableForMember + "개입니다.");
        }

        try {
            PurchaseTicket ticket = ticketRepository.saveAndFlush(new PurchaseTicket(
                    request.productId(), request.memberId(), request.quantity(), request.requestKey()
            ));
            return response(ticket);
        } catch (DataIntegrityViolationException duplicateRequest) {
            return ticketRepository.findByRequestKey(request.requestKey())
                    .map(this::response)
                    .orElseThrow(() -> duplicateRequest);
        }
    }

    @Transactional(readOnly = true)
    public PurchaseQueueResponse status(Long ticketId) {
        return response(ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 요청을 찾을 수 없습니다.")));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> inventories() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional
    public InventoryResponse registerInventory(InventoryRegistrationRequest request) {
        ProductInventory existing = inventoryRepository.findByIdForUpdate(request.productId()).orElse(null);
        if (existing != null) {
            existing.updateProductInfo(request.productTitle(), request.unitPrice());
            return InventoryResponse.from(existing);
        }
        return InventoryResponse.from(inventoryRepository.save(new ProductInventory(
                request.productId(), request.productTitle(), request.totalQuantity(), 0, request.unitPrice()
        )));
    }

    @Transactional
    public PurchaseQueueResponse confirmPaidReservation(Long ticketId, Long memberId) {
        PurchaseTicket ticket = ticketRepository.findLockedById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 요청을 찾을 수 없습니다."));
        if (!ticket.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 구매 요청만 결제할 수 있습니다.");
        }
        if (ticket.getStatus() == PurchaseStatus.CONFIRMED) return response(ticket);
        if (ticket.getStatus() != PurchaseStatus.RESERVED) {
            throw new IllegalStateException("결제 가능한 재고 예약 상태가 아닙니다.");
        }

        ProductInventory inventory = inventoryRepository.findByIdForUpdate(ticket.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품 재고를 찾을 수 없습니다."));
        if (ticket.getExpiresAt() == null || !ticket.getExpiresAt().isAfter(LocalDateTime.now())) {
            inventory.releaseReservation(ticket.getQuantity());
            ticket.expire();
            recordUnpaidExpirationSafely(ticket.getMemberId());
            return response(ticket);
        }

        inventory.confirmReservation(ticket.getQuantity());
        ticket.confirm();
        return response(ticket);
    }

    @Transactional
    public PurchaseQueueResponse cancel(Long ticketId, PurchaseCancelRequest request) {
        PurchaseTicket ticket = ticketRepository.findLockedById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 요청을 찾을 수 없습니다."));
        if (!ticket.getMemberId().equals(request.memberId())) {
            throw new IllegalArgumentException("본인의 구매 요청만 취소할 수 있습니다.");
        }
        if (ticket.getStatus() == PurchaseStatus.CANCELLED) return response(ticket);
        if (ticket.getStatus() == PurchaseStatus.CONFIRMED) {
            throw new IllegalStateException("이미 결제가 완료된 구매는 대기열에서 취소할 수 없습니다.");
        }
        if (ticket.getStatus() == PurchaseStatus.EXPIRED || ticket.getStatus() == PurchaseStatus.SOLD_OUT) {
            return response(ticket);
        }
        if (ticket.getStatus() != PurchaseStatus.WAITING
                && ticket.getStatus() != PurchaseStatus.PROCESSING
                && ticket.getStatus() != PurchaseStatus.RESERVED) {
            throw new IllegalStateException("현재 상태에서는 구매 요청을 취소할 수 없습니다.");
        }

        if (ticket.getStatus() == PurchaseStatus.RESERVED) {
            ProductInventory inventory = inventoryRepository.findByIdForUpdate(ticket.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 재고를 찾을 수 없습니다."));
            inventory.releaseReservation(ticket.getQuantity());
        }
        ticket.cancel();
        return response(ticket);
    }

    @Scheduled(fixedDelayString = "${purchase.queue.process-delay-ms:450}")
    @Transactional
    public void processNext() {
        expireReservations();
        for (Long productId : inventoryRepository.findAllProductIds()) {
            ProductInventory inventory = inventoryRepository.findByIdForUpdate(productId).orElse(null);
            if (inventory == null) continue;

            while (true) {
                PurchaseTicket ticket = ticketRepository
                        .findFirstByProductIdAndStatusOrderByIdAsc(productId, PurchaseStatus.WAITING)
                        .orElse(null);
                if (ticket == null || ticket.getStatus() != PurchaseStatus.WAITING) break;

                ticket.startProcessing();
                if (inventory.remainingQuantity() < ticket.getQuantity()) {
                    ticket.rejectAsSoldOut();
                    continue;
                }
                if (inventory.reserve(ticket.getQuantity())) {
                    ticket.reserveUntil(LocalDateTime.now().plusSeconds(reservationSeconds));
                    continue;
                }

                // 남은 재고는 있지만 앞선 구매자들이 결제 중이면 현재 순번을 그대로 유지한다.
                ticket.returnToWaiting();
                break;
            }
        }
    }

    private void expireReservations() {
        List<PurchaseTicket> expired = ticketRepository.findAllByStatusAndExpiresAtBeforeOrderByIdAsc(
                PurchaseStatus.RESERVED, LocalDateTime.now()
        );
        for (PurchaseTicket ticket : expired) {
            ProductInventory inventory = inventoryRepository.findByIdForUpdate(ticket.getProductId()).orElse(null);
            if (inventory != null) inventory.releaseReservation(ticket.getQuantity());
            ticket.expire();
            recordUnpaidExpirationSafely(ticket.getMemberId());
        }
    }

    private void recordUnpaidExpirationSafely(Long memberId) {
        try {
            penaltyService.recordUnpaidExpiration(memberId);
        } catch (RuntimeException exception) {
            log.error("Failed to record unpaid reservation expiration for member {}", memberId, exception);
        }
    }

    private PurchaseQueueResponse response(PurchaseTicket ticket) {
        ProductInventory inventory = inventoryRepository.findById(ticket.getProductId()).orElse(null);
        long waitingAhead = ticket.getStatus() == PurchaseStatus.WAITING
                ? ticketRepository.countByStatusInAndProductIdAndIdLessThan(
                        ACTIVE_STATUSES, ticket.getProductId(), ticket.getId())
                : 0;
        long position = ticket.getStatus() == PurchaseStatus.WAITING ? waitingAhead + 1 : 0;
        int total = inventory == null ? 0 : inventory.getTotalQuantity();
        int sold = inventory == null ? 0 : inventory.getSoldQuantity();
        int reserved = inventory == null ? 0 : inventory.getReservedQuantity();
        int available = inventory == null ? 0 : inventory.availableQuantity();
        int memberRequested = (int) ticketRepository.sumQuantityByProductAndMemberAndStatuses(
                ticket.getProductId(), ticket.getMemberId(), MEMBER_LIMIT_STATUSES
        );
        int memberRemainingLimit = Math.max(0, maxPerMember - memberRequested);
        String message = switch (ticket.getStatus()) {
            case WAITING -> "구매 대기열에 접수되었습니다.";
            case PROCESSING -> "구매 가능 수량을 확인하고 있습니다.";
            case RESERVED -> "구매 수량을 임시 확보했습니다. 제한 시간 안에 결제해 주세요.";
            case CONFIRMED -> "결제가 완료되어 구매가 확정되었습니다.";
            case EXPIRED -> "결제 시간이 만료되어 다음 순번에게 기회가 넘어갔습니다.";
            case CANCELLED -> "구매 요청을 취소했습니다. 확보된 수량은 다음 순번에게 넘어갑니다.";
            case SOLD_OUT -> "앞선 요청으로 준비된 수량이 모두 소진되었습니다.";
        };
        return new PurchaseQueueResponse(ticket.getId(), ticket.getProductId(), ticket.getQuantity(),
                ticket.getStatus(), waitingAhead, position, total, sold, reserved, available,
                maxPerMember, memberRequested, memberRemainingLimit, ticket.getExpiresAt(), message);
    }
}
