package com.antigravity.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fraud.ml")
public class MLModelConfig {

    private String modelPath = "classpath:models/fraud_model.onnx";
    private boolean fallbackToRules = true;
    private DecisionThresholds decisionThresholds = new DecisionThresholds();

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public boolean isFallbackToRules() { return fallbackToRules; }
    public void setFallbackToRules(boolean fallbackToRules) { this.fallbackToRules = fallbackToRules; }

    public DecisionThresholds getDecisionThresholds() { return decisionThresholds; }
    public void setDecisionThresholds(DecisionThresholds decisionThresholds) { this.decisionThresholds = decisionThresholds; }

    public static class DecisionThresholds {
        private double accept = 0.30;
        private double review = 0.70;

        public double getAccept() { return accept; }
        public void setAccept(double accept) { this.accept = accept; }

        public double getReview() { return review; }
        public void setReview(double review) { this.review = review; }
    }
}
