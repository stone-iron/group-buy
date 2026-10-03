package com.livegroupbuy.backend.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseCancelRequest(@NotNull @Positive Long memberId) {
}
