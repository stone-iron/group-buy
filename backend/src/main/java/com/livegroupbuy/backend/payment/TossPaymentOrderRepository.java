package com.livegroupbuy.backend.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TossPaymentOrderRepository extends JpaRepository<TossPaymentOrder, Long> {

    Optional<TossPaymentOrder> findByTicketId(Long ticketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from TossPaymentOrder payment where payment.orderId = :orderId")
    Optional<TossPaymentOrder> findLockedByOrderId(@Param("orderId") String orderId);
}
