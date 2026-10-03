package com.livegroupbuy.backend.delivery;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryTrackingController {

    private final DeliveryTrackingService deliveryTrackingService;

    public DeliveryTrackingController(DeliveryTrackingService deliveryTrackingService) {
        this.deliveryTrackingService = deliveryTrackingService;
    }

    @GetMapping("/track")
    public DeliveryTrackingResponse track(
            @RequestParam String carrierCode,
            @RequestParam String invoice
    ) {
        if (!carrierCode.matches("\\d{2,3}")) {
            throw new IllegalArgumentException("택배사를 선택해 주세요.");
        }
        if (!invoice.matches("[A-Za-z0-9-]{8,30}")) {
            throw new IllegalArgumentException("운송장 번호를 8자 이상 입력해 주세요.");
        }
        return deliveryTrackingService.track(carrierCode, invoice);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServiceError(IllegalStateException exception) {
        return ResponseEntity.status(502).body(Map.of("message", exception.getMessage()));
    }
}
