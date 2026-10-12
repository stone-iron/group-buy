package com.livegroupbuy.backend.groupbuy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.livegroupbuy.backend.member.Member;
import com.livegroupbuy.backend.member.MemberRepository;
import com.livegroupbuy.backend.member.MemberRole;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group-buy-query;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "purchase.queue.process-delay-ms=3600000"
})
class GroupBuyParticipationQueryTest {

    @Autowired
    private GroupBuyService service;

    @Autowired
    private GroupBuyCampaignRepository campaignRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void buyerAndSellerCanReadSavedParticipation() {
        Member buyer = memberRepository.save(new Member(
                "구매자", "participation-query@example.com", "password-hash", MemberRole.BUYER));
        long productId = 9_001L;
        campaignRepository.save(new GroupBuyCampaign(productId, "조회 테스트 공동구매", 10, 0));

        service.join(productId, new GroupBuyJoinRequest(buyer.getId(), 2));

        assertThat(service.participationsForMember(buyer.getId()))
                .singleElement()
                .satisfies(participation -> {
                    assertThat(participation.productId()).isEqualTo(productId);
                    assertThat(participation.productTitle()).isEqualTo("조회 테스트 공동구매");
                    assertThat(participation.quantity()).isEqualTo(2);
                    assertThat(participation.joinedQuantity()).isEqualTo(2);
                    assertThat(participation.status()).isEqualTo("RECRUITING");
                });

        assertThat(service.participationsForProduct(productId))
                .singleElement()
                .satisfies(participation -> {
                    assertThat(participation.memberId()).isEqualTo(buyer.getId());
                    assertThat(participation.memberName()).isEqualTo("구매자");
                    assertThat(participation.quantity()).isEqualTo(2);
                    assertThat(participation.participatedAt()).isNotNull();
                });
    }
}
