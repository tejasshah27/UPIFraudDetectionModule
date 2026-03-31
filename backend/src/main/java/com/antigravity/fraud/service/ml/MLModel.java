package com.antigravity.fraud.service.ml;

/**
 * Extensibility contract for all Machine Learning scoring engines.
 * 
 * <p>By adhering to this rigid interface, the backend logic remains decoupled from the specific 
 * AI framework (e.g. XGBoost vs TensorFlow). It expects to receive a normalized array of floats 
 * and return an authoritative MLPrediction containing the risk score.</p>
 */
public interface MLModel {

    MLPrediction predict(float[] features);

    String getModelId();

    String getModelVersion();

    ModelMetadata getMetadata();
}
