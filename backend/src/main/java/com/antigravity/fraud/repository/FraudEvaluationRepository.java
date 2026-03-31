package com.antigravity.fraud.repository;

import com.antigravity.fraud.model.FraudEvaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for FraudEvaluation documents in MongoDB.
 */
@Repository
public interface FraudEvaluationRepository extends MongoRepository<FraudEvaluation, String> {

    /**
     * Find a fraud evaluation by transaction ID.
     */
    Optional<FraudEvaluation> findByTxId(String txId);

    /**
     * Find all fraud evaluations with a specific decision, ordered by evaluation date descending.
     */
    List<FraudEvaluation> findByDecisionOrderByEvaluatedAtDesc(String decision);
}
