package com.antigravity.fraud.service;

import com.antigravity.fraud.dto.FraudAnalysisResponse;
import com.antigravity.fraud.dto.TransactionRequest;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

@Service
public class DataGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DataGeneratorService.class);

    private static final String[] TX_TYPES    = {"P2P", "P2M", "BILL_PAY", "RECHARGE"};
    private static final int[]    TX_WEIGHTS  = {55, 25, 12, 8};   // must sum to 100
    private static final String[] MCC_CODES   = {"5411", "5812", "5999", "7995", "5944", "4814", "6211"};
    private static final String[] CITIES      = {
            "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune", "Ahmedabad"
    };

    private final TransactionService transactionService;
    private final Faker faker = new Faker();

    public DataGeneratorService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Generate {@code count} synthetic UPI transactions, submit each through the fraud pipeline,
     * and return the collected analysis responses.
     */
    public List<FraudAnalysisResponse> generate(int count) {
        log.info("Generating {} synthetic transactions", count);

        List<FraudAnalysisResponse> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            TransactionRequest request = buildSyntheticRequest();
            FraudAnalysisResponse response = transactionService.submit(request);
            results.add(response);
        }

        log.info("Finished generating {} synthetic transactions", count);
        return results;
    }

    // -------------------------------------------------------------------------
    // Builders
    // -------------------------------------------------------------------------

    private TransactionRequest buildSyntheticRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setSenderId("USER_" + faker.number().digits(6));
        req.setReceiverId(randomReceiverId());
        req.setAmount(randomAmount());
        req.setTxType(randomTxType());
        req.setMccCode(MCC_CODES[faker.number().numberBetween(0, MCC_CODES.length)]);
        req.setIpAddress(faker.internet().ipV4Address());
        req.setCity(CITIES[faker.number().numberBetween(0, CITIES.length)]);
        req.setTimestamp(LocalDateTime.now().minusMinutes(faker.number().numberBetween(0, 1440)));
        req.setCurrency("INR");
        return req;
    }

    private String randomReceiverId() {
        // ~30% chance it's a merchant
        return faker.number().numberBetween(1, 10) <= 3
                ? "MERCH_" + faker.number().digits(4)
                : "USER_" + faker.number().digits(6);
    }

    /**
     * Amount buckets:
     *   60% small  : 10 – 500
     *   30% medium : 500 – 5 000
     *   10% large  : 5 000 – 100 000
     */
    private double randomAmount() {
        int roll = faker.number().numberBetween(1, 101); // 1..100
        if (roll <= 60) {
            return faker.number().randomDouble(2, 10, 500);
        } else if (roll <= 90) {
            return faker.number().randomDouble(2, 500, 5000);
        } else {
            return faker.number().randomDouble(2, 5000, 100000);
        }
    }

    /**
     * Weighted random pick from TX_TYPES using cumulative probability.
     * Weights: P2P=55, P2M=25, BILL_PAY=12, RECHARGE=8.
     */
    private String randomTxType() {
        int roll = faker.number().numberBetween(1, 101); // 1..100
        int cumulative = 0;
        for (int i = 0; i < TX_TYPES.length; i++) {
            cumulative += TX_WEIGHTS[i];
            if (roll <= cumulative) {
                return TX_TYPES[i];
            }
        }
        return TX_TYPES[0]; // fallback — should never reach here
    }
}
