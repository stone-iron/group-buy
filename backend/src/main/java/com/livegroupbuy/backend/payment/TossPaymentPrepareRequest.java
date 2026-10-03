package com.livegroupbuy.backend.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TossPaymentPrepareRequest(
        @NotNull @Positive Long ticketId,
        @NotNull @Positive Long memberId,
        @NotBlank @Size(max = 50) String recipientName,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 10) String postalCode,
        @NotBlank @Size(max = 200) String address,
        @Size(max = 200) String addressDetail,
        @Size(max = 200) String deliveryMessage
) {
}
