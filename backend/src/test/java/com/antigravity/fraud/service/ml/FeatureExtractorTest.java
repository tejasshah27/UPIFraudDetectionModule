package com.antigravity.fraud.service.ml;

import com.antigravity.fraud.model.Transaction;
import com.antigravity.fraud.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureExtractorTest {

    private final FeatureExtractor extractor = new FeatureExtractor();

    @Test
    void extract_shouldMapFeaturesCorrectly() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setAmount(1500.0);
        tx.setTimestamp(LocalDateTime.of(2025, 1, 1, 12, 0)); // 12th hour -> 0.5
        tx.setTxType("P2M"); // 1.0
        tx.setMccCode("7995"); // High risk -> 2.0
        tx.setIpAddress("192.168.1.5");
        tx.setCity("Mumbai");

        UserProfile profile = new UserProfile();
        profile.setAvgAmount(500.0);
        profile.setStdDevAmount(200.0);
        profile.setKnownIpAddresses(Set.of("192.168.1.100")); // current not found -> 1.0 (new IP)
        profile.setKnownCities(Set.of("Mumbai", "Delhi")); // current found -> 0.0 (old city)

        // Act
        float[] features = extractor.extract(tx, profile, 5); // velocity 5 -> 5.0

        // Assert
        assertEquals(8, features.length);
        
        // 0: normalizedAmount (1500 / 500 = 3.0)
        assertEquals(3.0f, features[0], 0.001);
        
        // 1: hourOfDay (12 / 24 = 0.5)
        assertEquals(0.5f, features[1], 0.001);
        
        // 2: txType (P2M = 1.0)
        assertEquals(1.0f, features[2], 0.001);
        
        // 3: mccRiskBucket ("7995" = High = 2.0)
        assertEquals(2.0f, features[3], 0.001);
        
        // 4: isNewIp (not in known = 1.0)
        assertEquals(1.0f, features[4], 0.001);
        
        // 5: isNewCity (in known = 0.0)
        assertEquals(0.0f, features[5], 0.001);
        
        // 6: velocityCount (passed 5 = 5.0)
        assertEquals(5.0f, features[6], 0.001);
        
        // 7: amountZScore ((1500 - 500) / 200 = 5.0)
        assertEquals(5.0f, features[7], 0.001);
    }

    @Test
    void extract_shouldHandleZeroAndNullsSafely() {
        Transaction tx = new Transaction();
        tx.setAmount(50.0);
        tx.setTimestamp(LocalDateTime.of(2025, 1, 1, 0, 0)); // 0 / 24 = 0.0
        tx.setTxType(null); // defaults 0
        tx.setMccCode(null); // defaults 0
        tx.setIpAddress(null);
        tx.setCity(null);

        UserProfile profile = new UserProfile(); // Empty profile, defaults 0.0 for sum/avg
        // Extractor divides by Math.max(avg, 1.0). So 50.0 / 1.0 = 50.0

        float[] features = extractor.extract(tx, profile, 0);

        assertEquals(8, features.length);
        assertEquals(50.0f, features[0], 0.001); // 50 / max(0, 1) = 50
        assertEquals(0.0f, features[1], 0.001);
        assertEquals(0.0f, features[2], 0.001);
        assertEquals(0.0f, features[3], 0.001);
        // Both IP and city empty in profile, and tx has null -> Contains(null) is false -> 1.0
        assertEquals(1.0f, features[4], 0.001);
        assertEquals(1.0f, features[5], 0.001);
        assertEquals(0.0f, features[6], 0.001);
        assertEquals(50.0f, features[7], 0.001); // (50 - 0) / max(0, 1) = 50
    }
}
