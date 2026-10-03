package com.livegroupbuy.backend.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:purchase-queue;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "purchase.queue.process-delay-ms=3600000",
        "purchase.queue.reservation-seconds=3"
})
class PurchaseQueueConcurrencyTest {

    @Autowired
    private PurchaseQueueService service;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    @Autowired
    private PurchaseTicketRepository ticketRepository;

    @Autowired
    private PurchasePenaltyService penaltyService;

    @Autowired
    private PurchaseAbuseProfileRepository abuseProfileRepository;

    @Test
    void confirmsOnlyTheLimitedQuantityWhenManyRequestsArriveTogether() throws Exception {
        long productId = 999L;
        inventoryRepository.save(new ProductInventory(productId, "동시 구매 테스트 상품", 5, 0));

        try (ExecutorService executor = Executors.newFixedThreadPool(12)) {
            List<Future<PurchaseQueueResponse>> enqueues = IntStream.range(0, 20)
                    .mapToObj(index -> executor.submit(() -> service.enqueue(new PurchaseQueueRequest(
                            productId, (long) index + 1, 1, UUID.randomUUID().toString()
                    ))))
                    .toList();
            for (Future<PurchaseQueueResponse> enqueue : enqueues) enqueue.get();

            List<? extends Future<?>> processors = IntStream.range(0, 80)
                    .mapToObj(index -> executor.submit(service::processNext))
                    .toList();
            for (Future<?> processor : processors) processor.get();

            List<PurchaseTicket> reservedTickets = ticketRepository.findAll().stream()
                    .filter(ticket -> ticket.getProductId().equals(productId))
                    .filter(ticket -> ticket.getStatus() == PurchaseStatus.RESERVED)
                    .toList();
            assertThat(reservedTickets).hasSize(5);

            List<Future<PurchaseQueueResponse>> payments = reservedTickets.stream()
                    .map(ticket -> executor.submit(() -> service.confirmPaidReservation(
                            ticket.getId(), ticket.getMemberId()
                    )))
                    .toList();
            for (Future<PurchaseQueueResponse> payment : payments) payment.get();
        }

        for (int index = 0; index < 20; index++) service.processNext();

        List<PurchaseTicket> tickets = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getProductId().equals(productId))
                .toList();
        long confirmed = tickets.stream().filter(ticket -> ticket.getStatus() == PurchaseStatus.CONFIRMED).count();
        long soldOut = tickets.stream().filter(ticket -> ticket.getStatus() == PurchaseStatus.SOLD_OUT).count();
        ProductInventory inventory = inventoryRepository.findById(productId).orElseThrow();

