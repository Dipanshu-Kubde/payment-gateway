package com.paymentgateway.common.event;

import com.paymentgateway.common.enums.PaymentMethod;
import com.paymentgateway.common.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published when a payment is processed (success or failure).
 * Consumed by: Transaction Service, Notification Service, Settlement Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private String transactionId;
    private String orderId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private String failureReason;
    private int riskScore;
    private String customerEmail;
    private LocalDateTime timestamp;
}
