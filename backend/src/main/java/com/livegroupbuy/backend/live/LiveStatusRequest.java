package com.livegroupbuy.backend.live;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiveStatusRequest(
        @NotBlank String status,
        @Size(max = 300) String rejectionReason
) {
}
