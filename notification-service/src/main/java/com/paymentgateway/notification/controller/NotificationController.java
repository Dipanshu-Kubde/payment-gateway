package com.paymentgateway.notification.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.notification.entity.NotificationLog;
import com.paymentgateway.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for querying notification history.
 *
 * Notifications are created automatically by Kafka event consumers.
 * This controller exposes read-only endpoints for audit / dashboard use.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/notifications
     * Retrieve all notification logs, paginated.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationLog>>> getAllNotifications(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.info("GET /api/notifications — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<NotificationLog> notifications = notificationService.getNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    /**
     * GET /api/notifications/merchant/{merchantId}
     * Retrieve notification logs for a specific merchant, paginated.
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Page<NotificationLog>>> getNotificationsByMerchant(
            @PathVariable Long merchantId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.info("GET /api/notifications/merchant/{} — page={}, size={}",
                merchantId, pageable.getPageNumber(), pageable.getPageSize());
        Page<NotificationLog> notifications = notificationService.getNotificationsByMerchant(merchantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Merchant notifications retrieved successfully", notifications));
    }

    /**
     * GET /api/notifications/transaction/{transactionId}
     * Retrieve all notifications related to a specific transaction.
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getNotificationsByTransaction(
            @PathVariable String transactionId
    ) {
        log.info("GET /api/notifications/transaction/{}", transactionId);
        List<NotificationLog> notifications = notificationService.getNotificationsByTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction notifications retrieved successfully", notifications));
    }
}
