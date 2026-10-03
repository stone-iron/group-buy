package com.livegroupbuy.backend.payment;

record TossConfirmApiResponse(
        String paymentKey,
        String orderId,
        long totalAmount,
        String status
) {
}
