package com.livegroupbuy.backend.groupbuy;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/group-buys")
public class GroupBuyController {

    private final GroupBuyService service;

    public GroupBuyController(GroupBuyService service) { this.service = service; }

    @GetMapping
    public List<GroupBuyCampaignResponse> campaigns() { return service.campaigns(); }

    @PostMapping("/campaigns")
    public GroupBuyCampaignResponse register(@Valid @RequestBody GroupBuyCampaignRequest request) {
        return service.register(request);
    }

    @PostMapping("/{productId}/participate")
    public GroupBuyCampaignResponse join(@PathVariable Long productId,
                                         @Valid @RequestBody GroupBuyJoinRequest request) {
        return service.join(productId, request);
    }
}
