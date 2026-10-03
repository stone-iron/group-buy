package com.livegroupbuy.backend.groupbuy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupBuyJoinRequest(
        @NotNull @Positive Long memberId,
        @Positive @Max(3) int quantity
) {
}
