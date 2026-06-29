package com.paymentgateway.settlement.entity;

import com.paymentgateway.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a settlement batch for a merchant.
 * Each settlement captures the gross earnings, platform fee deduction,
 * and net payout amount for a settlement cycle.
 */
@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlement_merchant_id", columnList = "merchantId"),
        @Index(name = "idx_settlement_status", columnList = "status"),
        @Index(name = "idx_settlement_date", columnList = "settlementDate")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String settlementId;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal platformFee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Generate a UUID-based settlementId before persisting if not already set.
     */
    @PrePersist
    public void prePersist() {
        if (this.settlementId == null || this.settlementId.isBlank()) {
            this.settlementId = UUID.randomUUID().toString();
        }
    }
}
