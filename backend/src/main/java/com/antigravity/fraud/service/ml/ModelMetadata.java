package com.antigravity.fraud.service.ml;

import java.time.LocalDateTime;

public class ModelMetadata {

    private final String modelId;
    private final String modelVersion;
    private final String algorithm;
    private final String[] featureNames;
    private final LocalDateTime loadedAt;

    public ModelMetadata(String modelId, String modelVersion, String algorithm,
                         String[] featureNames, LocalDateTime loadedAt) {
        this.modelId = modelId;
        this.modelVersion = modelVersion;
        this.algorithm = algorithm;
        this.featureNames = featureNames;
        this.loadedAt = loadedAt;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    @Override
    public String toString() {
        return "ModelMetadata{" +
                "modelId='" + modelId + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", featureNames=" + java.util.Arrays.toString(featureNames) +
                ", loadedAt=" + loadedAt +
                '}';
    }
}
