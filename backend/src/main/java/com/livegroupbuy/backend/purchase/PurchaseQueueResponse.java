package com.livegroupbuy.backend.purchase;

import java.time.LocalDateTime;

public record PurchaseQueueResponse(
        Long ticketId,
        Long productId,
        int quantity,
        PurchaseStatus status,
        long waitingAhead,
        long position,
        int totalQuantity,
        int soldQuantity,
        int reservedQuantity,
        int remainingQuantity,
        int maxPerMember,
        int memberRequestedQuantity,
        int memberRemainingLimit,
        LocalDateTime expiresAt,
        String message
) {
}
