package com.paymentgateway.fraud.kafka;

import com.paymentgateway.common.dto.FraudCheckResult;
import com.paymentgateway.common.event.FraudCheckResultEvent;
import com.paymentgateway.common.event.PaymentInitiatedEvent;
import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.service.FraudDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer that listens for payment.initiated events,
 * triggers fraud evaluation, and publishes the result.
 */
@Component
@Slf4j
public class FraudCheckConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final FraudEventPublisher fraudEventPublisher;

    public FraudCheckConsumer(FraudDetectionService fraudDetectionService,
                              FraudEventPublisher fraudEventPublisher) {
        this.fraudDetectionService = fraudDetectionService;
        this.fraudEventPublisher = fraudEventPublisher;
    }

    @KafkaListener(topics = "payment.initiated", groupId = "fraud-service-group")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Received payment.initiated event for transaction: {}", event.getTransactionId());

        try {
            // Build fraud check request from Kafka event
            FraudCheckRequest request = FraudCheckRequest.builder()
                    .transactionId(event.getTransactionId())
                    .orderId(event.getOrderId())
                    .merchantId(event.getMerchantId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .paymentMethod(event.getPaymentMethod() != null ? event.getPaymentMethod().name() : null)
                    .customerEmail(event.getCustomerEmail())
                    .customerIp(event.getCustomerIp())
                    .cardBinCountry(event.getCardBinCountry())
                    .timestamp(event.getTimestamp())
                    .build();

            // Evaluate fraud
            FraudCheckResult result = fraudDetectionService.evaluateTransaction(request);

            // Publish result back to Kafka
            FraudCheckResultEvent resultEvent = FraudCheckResultEvent.builder()
                    .transactionId(result.getTransactionId())
                    .orderId(event.getOrderId())
                    .riskScore(result.getRiskScore())
                    .riskLevel(result.getRiskLevel())
                    .triggeredRules(result.getTriggeredRules())
                    .blocked(result.isBlocked())
                    .recommendation(result.getRecommendation())
                    .timestamp(LocalDateTime.now())
                    .build();

            fraudEventPublisher.publishFraudResult(resultEvent);

            log.info("Fraud check completed for txn {}: score={}, level={}, blocked={}",
                    event.getTransactionId(), result.getRiskScore(),
                    result.getRiskLevel(), result.isBlocked());

        } catch (Exception e) {
            log.error("Error processing fraud check for transaction {}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }
}
