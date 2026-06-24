package com.paymentgateway.common.event;

import com.paymentgateway.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Published by Fraud Detection Service after evaluating a transaction.
 * Consumed by: Payment Service (to decide approve/block)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResultEvent {

    private String transactionId;
    private String orderId;
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> triggeredRules;
    private boolean blocked;
    private String recommendation;
    private LocalDateTime timestamp;
}
