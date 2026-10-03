package com.livegroupbuy.backend.purchase;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

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
                                         HttpServletRequest servletRequest) {
        rateLimitService.assertRequestAllowed(request.memberId(), clientIp(servletRequest));
        return service.enqueue(request);
    }

    @GetMapping("/{ticketId}")
    public PurchaseQueueResponse status(@PathVariable Long ticketId) {
        return service.status(ticketId);
    }

    @PostMapping("/{ticketId}/cancel")
    public PurchaseQueueResponse cancel(@PathVariable Long ticketId,
                                        @Valid @RequestBody PurchaseCancelRequest request) {
        return service.cancel(ticketId, request);
    }

    @GetMapping("/inventories")
    public List<InventoryResponse> inventories() {
        return service.inventories();
    }

    @PostMapping("/inventories")
    public InventoryResponse registerInventory(@Valid @RequestBody InventoryRegistrationRequest request) {
        return service.registerInventory(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
