package com.livegroupbuy.backend.groupbuy;

import java.time.LocalDateTime;

public record SellerGroupBuyParticipationResponse(
        Long id,
        Long productId,
        Long memberId,
        String memberName,
        int quantity,
        LocalDateTime participatedAt
) {
    static SellerGroupBuyParticipationResponse from(GroupBuyParticipation participation,
                                                      String memberName) {
        return new SellerGroupBuyParticipationResponse(
                participation.getId(),
                participation.getProductId(),
                participation.getMemberId(),
                memberName,
                participation.getQuantity(),
                participation.getCreatedAt()
        );
    }
}
