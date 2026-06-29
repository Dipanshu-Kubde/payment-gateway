package com.paymentgateway.transaction.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.common.dto.TransactionStatsDTO;
import com.paymentgateway.common.enums.TransactionStatus;
import com.paymentgateway.transaction.entity.Transaction;
import com.paymentgateway.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for transaction querying and analytics.
 *
 * <p>All endpoints return data wrapped in {@link ApiResponse} for consistent
 * envelope structure across the payment gateway.</p>
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // ────────────────────────────────────────────────────────────────
    //  Transaction Queries
    // ────────────────────────────────────────────────────────────────

    /**
     * List transactions with optional filters.
     *
     * @param status     optional status filter
     * @param merchantId optional merchant filter
     * @param dateFrom   optional start date (ISO format)
     * @param dateTo     optional end date (ISO format)
     * @param page       page number (0-indexed, default 0)
     * @param size       page size (default 20)
     * @param sortBy     sort field (default "createdAt")
     * @param direction  sort direction (default "desc")
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Transaction>>> getAllTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> result;

        // Apply filters in priority order
        if (merchantId != null && status != null) {
            result = transactionService.getTransactionsByMerchantAndStatus(merchantId, status, pageable);
        } else if (merchantId != null && dateFrom != null && dateTo != null) {
            result = transactionService.getTransactionsByMerchantAndDateRange(merchantId, dateFrom, dateTo, pageable);
        } else if (merchantId != null) {
            result = transactionService.getTransactionsByMerchant(merchantId, pageable);
        } else if (status != null) {
            result = transactionService.getTransactionsByStatus(status, pageable);
        } else if (dateFrom != null && dateTo != null) {
            result = transactionService.getTransactionsByDateRange(dateFrom, dateTo, pageable);
        } else {
            result = transactionService.getAllTransactions(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", result));
    }

    /**
     * Get a single transaction by its UUID-based transactionId.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionById(
            @PathVariable String transactionId) {
        log.debug("GET /api/transactions/{}", transactionId);
        Transaction transaction = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", transaction));
    }

    /**
     * Get all transactions for a specific merchant (paginated).
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getTransactionsByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> transactions = transactionService.getTransactionsByMerchant(merchantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Merchant transactions retrieved successfully", transactions));
    }

    // ────────────────────────────────────────────────────────────────
    //  Analytics Endpoints
    // ────────────────────────────────────────────────────────────────

    /**
     * Global transaction statistics (counts, rates, revenue).
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TransactionStatsDTO>> getTransactionStats() {
        log.debug("GET /api/transactions/stats");
        TransactionStatsDTO stats = transactionService.getTransactionStats();
        return ResponseEntity.ok(ApiResponse.success("Transaction statistics retrieved successfully", stats));
    }

    /**
     * Per-merchant transaction statistics.
     */
    @GetMapping("/stats/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<TransactionStatsDTO>> getTransactionStatsByMerchant(
            @PathVariable Long merchantId) {
        log.debug("GET /api/transactions/stats/merchant/{}", merchantId);
        TransactionStatsDTO stats = transactionService.getTransactionStatsByMerchant(merchantId);
        return ResponseEntity.ok(ApiResponse.success("Merchant transaction statistics retrieved successfully", stats));
    }

    /**
     * Daily transaction volume for chart rendering.
     *
     * @param days number of past days to include (default 7)
     * @return list of {date, count} entries
     */
    @GetMapping("/volume/daily")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailyTransactionVolume(
            @RequestParam(defaultValue = "7") int days) {
        log.debug("GET /api/transactions/volume/daily?days={}", days);
        List<Map<String, Object>> volume = transactionService.getDailyTransactionVolume(days);
        return ResponseEntity.ok(ApiResponse.success("Daily transaction volume retrieved successfully", volume));
    }

    /**
     * Payment method distribution for pie-chart rendering.
     *
     * @return map of PaymentMethod name → transaction count
     */
    @GetMapping("/distribution/payment-method")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPaymentMethodDistribution() {
        log.debug("GET /api/transactions/distribution/payment-method");
        Map<String, Long> distribution = transactionService.getPaymentMethodDistribution();
        return ResponseEntity.ok(ApiResponse.success("Payment method distribution retrieved successfully", distribution));
    }
}
