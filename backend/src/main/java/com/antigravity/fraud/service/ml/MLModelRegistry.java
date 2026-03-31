package com.antigravity.fraud.service.ml;

import com.antigravity.fraud.config.MLModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Thread-safe registry holding the currently active Machine Learning model in memory.
 * 
 * <p>Provides "Hot-Swapping" capabilities. An administrator can upload a newly trained 
 * .onnx model safely without risking downtime or requiring a Spring Boot server restart.</p>
 */
@Component
public class MLModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(MLModelRegistry.class);

    private volatile MLModel activeModel;

    private final MLModelConfig config;
    private final ResourceLoader resourceLoader;

    public MLModelRegistry(MLModelConfig config, ResourceLoader resourceLoader) {
        this.config = config;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadModel() {
        try {
            var resource = resourceLoader.getResource(config.getModelPath());
            String absolutePath = resource.getFile().getAbsolutePath();
            activeModel = new OnnxMLModel(absolutePath, "fraud-model-v1");
            log.info("ML model loaded: {}", activeModel.getMetadata());
        } catch (Exception e) {
            if (config.isFallbackToRules()) {
                log.warn("ML model failed to load — falling back to rules-only scoring. Cause: {}", e.getMessage());
                activeModel = null;
            } else {
                throw new RuntimeException("ML model failed to load and fallback-to-rules is disabled", e);
            }
        }
    }

    public Optional<MLModel> getActiveModel() {
        return Optional.ofNullable(activeModel);
    }

    public ModelMetadata reloadModel(String newPath) {
        OnnxMLModel newModel = new OnnxMLModel(newPath, "fraud-model-v1");
        MLModel old = this.activeModel;
        this.activeModel = newModel;
        if (old instanceof OnnxMLModel onnxOld) {
            onnxOld.close();
        }
        ModelMetadata metadata = newModel.getMetadata();
        log.info("ML model hot-swapped: {}", metadata);
        return metadata;
    }
}
