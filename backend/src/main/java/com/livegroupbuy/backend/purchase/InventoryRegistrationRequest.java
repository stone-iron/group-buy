package com.livegroupbuy.backend.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRegistrationRequest(
        @NotNull @Positive Long productId,
        @NotBlank String productTitle,
        @Positive int totalQuantity,
        @Positive int unitPrice
) {
}
