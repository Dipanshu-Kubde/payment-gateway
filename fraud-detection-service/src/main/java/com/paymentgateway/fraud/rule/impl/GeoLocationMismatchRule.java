package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <b>Rule: GEO_LOCATION_MISMATCH</b> — Flags transactions where the customer's IP
 * geolocation does not match their card-issuing country.
 *
 * <p>Cross-border mismatches are one of the strongest fraud signals. A legitimate
 * cardholder in India (IP → IN) using a card issued in India (BIN → IN) is normal,
 * but someone in Nigeria (IP → NG) using the same card is highly suspicious.
 *
 * <h3>Simulation</h3>
 * <p>In production, this rule would call an IP geolocation service (e.g., MaxMind GeoIP2).
 * For demonstration purposes, it uses a simple first-octet-to-country mapping:
 * <pre>
 *   1.x.x.x  → US      103.x.x.x → IN
 *   2.x.x.x  → EU      41.x.x.x  → NG
 *   5.x.x.x  → RU      196.x.x.x → ZA
 *   14.x.x.x → CN      177.x.x.x → BR
 *   27.x.x.x → AU      185.x.x.x → IR
 * </pre>
 */
@Component
public class GeoLocationMismatchRule implements FraudRule {

    /**
     * Simulated IP-to-country mapping based on first octet.
     * In production, replace with a real GeoIP lookup.
     */
    private static final Map<String, String> IP_COUNTRY_MAP = Map.ofEntries(
            Map.entry("1", "US"),
            Map.entry("2", "EU"),
            Map.entry("3", "US"),
            Map.entry("5", "RU"),
            Map.entry("8", "GB"),
            Map.entry("14", "CN"),
            Map.entry("27", "AU"),
            Map.entry("41", "NG"),
            Map.entry("43", "JP"),
            Map.entry("49", "KR"),
            Map.entry("61", "AU"),
            Map.entry("77", "RU"),
            Map.entry("80", "DE"),
            Map.entry("81", "JP"),
            Map.entry("103", "IN"),
            Map.entry("106", "IN"),
            Map.entry("110", "IN"),
            Map.entry("117", "CN"),
            Map.entry("154", "NG"),
            Map.entry("169", "US"),
            Map.entry("172", "US"),
            Map.entry("177", "BR"),
            Map.entry("185", "IR"),
            Map.entry("192", "US"),
            Map.entry("196", "ZA"),
            Map.entry("197", "EG"),
            Map.entry("202", "IN"),
            Map.entry("203", "AU"),
            Map.entry("212", "TR")
    );

    @Override
    public String getName() {
        return "GEO_LOCATION_MISMATCH";
    }

    @Override
    public String getDescription() {
        return "Detects mismatch between customer IP geolocation and card-issuing country (BIN country).";
    }

    @Override
    public int getMaxScore() {
        return 15;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        String cardBinCountry = request.getCardBinCountry();
        String customerIp = request.getCustomerIp();

        // Cannot evaluate without both data points
        if (cardBinCountry == null || cardBinCountry.isBlank()) {
            return FraudRuleResult.clean("Card BIN country not provided — skipping geo check.");
        }
        if (customerIp == null || customerIp.isBlank()) {
            return FraudRuleResult.clean("Customer IP not provided — skipping geo check.");
        }

        String ipCountry = resolveCountryFromIp(customerIp);
        if (ipCountry == null) {
            return FraudRuleResult.clean(
                    String.format("Could not resolve country for IP %s — skipping geo check.", customerIp));
        }

        if (!ipCountry.equalsIgnoreCase(cardBinCountry.trim())) {
            return FraudRuleResult.flag(15,
                    String.format("Geo mismatch: IP %s resolved to %s, but card BIN country is %s.",
                            customerIp, ipCountry, cardBinCountry));
        }

        return FraudRuleResult.clean(
                String.format("Geo match: IP country (%s) matches card BIN country (%s).", ipCountry, cardBinCountry));
    }

    /**
     * Simulates an IP-to-country lookup using the first octet.
     * Replace with MaxMind GeoIP2 or similar in production.
     */
    private String resolveCountryFromIp(String ip) {
        try {
            String firstOctet = ip.split("\\.")[0];
            return IP_COUNTRY_MAP.getOrDefault(firstOctet, null);
        } catch (Exception e) {
            return null;
        }
    }
}
