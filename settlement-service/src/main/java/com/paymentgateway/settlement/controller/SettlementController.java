package com.paymentgateway.settlement.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.settlement.entity.MerchantBalance;
import com.paymentgateway.settlement.entity.Settlement;
import com.paymentgateway.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for settlement operations.
 *
 * Provides endpoints for querying settlements, checking merchant balances,
 * and manually triggering settlement payouts.
 */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * List all settlements for a given merchant.
     *
     * @param merchantId the merchant ID
     * @return list of settlement records
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<Settlement>>> getSettlementsByMerchant(
            @PathVariable Long merchantId) {
        log.info("GET /api/settlements/merchant/{}", merchantId);
        List<Settlement> settlements = settlementService.getSettlementsByMerchant(merchantId);
        return ResponseEntity.ok(
                ApiResponse.success("Settlements retrieved successfully", settlements));
    }

    /**
     * Get a specific settlement by its UUID-based settlementId.
     *
     * @param settlementId the settlement UUID
     * @return the settlement record
     */
    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<Settlement>> getSettlementById(
            @PathVariable String settlementId) {
        log.info("GET /api/settlements/{}", settlementId);
        Settlement settlement = settlementService.getSettlementById(settlementId);
        return ResponseEntity.ok(
                ApiResponse.success("Settlement retrieved successfully", settlement));
    }

    /**
     * Get the current balance snapshot for a merchant.
     *
     * @param merchantId the merchant ID
     * @return the merchant balance record
     */
    @GetMapping("/balance/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantBalance>> getMerchantBalance(
            @PathVariable Long merchantId) {
        log.info("GET /api/settlements/balance/{}", merchantId);
        MerchantBalance balance = settlementService.getMerchantBalance(merchantId);
        return ResponseEntity.ok(
                ApiResponse.success("Merchant balance retrieved successfully", balance));
    }

    /**
     * Manually trigger a settlement for a specific merchant.
     * Intended for admin use — bypasses the daily cron schedule.
     *
     * @param merchantId the merchant ID
     * @return the created settlement record
     */
    @PostMapping("/trigger/{merchantId}")
    public ResponseEntity<ApiResponse<Settlement>> triggerManualSettlement(
            @PathVariable Long merchantId) {
        log.info("POST /api/settlements/trigger/{}", merchantId);
        Settlement settlement = settlementService.triggerManualSettlement(merchantId);
        return ResponseEntity.ok(
                ApiResponse.success("Settlement triggered successfully", settlement));
    }
}
