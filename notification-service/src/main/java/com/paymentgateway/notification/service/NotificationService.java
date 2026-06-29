package com.paymentgateway.notification.service;

import com.paymentgateway.common.enums.NotificationType;
import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.common.event.FraudCheckResultEvent;
import com.paymentgateway.common.event.PaymentProcessedEvent;
import com.paymentgateway.common.event.SettlementEvent;
import com.paymentgateway.notification.entity.NotificationLog;
import com.paymentgateway.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification business logic.
 *
 * Processes domain events and dispatches notifications to the appropriate
 * channel (currently simulated via console logging). Every notification is
 * persisted as an audit record in the notification_logs table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    // ──────────────────────────────────────────────────────────────
    //  Payment notifications
    // ──────────────────────────────────────────────────────────────

    /**
     * Send a payment-success notification to the customer.
     */
    @Transactional
    public NotificationLog sendPaymentSuccessNotification(PaymentProcessedEvent event) {
        String subject = "Payment Successful";
        String body = String.format(
                """
                Dear Customer,

                Your payment has been processed successfully.

                Transaction ID : %s
                Order ID       : %s
                Amount         : %s %s
                Payment Method : %s
                Status         : %s
                Date           : %s

                Thank you for your payment!

                — Payment Gateway Team""",
                event.getTransactionId(),
                event.getOrderId(),
                event.getCurrency(),
                event.getAmount(),
                event.getPaymentMethod(),
                event.getStatus(),
                event.getTimestamp()
        );

        return dispatchAndLog(
                NotificationType.EMAIL,
                event.getCustomerEmail(),
                subject,
                body,
                event.getTransactionId(),
                event.getMerchantId()
        );
    }

    /**
     * Send a payment-failure notification to the customer.
     */
    @Transactional
    public NotificationLog sendPaymentFailureNotification(PaymentProcessedEvent event) {
        String subject = "Payment Failed";
        String body = String.format(
                """
                Dear Customer,

                Unfortunately, your payment could not be processed.

                Transaction ID : %s
                Order ID       : %s
                Amount         : %s %s
                Payment Method : %s
                Status         : %s
                Reason         : %s
                Date           : %s

                Please try again or contact support for assistance.

                — Payment Gateway Team""",
                event.getTransactionId(),
                event.getOrderId(),
                event.getCurrency(),
                event.getAmount(),
                event.getPaymentMethod(),
                event.getStatus(),
                event.getFailureReason() != null ? event.getFailureReason() : "Unknown",
                event.getTimestamp()
        );

        return dispatchAndLog(
                NotificationType.EMAIL,
                event.getCustomerEmail(),
                subject,
                body,
                event.getTransactionId(),
                event.getMerchantId()
        );
    }

    // ──────────────────────────────────────────────────────────────
    //  Fraud alert notifications
    // ──────────────────────────────────────────────────────────────

    /**
     * Send a fraud alert notification when risk level is HIGH or CRITICAL.
     * Returns null if the risk level doesn't warrant an alert.
     */
    @Transactional
    public NotificationLog sendFraudAlertNotification(FraudCheckResultEvent event) {
        if (event.getRiskLevel() != RiskLevel.HIGH && event.getRiskLevel() != RiskLevel.CRITICAL) {
            log.debug("Skipping fraud alert for transaction {} — risk level {} is below threshold",
                    event.getTransactionId(), event.getRiskLevel());
            return null;
        }

        String subject = "⚠️ Suspicious Transaction Detected";
        String body = String.format(
                """
                ⚠️ FRAUD ALERT — Suspicious Activity Detected

                Transaction ID  : %s
                Order ID        : %s
                Risk Score      : %d / 100
                Risk Level      : %s
                Blocked         : %s
                Triggered Rules : %s
                Recommendation  : %s
                Detected At     : %s

                Please review this transaction immediately in the merchant dashboard.

                — Payment Gateway Fraud Detection""",
                event.getTransactionId(),
                event.getOrderId(),
                event.getRiskScore(),
                event.getRiskLevel(),
                event.isBlocked() ? "YES" : "NO",
                event.getTriggeredRules() != null ? String.join(", ", event.getTriggeredRules()) : "N/A",
                event.getRecommendation() != null ? event.getRecommendation() : "N/A",
                event.getTimestamp()
        );

        return dispatchAndLog(
                NotificationType.EMAIL,
                "fraud-team@paymentgateway.com",  // Internal fraud team
                subject,
                body,
                event.getTransactionId(),
                null // merchantId not available in fraud event
        );
    }

    // ──────────────────────────────────────────────────────────────
    //  Settlement notifications
    // ──────────────────────────────────────────────────────────────

    /**
     * Send a settlement-completed notification to the merchant.
     */
    @Transactional
    public NotificationLog sendSettlementNotification(SettlementEvent event) {
        String subject = "Settlement Processed";
        String body = String.format(
                """
                Dear Merchant,

                Your settlement has been processed successfully.

                Settlement ID      : %s
                Gross Amount       : %s
                Platform Fee       : %s
                Net Amount         : %s
                Transactions Count : %d
                Status             : %s
                Processed At       : %s

                The net amount will be credited to your registered bank account.

                — Payment Gateway Settlements""",
                event.getSettlementId(),
                event.getGrossAmount(),
                event.getPlatformFee(),
                event.getNetAmount(),
                event.getTransactionCount(),
                event.getStatus(),
                event.getTimestamp()
        );

        return dispatchAndLog(
                NotificationType.EMAIL,
                "merchant-" + event.getMerchantId() + "@paymentgateway.com",
                subject,
                body,
                event.getSettlementId(),
                event.getMerchantId()
        );
    }

    // ──────────────────────────────────────────────────────────────
    //  Query methods
    // ──────────────────────────────────────────────────────────────

    /**
     * Retrieve all notifications, paginated.
     */
    @Transactional(readOnly = true)
    public Page<NotificationLog> getNotifications(Pageable pageable) {
        return notificationLogRepository.findAll(pageable);
    }

    /**
     * Retrieve notifications for a specific merchant, paginated.
     */
    @Transactional(readOnly = true)
    public Page<NotificationLog> getNotificationsByMerchant(Long merchantId, Pageable pageable) {
        return notificationLogRepository.findByMerchantId(merchantId, pageable);
    }

    /**
     * Retrieve notifications related to a specific transaction.
     */
    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByTransaction(String transactionId) {
        return notificationLogRepository.findByRelatedTransactionId(transactionId);
    }

    // ──────────────────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Simulate dispatching a notification (log to console) and persist
     * an audit record. In production, this would integrate with
     * SendGrid, Twilio, or a webhook dispatcher.
     */
    private NotificationLog dispatchAndLog(
            NotificationType type,
            String recipient,
            String subject,
            String body,
            String relatedTransactionId,
            Long merchantId
    ) {
        // Simulate delivery
        log.info("═══════════════════════════════════════════════════════════");
        log.info("📧 DISPATCHING {} NOTIFICATION", type);
        log.info("   To      : {}", recipient);
        log.info("   Subject : {}", subject);
        log.info("   TxnRef  : {}", relatedTransactionId);
        log.info("═══════════════════════════════════════════════════════════");
        log.debug("   Body:\n{}", body);

        // Persist audit record
        NotificationLog notificationLog = NotificationLog.builder()
                .type(type)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status("SENT")
                .relatedTransactionId(relatedTransactionId)
                .merchantId(merchantId)
                .build();

        NotificationLog saved = notificationLogRepository.save(notificationLog);
        log.info("✅ Notification logged with id={}", saved.getId());

        return saved;
    }
}
