package com.orderflow.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock;

    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void reserveStock(Integer quantity) {
        if (availableStock < quantity) {
            throw new IllegalStateException("Insufficient stock available");
        }
        this.reservedStock += quantity;
        this.availableStock -= quantity;
    }

    public void releaseStock(Integer quantity) {
        this.reservedStock -= quantity;
        this.availableStock += quantity;
    }

    public void confirmReservation(Integer quantity) {
        this.reservedStock -= quantity;
        this.totalStock -= quantity;
    }
}