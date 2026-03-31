package com.antigravity.fraud.service.detection;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * FraudAnnotationChain - orchestrates execution of all enabled rules.
 * Implements Chain of Responsibility pattern: rules run sequentially,
 * each mutating the context with reason codes and breakdown items.
 */
@Component
public class FraudAnnotationChain {

    private final List<FraudRule> rules;

    /**
     * Constructor - auto-wired list of all FraudRule beans.
     *
     * @param rules all FraudRule implementations in the Spring context
     */
    public FraudAnnotationChain(List<FraudRule> rules) {
        this.rules = rules;
    }

    /**
     * Run all enabled rules against the context.
     *
     * @param context the rule context to mutate
     */
    public void annotate(RuleContext context) {
        for (FraudRule rule : rules) {
            if (rule.isEnabled()) {
                rule.annotate(context);
            }
        }
    }
}
