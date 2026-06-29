package com.paymentgateway.payment.entity;

import com.paymentgateway.common.enums.PaymentMethod;
import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.enums.TransactionStatus;
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
 * Represents an individual payment transaction attempt.
 * A single PaymentOrder can have multiple transactions (e.g., retries).
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_txn_transaction_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_txn_order_id", columnList = "orderId"),
        @Index(name = "idx_txn_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_retry", columnList = "status, nextRetryAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(nullable = false, length = 36)
    private String orderId;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private RiskLevel riskLevel;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    private LocalDateTime nextRetryAt;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 45)
    private String customerIp;

    @Column(length = 64)
    private String idempotencyKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Generates a new unique transaction ID before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (this.transactionId == null) {
            this.transactionId = UUID.randomUUID().toString();
        }
    }

    /**
     * Checks whether this transaction is eligible for retry.
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries
                && (this.status == TransactionStatus.FAILED || this.status == TransactionStatus.RETRY_SCHEDULED);
    }

    /**
     * Calculates the next retry delay using exponential backoff.
     * Base delay doubles with each retry: 1s, 2s, 4s
     */
    public long getNextRetryDelaySeconds() {
        return (long) Math.pow(2, this.retryCount);
    }
}
