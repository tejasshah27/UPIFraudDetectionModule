package com.antigravity.fraud.service.ml;

public class MLPrediction {

    private final double score;
    private final String modelId;

    public MLPrediction(double score, String modelId) {
        this.score = score;
        this.modelId = modelId;
    }

    public double getScore() {
        return score;
    }

    public String getModelId() {
        return modelId;
    }

    @Override
    public String toString() {
        return "MLPrediction{score=" + score + ", modelId='" + modelId + "'}";
    }
}
