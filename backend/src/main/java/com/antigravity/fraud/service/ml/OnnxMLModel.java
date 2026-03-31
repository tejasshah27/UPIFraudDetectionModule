package com.antigravity.fraud.service.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Concrete implementation of the ML Model using Microsoft's ONNX Runtime.
 * 
 * <p>This class wraps the native C++ ONNX execution environment to run XGBoost (or any compatible
 * scikit-learn/neural network) inferences at millisecond speeds. It manages the tensor memory 
 * allocations strictly to prevent memory leaks during heavy fraud analysis traffic.</p>
 */
public class OnnxMLModel implements MLModel {

    private static final Logger log = LoggerFactory.getLogger(OnnxMLModel.class);

    private static final String[] FEATURE_NAMES = {
            "normalizedAmount",
            "hourOfDay",
            "txTypeEncoded",
            "mccRiskBucket",
            "isNewIp",
            "isNewCity",
            "velocityCount",
            "amountZScore"
    };

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String modelId;
    private final String modelVersion = "1.0";
    private final LocalDateTime loadedAt;

    public OnnxMLModel(String modelPath, String modelId) {
        this.modelId = modelId;
        this.loadedAt = LocalDateTime.now();
        try {
            this.env = OrtEnvironment.getEnvironment();
            this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
            log.info("ONNX session created for model '{}' from path: {}", modelId, modelPath);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model from path: " + modelPath, e);
        }
    }

    @Override
    public MLPrediction predict(float[] features) {
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, new float[][]{features});
             OrtSession.Result result = session.run(Collections.singletonMap("float_input", inputTensor))) {

            // Output at index 1 is the probability map (shape: [1][2])
            OnnxTensor probTensor = (OnnxTensor) result.get(1);
            float[][] probs = (float[][]) probTensor.getValue();
            double fraudProbability = probs[0][1];
            return new MLPrediction(fraudProbability, modelId);

        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed for model: " + modelId, e);
        }
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public String getModelVersion() {
        return modelVersion;
    }

    @Override
    public ModelMetadata getMetadata() {
        return new ModelMetadata(modelId, modelVersion, "XGBoost", FEATURE_NAMES, loadedAt);
    }

    public void close() {
        try {
            session.close();
            env.close();
            log.info("ONNX session closed for model '{}'", modelId);
        } catch (OrtException e) {
            log.warn("Error closing ONNX session for model '{}': {}", modelId, e.getMessage());
        }
    }
}
