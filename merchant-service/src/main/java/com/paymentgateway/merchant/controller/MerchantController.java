package com.paymentgateway.merchant.controller;

import com.paymentgateway.common.dto.ApiResponse;
import com.paymentgateway.common.dto.AuthResponse;
import com.paymentgateway.common.dto.MerchantDTO;
import com.paymentgateway.common.dto.MerchantLoginRequest;
import com.paymentgateway.common.dto.MerchantRegistrationRequest;
import com.paymentgateway.merchant.dto.ApiKeyResponse;
import com.paymentgateway.merchant.dto.DashboardSummary;
import com.paymentgateway.merchant.dto.UpdateMerchantRequest;
import com.paymentgateway.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * POST /api/merchants/register
     * Register a new merchant account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantDTO>> register(
            @Valid @RequestBody MerchantRegistrationRequest request) {
        log.info("Merchant registration request for: {}", request.getEmail());
        MerchantDTO merchant = merchantService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant registered successfully. Save your API keys — they will NOT be shown again.", merchant));
    }

    /**
     * POST /api/merchants/login
     * Authenticate and receive a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody MerchantLoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());
        AuthResponse authResponse = merchantService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * GET /api/merchants/{id}
     * Retrieve merchant profile by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantDTO>> getMerchant(@PathVariable Long id) {
        MerchantDTO merchant = merchantService.getMerchantById(id);
        return ResponseEntity.ok(ApiResponse.success(merchant));
    }

    /**
     * PUT /api/merchants/{id}
     * Update merchant profile.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantDTO>> updateMerchant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMerchantRequest request) {
        log.info("Update request for merchant ID: {}", id);
        MerchantDTO updated = merchantService.updateMerchant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Merchant updated successfully", updated));
    }

    /**
     * POST /api/merchants/{id}/regenerate-keys
     * Regenerate API key/secret pair. Old keys become invalid.
     */
    @PostMapping("/{id}/regenerate-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> regenerateApiKeys(@PathVariable Long id) {
        log.info("API key regeneration request for merchant ID: {}", id);
        ApiKeyResponse response = merchantService.regenerateApiKeys(id);
        return ResponseEntity.ok(ApiResponse.success("API keys regenerated successfully", response));
    }

    /**
     * GET /api/merchants/{id}/dashboard
     * Retrieve merchant dashboard summary.
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboard(@PathVariable Long id) {
        DashboardSummary summary = merchantService.getDashboardSummary(id);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
