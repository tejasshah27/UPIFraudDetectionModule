package com.antigravity.fraud.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * FraudEvaluation domain model - represents the complete audit record of a fraud analysis.
 */
@Document(collection = "fraud_evaluations")
public class FraudEvaluation {

    @Id
    private String id;

    private String txId;
    private double riskScore;
    private String decision;
    private String scoringEngine;
    private List<String> reasons;
    private List<RuleBreakdownItem> ruleBreakdown;
    private LocalDateTime evaluatedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * No-args constructor for MongoDB.
     */
    public FraudEvaluation() {
        this.reasons = new ArrayList<>();
        this.ruleBreakdown = new ArrayList<>();
    }

    /**
     * All-args constructor.
     */
    public FraudEvaluation(String id, String txId, double riskScore, String decision,
            String scoringEngine, List<String> reasons, List<RuleBreakdownItem> ruleBreakdown,
            LocalDateTime evaluatedAt, LocalDateTime createdAt) {
        this.id = id;
        this.txId = txId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.scoringEngine = scoringEngine;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.ruleBreakdown = ruleBreakdown != null ? ruleBreakdown : new ArrayList<>();
        this.evaluatedAt = evaluatedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getScoringEngine() {
        return scoringEngine;
    }

    public void setScoringEngine(String scoringEngine) {
        this.scoringEngine = scoringEngine;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    public List<RuleBreakdownItem> getRuleBreakdown() {
        return ruleBreakdown;
    }

    public void setRuleBreakdown(List<RuleBreakdownItem> ruleBreakdown) {
        this.ruleBreakdown = ruleBreakdown != null ? ruleBreakdown : new ArrayList<>();
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FraudEvaluation{" +
                "id='" + id + '\'' +
                ", txId='" + txId + '\'' +
                ", riskScore=" + riskScore +
                ", decision='" + decision + '\'' +
                ", scoringEngine='" + scoringEngine + '\'' +
                ", reasons=" + reasons +
                ", ruleBreakdown=" + ruleBreakdown +
                ", evaluatedAt=" + evaluatedAt +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Inner static class representing a single rule breakdown item.
     */
    public static class RuleBreakdownItem {
        private String rule;
        private boolean triggered;
        private String detail;

        /**
         * No-args constructor.
         */
        public RuleBreakdownItem() {
        }

        /**
         * All-args constructor.
         */
        public RuleBreakdownItem(String rule, boolean triggered, String detail) {
            this.rule = rule;
            this.triggered = triggered;
            this.detail = detail;
        }

        // Getters and Setters

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public boolean isTriggered() {
            return triggered;
        }

        public void setTriggered(boolean triggered) {
            this.triggered = triggered;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        @Override
        public String toString() {
            return "RuleBreakdownItem{" +
                    "rule='" + rule + '\'' +
                    ", triggered=" + triggered +
                    ", detail='" + detail + '\'' +
                    '}';
        }
    }
}
