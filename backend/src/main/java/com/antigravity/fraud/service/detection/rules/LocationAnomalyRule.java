package com.antigravity.fraud.service.detection.rules;

import com.antigravity.fraud.config.RuleConfig;
import com.antigravity.fraud.service.detection.FraudRule;
import com.antigravity.fraud.service.detection.RuleContext;
import org.springframework.stereotype.Component;

/**
 * LocationAnomalyRule - flags transactions from new cities or IP addresses.
 */
@Component
public class LocationAnomalyRule implements FraudRule {

    private final RuleConfig config;

    public LocationAnomalyRule(RuleConfig config) {
        this.config = config;
    }

    @Override
    public String getRuleName() {
        return "LocationAnomaly";
    }

    @Override
    public boolean isEnabled() {
        return config.getLocationAnomaly().isEnabled();
    }

    @Override
    public void annotate(RuleContext context) {
        String city = context.getTransaction().getCity();
        String ip = context.getTransaction().getIpAddress();

        boolean isNewCity = !context.getUserProfile().getKnownCities().contains(city);
        boolean isNewIp = !context.getUserProfile().getKnownIpAddresses().contains(ip);

        if (isNewCity) {
            context.addReason("new_city");
            context.addBreakdown("LocationAnomaly", true, "New city: " + city);
        }

        if (isNewIp) {
            context.addReason("new_ip");
            context.addBreakdown("LocationAnomaly", true, "New IP address: " + ip);
        }

        if (!isNewCity && !isNewIp) {
            context.addBreakdown("LocationAnomaly", false, "Known location and IP");
        }
    }
}
