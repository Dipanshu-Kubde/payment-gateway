package com.paymentgateway.fraud.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.common.dto.FraudCheckResult;
import com.paymentgateway.fraud.entity.FraudRecord;
import com.paymentgateway.fraud.service.FraudDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for fraud detection operations.
 * Provides endpoints for viewing fraud results, managing rules,
 * and manual review workflows.
 */
@RestController
@RequestMapping("/api/fraud")
@Slf4j
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/check/{transactionId}")
    public ResponseEntity<ApiResponse<FraudCheckResult>> getFraudResult(
            @PathVariable String transactionId) {
        FraudCheckResult result = fraudDetectionService.getFraudResult(transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveRules() {
        List<Map<String, Object>> rules = fraudDetectionService.getActiveRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFraudStats() {
        Map<String, Object> stats = fraudDetectionService.getFraudStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/flagged")
    public ResponseEntity<ApiResponse<Page<FraudRecord>>> getFlaggedTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudRecord> flagged = fraudDetectionService.getFlaggedTransactions(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(flagged));
    }

    @PostMapping("/review/{transactionId}/approve")
    public ResponseEntity<ApiResponse<FraudRecord>> approveTransaction(
            @PathVariable String transactionId) {
        log.info("Manual approval for transaction: {}", transactionId);
        FraudRecord record = fraudDetectionService.approveTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction approved", record));
    }

    @PostMapping("/review/{transactionId}/reject")
    public ResponseEntity<ApiResponse<FraudRecord>> rejectTransaction(
            @PathVariable String transactionId) {
        log.info("Manual rejection for transaction: {}", transactionId);
        FraudRecord record = fraudDetectionService.rejectTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success("Transaction rejected", record));
    }
}
