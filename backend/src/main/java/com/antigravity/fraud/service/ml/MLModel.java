package com.antigravity.fraud.service.ml;

public interface MLModel {

    MLPrediction predict(float[] features);

    String getModelId();

    String getModelVersion();

    ModelMetadata getMetadata();
}
