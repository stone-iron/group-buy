package com.livegroupbuy.backend.payment;

public record TossPaymentPrepareResponse(
        Long ticketId,
        String orderId,
        String orderName,
        String customerKey,
        long amount
) {
    static TossPaymentPrepareResponse from(TossPaymentOrder order) {
        return new TossPaymentPrepareResponse(
                order.getTicketId(), order.getOrderId(), order.getOrderName(),
                order.getCustomerKey(), order.getAmount()
        );
    }
}
