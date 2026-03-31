package com.antigravity.fraud.service.detection.rules;

import com.antigravity.fraud.config.RuleConfig;
import com.antigravity.fraud.service.detection.FraudRule;
import com.antigravity.fraud.service.detection.RuleContext;
import org.springframework.stereotype.Component;

/**
 * VelocityRule - flags transactions when user exceeds transaction velocity thresholds.
 * Detects rapid-fire transaction patterns (e.g. >5 transactions in 10 minutes).
 */
@Component
public class VelocityRule implements FraudRule {

    private final RuleConfig config;

    public VelocityRule(RuleConfig config) {
        this.config = config;
    }

    @Override
    public String getRuleName() {
        return "VelocityCheck";
    }

    @Override
    public boolean isEnabled() {
        return config.getVelocity().isEnabled();
    }

    @Override
    public void annotate(RuleContext context) {
        int recentCount = context.getRecentTxCount();
        int maxTxInWindow = config.getVelocity().getMaxTxInWindow();
        int windowMinutes = config.getVelocity().getWindowMinutes();

        if (recentCount > maxTxInWindow) {
            context.addReason("velocity_breach");
            context.addBreakdown("VelocityCheck", true,
                    recentCount + " tx in last " + windowMinutes + " min (max: " + maxTxInWindow + ")");
        } else {
            context.addBreakdown("VelocityCheck", false,
                    recentCount + " tx in last " + windowMinutes + " min");
        }
    }
}
