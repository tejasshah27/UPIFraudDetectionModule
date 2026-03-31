package com.antigravity.fraud.service.detection;

/**
 * FraudRule - interface for rule implementations.
 * Rules annotate context with reason codes and breakdown items; they do NOT score.
 */
public interface FraudRule {

    /**
     * Annotate the context with reason codes and rule breakdown items.
     *
     * @param context the rule context to mutate
     */
    void annotate(RuleContext context);

    /**
     * Get the human-readable name of this rule.
     *
     * @return the rule name
     */
    String getRuleName();

    /**
     * Check if this rule is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
}
