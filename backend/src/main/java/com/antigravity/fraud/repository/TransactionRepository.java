package com.antigravity.fraud.repository;

import com.antigravity.fraud.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Transaction documents in MongoDB.
 */
@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    /**
     * Find a transaction by its business transaction ID.
     */
    Optional<Transaction> findByTxId(String txId);

    /**
     * Find all transactions for a given sender, ordered by timestamp descending.
     */
    List<Transaction> findBySenderIdOrderByTimestampDesc(String senderId);

    /**
     * Find transactions for a sender within a specific time range.
     */
    List<Transaction> findBySenderIdAndTimestampBetween(String senderId, LocalDateTime from,
            LocalDateTime to);
}
