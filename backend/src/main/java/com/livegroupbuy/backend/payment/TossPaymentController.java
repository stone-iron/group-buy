package com.livegroupbuy.backend.payment;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livegroupbuy.backend.auth.LoginMember;
import com.livegroupbuy.backend.auth.LoginSessionManager;
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
    public TossPaymentPrepareResponse prepare(@Valid @RequestBody TossPaymentPrepareRequest request,
                                              @AuthenticationPrincipal LoginMember member) {
        Long memberId = LoginSessionManager.require(member).id();
        return service.prepare(new TossPaymentPrepareRequest(
                request.ticketId(), memberId, request.recipientName(), request.phone(), request.postalCode(),
                request.address(), request.addressDetail(), request.deliveryMessage()
        ));
    }

    @PostMapping("/confirm")
    public PurchaseQueueResponse confirm(@Valid @RequestBody TossPaymentConfirmRequest request,
                                         @AuthenticationPrincipal LoginMember member) {
        return service.confirm(request, LoginSessionManager.require(member).id());
    }
}
