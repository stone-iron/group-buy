package com.livegroupbuy.backend.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livegroupbuy.backend.purchase.PurchaseQueueResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments/toss")
public class TossPaymentController {

    private final TossPaymentService service;

    public TossPaymentController(TossPaymentService service) {
        this.service = service;
    }

    @PostMapping("/prepare")
    public TossPaymentPrepareResponse prepare(@Valid @RequestBody TossPaymentPrepareRequest request) {
        return service.prepare(request);
    }

    @PostMapping("/confirm")
    public PurchaseQueueResponse confirm(@Valid @RequestBody TossPaymentConfirmRequest request) {
        return service.confirm(request);
    }
}
