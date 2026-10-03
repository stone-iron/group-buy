package com.livegroupbuy.backend.groupbuy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupBuyCampaignRequest(
        @NotNull @Positive Long productId,
        @NotBlank String productTitle,
        @Positive int goalQuantity
) {
}
