package com.antigravity.fraud.service.detection.rules;

import com.antigravity.fraud.config.RuleConfig;
import com.antigravity.fraud.service.detection.FraudRule;
import com.antigravity.fraud.service.detection.RuleContext;
import org.springframework.stereotype.Component;

/**
 * OddHourRule - flags transactions occurring during unusual hours (1:00-5:59 AM by default).
 */
@Component
public class OddHourRule implements FraudRule {

    private final RuleConfig config;

    public OddHourRule(RuleConfig config) {
        this.config = config;
    }

    @Override
    public String getRuleName() {
        return "OddHour";
    }

    @Override
    public boolean isEnabled() {
        return config.getOddHour().isEnabled();
    }

    @Override
    public void annotate(RuleContext context) {
        int hour = context.getTransaction().getTimestamp().getHour();
        int riskStartHour = config.getOddHour().getRiskStartHour();
        int riskEndHour = config.getOddHour().getRiskEndHour();

        if (hour >= riskStartHour && hour <= riskEndHour) {
            context.addReason("odd_hour_transaction");
            context.addBreakdown("OddHour", true,
                    "Transaction at hour " + hour + " (risk window: " + riskStartHour + "-" + riskEndHour + ")");
        } else {
            context.addBreakdown("OddHour", false, "Transaction at normal hour " + hour);
        }
    }
}
