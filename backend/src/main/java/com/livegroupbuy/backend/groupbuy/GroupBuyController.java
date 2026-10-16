package com.livegroupbuy.backend.groupbuy;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.livegroupbuy.backend.auth.LoginMember;
import com.livegroupbuy.backend.auth.LoginSessionManager;

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
                                         @Valid @RequestBody GroupBuyJoinRequest request,
                                         @AuthenticationPrincipal LoginMember member) {
        Long memberId = LoginSessionManager.require(member).id();
        return service.join(productId, new GroupBuyJoinRequest(memberId, request.quantity()));
    }

    @GetMapping("/participations")
    public List<BuyerGroupBuyParticipationResponse> memberParticipations(@AuthenticationPrincipal LoginMember member) {
        return service.participationsForMember(LoginSessionManager.require(member).id());
    }

    @GetMapping("/{productId}/participations")
    public List<SellerGroupBuyParticipationResponse> productParticipations(@PathVariable Long productId) {
        return service.participationsForProduct(productId);
    }
}
