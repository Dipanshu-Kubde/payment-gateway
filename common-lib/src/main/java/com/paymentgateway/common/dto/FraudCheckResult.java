package com.paymentgateway.common.dto;

import com.paymentgateway.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {

    private String transactionId;
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> triggeredRules;
    private String recommendation;
    private boolean blocked;
}
