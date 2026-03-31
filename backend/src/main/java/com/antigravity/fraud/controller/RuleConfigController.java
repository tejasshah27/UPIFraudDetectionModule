package com.antigravity.fraud.controller;

import com.antigravity.fraud.config.RuleConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleConfigController {

    private final RuleConfig ruleConfig;

    public RuleConfigController(RuleConfig ruleConfig) {
        this.ruleConfig = ruleConfig;
    }

    @GetMapping
    public ResponseEntity<RuleConfig> getRules() {
        return ResponseEntity.ok(ruleConfig);
    }

    @PutMapping
    public ResponseEntity<RuleConfig> updateRules(
            @RequestBody RuleConfig updatedConfig) {
        ruleConfig.setAmountThreshold(updatedConfig.getAmountThreshold());
        ruleConfig.setOddHour(updatedConfig.getOddHour());
        ruleConfig.setVelocity(updatedConfig.getVelocity());
        ruleConfig.setLocationAnomaly(updatedConfig.getLocationAnomaly());
        return ResponseEntity.ok(ruleConfig);
    }
}
