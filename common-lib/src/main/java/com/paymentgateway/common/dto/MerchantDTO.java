package com.paymentgateway.common.dto;

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
public class MerchantDTO {

    private Long id;
    private String businessName;
    private String email;
    private String phone;
    private String apiKey;
    private String webhookUrl;
    private BigDecimal feePercentage;
    private MerchantStatus status;
    private LocalDateTime createdAt;
}
