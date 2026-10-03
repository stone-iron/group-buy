package com.livegroupbuy.backend.purchase;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PurchaseAbuseProfileRepository extends JpaRepository<PurchaseAbuseProfile, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from PurchaseAbuseProfile profile where profile.memberId = :memberId")
    Optional<PurchaseAbuseProfile> findLockedByMemberId(@Param("memberId") Long memberId);
}
