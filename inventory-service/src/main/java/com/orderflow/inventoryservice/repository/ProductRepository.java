package com.orderflow.inventoryservice.repository;

import com.orderflow.inventoryservice.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByTenantIdAndProductId(String tenantId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.productId = :productId")
    Optional<Product> findByTenantIdAndProductIdWithLock(
            @Param("tenantId") String tenantId,
            @Param("productId") Long productId
    );
}