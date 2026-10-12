package com.livegroupbuy.backend.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 실제 MySQL(+Redis)에서 구매 대기열의 동시성을 검증하는 부하 테스트.
 *
 * 일반 빌드에서는 실행되지 않고, 아래처럼 명시적으로 켰을 때만 실행된다.
 *   ./mvnw test -Dtest=PurchaseQueueLoadTest -Dloadtest=true
 * 결과 요약은 콘솔과 target/loadtest-report.txt 에 저장된다.
 */
@EnabledIfSystemProperty(named = "loadtest", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "spring.datasource.url=${LOADTEST_DB_URL:jdbc:mysql://localhost:3306/live_group_buy_loadtest?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.hikari.maximum-pool-size=60",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "purchase.queue.process-delay-ms=3600000",
        "purchase.queue.reservation-seconds=600",
        "purchase.abuse.rate-limit.enabled=true"
})
class PurchaseQueueLoadTest {

    private static final List<String> REPORT = Collections.synchronizedList(new ArrayList<>());

    @Autowired private PurchaseQueueService service;
    @Autowired private ProductInventoryRepository inventoryRepository;
    @Autowired private PurchaseTicketRepository ticketRepository;
    @Autowired private PurchasePenaltyService penaltyService;
    @Autowired private PurchaseAbuseProfileRepository abuseProfileRepository;
    @Autowired private PurchaseRateLimitService rateLimitService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    // ------------------------------------------------------------------
    // 1. 1,000명 동시 요청 / 재고 100개
    // ------------------------------------------------------------------
    @Test
    @Order(1)
    void 동시_1000명이_재고_100개에_몰려도_초과예약없이_도착순서대로_확정된다() throws Exception {
        long productId = 9_001L;
        int stock = 100;
        int users = 1_000;
        inventoryRepository.save(new ProductInventory(productId, "부하 테스트 상품", stock, 0));

        // 요청이 들어오는 동안 스케줄러가 여러 서버에서 동시에 도는 상황을 흉내 낸다.
        Background background = Background.start(this, productId, stock, 4);

        long[] latencies = new long[users];
        Map<String, AtomicInteger> enqueueErrors = new ConcurrentHashMap<>();
        CountDownLatch ready = new CountDownLatch(users);
        CountDownLatch start = new CountDownLatch(1);
        long wallStart;
        long wallEnd;
        try (ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < users; i++) {
                int index = i;
                clients.submit(() -> {
                    ready.countDown();
                    start.await();
                    long t = System.nanoTime();
                    try {
                        service.enqueue(new PurchaseQueueRequest(productId, 100_000L + index, 1, "load-" + index));
                    } catch (RuntimeException e) {
                        count(enqueueErrors, e);
                    } finally {
                        latencies[index] = System.nanoTime() - t;
                    }
                    return null;
                });
            }
            ready.await();
            wallStart = System.nanoTime();
            start.countDown(); // 1,000개 요청이 동시에 출발
        }
        wallEnd = System.nanoTime();
        background.stop();

        for (int i = 0; i < 3; i++) service.processNext();

        List<PurchaseTicket> tickets = tickets(productId);
        Map<PurchaseStatus, Long> byStatus = byStatus(tickets);
        ProductInventory inventory = inventoryRepository.findById(productId).orElseThrow();

        Set<Long> firstArrivals = tickets.stream().limit(stock).map(PurchaseTicket::getId).collect(Collectors.toSet());
        Set<Long> reservedIds = tickets.stream()
                .filter(t -> t.getStatus() == PurchaseStatus.RESERVED)
                .map(PurchaseTicket::getId).collect(Collectors.toSet());
        long outOfOrder = reservedIds.stream().filter(id -> !firstArrivals.contains(id)).count();

