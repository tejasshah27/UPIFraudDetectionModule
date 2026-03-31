package com.antigravity.fraud.controller;

import com.antigravity.fraud.dto.FraudAnalysisResponse;
import com.antigravity.fraud.dto.TransactionRequest;
import com.antigravity.fraud.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<FraudAnalysisResponse> analyze(
            @Valid @RequestBody TransactionRequest request) {
        FraudAnalysisResponse response = transactionService.analyze(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<FraudAnalysisResponse> submit(
            @Valid @RequestBody TransactionRequest request) {
        FraudAnalysisResponse response = transactionService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudAnalysisResponse> getTransaction(
            @PathVariable String id) {
        Optional<FraudAnalysisResponse> response = transactionService.getTransaction(id);
        if (response.isPresent()) {
            return ResponseEntity.ok(response.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FraudAnalysisResponse>> getUserTransactions(
            @PathVariable String userId) {
        List<FraudAnalysisResponse> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
}
