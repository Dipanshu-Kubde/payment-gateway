package com.paymentgateway.common.event;

import com.paymentgateway.common.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published when a payment is initiated.
 * Consumed by: Fraud Detection Service, Transaction Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiatedEvent {

    private String transactionId;
    private String orderId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String customerEmail;
    private String customerIp;
    private String cardBinCountry;
    private LocalDateTime timestamp;
}
