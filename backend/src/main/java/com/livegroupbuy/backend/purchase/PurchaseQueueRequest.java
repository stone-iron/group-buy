package com.livegroupbuy.backend.purchase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseQueueRequest(
        @NotNull Long productId,
        @NotNull Long memberId,
        @Min(1) @Max(10) int quantity,
        @NotBlank @Size(max = 80) String requestKey
) {
}
