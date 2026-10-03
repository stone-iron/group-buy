package com.livegroupbuy.backend.live;

public record LiveChatMessage(
        String id,
        String user,
        String message,
        long sentAt
) {
}
