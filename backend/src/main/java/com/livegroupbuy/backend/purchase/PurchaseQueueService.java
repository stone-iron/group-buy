package com.livegroupbuy.backend.purchase;

import java.util.List;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 한정 수량 구매 대기열.
 *
 * <p><b>잠금 규칙</b></p>
 * <ul>
 *   <li>재고를 바꾸는 트랜잭션은 READ COMMITTED로 실행한다. MySQL 기본값인 REPEATABLE READ에서는
 *       첫 조회 시점의 스냅샷을 계속 읽기 때문에, 재고 잠금을 기다리는 동안 다른 요청이 만든 티켓을 보지 못해
 *       1인 요청 제한이 우회된다. 또 범위 잠금(gap lock)이 INSERT를 막아 데드락이 생긴다.</li>
 *   <li>중복·한도 확인은 반드시 재고 잠금을 얻은 <i>뒤에</i> 한다. 같은 상품의 요청은 재고 잠금 앞에서 줄을 서므로
 *       뒤 요청은 앞 요청이 커밋한 티켓을 확인할 수 있다.</li>
 *   <li>대기열 처리는 상품마다 별도 트랜잭션으로 실행해, 한 번에 하나의 재고 잠금만 보유한다.</li>
 * </ul>
 */
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
    private final TransactionTemplate productTransaction;
    private final long reservationSeconds;
    private final int maxPerMember;

    public PurchaseQueueService(PurchaseTicketRepository ticketRepository,
                                ProductInventoryRepository inventoryRepository,
                                PurchasePenaltyService penaltyService,
                                PlatformTransactionManager transactionManager,
                                @Value("${purchase.queue.reservation-seconds:300}") long reservationSeconds,
                                @Value("${purchase.queue.max-per-member:3}") int maxPerMember) {
        this.ticketRepository = ticketRepository;
        this.inventoryRepository = inventoryRepository;
        this.penaltyService = penaltyService;
        this.productTransaction = new TransactionTemplate(transactionManager);
        this.productTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.reservationSeconds = reservationSeconds;
        this.maxPerMember = maxPerMember;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PurchaseQueueResponse enqueue(PurchaseQueueRequest request) {
        penaltyService.assertParticipationAllowed(request.memberId());
        // 이미 처리된 재요청은 잠금 없이 바로 돌려준다.
        PurchaseTicket existing = ticketRepository.findByRequestKey(request.requestKey()).orElse(null);
        if (existing != null) return response(existing);

        inventoryRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("구매할 수 없는 상품입니다."));

        // 잠금을 기다리는 동안 같은 requestKey의 앞선 요청이 티켓을 만들었을 수 있다.
        existing = ticketRepository.findByRequestKey(request.requestKey()).orElse(null);
        if (existing != null) return response(existing);

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
            // 같은 상품의 중복 요청은 위에서 걸러지므로, 여기 오는 것은 다른 상품에 이미 쓰인 requestKey뿐이다.
            // INSERT가 실패한 Hibernate 세션은 다시 쓸 수 없으므로 조회하지 않고 바로 거절한다.
            throw new IllegalArgumentException("이미 다른 구매 요청에 사용된 요청 키입니다.", duplicateRequest);
        }
    }

    @Transactional(readOnly = true)
    public PurchaseQueueResponse status(Long ticketId) {
        return response(ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 요청을 찾을 수 없습니다.")));
    }

    @Transactional(readOnly = true)
    public PurchaseQueueResponse statusForMember(Long ticketId, Long memberId) {
        PurchaseTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("대기열 요청을 찾을 수 없습니다."));
        if (!ticket.getMemberId().equals(memberId)) {
            // 다른 회원의 티켓 존재 여부도 드러나지 않도록 같은 메시지를 쓴다.
            throw new IllegalArgumentException("대기열 요청을 찾을 수 없습니다.");
        }
        return response(ticket);
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

    /** 잠금 순서: 티켓 → 재고 (취소·만료 처리와 같은 순서) */
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    /** 잠금 순서: 티켓 → 재고 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    /**
     * 상품마다 별도 트랜잭션으로 만료 처리와 순번 배정을 한다.
     * 한 트랜잭션이 여러 상품의 재고 잠금을 동시에 쥐지 않으므로, 결제·취소와 잠금 순서가 엇갈리지 않는다.
     * 한 상품에서 오류가 나도 다른 상품은 계속 처리하고, 오류는 마지막에 다시 던져 기록되게 한다.
     */
    @Scheduled(fixedDelayString = "${purchase.queue.process-delay-ms:450}")
    public void processNext() {
        RuntimeException firstFailure = null;
        for (Long productId : inventoryRepository.findAllProductIds()) {
            try {
                productTransaction.executeWithoutResult(status -> processProduct(productId));
            } catch (RuntimeException exception) {
                log.warn("Failed to process purchase queue for product {}: {}", productId, exception.getMessage());
                if (firstFailure == null) firstFailure = exception;
            }
        }
        if (firstFailure != null) throw firstFailure;
    }

    /** 잠금 순서: 만료된 티켓 → 재고 → 대기 티켓 */
    private void processProduct(Long productId) {
        List<PurchaseTicket> expired = ticketRepository.findAllByProductIdAndStatusAndExpiresAtBeforeOrderByIdAsc(
                productId, PurchaseStatus.RESERVED, LocalDateTime.now()
        );
        ProductInventory inventory = inventoryRepository.findByIdForUpdate(productId).orElse(null);
        if (inventory == null) return;

        for (PurchaseTicket ticket : expired) {
            inventory.releaseReservation(ticket.getQuantity());
            ticket.expire();
            recordUnpaidExpirationSafely(ticket.getMemberId());
        }

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
