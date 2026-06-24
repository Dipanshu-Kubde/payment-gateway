package com.paymentgateway.common.event;

import com.paymentgateway.common.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published when a settlement is processed for a merchant.
 * Consumed by: Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementEvent {

    private String settlementId;
    private Long merchantId;
    private BigDecimal grossAmount;
    private BigDecimal platformFee;
    private BigDecimal netAmount;
    private int transactionCount;
    private SettlementStatus status;
    private LocalDateTime timestamp;
}
