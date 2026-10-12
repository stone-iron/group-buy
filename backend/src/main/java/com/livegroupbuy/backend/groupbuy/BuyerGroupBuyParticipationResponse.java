package com.livegroupbuy.backend.groupbuy;

import java.time.LocalDateTime;

public record BuyerGroupBuyParticipationResponse(
        Long id,
        Long productId,
        String productTitle,
        int quantity,
        LocalDateTime participatedAt,
        int goalQuantity,
        int joinedQuantity,
        int remainingQuantity,
        boolean goalReached,
        String status
) {
    static BuyerGroupBuyParticipationResponse from(GroupBuyParticipation participation,
                                                     GroupBuyCampaign campaign) {
        return new BuyerGroupBuyParticipationResponse(
                participation.getId(),
                participation.getProductId(),
                campaign.getProductTitle(),
                participation.getQuantity(),
                participation.getCreatedAt(),
                campaign.getGoalQuantity(),
                campaign.getJoinedQuantity(),
                campaign.remainingQuantity(),
                campaign.goalReached(),
                campaign.goalReached() ? "GOAL_REACHED" : "RECRUITING"
        );
    }
}
