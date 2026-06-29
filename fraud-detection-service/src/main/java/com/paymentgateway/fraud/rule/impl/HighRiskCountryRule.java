package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * <b>Rule: HIGH_RISK_COUNTRY</b> — Flags transactions originating from countries
 * with elevated fraud rates.
 *
 * <p>Certain jurisdictions are statistically associated with higher volumes of
 * payment fraud. This rule checks whether the customer's IP resolves to one of
 * those countries and assigns a risk score accordingly.
 *
 * <p>The high-risk country list is based on industry-standard watchlists and can
 * be externalised to a database or config file in production.
 */
@Component
public class HighRiskCountryRule implements FraudRule {

    /** Countries with elevated fraud activity (ISO 3166-1 alpha-2). */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "NG",   // Nigeria
            "RU",   // Russia
            "IR",   // Iran
            "KP",   // North Korea
            "VN",   // Vietnam
            "ID",   // Indonesia
            "PH",   // Philippines
            "RO",   // Romania
            "UA",   // Ukraine
            "BY"    // Belarus
    );

    /** Simulated IP → country mapping (mirrors GeoLocationMismatchRule). */
    private static final Map<String, String> IP_COUNTRY_MAP = Map.ofEntries(
            Map.entry("1", "US"),
            Map.entry("5", "RU"),
            Map.entry("14", "CN"),
            Map.entry("41", "NG"),
            Map.entry("77", "RU"),
            Map.entry("103", "IN"),
            Map.entry("154", "NG"),
            Map.entry("177", "BR"),
            Map.entry("185", "IR"),
            Map.entry("196", "ZA"),
            Map.entry("202", "IN"),
            Map.entry("212", "TR")
    );

    @Override
    public String getName() {
        return "HIGH_RISK_COUNTRY";
    }

    @Override
    public String getDescription() {
        return "Flags transactions originating from countries with statistically elevated fraud rates.";
    }

    @Override
    public int getMaxScore() {
        return 10;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        String customerIp = request.getCustomerIp();
        if (customerIp == null || customerIp.isBlank()) {
            return FraudRuleResult.clean("Customer IP not provided — skipping country risk check.");
        }

        String country = resolveCountryFromIp(customerIp);
        if (country == null) {
            return FraudRuleResult.clean(
                    String.format("Could not resolve country for IP %s — skipping.", customerIp));
        }

        if (HIGH_RISK_COUNTRIES.contains(country)) {
            return FraudRuleResult.flag(10,
                    String.format("Transaction originates from high-risk country: %s (IP: %s).", country, customerIp));
        }

        return FraudRuleResult.clean(
                String.format("Country %s is not on the high-risk watchlist.", country));
    }

    private String resolveCountryFromIp(String ip) {
        try {
            String firstOctet = ip.split("\\.")[0];
            return IP_COUNTRY_MAP.getOrDefault(firstOctet, null);
        } catch (Exception e) {
            return null;
        }
    }
}
