package com.paymentgateway.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatsDTO {

    private long totalTransactions;
    private long successCount;
    private long failedCount;
    private long pendingCount;
    private long fraudFlaggedCount;
    private BigDecimal totalRevenue;
    private double successRate;
    private double fraudRate;
}
