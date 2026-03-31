package com.antigravity.fraud.service;

import com.antigravity.fraud.config.MLModelConfig;
import com.antigravity.fraud.dto.FraudAnalysisResponse;
import com.antigravity.fraud.dto.TransactionRequest;
import com.antigravity.fraud.model.FraudEvaluation;
import com.antigravity.fraud.model.Transaction;
import com.antigravity.fraud.model.UserProfile;
import com.antigravity.fraud.repository.FraudEvaluationRepository;
import com.antigravity.fraud.repository.TransactionRepository;
import com.antigravity.fraud.service.detection.FraudAnnotationChain;
import com.antigravity.fraud.service.detection.RuleContext;
import com.antigravity.fraud.service.ml.FeatureExtractor;
import com.antigravity.fraud.service.ml.MLModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core orchestration service for the UPI Fraud Detection hybrid-scoring engine.
 * 
 * <p>This service controls the primary transaction flow:</p>
 * <ol>
 *   <li>Extracts a mathematical feature vector via FeatureExtractor</li>
 *   <li>Obtains a continuous risk score (0.0 to 1.0) from the authoritative machine learning model (XGBoost ONNX)</li>
 *   <li>Passes the transaction through a suite of business rules (Chain of Responsibility) to generate human-readable reason codes annotations</li>
 *   <li>Persists the transaction and audit evaluation records to MongoDB</li>
 * </ol>
 * 
 * <p>Note: Business rules evaluated here DO NOT calculate or override the numeric risk score (unless the ML model goes completely offline and triggers a fallback).</p>
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository txRepository;
    private final FraudEvaluationRepository evalRepository;
    private final UserProfileService profileService;
    private final MLModelRegistry modelRegistry;
    private final FeatureExtractor featureExtractor;
    private final FraudAnnotationChain annotationChain;
    private final MLModelConfig mlConfig;

    public TransactionService(TransactionRepository txRepository,
                              FraudEvaluationRepository evalRepository,
                              UserProfileService profileService,
                              MLModelRegistry modelRegistry,
                              FeatureExtractor featureExtractor,
                              FraudAnnotationChain annotationChain,
                              MLModelConfig mlConfig) {
        this.txRepository = txRepository;
        this.evalRepository = evalRepository;
        this.profileService = profileService;
        this.modelRegistry = modelRegistry;
        this.featureExtractor = featureExtractor;
        this.annotationChain = annotationChain;
        this.mlConfig = mlConfig;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Analyse a transaction without persisting it or updating user state.
     */
    public FraudAnalysisResponse analyze(TransactionRequest request) {
        log.info("Analyzing transaction for senderId={}, amount={}", request.getSenderId(), request.getAmount());

        Transaction transaction = buildTransaction(request);
        UserProfile profile = profileService.getOrCreateProfile(request.getSenderId());

        int recentTxCount = countRecentTransactions(request.getSenderId(), transaction.getTimestamp());
        float[] features = featureExtractor.extract(transaction, profile, recentTxCount);
        log.debug("Feature vector for txId={}: {}", transaction.getTxId(), Arrays.toString(features));

        double riskScore;
        String scoringEngine;
        var modelOpt = modelRegistry.getActiveModel();
        if (modelOpt.isPresent()) {
            riskScore = modelOpt.get().predict(features).getScore();
            scoringEngine = "ONNX_ML";
        } else {
            riskScore = computeRuleBasedScore(features);
            scoringEngine = "RULES_ONLY";
            log.info("ML model unavailable — falling back to rule-based scoring for txId={}", transaction.getTxId());
        }

        RuleContext ctx = new RuleContext(transaction, profile, recentTxCount);
        annotationChain.annotate(ctx);

        String decision = resolveDecision(riskScore);
        log.info("txId={} | scoringEngine={} | riskScore={} | decision={}",
                transaction.getTxId(), scoringEngine, riskScore, decision);

        return buildResponse(transaction.getTxId(), riskScore, decision, scoringEngine, ctx);
    }

    /**
     * Analyse, persist the transaction, persist the evaluation, and update the user profile.
     */
    public FraudAnalysisResponse submit(TransactionRequest request) {
        log.info("Submitting transaction for senderId={}, amount={}", request.getSenderId(), request.getAmount());

        // Step 1 — build and immediately persist
        Transaction transaction = buildTransaction(request);
        txRepository.save(transaction);

        UserProfile profile = profileService.getOrCreateProfile(request.getSenderId());

        int recentTxCount = countRecentTransactions(request.getSenderId(), transaction.getTimestamp());
        float[] features = featureExtractor.extract(transaction, profile, recentTxCount);
        log.debug("Feature vector for txId={}: {}", transaction.getTxId(), Arrays.toString(features));

        double riskScore;
        String scoringEngine;
        var modelOpt = modelRegistry.getActiveModel();
        if (modelOpt.isPresent()) {
            riskScore = modelOpt.get().predict(features).getScore();
            scoringEngine = "ONNX_ML";
        } else {
            riskScore = computeRuleBasedScore(features);
            scoringEngine = "RULES_ONLY";
            log.info("ML model unavailable — falling back to rule-based scoring for txId={}", transaction.getTxId());
        }

        RuleContext ctx = new RuleContext(transaction, profile, recentTxCount);
        annotationChain.annotate(ctx);

        String decision = resolveDecision(riskScore);
        log.info("txId={} | scoringEngine={} | riskScore={} | decision={}",
                transaction.getTxId(), scoringEngine, riskScore, decision);

        FraudAnalysisResponse response = buildResponse(transaction.getTxId(), riskScore, decision, scoringEngine, ctx);

        // Persist evaluation audit record
        FraudEvaluation eval = new FraudEvaluation();
        eval.setTxId(transaction.getTxId());
        eval.setRiskScore(riskScore);
        eval.setDecision(decision);
        eval.setScoringEngine(scoringEngine);
        eval.setReasons(ctx.getTriggeredReasons());
        eval.setRuleBreakdown(ctx.getRuleBreakdown());
        eval.setEvaluatedAt(LocalDateTime.now());
        evalRepository.save(eval);

        // Update user behavioural baseline
        profileService.updateProfile(profile, transaction);

        // Finalise transaction status
        transaction.setStatus(decision.equals("REJECT") ? "BLOCKED" : "COMPLETED");
        txRepository.save(transaction);

        return response;
    }

    /**
     * Retrieve a previously submitted transaction together with its fraud evaluation.
     */
    public Optional<FraudAnalysisResponse> getTransaction(String txId) {
        Optional<Transaction> txOpt = txRepository.findByTxId(txId);
        if (txOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<FraudEvaluation> evalOpt = evalRepository.findByTxId(txId);
        if (evalOpt.isPresent()) {
            return Optional.of(FraudAnalysisResponse.from(evalOpt.get()));
        }
        // Transaction exists but no evaluation record — return minimal response
        FraudAnalysisResponse minimal = new FraudAnalysisResponse();
        minimal.setTxId(txId);
        return Optional.of(minimal);
    }

    /**
     * Return the full fraud analysis history for a user, most recent first.
     */
    public List<FraudAnalysisResponse> getUserTransactions(String userId) {
        List<Transaction> transactions = txRepository.findBySenderIdOrderByTimestampDesc(userId);
        return transactions.stream()
                .map(tx -> {
                    Optional<FraudEvaluation> evalOpt = evalRepository.findByTxId(tx.getTxId());
                    if (evalOpt.isPresent()) {
                        return FraudAnalysisResponse.from(evalOpt.get());
                    }
                    FraudAnalysisResponse minimal = new FraudAnalysisResponse();
                    minimal.setTxId(tx.getTxId());
                    return minimal;
                })
                .collect(Collectors.toList());
    }

    /**
     * Return the most recent transactions globally.
     */
    public List<FraudAnalysisResponse> getAllTransactions(int limit) {
        List<Transaction> transactions = txRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp"))
                .stream().limit(limit).collect(Collectors.toList());
        return transactions.stream()
                .map(tx -> {
                    Optional<FraudEvaluation> evalOpt = evalRepository.findByTxId(tx.getTxId());
                    if (evalOpt.isPresent()) {
                        return FraudAnalysisResponse.from(evalOpt.get());
                    }
                    FraudAnalysisResponse minimal = new FraudAnalysisResponse();
                    minimal.setTxId(tx.getTxId());
                    return minimal;
                })
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Transaction buildTransaction(TransactionRequest request) {
        Transaction tx = new Transaction();
        tx.setTxId("TXN_" + UUID.randomUUID().toString());
        tx.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        tx.setSenderId(request.getSenderId());
        tx.setReceiverId(request.getReceiverId());
        tx.setAmount(request.getAmount());
        tx.setTxType(request.getTxType());
        tx.setMccCode(request.getMccCode());
        tx.setIpAddress(request.getIpAddress());
        tx.setCity(request.getCity());
        tx.setCurrency(request.getCurrency());
        tx.setStatus("PENDING");
        return tx;
    }

    private int countRecentTransactions(String senderId, LocalDateTime txTimestamp) {
        LocalDateTime windowStart = txTimestamp.minusMinutes(10);
        return txRepository
                .findBySenderIdAndTimestampBetween(senderId, windowStart, txTimestamp)
                .size();
    }

    private String resolveDecision(double riskScore) {
        double acceptThreshold = mlConfig.getDecisionThresholds().getAccept();
        double reviewThreshold = mlConfig.getDecisionThresholds().getReview();
        if (riskScore < acceptThreshold) return "ACCEPT";
        if (riskScore < reviewThreshold) return "REVIEW";
        return "REJECT";
    }

    /**
     * Fallback scoring when no ONNX model is loaded.
     * Uses a simple weighted sum of the most discriminative features.
     *
     * Feature indices:
     *   4 = isNewIp, 5 = isNewCity, 6 = velocityCount, 0 = normalizedAmount
     */
    private double computeRuleBasedScore(float[] features) {
        double score = 0.3 * features[4]
                + 0.3 * features[5]
                + 0.2 * Math.min(features[6] / 5.0, 1.0)
                + 0.2 * Math.min(features[0] / 5.0, 1.0);
        return Math.max(0.0, Math.min(score, 1.0));
    }

    private FraudAnalysisResponse buildResponse(String txId, double riskScore, String decision,
                                                 String scoringEngine, RuleContext ctx) {
        FraudAnalysisResponse response = new FraudAnalysisResponse();
        response.setTxId(txId);
        response.setRiskScore(riskScore);
        response.setDecision(decision);
        response.setScoringEngine(scoringEngine);
        response.setReasons(ctx.getTriggeredReasons() != null
                ? ctx.getTriggeredReasons() : Collections.emptyList());

        if (ctx.getRuleBreakdown() != null) {
            List<FraudAnalysisResponse.RuleBreakdownItem> breakdown = ctx.getRuleBreakdown().stream()
                    .map(item -> {
                        FraudAnalysisResponse.RuleBreakdownItem dto = new FraudAnalysisResponse.RuleBreakdownItem();
                        dto.setRule(item.getRule());
                        dto.setTriggered(item.isTriggered());
                        dto.setDetail(item.getDetail());
                        return dto;
                    })
                    .collect(Collectors.toList());
            response.setRuleBreakdown(breakdown);
        } else {
            response.setRuleBreakdown(Collections.emptyList());
        }

        return response;
    }
}
