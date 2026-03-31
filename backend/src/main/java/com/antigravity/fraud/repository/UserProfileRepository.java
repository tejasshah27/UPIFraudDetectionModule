package com.antigravity.fraud.repository;

import com.antigravity.fraud.model.UserProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for UserProfile documents in MongoDB.
 */
@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    /**
     * Find a user profile by user ID.
     */
    Optional<UserProfile> findByUserId(String userId);
}