        double wallMs = (wallEnd - wallStart) / 1_000_000.0;
        REPORT.add("[1] 동시 요청 " + users + "건 / 재고 " + stock + "개");
        REPORT.add("    티켓 발급: " + tickets.size() + "건, 발급 실패: " + total(enqueueErrors) + "건 " + enqueueErrors);
        REPORT.add(String.format("    전체 처리 시간: %.0f ms, 처리량: %.0f req/s", wallMs, users / (wallMs / 1000)));
        REPORT.add("    요청 응답 시간: " + percentiles(latencies));
        REPORT.add("    임시 예약(RESERVED): " + byStatus.getOrDefault(PurchaseStatus.RESERVED, 0L)
                + "건, 대기(WAITING): " + byStatus.getOrDefault(PurchaseStatus.WAITING, 0L) + "건");
        REPORT.add("    순서 역전(앞 100번 밖의 티켓이 예약된 경우): " + outOfOrder + "건");
        REPORT.add("    처리 중 관측된 최대 점유 수량(판매+예약): " + background.maxHeld.get()
                + " / 재고 " + stock + ", 초과 관측: " + background.oversell.get() + "회");
        REPORT.add("    스케줄러 동시 실행 중 예외: " + background.errors);

        assertThat(enqueueErrors).as("구매 요청 실패").isEmpty();
        assertThat(tickets).hasSize(users);
        assertThat(reservedIds).hasSize(stock);
        assertThat(outOfOrder).isZero();
        assertThat(background.oversell.get()).isZero();
        assertThat(inventory.getReservedQuantity()).isEqualTo(stock);
        assertThat(inventory.availableQuantity()).isZero();

        // --- 2단계: 예약자 100명이 동시에 결제 완료, 동시에 스케줄러도 돈다 ---
        Background paying = Background.start(this, productId, stock, 4);
        Map<String, AtomicInteger> confirmErrors = new ConcurrentHashMap<>();
        List<PurchaseTicket> retry = Collections.synchronizedList(new ArrayList<>());
        List<PurchaseTicket> reserved = tickets.stream().filter(t -> t.getStatus() == PurchaseStatus.RESERVED).toList();
        try (ExecutorService payers = Executors.newVirtualThreadPerTaskExecutor()) {
            for (PurchaseTicket ticket : reserved) {
                payers.submit(() -> {
                    try {
                        service.confirmPaidReservation(ticket.getId(), ticket.getMemberId());
                    } catch (RuntimeException e) {
                        count(confirmErrors, e);
                        retry.add(ticket);
                    }
                });
            }
        }
        paying.stop();
        for (PurchaseTicket ticket : retry) service.confirmPaidReservation(ticket.getId(), ticket.getMemberId());
        for (int i = 0; i < 3; i++) service.processNext();

        Map<PurchaseStatus, Long> finalStatus = byStatus(tickets(productId));
        inventory = inventoryRepository.findById(productId).orElseThrow();
        REPORT.add("    [결제 단계] 동시 결제 " + reserved.size() + "건 중 1차 실패: " + retry.size() + "건 " + confirmErrors
                + " → 재시도 후 모두 처리");
        REPORT.add("    [결제 단계] 스케줄러 동시 실행 중 예외: " + paying.errors
                + ", 최대 점유: " + paying.maxHeld.get() + ", 초과 관측: " + paying.oversell.get() + "회");
        REPORT.add("    최종: 구매 확정 " + finalStatus.getOrDefault(PurchaseStatus.CONFIRMED, 0L)
                + "건, 품절 안내 " + finalStatus.getOrDefault(PurchaseStatus.SOLD_OUT, 0L)
                + "건, 판매 수량 " + inventory.getSoldQuantity() + ", 남은 예약 " + inventory.getReservedQuantity());

