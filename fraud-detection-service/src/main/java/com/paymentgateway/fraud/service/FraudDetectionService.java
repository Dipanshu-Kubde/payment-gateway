package com.paymentgateway.fraud.service;

import com.paymentgateway.common.dto.FraudCheckResult;
import com.paymentgateway.common.enums.RiskLevel;
import com.paymentgateway.fraud.entity.FraudRecord;
import com.paymentgateway.fraud.repository.FraudRecordRepository;
import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core fraud detection engine.
 * Evaluates transactions against all registered FraudRule beans,
 * calculates a composite risk score (0-100), and persists results.
 */
@Service
@Slf4j
public class FraudDetectionService {

    private final List<FraudRule> rules;
    private final FraudRecordRepository fraudRecordRepository;

    public FraudDetectionService(List<FraudRule> rules, FraudRecordRepository fraudRecordRepository) {
        this.rules = rules;
        this.fraudRecordRepository = fraudRecordRepository;
        log.info("Fraud Detection Engine initialized with {} rules: {}",
                rules.size(),
                rules.stream().map(FraudRule::getName).collect(Collectors.joining(", ")));
    }

    /**
     * Evaluate a transaction against all fraud rules and persist the result.
     */
    @Transactional
    public FraudCheckResult evaluateTransaction(FraudCheckRequest request) {
        log.info("Evaluating fraud for transaction: {}, amount: {}",
                request.getTransactionId(), request.getAmount());

        int totalScore = 0;
        List<String> triggeredRules = new ArrayList<>();
        List<String> ruleDetails = new ArrayList<>();

        for (FraudRule rule : rules) {
            try {
                FraudRuleResult result = rule.evaluate(request);
                if (result.isTriggered()) {
                    totalScore += result.getScore();
                    triggeredRules.add(rule.getName());
                    ruleDetails.add(String.format("%s (+%d): %s",
                            rule.getName(), result.getScore(), result.getDetails()));
                    log.info("Rule triggered: {} (+{}) for txn {}",
                            rule.getName(), result.getScore(), request.getTransactionId());
                }
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getName(), e.getMessage());
            }
        }

        // Cap score at 100
        totalScore = Math.min(totalScore, 100);

        RiskLevel riskLevel = determineRiskLevel(totalScore);
        boolean blocked = riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
        String recommendation = generateRecommendation(riskLevel, triggeredRules);

        // Persist fraud record
        FraudRecord record = FraudRecord.builder()
                .transactionId(request.getTransactionId())
                .orderId(request.getOrderId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .riskScore(totalScore)
                .riskLevel(riskLevel)
                .triggeredRules(String.join(",", triggeredRules))
                .recommendation(recommendation)
                .blocked(blocked)
                .build();

        fraudRecordRepository.save(record);

        log.info("Fraud evaluation complete for txn {}: score={}, level={}, blocked={}, rules={}",
                request.getTransactionId(), totalScore, riskLevel, blocked, triggeredRules);

        return FraudCheckResult.builder()
                .transactionId(request.getTransactionId())
                .riskScore(totalScore)
                .riskLevel(riskLevel)
                .triggeredRules(triggeredRules)
                .recommendation(recommendation)
                .blocked(blocked)
                .build();
    }

    @Transactional(readOnly = true)
    public FraudCheckResult getFraudResult(String transactionId) {
        FraudRecord record = fraudRecordRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Fraud record not found for transaction: " + transactionId));

        return FraudCheckResult.builder()
                .transactionId(record.getTransactionId())
                .riskScore(record.getRiskScore())
                .riskLevel(record.getRiskLevel())
                .triggeredRules(record.getTriggeredRules() != null && !record.getTriggeredRules().isEmpty()
                        ? Arrays.asList(record.getTriggeredRules().split(","))
                        : Collections.emptyList())
                .recommendation(record.getRecommendation())
                .blocked(record.getBlocked())
                .build();
    }

    public List<Map<String, Object>> getActiveRules() {
        return rules.stream().map(rule -> {
            Map<String, Object> ruleInfo = new LinkedHashMap<>();
            ruleInfo.put("name", rule.getName());
            ruleInfo.put("description", rule.getDescription());
            ruleInfo.put("maxScore", rule.getMaxScore());
            return ruleInfo;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFraudStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalChecks", fraudRecordRepository.count());
        stats.put("totalBlocked", fraudRecordRepository.countByBlocked(true));
        stats.put("lowRisk", fraudRecordRepository.countByRiskLevel(RiskLevel.LOW));
        stats.put("mediumRisk", fraudRecordRepository.countByRiskLevel(RiskLevel.MEDIUM));
        stats.put("highRisk", fraudRecordRepository.countByRiskLevel(RiskLevel.HIGH));
        stats.put("criticalRisk", fraudRecordRepository.countByRiskLevel(RiskLevel.CRITICAL));

        long total = fraudRecordRepository.count();
        long blocked = fraudRecordRepository.countByBlocked(true);
        stats.put("blockRate", total > 0 ? (double) blocked / total * 100 : 0.0);

        return stats;
    }

    @Transactional(readOnly = true)
    public Page<FraudRecord> getFlaggedTransactions(Pageable pageable) {
        return fraudRecordRepository.findByBlockedTrue(pageable);
    }

    @Transactional
    public FraudRecord approveTransaction(String transactionId) {
        FraudRecord record = fraudRecordRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Fraud record not found: " + transactionId));
        record.setBlocked(false);
        record.setReviewedBy("ADMIN");
        record.setReviewedAt(LocalDateTime.now());
        record.setRecommendation("MANUALLY APPROVED");
        return fraudRecordRepository.save(record);
    }

    @Transactional
    public FraudRecord rejectTransaction(String transactionId) {
        FraudRecord record = fraudRecordRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Fraud record not found: " + transactionId));
        record.setBlocked(true);
        record.setReviewedBy("ADMIN");
        record.setReviewedAt(LocalDateTime.now());
        record.setRecommendation("MANUALLY REJECTED");
        return fraudRecordRepository.save(record);
    }

    private RiskLevel determineRiskLevel(int score) {
        if (score <= 30) return RiskLevel.LOW;
        if (score <= 60) return RiskLevel.MEDIUM;
        if (score <= 80) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    private String generateRecommendation(RiskLevel level, List<String> triggeredRules) {
        return switch (level) {
            case LOW -> "Transaction appears safe. Auto-approved.";
            case MEDIUM -> "Moderate risk detected. Consider additional verification. Rules: " + String.join(", ", triggeredRules);
            case HIGH -> "High risk detected. Transaction blocked pending manual review. Rules: " + String.join(", ", triggeredRules);
            case CRITICAL -> "Critical fraud indicators detected. Transaction auto-blocked. Immediate investigation required. Rules: " + String.join(", ", triggeredRules);
        };
    }
}
