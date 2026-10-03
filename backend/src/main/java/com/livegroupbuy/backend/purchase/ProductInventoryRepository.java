package com.livegroupbuy.backend.purchase;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from ProductInventory inventory where inventory.productId = :productId")
    Optional<ProductInventory> findByIdForUpdate(@Param("productId") Long productId);

    @Query("select inventory.productId from ProductInventory inventory order by inventory.productId asc")
    List<Long> findAllProductIds();
}
