package com.antigravity.fraud.service;

import com.antigravity.fraud.model.Transaction;
import com.antigravity.fraud.model.UserProfile;
import com.antigravity.fraud.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository profileRepository;

    public UserProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Loads the behavioural baseline for the given user, or creates a default one if none exists.
     */
    public UserProfile getOrCreateProfile(String userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            log.info("No profile found for userId={}. Creating default profile.", userId);
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setAvgAmount(1000.0);
            profile.setStdDevAmount(500.0);
            profile.setTotalTransactions(0);
            profile.setKnownIpAddresses(new HashSet<>());
            profile.setKnownCities(new HashSet<>());
            profile.setLastUpdated(LocalDateTime.now());
            return profileRepository.save(profile);
        });
    }

    /**
     * Updates the user's rolling behavioural stats using an exponential moving average
     * after a completed transaction.
     */
    public UserProfile updateProfile(UserProfile profile, Transaction tx) {
        profile.setTotalTransactions(profile.getTotalTransactions() + 1);

        int n = profile.getTotalTransactions(); // already incremented
        double alpha = Math.min(1.0 / n, 0.1); // decay factor — converges to 0.1 after 10 tx

        double newAvg = (1 - alpha) * profile.getAvgAmount() + alpha * tx.getAmount();
        double newStdDev = Math.sqrt(
                (1 - alpha) * Math.pow(profile.getStdDevAmount(), 2)
                + alpha * Math.pow(tx.getAmount() - newAvg, 2)
        );

        profile.setAvgAmount(newAvg);
        profile.setStdDevAmount(Math.max(newStdDev, 1.0)); // guard against collapse to zero

        if (tx.getIpAddress() != null && !tx.getIpAddress().isBlank()) {
            profile.getKnownIpAddresses().add(tx.getIpAddress());
        }
        if (tx.getCity() != null && !tx.getCity().isBlank()) {
            profile.getKnownCities().add(tx.getCity());
        }

        profile.setLastUpdated(LocalDateTime.now());

        log.debug("Updated profile for userId={}: avgAmount={}, stdDevAmount={}, totalTx={}",
                profile.getUserId(), profile.getAvgAmount(), profile.getStdDevAmount(),
                profile.getTotalTransactions());

        return profileRepository.save(profile);
    }
}
