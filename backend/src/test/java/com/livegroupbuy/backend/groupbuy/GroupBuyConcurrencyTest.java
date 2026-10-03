package com.livegroupbuy.backend.groupbuy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group-buy;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "purchase.queue.process-delay-ms=3600000"
})
class GroupBuyConcurrencyTest {

    @Autowired
    private GroupBuyService service;

    @Autowired
    private GroupBuyCampaignRepository campaignRepository;

    @Test
    void oneHundredConcurrentParticipantsCannotExceedTheGoalOfTen() throws Exception {
        long productId = 2001L;
        campaignRepository.save(new GroupBuyCampaign(productId, "동시 공동구매 테스트", 10, 0));

        try (ExecutorService executor = Executors.newFixedThreadPool(20)) {
            List<Future<Boolean>> attempts = IntStream.range(0, 100)
                    .mapToObj(index -> executor.submit(() -> {
                        try {
                            service.join(productId, new GroupBuyJoinRequest(20_000L + index, 1));
                            return true;
                        } catch (IllegalArgumentException | IllegalStateException rejected) {
                            return false;
                        }
                    }))
                    .toList();

            int accepted = 0;
            for (Future<Boolean> attempt : attempts) if (attempt.get()) accepted++;
            assertThat(accepted).isEqualTo(10);
        }

        GroupBuyCampaign campaign = campaignRepository.findById(productId).orElseThrow();
        assertThat(campaign.getJoinedQuantity()).isEqualTo(10);
        assertThat(campaign.goalReached()).isTrue();
    }
}
