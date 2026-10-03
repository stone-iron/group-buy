package com.livegroupbuy.backend.groupbuy;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupBuyService {

    private final GroupBuyCampaignRepository campaignRepository;
    private final GroupBuyParticipationRepository participationRepository;

    public GroupBuyService(GroupBuyCampaignRepository campaignRepository,
                           GroupBuyParticipationRepository participationRepository) {
        this.campaignRepository = campaignRepository;
        this.participationRepository = participationRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupBuyCampaignResponse> campaigns() {
        return campaignRepository.findAll().stream()
                .map(campaign -> GroupBuyCampaignResponse.from(campaign, statusMessage(campaign)))
                .toList();
    }

    @Transactional
    public GroupBuyCampaignResponse register(GroupBuyCampaignRequest request) {
        GroupBuyCampaign campaign = campaignRepository.findByIdForUpdate(request.productId()).orElse(null);
        if (campaign == null) {
            campaign = new GroupBuyCampaign(request.productId(), request.productTitle(), request.goalQuantity(), 0);
        } else {
            campaign.updateInfo(request.productTitle(), request.goalQuantity());
        }
        return GroupBuyCampaignResponse.from(campaignRepository.save(campaign), statusMessage(campaign));
    }

    @Transactional
    public GroupBuyCampaignResponse join(Long productId, GroupBuyJoinRequest request) {
        GroupBuyCampaign campaign = campaignRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("참여할 수 없는 공동구매 상품입니다."));
        if (campaign.goalReached()) {
            throw new IllegalStateException("이미 목표 수량을 달성한 공동구매입니다.");
        }
        if (participationRepository.findByProductIdAndMemberId(productId, request.memberId()).isPresent()) {
            throw new IllegalStateException("이미 참여한 공동구매입니다.");
        }

        campaign.join(request.quantity());
        participationRepository.save(new GroupBuyParticipation(productId, request.memberId(), request.quantity()));
        String message = campaign.goalReached()
                ? "목표 수량을 달성했습니다. 참여자에게 결제 일정을 안내합니다."
                : "공동구매 참여 신청이 완료되었습니다. 목표 달성 전에는 결제되지 않습니다.";
        return GroupBuyCampaignResponse.from(campaign, message);
    }

    private static String statusMessage(GroupBuyCampaign campaign) {
        return campaign.goalReached()
                ? "목표 달성 · 결제 안내 예정"
                : "참여 모집 중 · 아직 결제되지 않음";
    }
}
