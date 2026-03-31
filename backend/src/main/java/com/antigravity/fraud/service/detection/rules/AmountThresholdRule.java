package com.antigravity.fraud.service.detection.rules;

import com.antigravity.fraud.config.RuleConfig;
import com.antigravity.fraud.service.detection.FraudRule;
import com.antigravity.fraud.service.detection.RuleContext;
import org.springframework.stereotype.Component;

/**
 * AmountThresholdRule - flags transactions exceeding day/night limits or using high-risk MCCs.
 */
@Component
public class AmountThresholdRule implements FraudRule {

    private final RuleConfig config;

    public AmountThresholdRule(RuleConfig config) {
        this.config = config;
    }

    @Override
    public String getRuleName() {
        return "AmountThreshold";
    }

    @Override
    public boolean isEnabled() {
        return config.getAmountThreshold().isEnabled();
    }

    @Override
    public void annotate(RuleContext context) {
        double amount = context.getTransaction().getAmount();
        int hour = context.getTransaction().getTimestamp().getHour();

        // Determine limit based on time of day (22:00-05:59 is night)
        double limit;
        if (hour >= 22 || hour < 6) {
            limit = config.getAmountThreshold().getNightMax();
        } else {
            limit = config.getAmountThreshold().getDayMax();
        }

        // Check amount against limit
        if (amount > limit) {
            context.addReason("amount_exceeds_threshold");
            context.addBreakdown("AmountThreshold", true,
                    "Amount " + amount + " exceeds limit " + limit);
        } else {
            context.addBreakdown("AmountThreshold", false, "Amount within threshold");
        }

        // Check high-risk MCC
        String mcc = context.getTransaction().getMccCode();
        if (mcc != null && config.getAmountThreshold().getHighRiskMcc().contains(mcc)) {
            context.addReason("high_risk_mcc");
            context.addBreakdown("HighRiskMcc", true, "MCC " + mcc + " is high risk");
        }
    }
}