        assertThat(paying.oversell.get()).isZero();
        assertThat(finalStatus.get(PurchaseStatus.CONFIRMED)).isEqualTo((long) stock);
        assertThat(finalStatus.get(PurchaseStatus.SOLD_OUT)).isEqualTo((long) (users - stock));
        assertThat(inventory.getSoldQuantity()).isEqualTo(stock);
        assertThat(inventory.getReservedQuantity()).isZero();
    }

    // ------------------------------------------------------------------
    // 2. 같은 requestKey 로 50번 동시 요청 (더블 클릭, 네트워크 재전송)
    // ------------------------------------------------------------------
    @Test
    @Order(2)
    void 같은_requestKey_동시_50회_요청은_티켓_하나로_처리된다() throws Exception {
        long productId = 9_002L;
        inventoryRepository.save(new ProductInventory(productId, "중복 요청 테스트 상품", 10, 0));
        Set<Long> ticketIds = ConcurrentHashMap.newKeySet();
        Map<String, AtomicInteger> errors = new ConcurrentHashMap<>();

        burst(50, i -> {
            try {
                ticketIds.add(service.enqueue(new PurchaseQueueRequest(productId, 200_001L, 1, "same-key")).ticketId());
            } catch (RuntimeException e) {
                count(errors, e);
            }
            return null;
        });

        int created = tickets(productId).size();
        REPORT.add("[2] 같은 requestKey 동시 50회 → 생성된 티켓 " + created + "건, 응답 티켓 ID 종류 "
                + ticketIds.size() + "개, 오류 " + errors);
        assertThat(created).isEqualTo(1);
        assertThat(ticketIds).as("모든 중복 요청이 같은 티켓을 돌려받아야 한다").hasSize(1);
        assertThat(errors).as("중복 요청 오류").isEmpty();
    }

    // ------------------------------------------------------------------
    // 3. 한 회원이 서로 다른 requestKey 로 50번 동시 요청 (매크로)
    // ------------------------------------------------------------------
    @Test
    @Order(3)
    void 한_회원의_동시_다중요청은_활성_티켓_하나만_만든다() throws Exception {
        long productId = 9_003L;
        inventoryRepository.save(new ProductInventory(productId, "매크로 테스트 상품", 10, 0));
        Map<String, AtomicInteger> errors = new ConcurrentHashMap<>();

        burst(50, i -> {
            try {
                service.enqueue(new PurchaseQueueRequest(productId, 300_001L, 1, "macro-" + i));
            } catch (RuntimeException e) {
                count(errors, e);
            }
            return null;
        });

        int created = tickets(productId).size();
        REPORT.add("[3] 한 회원이 다른 requestKey로 동시 50회 → 생성된 티켓 " + created + "건, 오류 " + errors);
        assertThat(created).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // 4. 미결제 만료 → 다음 순번으로 넘어가는지 (실제 MySQL)
    // ------------------------------------------------------------------
    @Test
    @Order(4)
    void 미결제_예약이_만료되면_다음_순번에게_넘어가고_패널티가_기록된다() throws Exception {
        long productId = 9_004L;
        int stock = 5;
        inventoryRepository.save(new ProductInventory(productId, "만료 테스트 상품", stock, 0));
        for (int i = 0; i < 10; i++) {
            service.enqueue(new PurchaseQueueRequest(productId, 400_000L + i, 1, "expire-" + i));
        }

        // 예약 유지 시간을 2초로 줄인 서비스로 스케줄러 동작을 재현
        PurchaseQueueService shortHold = new PurchaseQueueService(
                ticketRepository, inventoryRepository, penaltyService, transactionManager, 2, 3);

        shortHold.processNext();
        List<PurchaseTicket> before = tickets(productId);
        Thread.sleep(2_500);
        shortHold.processNext();
        List<PurchaseTicket> after = tickets(productId);

        long expiredFirstFive = after.subList(0, stock).stream().filter(t -> t.getStatus() == PurchaseStatus.EXPIRED).count();
        long reservedNextFive = after.subList(stock, 10).stream().filter(t -> t.getStatus() == PurchaseStatus.RESERVED).count();
        long penalized = after.subList(0, stock).stream()
                .filter(t -> abuseProfileRepository.findById(t.getMemberId())
                        .map(p -> p.getUnpaidExpirationCount() == 1).orElse(false))
                .count();
        ProductInventory inventory = inventoryRepository.findById(productId).orElseThrow();

        REPORT.add("[4] 재고 5개 / 대기 10명 → 1차 예약 " + byStatus(before).getOrDefault(PurchaseStatus.RESERVED, 0L)
                + "명, 2초 후 만료 " + expiredFirstFive + "명, 다음 순번 예약 " + reservedNextFive
                + "명, 미결제 기록 " + penalized + "명, 예약 수량 " + inventory.getReservedQuantity());
        assertThat(expiredFirstFive).isEqualTo(stock);
        assertThat(reservedNextFive).isEqualTo(stock);
        assertThat(penalized).isEqualTo(stock);
        assertThat(inventory.getReservedQuantity()).isEqualTo(stock);
    }

    // ------------------------------------------------------------------
    // 5. Redis 속도 제한 (실제 Redis)
    // ------------------------------------------------------------------
    @Test
    @Order(5)
    void Redis_속도제한은_회원_초당3회_IP_초당20회만_허용한다() throws Exception {
        boolean redisUp;
        try {
            redisUp = "PONG".equals(redisTemplate.execute((RedisCallback<String>) c -> c.ping()));
        } catch (RuntimeException e) {
            redisUp = false;
        }
        assumeTrue(redisUp, "Redis가 실행 중이 아니어서 속도 제한 테스트를 건너뜁니다.");
        Set<String> keys = redisTemplate.keys("purchase:rate:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);

        AtomicInteger memberAllowed = new AtomicInteger();
        burst(100, i -> {
            try {
                rateLimitService.assertRequestAllowed(500_001L, "198.51.100." + i);
                memberAllowed.incrementAndGet();
            } catch (PurchaseAccessDeniedException ignored) {
            }
            return null;
        });

        AtomicInteger ipAllowed = new AtomicInteger();
        burst(100, i -> {
            try {
                rateLimitService.assertRequestAllowed(600_000L + i, "203.0.113.7");
                ipAllowed.incrementAndGet();
            } catch (PurchaseAccessDeniedException ignored) {
            }
            return null;
        });

        REPORT.add("[5] 같은 회원 동시 100회 → 허용 " + memberAllowed.get() + " / 차단 " + (100 - memberAllowed.get())
                + ", 같은 IP 동시 100회 → 허용 " + ipAllowed.get() + " / 차단 " + (100 - ipAllowed.get()));
        assertThat(memberAllowed.get()).isEqualTo(3);
        assertThat(ipAllowed.get()).isEqualTo(20);
    }

    @AfterAll
    static void writeReport() throws Exception {
        String text = String.join(System.lineSeparator(), REPORT);
        System.out.println(System.lineSeparator() + "===== 구매 대기열 부하 테스트 결과 =====" + System.lineSeparator() + text);
        Path out = Path.of("target", "loadtest-report.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, text + System.lineSeparator());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private List<PurchaseTicket> tickets(long productId) {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getProductId().equals(productId))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
    }

    private static Map<PurchaseStatus, Long> byStatus(List<PurchaseTicket> tickets) {
        return tickets.stream().collect(Collectors.groupingBy(PurchaseTicket::getStatus, Collectors.counting()));
    }

    private static void burst(int n, Function<Integer, Void> task) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                int index = i;
                ex.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.apply(index);
                });
            }
            ready.await();
            start.countDown();
        }
    }

    private static void count(Map<String, AtomicInteger> counter, Throwable e) {
        counter.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
    }

    private static int total(Map<String, AtomicInteger> counter) {
        return counter.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    private static String percentiles(long[] nanos) {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        Function<Double, Double> p = q -> sorted[Math.min(sorted.length - 1, (int) Math.ceil(q * sorted.length) - 1)] / 1_000_000.0;
        return String.format("p50 %.1f ms, p95 %.1f ms, p99 %.1f ms, max %.1f ms",
                p.apply(0.50), p.apply(0.95), p.apply(0.99), sorted[sorted.length - 1] / 1_000_000.0);
    }

    /** 스케줄러 여러 대 + 재고 감시 스레드. */
    private static final class Background {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger maxHeld = new AtomicInteger();
        final AtomicInteger oversell = new AtomicInteger();
        final Map<String, AtomicInteger> errors = new ConcurrentHashMap<>();
        ExecutorService pool;

        static Background start(PurchaseQueueLoadTest test, long productId, int stock, int schedulers) {
            Background b = new Background();
            b.pool = Executors.newFixedThreadPool(schedulers + 1);
            b.pool.submit(() -> {
                while (b.running.get()) {
                    Map<String, Object> row = test.jdbc.queryForMap(
                            "select sold_quantity, reserved_quantity from product_inventories where product_id = ?",
                            productId);
                    int held = ((Number) row.get("sold_quantity")).intValue()
                            + ((Number) row.get("reserved_quantity")).intValue();
                    b.maxHeld.accumulateAndGet(held, Math::max);
                    if (held > stock) b.oversell.incrementAndGet();
                    Thread.sleep(5);
                }
                return null;
            });
            for (int i = 0; i < schedulers; i++) {
                b.pool.submit(() -> {
                    while (b.running.get()) {
                        try {
                            test.service.processNext();
                        } catch (RuntimeException e) {
                            count(b.errors, e);
                        }
                        Thread.sleep(20);
                    }
                    return null;
                });
            }
            return b;
        }

        void stop() throws InterruptedException {
            running.set(false);
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }

        @Override
        public String toString() {
            return new TreeMap<>(errors).toString();
        }
    }
}
