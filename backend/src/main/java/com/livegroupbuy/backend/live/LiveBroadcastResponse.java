package com.livegroupbuy.backend.live;

import java.util.Locale;

public record LiveBroadcastResponse(
        Long id,
        String title,
        String seller,
        Long productId,
        String productTitle,
        String emoji,
        String imageUrl,
        String status,
        String scheduledAt,
        int viewers,
        String description,
        String rejectionReason,
        String streamKey,
        Long startedAt
) {
    public static LiveBroadcastResponse from(LiveBroadcast broadcast) {
        return new LiveBroadcastResponse(
                broadcast.getId(), broadcast.getTitle(), broadcast.getSeller(), broadcast.getProductId(),
                broadcast.getProductTitle(), broadcast.getEmoji(), broadcast.getImageUrl(),
                broadcast.getStatus().name().toLowerCase(Locale.ROOT), broadcast.getScheduledAt(),
                broadcast.getViewers(), broadcast.getDescription(), broadcast.getRejectionReason(),
                broadcast.getStreamKey(), broadcast.getStartedAt()
        );
    }
}
