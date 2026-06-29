package com.paymentgateway.transaction.entity;

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

/**
 * Core entity representing a payment transaction record.
 *
 * <p>Transaction records are created by consuming {@code payment.initiated} Kafka events
 * and updated via {@code payment.processed} events. This entity serves as the
 * single source of truth for transaction history and analytics.</p>
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_merchant_created", columnList = "merchantId, createdAt"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(length = 64)
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

    @Column
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskLevel riskLevel;

    @Column(length = 500)
    private String failureReason;

    @Column
    @Builder.Default
    private Integer retryCount = 0;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 45)
    private String customerIp;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
