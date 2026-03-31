package com.antigravity.fraud.service.ml;

import com.antigravity.fraud.model.Transaction;
import com.antigravity.fraud.model.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Responsible for translating raw Transaction and UserProfile behavioral data 
 * into a standardized numerical float array to be ingested by the ML Model.
 * 
 * <p><strong>CRITICAL:</strong> The size and order of the array returned by this class MUST 
 * strictly match the input schema defined in the Python training pipeline (`ml/train.py`). 
 * Modifying the array schema here requires retraining the ONNX model.</p>
 */
@Component
public class FeatureExtractor {

    private static final Set<String> HIGH_RISK_MCC = Set.of("7995", "5944", "5813", "9754");
    private static final Set<String> MEDIUM_RISK_MCC = Set.of("5912", "5999", "6211");

    /**
     * Extracts a float[8] feature vector from a transaction and its sender's profile.
     * Index order must stay in sync with Python train.py.
     *
     * @param tx             the transaction being evaluated
     * @param profile        the sender's behavioural profile
     * @param recentTxCount  number of transactions by this sender in the last 10 minutes
     * @return float[8] feature vector
     */
    public float[] extract(Transaction tx, UserProfile profile, int recentTxCount) {
        float[] features = new float[8];

        // [0] normalizedAmount — amount relative to user's average
        features[0] = (float) (tx.getAmount() / Math.max(profile.getAvgAmount(), 1.0));

        // [1] hourOfDay — normalised to [0, 1)
        features[1] = (float) (tx.getTimestamp().getHour() / 24.0);

        // [2] txTypeEncoded — P2P=0, P2M=1, BILL_PAY=2, RECHARGE=3
        features[2] = encodeTxType(tx.getTxType());

        // [3] mccRiskBucket — 0=low, 1=medium, 2=high
        features[3] = encodeMccRisk(tx.getMccCode());

        // [4] isNewIp — 1 if IP has never been seen for this user
        features[4] = profile.getKnownIpAddresses().contains(tx.getIpAddress()) ? 0.0f : 1.0f;

        // [5] isNewCity — 1 if city has never been seen for this user
        features[5] = profile.getKnownCities().contains(tx.getCity()) ? 0.0f : 1.0f;

        // [6] velocityCount — tx count in last 10-minute window (passed in by caller)
        features[6] = (float) recentTxCount;

        // [7] amountZScore — standard score vs user's historical distribution
        features[7] = (float) ((tx.getAmount() - profile.getAvgAmount())
                / Math.max(profile.getStdDevAmount(), 1.0));

        return features;
    }

    private float encodeTxType(String txType) {
        if (txType == null) return 0.0f;
        return switch (txType) {
            case "P2P"      -> 0.0f;
            case "P2M"      -> 1.0f;
            case "BILL_PAY" -> 2.0f;
            case "RECHARGE" -> 3.0f;
            default         -> 0.0f;
        };
    }

    private float encodeMccRisk(String mcc) {
        if (mcc == null) return 0.0f;
        if (HIGH_RISK_MCC.contains(mcc))   return 2.0f;
        if (MEDIUM_RISK_MCC.contains(mcc)) return 1.0f;
        return 0.0f;
    }
}
