package com.livegroupbuy.backend.groupbuy;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class GroupBuyCampaignInitializer implements CommandLineRunner {

    private final GroupBuyCampaignRepository repository;

    public GroupBuyCampaignInitializer(GroupBuyCampaignRepository repository) { this.repository = repository; }

    @Override
    public void run(String... args) {
        List<GroupBuyCampaign> defaults = List.of(
                new GroupBuyCampaign(1L, "제주 노지 감귤 5kg 산지직송", 40, 34),
                new GroupBuyCampaign(2L, "1++ 한우 불고기·국거리 세트", 80, 72),
                new GroupBuyCampaign(3L, "저온숙성 국산 참기름 2병", 30, 18),
                new GroupBuyCampaign(5L, "2026년 햅쌀 신동진 10kg", 70, 56),
                new GroupBuyCampaign(6L, "고당도 가정용 홍로 사과 3kg", 40, 29),
                new GroupBuyCampaign(7L, "스페셜티 원두 3종 골라담기 600g", 35, 21),
                new GroupBuyCampaign(9L, "매일견과 프리미엄 30봉 세트", 50, 38),
                new GroupBuyCampaign(12L, "프랑스 버터 크루아상 생지 20개", 30, 26)
        );
        defaults.stream().filter(campaign -> !repository.existsById(campaign.getProductId())).forEach(repository::save);
    }
}
