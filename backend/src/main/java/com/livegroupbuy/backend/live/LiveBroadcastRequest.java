package com.livegroupbuy.backend.live;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LiveBroadcastRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 50) String seller,
        @NotNull Long productId,
        @NotBlank @Size(max = 160) String productTitle,
        @NotBlank @Size(max = 16) String emoji,
        String imageUrl,
        @NotBlank @Size(max = 80) String scheduledAt,
        @NotBlank @Size(max = 500) String description,
        String status,
        Integer viewers,
        @Size(max = 300) String rejectionReason,
        @Size(max = 100) String streamKey,
        Long startedAt
) {
}
