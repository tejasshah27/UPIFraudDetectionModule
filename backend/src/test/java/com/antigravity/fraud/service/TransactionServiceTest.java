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
import com.antigravity.fraud.service.ml.MLModel;
import com.antigravity.fraud.service.ml.MLModelRegistry;
import com.antigravity.fraud.service.ml.MLPrediction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository txRepository;
    @Mock
    private FraudEvaluationRepository evalRepository;
    @Mock
    private UserProfileService profileService;
    @Mock
    private MLModelRegistry modelRegistry;
    @Mock
    private FeatureExtractor featureExtractor;
    @Mock
    private FraudAnnotationChain annotationChain;
    @Mock
    private MLModelConfig mlConfig;

    @Mock
    private MLModel mlModel;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Setup minimal config
        MLModelConfig.DecisionThresholds thresholds = new MLModelConfig.DecisionThresholds();
        thresholds.setAccept(0.3);
        thresholds.setReview(0.7);
        lenient().when(mlConfig.getDecisionThresholds()).thenReturn(thresholds);
    }

    @Test
    void submit_shouldProcessAndPersistUsingMLModel() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setSenderId("user1");
        request.setAmount(100.0);

        UserProfile mockProfile = new UserProfile();
        float[] mockFeatures = new float[] { 0.1f, 0.2f };
        MLPrediction mockPrediction = new MLPrediction(0.85, "testModel");

        when(profileService.getOrCreateProfile("user1")).thenReturn(mockProfile);
        when(featureExtractor.extract(any(Transaction.class), any(UserProfile.class), anyInt()))
                .thenReturn(mockFeatures);
        when(modelRegistry.getActiveModel()).thenReturn(Optional.of(mlModel));
        when(mlModel.predict(mockFeatures)).thenReturn(mockPrediction);

        // Act
        FraudAnalysisResponse response = transactionService.submit(request);

        // Assert
        assertEquals("REJECT", response.getDecision());
        assertEquals(0.85, response.getRiskScore());
        assertEquals("ONNX_ML", response.getScoringEngine());

        verify(txRepository, times(2)).save(any(Transaction.class)); // 1. Initial 2. Status Update
        verify(evalRepository, times(1)).save(any(FraudEvaluation.class));
        verify(profileService, times(1)).updateProfile(eq(mockProfile), any(Transaction.class));
        verify(annotationChain, times(1)).annotate(any(RuleContext.class));
    }

    @Test
    void analyze_shouldNotPersistButReturnAnalysis() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setSenderId("user2");
        request.setAmount(500.0);

        UserProfile mockProfile = new UserProfile();
        float[] mockFeatures = new float[] { 0.1f };

        // Simulating ML Model missing, testing fallback
        when(modelRegistry.getActiveModel()).thenReturn(Optional.empty());
        when(profileService.getOrCreateProfile("user2")).thenReturn(mockProfile);
        // Fallback checks specific indices: 4, 5, 0, 6. Let's make features[0]=0,
        // features[4,5,6]=0 -> Score 0
        when(featureExtractor.extract(any(), any(), anyInt())).thenReturn(new float[8]);

        // Act
        FraudAnalysisResponse response = transactionService.analyze(request);

        // Assert
        assertEquals("ACCEPT", response.getDecision(), "Score should be 0.0 leading to ACCEPT");
        assertEquals("RULES_ONLY", response.getScoringEngine());

        verify(txRepository, never()).save(any(Transaction.class));
        verify(evalRepository, never()).save(any(FraudEvaluation.class));
        verify(profileService, never()).updateProfile(any(), any());
    }
}
