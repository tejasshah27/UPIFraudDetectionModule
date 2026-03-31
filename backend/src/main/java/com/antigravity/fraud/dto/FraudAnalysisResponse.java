package com.antigravity.fraud.dto;

import com.antigravity.fraud.model.FraudEvaluation;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for fraud analysis response returned to clients.
 */
public class FraudAnalysisResponse {

    private String txId;
    private double riskScore;
    private String decision;
    private String scoringEngine;
    private List<String> reasons;
    private List<RuleBreakdownItem> ruleBreakdown;

    /**
     * No-args constructor.
     */
    public FraudAnalysisResponse() {
        this.reasons = new ArrayList<>();
        this.ruleBreakdown = new ArrayList<>();
    }

    /**
     * Constructor with all fields.
     */
    public FraudAnalysisResponse(String txId, double riskScore, String decision,
            String scoringEngine, List<String> reasons, List<RuleBreakdownItem> ruleBreakdown) {
        this.txId = txId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.scoringEngine = scoringEngine;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
        this.ruleBreakdown = ruleBreakdown != null ? ruleBreakdown : new ArrayList<>();
    }

    // Getters and Setters

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

    /**
     * Factory method to create a FraudAnalysisResponse from a FraudEvaluation domain object.
     */
    public static FraudAnalysisResponse from(FraudEvaluation eval) {
        FraudAnalysisResponse response = new FraudAnalysisResponse();
        response.setTxId(eval.getTxId());
        response.setRiskScore(eval.getRiskScore());
        response.setDecision(eval.getDecision());
        response.setScoringEngine(eval.getScoringEngine());
        response.setReasons(eval.getReasons());

        // Convert domain RuleBreakdownItem to DTO RuleBreakdownItem
        List<RuleBreakdownItem> dtoItems = new ArrayList<>();
        for (FraudEvaluation.RuleBreakdownItem domainItem : eval.getRuleBreakdown()) {
            RuleBreakdownItem dtoItem = new RuleBreakdownItem(
                    domainItem.getRule(),
                    domainItem.isTriggered(),
                    domainItem.getDetail()
            );
            dtoItems.add(dtoItem);
        }
        response.setRuleBreakdown(dtoItems);

        return response;
    }

    @Override
    public String toString() {
        return "FraudAnalysisResponse{" +
                "txId='" + txId + '\'' +
                ", riskScore=" + riskScore +
                ", decision='" + decision + '\'' +
                ", scoringEngine='" + scoringEngine + '\'' +
                ", reasons=" + reasons +
                ", ruleBreakdown=" + ruleBreakdown +
                '}';
    }

    /**
     * Inner static class representing a rule breakdown item in the response.
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
