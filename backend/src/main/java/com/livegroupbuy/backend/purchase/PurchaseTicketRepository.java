package com.livegroupbuy.backend.purchase;

import java.util.Collection;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PurchaseTicketRepository extends JpaRepository<PurchaseTicket, Long> {

    Optional<PurchaseTicket> findByRequestKey(String requestKey);

    Optional<PurchaseTicket> findFirstByProductIdAndMemberIdAndStatusInOrderByIdDesc(
            Long productId, Long memberId, Collection<PurchaseStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseTicket> findFirstByProductIdAndStatusOrderByIdAsc(Long productId, PurchaseStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from PurchaseTicket ticket where ticket.id = :id")
    Optional<PurchaseTicket> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PurchaseTicket> findAllByStatusAndExpiresAtBeforeOrderByIdAsc(PurchaseStatus status, LocalDateTime expiresAt);

    long countByStatusInAndProductIdAndIdLessThan(Collection<PurchaseStatus> statuses, Long productId, Long id);

    @Query("select coalesce(sum(ticket.quantity), 0) from PurchaseTicket ticket " +
            "where ticket.productId = :productId and ticket.memberId = :memberId and ticket.status in :statuses")
    long sumQuantityByProductAndMemberAndStatuses(@Param("productId") Long productId,
                                                  @Param("memberId") Long memberId,
                                                  @Param("statuses") Collection<PurchaseStatus> statuses);
}
