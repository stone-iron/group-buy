package com.livegroupbuy.backend.purchase;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_abuse_profiles")
public class PurchaseAbuseProfile {

    @Id
    private Long memberId;

    @Column(nullable = false)
    private int unpaidExpirationCount;

    private LocalDateTime penaltyWindowStartedAt;

    private LocalDateTime blockedUntil;

    protected PurchaseAbuseProfile() {
    }

    public PurchaseAbuseProfile(Long memberId) {
        this.memberId = memberId;
        this.unpaidExpirationCount = 0;
    }

    public void recordUnpaidExpiration(LocalDateTime now, long windowHours,
                                       int blockThreshold, long blockMinutes,
                                       int severeThreshold, long severeBlockHours) {
        if (penaltyWindowStartedAt == null || !penaltyWindowStartedAt.plusHours(windowHours).isAfter(now)) {
            unpaidExpirationCount = 0;
            penaltyWindowStartedAt = now;
            blockedUntil = null;
        }
        if (penaltyWindowStartedAt == null) penaltyWindowStartedAt = now;
        unpaidExpirationCount += 1;

        LocalDateTime nextBlockedUntil = null;
        if (unpaidExpirationCount >= severeThreshold) {
            nextBlockedUntil = now.plusHours(severeBlockHours);
        } else if (unpaidExpirationCount >= blockThreshold) {
            nextBlockedUntil = now.plusMinutes(blockMinutes);
        }
        if (nextBlockedUntil != null && (blockedUntil == null || nextBlockedUntil.isAfter(blockedUntil))) {
            blockedUntil = nextBlockedUntil;
        }
    }

    public boolean isBlockedAt(LocalDateTime now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }

    public Long getMemberId() { return memberId; }
    public int getUnpaidExpirationCount() { return unpaidExpirationCount; }
    public LocalDateTime getPenaltyWindowStartedAt() { return penaltyWindowStartedAt; }
    public LocalDateTime getBlockedUntil() { return blockedUntil; }
}
