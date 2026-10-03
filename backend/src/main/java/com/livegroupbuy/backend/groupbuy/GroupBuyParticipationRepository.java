package com.livegroupbuy.backend.groupbuy;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupBuyParticipationRepository extends JpaRepository<GroupBuyParticipation, Long> {
    Optional<GroupBuyParticipation> findByProductIdAndMemberId(Long productId, Long memberId);
}
