package com.livegroupbuy.backend.delivery;

import java.util.List;

public record DeliveryTrackingResponse(
        boolean demoMode,
        String carrierCode,
        String carrierName,
        String invoice,
        String status,
        boolean complete,
        String itemName,
        String recipient,
        String estimatedDelivery,
        String message,
        List<TrackingEvent> events
) {
    public record TrackingEvent(String time, String location, String description) {
    }
}
