package com.antigravity.fraud.controller;

import com.antigravity.fraud.service.ml.MLModelRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/model")
public class MLModelController {

    private final MLModelRegistry modelRegistry;

    public MLModelController(MLModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @GetMapping("/info")
    public ResponseEntity<Object> getModelInfo() {
        if (modelRegistry.getActiveModel().isPresent()) {
            Object metadata = modelRegistry.getActiveModel().get().getMetadata();
            return ResponseEntity.ok(metadata);
        }
        return ResponseEntity.ok(Map.of("status", "no model loaded", "fallback", "rules-only"));
    }

    @PostMapping("/reload")
    public ResponseEntity<Object> reloadModel(
            @RequestParam String path) {
        try {
            Object metadata = modelRegistry.reloadModel(path);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
