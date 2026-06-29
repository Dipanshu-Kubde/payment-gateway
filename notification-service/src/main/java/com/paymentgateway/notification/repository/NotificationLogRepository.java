package com.paymentgateway.notification.repository;

import com.paymentgateway.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for notification audit logs.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Find all notifications for a specific merchant, paginated.
     */
    Page<NotificationLog> findByMerchantId(Long merchantId, Pageable pageable);

    /**
     * Find all notifications related to a specific transaction.
     */
    List<NotificationLog> findByRelatedTransactionId(String relatedTransactionId);

    /**
     * Count notifications by delivery status (SENT / FAILED).
     */
    long countByStatus(String status);
}
