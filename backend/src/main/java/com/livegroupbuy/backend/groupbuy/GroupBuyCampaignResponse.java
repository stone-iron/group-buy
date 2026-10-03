package com.livegroupbuy.backend.groupbuy;

public record GroupBuyCampaignResponse(
        Long productId,
        String productTitle,
        int goalQuantity,
        int joinedQuantity,
        int remainingQuantity,
        boolean goalReached,
        String status,
        String message
) {
    static GroupBuyCampaignResponse from(GroupBuyCampaign campaign, String message) {
        return new GroupBuyCampaignResponse(
                campaign.getProductId(), campaign.getProductTitle(), campaign.getGoalQuantity(),
                campaign.getJoinedQuantity(), campaign.remainingQuantity(), campaign.goalReached(),
                campaign.goalReached() ? "GOAL_REACHED" : "RECRUITING", message
        );
    }
}
