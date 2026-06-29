package com.paymentgateway.merchant.service;

import com.paymentgateway.common.dto.AuthResponse;
import com.paymentgateway.common.dto.MerchantDTO;
import com.paymentgateway.common.dto.MerchantLoginRequest;
import com.paymentgateway.common.dto.MerchantRegistrationRequest;
import com.paymentgateway.common.enums.MerchantStatus;
import com.paymentgateway.common.exception.ResourceNotFoundException;
import com.paymentgateway.merchant.dto.ApiKeyResponse;
import com.paymentgateway.merchant.dto.DashboardSummary;
import com.paymentgateway.merchant.dto.UpdateMerchantRequest;
import com.paymentgateway.merchant.entity.Merchant;
import com.paymentgateway.merchant.repository.MerchantRepository;
import com.paymentgateway.merchant.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public MerchantService(MerchantRepository merchantRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Register a new merchant. Generates API key/secret pair (Stripe-like format),
     * hashes the password, and returns the MerchantDTO with plaintext API key
     * shown ONCE.
     */
    @Transactional
    public MerchantDTO register(MerchantRegistrationRequest request) {
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Generate Stripe-like API key and secret
        String apiKey = "pk_live_" + UUID.randomUUID().toString().replace("-", "");
        String apiSecret = "sk_live_" + UUID.randomUUID().toString().replace("-", "");

        Merchant merchant = Merchant.builder()
                .businessName(request.getBusinessName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .webhookUrl(request.getWebhookUrl())
                .apiKey(apiKey)
                .apiKeyHash(passwordEncoder.encode(apiKey))
                .apiSecret(apiSecret)
                .apiSecretHash(passwordEncoder.encode(apiSecret))
                .status(MerchantStatus.ACTIVE)
                .build();

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant registered successfully: {} (ID: {})", saved.getBusinessName(), saved.getId());

        // Return DTO with plaintext API key — shown ONCE during registration
        return toDTO(saved, apiKey);
    }

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /**
     * Authenticate merchant credentials and return a JWT.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(MerchantLoginRequest request) {
        Merchant merchant = merchantRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), merchant.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (merchant.getStatus() == MerchantStatus.SUSPENDED) {
            throw new IllegalArgumentException("Merchant account is suspended");
        }

        String token = jwtService.generateToken(merchant);
        log.info("Merchant logged in: {} (ID: {})", merchant.getBusinessName(), merchant.getId());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .merchantId(merchant.getId())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .build();
    }

    // -------------------------------------------------------------------------
    // Profile Management
    // -------------------------------------------------------------------------

    /**
     * Get merchant by ID.
     */
    @Transactional(readOnly = true)
    public MerchantDTO getMerchantById(Long id) {
        Merchant merchant = findMerchantOrThrow(id);
        return toDTO(merchant);
    }

    /**
     * Update merchant profile (business name, phone, webhook URL).
     */
    @Transactional
    public MerchantDTO updateMerchant(Long id, UpdateMerchantRequest request) {
        Merchant merchant = findMerchantOrThrow(id);

        if (request.getBusinessName() != null && !request.getBusinessName().isBlank()) {
            merchant.setBusinessName(request.getBusinessName());
        }
        if (request.getPhone() != null) {
            merchant.setPhone(request.getPhone());
        }
        if (request.getWebhookUrl() != null) {
            merchant.setWebhookUrl(request.getWebhookUrl());
        }

        Merchant updated = merchantRepository.save(merchant);
        log.info("Merchant updated: {} (ID: {})", updated.getBusinessName(), updated.getId());

        return toDTO(updated);
    }

    // -------------------------------------------------------------------------
    // API Key Management
    // -------------------------------------------------------------------------

    /**
     * Regenerate API key/secret pair for a merchant. Old keys are invalidated.
     * Returns the new plaintext keys (shown ONCE).
     */
    @Transactional
    public ApiKeyResponse regenerateApiKeys(Long id) {
        Merchant merchant = findMerchantOrThrow(id);

        String newApiKey = "pk_live_" + UUID.randomUUID().toString().replace("-", "");
        String newApiSecret = "sk_live_" + UUID.randomUUID().toString().replace("-", "");

        merchant.setApiKey(newApiKey);
        merchant.setApiKeyHash(passwordEncoder.encode(newApiKey));
        merchant.setApiSecret(newApiSecret);
        merchant.setApiSecretHash(passwordEncoder.encode(newApiSecret));

        merchantRepository.save(merchant);
        log.info("API keys regenerated for merchant ID: {}", id);

        return ApiKeyResponse.builder()
                .apiKey(newApiKey)
                .apiSecret(newApiSecret)
                .message("New API keys generated. Please store them securely — they will NOT be shown again.")
                .build();
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    /**
     * Return a dashboard summary for the merchant.
     */
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long id) {
        Merchant merchant = findMerchantOrThrow(id);

        return DashboardSummary.builder()
                .merchantId(merchant.getId())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .status(merchant.getStatus())
                .feePercentage(merchant.getFeePercentage())
                .webhookUrl(merchant.getWebhookUrl())
                .apiKeyConfigured(merchant.getApiKey() != null)
                .memberSince(merchant.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Merchant findMerchantOrThrow(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", id));
    }

    /**
     * Convert entity to DTO (without plaintext API key).
     */
    private MerchantDTO toDTO(Merchant merchant) {
        return toDTO(merchant, null);
    }

    /**
     * Convert entity to DTO. If apiKey is provided, it's the plaintext value
     * (used only during registration/regeneration).
     */
    private MerchantDTO toDTO(Merchant merchant, String plaintextApiKey) {
        return MerchantDTO.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .apiKey(plaintextApiKey)
                .webhookUrl(merchant.getWebhookUrl())
                .feePercentage(merchant.getFeePercentage())
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}
