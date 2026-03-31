package com.antigravity.fraud.service.detection;

import com.antigravity.fraud.model.FraudEvaluation;
import com.antigravity.fraud.model.Transaction;
import com.antigravity.fraud.model.UserProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * RuleContext - carries all data a rule needs to make decisions.
 * Mutable: rules append reasons and rule breakdown items.
 */
public class RuleContext {

    private final Transaction transaction;
    private final UserProfile userProfile;
    private final int recentTxCount;
    private final List<String> triggeredReasons;
    private final List<FraudEvaluation.RuleBreakdownItem> ruleBreakdown;

    /**
     * Constructor.
     *
     * @param transaction     the current transaction being evaluated
     * @param userProfile     the sender's behavioral profile
     * @param recentTxCount   number of transactions in the velocity window
     */
    public RuleContext(Transaction transaction, UserProfile userProfile, int recentTxCount) {
        this.transaction = transaction;
        this.userProfile = userProfile;
        this.recentTxCount = recentTxCount;
        this.triggeredReasons = new ArrayList<>();
        this.ruleBreakdown = new ArrayList<>();
    }

    // Getters

    public Transaction getTransaction() {
        return transaction;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public int getRecentTxCount() {
        return recentTxCount;
    }

    public List<String> getTriggeredReasons() {
        return triggeredReasons;
    }

    public List<FraudEvaluation.RuleBreakdownItem> getRuleBreakdown() {
        return ruleBreakdown;
    }

    // Mutation methods

    /**
     * Add a reason code to the triggered reasons list.
     *
     * @param reason the reason code (e.g. "velocity_breach")
     */
    public void addReason(String reason) {
        triggeredReasons.add(reason);
    }

    /**
     * Add a rule breakdown item to the breakdown list.
     *
     * @param ruleName the name of the rule
     * @param triggered whether the rule was triggered
     * @param detail    descriptive text about the rule result
     */
    public void addBreakdown(String ruleName, boolean triggered, String detail) {
        ruleBreakdown.add(new FraudEvaluation.RuleBreakdownItem(ruleName, triggered, detail));
    }
}
