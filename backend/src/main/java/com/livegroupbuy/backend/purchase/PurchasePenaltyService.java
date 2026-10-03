package com.livegroupbuy.backend.purchase;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchasePenaltyService {

    private final PurchaseAbuseProfileRepository repository;
    private final long windowHours;
    private final int blockThreshold;
    private final long blockMinutes;
    private final int severeThreshold;
    private final long severeBlockHours;

    public PurchasePenaltyService(PurchaseAbuseProfileRepository repository,
                                  @Value("${purchase.abuse.penalty.window-hours:24}") long windowHours,
                                  @Value("${purchase.abuse.penalty.block-threshold:3}") int blockThreshold,
                                  @Value("${purchase.abuse.penalty.block-minutes:30}") long blockMinutes,
                                  @Value("${purchase.abuse.penalty.severe-threshold:5}") int severeThreshold,
                                  @Value("${purchase.abuse.penalty.severe-block-hours:24}") long severeBlockHours) {
        this.repository = repository;
        this.windowHours = windowHours;
        this.blockThreshold = blockThreshold;
        this.blockMinutes = blockMinutes;
        this.severeThreshold = severeThreshold;
        this.severeBlockHours = severeBlockHours;
    }

    @Transactional(readOnly = true)
    public void assertParticipationAllowed(Long memberId) {
        PurchaseAbuseProfile profile = repository.findById(memberId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (profile == null || !profile.isBlockedAt(now)) return;

        long remaining = Math.max(1, Duration.between(now, profile.getBlockedUntil()).toSeconds());
        throw new PurchaseAccessDeniedException(
                "UNPAID_RESERVATION_PENALTY",
                "미결제 예약이 반복되어 한정판매 참여가 일시 제한되었습니다. " + readableDuration(remaining) + " 후 다시 시도해 주세요.",
                remaining
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUnpaidExpiration(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        PurchaseAbuseProfile profile = repository.findLockedByMemberId(memberId)
                .orElseGet(() -> new PurchaseAbuseProfile(memberId));
        profile.recordUnpaidExpiration(
                now, windowHours, blockThreshold, blockMinutes, severeThreshold, severeBlockHours
        );
        repository.save(profile);
    }

    private String readableDuration(long seconds) {
        if (seconds >= 3_600) return ((seconds + 3_599) / 3_600) + "시간";
        if (seconds >= 60) return ((seconds + 59) / 60) + "분";
        return seconds + "초";
    }
}
