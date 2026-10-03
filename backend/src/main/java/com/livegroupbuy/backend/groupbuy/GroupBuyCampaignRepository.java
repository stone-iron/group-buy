package com.livegroupbuy.backend.groupbuy;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface GroupBuyCampaignRepository extends JpaRepository<GroupBuyCampaign, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select campaign from GroupBuyCampaign campaign where campaign.productId = :productId")
    Optional<GroupBuyCampaign> findByIdForUpdate(@Param("productId") Long productId);
}
