package com.paymentgateway.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a payment order created by a merchant.
 * An order can have multiple payment transaction attempts.
 */
@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_order_id", columnList = "orderId", unique = true),
        @Index(name = "idx_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CREATED";

    @Column(length = 64)
    private String idempotencyKey;

    @Column(length = 500)
    private String callbackUrl;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Generates a new unique order ID before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (this.orderId == null) {
            this.orderId = UUID.randomUUID().toString();
        }
    }

    /**
     * Checks whether this order has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
