package com.antigravity.fraud.controller;

import com.antigravity.fraud.dto.FraudAnalysisResponse;
import com.antigravity.fraud.service.DataGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/data")
public class DataGeneratorController {

    private final DataGeneratorService dataGeneratorService;

    public DataGeneratorController(DataGeneratorService dataGeneratorService) {
        this.dataGeneratorService = dataGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Object> generateData(
            @RequestParam(defaultValue = "10") int count) {
        if (count < 1 || count > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "count must be between 1 and 1000"));
        }
        List<FraudAnalysisResponse> responses = dataGeneratorService.generate(count);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