        assertThat(tickets).hasSize(20);
        assertThat(confirmed).isEqualTo(5);
        assertThat(soldOut).isEqualTo(15);
        assertThat(inventory.getSoldQuantity()).isEqualTo(5);
        assertThat(inventory.remainingQuantity()).isZero();
    }

    @Test
    void expiredReservationIsReturnedAndPassedToTheNextTicket() throws Exception {
        long productId = 1000L;
        inventoryRepository.save(new ProductInventory(productId, "예약 만료 테스트 상품", 1, 0));
        PurchaseQueueResponse first = service.enqueue(new PurchaseQueueRequest(
                productId, 101L, 1, UUID.randomUUID().toString()
        ));
        PurchaseQueueResponse second = service.enqueue(new PurchaseQueueRequest(
                productId, 102L, 1, UUID.randomUUID().toString()
        ));

        service.processNext();
        assertThat(service.status(first.ticketId()).status()).isEqualTo(PurchaseStatus.RESERVED);
        assertThat(service.status(second.ticketId()).status()).isEqualTo(PurchaseStatus.WAITING);

        Thread.sleep(3_100);
        service.processNext();

        assertThat(service.status(first.ticketId()).status()).isEqualTo(PurchaseStatus.EXPIRED);
        assertThat(service.status(second.ticketId()).status()).isEqualTo(PurchaseStatus.RESERVED);
        ProductInventory inventory = inventoryRepository.findById(productId).orElseThrow();
        assertThat(inventory.getSoldQuantity()).isZero();
        assertThat(inventory.getReservedQuantity()).isEqualTo(1);
        assertThat(inventory.availableQuantity()).isZero();
    }

    @Test
    void repeatedClicksReturnTheExistingTicketAndCumulativeQuantityIsLimited() {
        long productId = 1001L;
        long memberId = 501L;
        inventoryRepository.save(new ProductInventory(productId, "1인 구매 제한 테스트 상품", 20, 0));

        PurchaseQueueResponse first = service.enqueue(new PurchaseQueueRequest(
                productId, memberId, 2, UUID.randomUUID().toString()
        ));
        PurchaseQueueResponse repeatedClick = service.enqueue(new PurchaseQueueRequest(
                productId, memberId, 1, UUID.randomUUID().toString()
        ));

        assertThat(repeatedClick.ticketId()).isEqualTo(first.ticketId());
        assertThat(ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getProductId().equals(productId))
                .toList())
                .hasSize(1);

        service.processNext();
        service.confirmPaidReservation(first.ticketId(), memberId);

        assertThatThrownBy(() -> service.enqueue(new PurchaseQueueRequest(
                productId, memberId, 2, UUID.randomUUID().toString()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1인당 최대 3개")
                .hasMessageContaining("추가 구매 가능 수량은 1개");
    }

    @Test
    void cancelledReservationIsReleasedToTheNextCustomer() {
        long productId = 1002L;
        inventoryRepository.save(new ProductInventory(productId, "구매 취소 테스트 상품", 1, 0));
        PurchaseQueueResponse first = service.enqueue(new PurchaseQueueRequest(
                productId, 601L, 1, UUID.randomUUID().toString()
        ));
        PurchaseQueueResponse second = service.enqueue(new PurchaseQueueRequest(
                productId, 602L, 1, UUID.randomUUID().toString()
        ));

        service.processNext();
        PurchaseQueueResponse cancelled = service.cancel(first.ticketId(), new PurchaseCancelRequest(601L));

        assertThat(cancelled.status()).isEqualTo(PurchaseStatus.CANCELLED);
        assertThat(inventoryRepository.findById(productId).orElseThrow().getReservedQuantity()).isZero();

        service.processNext();

        assertThat(service.status(second.ticketId()).status()).isEqualTo(PurchaseStatus.RESERVED);
        assertThat(inventoryRepository.findById(productId).orElseThrow().getReservedQuantity()).isEqualTo(1);
        assertThat(abuseProfileRepository.findById(601L)).isEmpty();
    }

    @Test
    void blocksMemberAfterThreeUnpaidReservationExpirations() {
        long productId = 1004L;
        long memberId = 701L;
        inventoryRepository.save(new ProductInventory(productId, "미결제 패널티 테스트 상품", 10, 0));

        penaltyService.recordUnpaidExpiration(memberId);
        penaltyService.recordUnpaidExpiration(memberId);
        penaltyService.recordUnpaidExpiration(memberId);

        assertThatThrownBy(() -> service.enqueue(new PurchaseQueueRequest(
                productId, memberId, 1, UUID.randomUUID().toString()
        )))
                .isInstanceOf(PurchaseAccessDeniedException.class)
                .hasMessageContaining("미결제 예약이 반복");
        assertThat(abuseProfileRepository.findById(memberId).orElseThrow().getUnpaidExpirationCount())
                .isEqualTo(3);
    }

    @Test
    void onlyTheFirstTenOfOneHundredCustomersReceiveCheckoutAccess() {
        long productId = 1003L;
        inventoryRepository.save(new ProductInventory(productId, "100명 선착순 테스트 상품", 10, 0));

        List<PurchaseQueueResponse> requests = IntStream.range(0, 100)
                .mapToObj(index -> service.enqueue(new PurchaseQueueRequest(
                        productId, 10_000L + index, 1, UUID.randomUUID().toString()
                )))
                .toList();

        service.processNext();

        List<PurchaseTicket> orderedTickets = ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getProductId().equals(productId))
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList();
        assertThat(orderedTickets.subList(0, 10))
                .allMatch(ticket -> ticket.getStatus() == PurchaseStatus.RESERVED);
        assertThat(orderedTickets.subList(10, 100))
                .allMatch(ticket -> ticket.getStatus() == PurchaseStatus.WAITING);
        assertThat(requests.get(10).ticketId()).isEqualTo(orderedTickets.get(10).getId());
        assertThat(inventoryRepository.findById(productId).orElseThrow().getReservedQuantity()).isEqualTo(10);

        service.cancel(orderedTickets.getFirst().getId(), new PurchaseCancelRequest(10_000L));
        service.processNext();

        assertThat(service.status(orderedTickets.get(10).getId()).status()).isEqualTo(PurchaseStatus.RESERVED);
        assertThat(inventoryRepository.findById(productId).orElseThrow().getReservedQuantity()).isEqualTo(10);
    }
}
