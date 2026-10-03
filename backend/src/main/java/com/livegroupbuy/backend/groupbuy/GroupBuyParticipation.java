package com.livegroupbuy.backend.groupbuy;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "group_buy_participations", uniqueConstraints =
        @UniqueConstraint(name = "uk_group_buy_product_member", columnNames = {"productId", "memberId"}))
public class GroupBuyParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GroupBuyParticipation() {
    }

    public GroupBuyParticipation(Long productId, Long memberId, int quantity) {
        this.productId = productId;
        this.memberId = memberId;
        this.quantity = quantity;
    }

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
