package com.paymentgateway.merchant.dto;

import com.paymentgateway.common.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private Long merchantId;
    private String businessName;
    private String email;
    private MerchantStatus status;
    private BigDecimal feePercentage;
    private String webhookUrl;
    private boolean apiKeyConfigured;
    private LocalDateTime memberSince;
}
