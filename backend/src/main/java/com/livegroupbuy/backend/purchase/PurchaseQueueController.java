package com.livegroupbuy.backend.purchase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import com.livegroupbuy.backend.auth.LoginMember;
import com.livegroupbuy.backend.auth.LoginSessionManager;

@RestController
@RequestMapping("/api/purchase-queue")
public class PurchaseQueueController {

    private final PurchaseQueueService service;
    private final PurchaseRateLimitService rateLimitService;

    public PurchaseQueueController(PurchaseQueueService service,
                                   PurchaseRateLimitService rateLimitService) {
        this.service = service;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PurchaseQueueResponse enqueue(@Valid @RequestBody PurchaseQueueRequest request,
                                         @AuthenticationPrincipal LoginMember member,
                                         HttpServletRequest servletRequest) {
        // 회원 번호는 요청 본문이 아니라 로그인 세션에서 가져온다.
        Long memberId = LoginSessionManager.require(member).id();
        rateLimitService.assertRequestAllowed(memberId, servletRequest.getRemoteAddr());
        return service.enqueue(new PurchaseQueueRequest(
                request.productId(), memberId, request.quantity(), request.requestKey()
        ));
    }

    @GetMapping("/{ticketId}")
    public PurchaseQueueResponse status(@PathVariable Long ticketId,
                                        @AuthenticationPrincipal LoginMember member) {
        return service.statusForMember(ticketId, LoginSessionManager.require(member).id());
    }

    @PostMapping("/{ticketId}/cancel")
    public PurchaseQueueResponse cancel(@PathVariable Long ticketId,
                                        @AuthenticationPrincipal LoginMember member) {
        return service.cancel(ticketId, new PurchaseCancelRequest(LoginSessionManager.require(member).id()));
    }

    @GetMapping("/inventories")
    public List<InventoryResponse> inventories() {
        return service.inventories();
    }

    @PostMapping("/inventories")
    public InventoryResponse registerInventory(@Valid @RequestBody InventoryRegistrationRequest request) {
        return service.registerInventory(request);
    }
}
