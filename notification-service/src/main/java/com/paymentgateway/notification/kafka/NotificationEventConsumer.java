package com.paymentgateway.notification.kafka;

import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.common.event.FraudCheckResultEvent;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.common.event.SettlementEvent;
import com.paymentgateway.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumers for the Notification Service.
 *
 * Listens to payment, fraud, and settlement events and triggers
 * the appropriate notification workflow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    /**
     * Consume payment-processed events and dispatch success or failure notifications.
     */
    @KafkaListener(
            topics = "payment.processed",
            groupId = "notification-service-payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("📥 Received PaymentProcessedEvent: transactionId={}, status={}",
                event.getTransactionId(), event.getStatus());

        try {
            if (event.getStatus() == TransactionStatus.SUCCESS) {
                notificationService.sendPaymentSuccessNotification(event);
            } else {
                notificationService.sendPaymentFailureNotification(event);
            }
        } catch (Exception e) {
            log.error("❌ Failed to process payment notification for transactionId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Consume fraud-check-result events and dispatch alerts for HIGH / CRITICAL risk.
     */
    @KafkaListener(
            topics = "fraud.check.result",
            groupId = "notification-service-fraud-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFraudCheckResult(FraudCheckResultEvent event) {
        log.info("📥 Received FraudCheckResultEvent: transactionId={}, riskLevel={}, riskScore={}",
                event.getTransactionId(), event.getRiskLevel(), event.getRiskScore());

        try {
            if (event.getRiskLevel() == RiskLevel.HIGH || event.getRiskLevel() == RiskLevel.CRITICAL) {
                notificationService.sendFraudAlertNotification(event);
            } else {
                log.debug("Fraud event for transactionId={} below alert threshold (riskLevel={}), skipping",
                        event.getTransactionId(), event.getRiskLevel());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process fraud alert for transactionId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * Consume settlement-completed events and dispatch settlement notifications.
     */
    @KafkaListener(
            topics = "settlement.completed",
            groupId = "notification-service-settlement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSettlementCompleted(SettlementEvent event) {
        log.info("📥 Received SettlementEvent: settlementId={}, merchantId={}, netAmount={}",
                event.getSettlementId(), event.getMerchantId(), event.getNetAmount());

        try {
            notificationService.sendSettlementNotification(event);
        } catch (Exception e) {
            log.error("❌ Failed to process settlement notification for settlementId={}: {}",
                    event.getSettlementId(), e.getMessage(), e);
        }
    }
}
