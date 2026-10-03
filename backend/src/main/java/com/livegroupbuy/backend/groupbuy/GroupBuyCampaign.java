package com.livegroupbuy.backend.groupbuy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_buy_campaigns")
public class GroupBuyCampaign {

    @Id
    private Long productId;

    @Column(nullable = false, length = 160)
    private String productTitle;

    @Column(nullable = false)
    private int goalQuantity;

    @Column(nullable = false)
    private int joinedQuantity;

    protected GroupBuyCampaign() {
    }

    public GroupBuyCampaign(Long productId, String productTitle, int goalQuantity, int joinedQuantity) {
        if (goalQuantity <= 0 || joinedQuantity < 0 || joinedQuantity > goalQuantity) {
            throw new IllegalArgumentException("공동구매 모집 수량이 올바르지 않습니다.");
        }
        this.productId = productId;
        this.productTitle = productTitle;
        this.goalQuantity = goalQuantity;
        this.joinedQuantity = joinedQuantity;
    }

    public void updateInfo(String productTitle, int goalQuantity) {
        if (goalQuantity < joinedQuantity) throw new IllegalArgumentException("목표 수량은 현재 참여 수량보다 작을 수 없습니다.");
        this.productTitle = productTitle;
        this.goalQuantity = goalQuantity;
    }

    public void join(int quantity) {
        if (quantity <= 0 || joinedQuantity + quantity > goalQuantity) {
            throw new IllegalArgumentException("남은 공동구매 모집 수량을 확인해 주세요.");
        }
        joinedQuantity += quantity;
    }

    public Long getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public int getGoalQuantity() { return goalQuantity; }
    public int getJoinedQuantity() { return joinedQuantity; }
    public int remainingQuantity() { return goalQuantity - joinedQuantity; }
    public boolean goalReached() { return joinedQuantity >= goalQuantity; }
}
