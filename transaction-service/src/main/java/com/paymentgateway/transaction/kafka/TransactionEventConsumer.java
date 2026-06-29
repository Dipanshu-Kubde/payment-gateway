package com.paymentgateway.transaction.kafka;

import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.PaymentInitiatedEvent;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.transaction.entity.Transaction;
import com.paymentgateway.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumer that drives transaction lifecycle.
 *
 * <ul>
 *   <li>{@code payment.initiated} → creates a new INITIATED transaction record</li>
 *   <li>{@code payment.processed} → updates the transaction with final status, risk info, and failure reason</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionService transactionService;

    /**
     * Handle payment-initiated events: create a transaction record in INITIATED state.
     */
    @KafkaListener(topics = "payment.initiated", groupId = "transaction-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Received payment.initiated event: transactionId={}, merchantId={}, amount={}",
                event.getTransactionId(), event.getMerchantId(), event.getAmount());

        try {
            Transaction transaction = Transaction.builder()
                    .transactionId(event.getTransactionId())
                    .orderId(event.getOrderId())
                    .merchantId(event.getMerchantId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .paymentMethod(event.getPaymentMethod())
                    .status(TransactionStatus.INITIATED)
                    .retryCount(0)
                    .customerEmail(event.getCustomerEmail())
                    .customerIp(event.getCustomerIp())
                    .build();

            transactionService.createTransaction(transaction);
            log.info("Transaction record created: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to process payment.initiated event for transactionId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Handle payment-processed events: update transaction status, risk scoring, and failure info.
     */
    @KafkaListener(topics = "payment.processed", groupId = "transaction-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment.processed event: transactionId={}, status={}, riskScore={}",
                event.getTransactionId(), event.getStatus(), event.getRiskScore());

        try {
            RiskLevel riskLevel = deriveRiskLevel(event.getRiskScore());

            transactionService.updateTransaction(
                    event.getTransactionId(),
                    event.getStatus(),
                    event.getRiskScore(),
                    riskLevel,
                    event.getFailureReason()
            );

            log.info("Transaction updated: {} → {}", event.getTransactionId(), event.getStatus());

        } catch (Exception e) {
            log.error("Failed to process payment.processed event for transactionId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Derive a {@link RiskLevel} from a numeric risk score using project-wide thresholds.
     *
     * <ul>
     *   <li>0–30  → LOW</li>
     *   <li>31–60 → MEDIUM</li>
     *   <li>61–80 → HIGH</li>
     *   <li>81–100 → CRITICAL</li>
     * </ul>
     */
    private RiskLevel deriveRiskLevel(int riskScore) {
        if (riskScore <= 30) return RiskLevel.LOW;
        if (riskScore <= 60) return RiskLevel.MEDIUM;
        if (riskScore <= 80) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }
}
