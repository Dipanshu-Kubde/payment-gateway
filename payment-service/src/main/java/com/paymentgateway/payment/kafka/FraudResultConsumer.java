package com.paymentgateway.payment.kafka;

import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.FraudCheckResultEvent;
import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes fraud check results from the Fraud Detection Service.
 * Updates the transaction with the risk assessment and blocks fraudulent payments.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FraudResultConsumer {

    private final PaymentTransactionRepository transactionRepository;

    /**
     * Handles FraudCheckResultEvent from the fraud.check.result topic.
     * Updates the transaction's risk score and risk level.
     * If the fraud service blocks the transaction, sets status to FRAUD_REVIEW.
     */
    @KafkaListener(
            topics = "fraud.check.result",
            groupId = "payment-service-fraud-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeFraudResult(FraudCheckResultEvent event) {
        log.info("Received FraudCheckResultEvent for transaction: {}, riskScore: {}, blocked: {}",
                event.getTransactionId(), event.getRiskScore(), event.isBlocked());

        transactionRepository.findByTransactionId(event.getTransactionId())
                .ifPresentOrElse(
                        transaction -> processResult(transaction, event),
                        () -> log.warn("Transaction not found for fraud result: {}", event.getTransactionId())
                );
    }

    private void processResult(PaymentTransaction transaction, FraudCheckResultEvent event) {
        // Update risk assessment fields
        transaction.setRiskScore(event.getRiskScore());
        transaction.setRiskLevel(event.getRiskLevel());

        if (event.isBlocked()) {
            log.warn("Transaction {} BLOCKED by fraud detection. Risk score: {}, Level: {}, Rules: {}",
                    transaction.getTransactionId(),
                    event.getRiskScore(),
                    event.getRiskLevel(),
                    event.getTriggeredRules());

            transaction.setStatus(TransactionStatus.FRAUD_REVIEW);
            transaction.setFailureReason("Blocked by fraud detection: " + event.getRecommendation());
        } else {
            log.info("Transaction {} passed fraud check. Risk score: {}, Level: {}",
                    transaction.getTransactionId(),
                    event.getRiskScore(),
                    event.getRiskLevel());
        }

        transactionRepository.save(transaction);
    }
}
