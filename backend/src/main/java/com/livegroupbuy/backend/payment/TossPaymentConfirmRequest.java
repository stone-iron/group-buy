package com.livegroupbuy.backend.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TossPaymentConfirmRequest(
        @NotNull @Positive Long ticketId,
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @Positive long amount
) {
}
