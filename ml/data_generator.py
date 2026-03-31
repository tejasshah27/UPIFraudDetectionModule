"""
Synthetic UPI transaction data generator for fraud detection model training.
Generates a labelled dataset with 8 features matching the Java FeatureExtractor
feature vector exactly.
"""

from __future__ import annotations

import numpy as np
import pandas as pd


FEATURE_COLUMNS = [
    "normalizedAmount",  # 0 — amount / userAvgAmount
    "hourOfDay",         # 1 — hour_of_day / 24.0
    "txTypeEncoded",     # 2 — P2P=0, P2M=1, BILL_PAY=2, RECHARGE=3
    "mccRiskBucket",     # 3 — 0=low, 1=medium, 2=high
    "isNewIp",           # 4 — 0/1
    "isNewCity",         # 5 — 0/1
    "velocityCount",     # 6 — tx count last 10 min (0–10 range)
    "amountZScore",      # 7 — (amount - avg) / std
]


def generate_dataset(n_samples: int = 10_000, random_seed: int = 42) -> pd.DataFrame:
    """Generate synthetic UPI transaction data.

    Args:
        n_samples:    Total number of rows to generate.
        random_seed:  NumPy random seed for reproducibility.

    Returns:
        DataFrame with FEATURE_COLUMNS + 'is_fraud' column.
    """
    rng = np.random.default_rng(random_seed)

    # ------------------------------------------------------------------ #
    # 1. Generate base (mostly-legitimate) feature values                  #
    # ------------------------------------------------------------------ #

    # normalizedAmount: log-normal centred near 1.0 (legitimate behaviour)
    normalized_amount = rng.lognormal(mean=0.0, sigma=0.5, size=n_samples)

    # hourOfDay: uniform 0-23, then normalised to 0–1
    hour_raw = rng.integers(0, 24, size=n_samples)
    hour_of_day = hour_raw / 24.0

    # txTypeEncoded: P2P most common (0), then P2M (1), BILL_PAY (2), RECHARGE (3)
    tx_type = rng.choice([0, 1, 2, 3], size=n_samples, p=[0.55, 0.25, 0.12, 0.08])

    # mccRiskBucket: mostly low risk
    mcc_bucket = rng.choice([0, 1, 2], size=n_samples, p=[0.75, 0.20, 0.05])

    # isNewIp: 10% base rate
    is_new_ip = rng.binomial(1, 0.10, size=n_samples)

    # isNewCity: 5% base rate
    is_new_city = rng.binomial(1, 0.05, size=n_samples)

    # velocityCount: mostly 0–2, integer
    velocity_count = rng.integers(0, 11, size=n_samples)
    # Skew toward low values for legitimate traffic
    velocity_count = np.minimum(velocity_count, rng.integers(0, 4, size=n_samples) + rng.integers(0, 4, size=n_samples))

    # amountZScore: standard normal
    amount_z_score = rng.standard_normal(size=n_samples)

    # ------------------------------------------------------------------ #
    # 2. Compute per-row fraud-risk score based on suspicious indicators   #
    # ------------------------------------------------------------------ #

    fraud_signals = np.zeros(n_samples, dtype=np.float64)
    fraud_signals += (normalized_amount > 4).astype(float)
    fraud_signals += (hour_of_day < 0.25).astype(float)
    fraud_signals += (mcc_bucket == 2).astype(float)
    fraud_signals += (is_new_ip == 1).astype(float)
    fraud_signals += (is_new_city == 1).astype(float)
    fraud_signals += (velocity_count >= 4).astype(float)
    fraud_signals += (amount_z_score > 2).astype(float)

    # ------------------------------------------------------------------ #
    # 3. Assign fraud label (~5% fraud rate)                              #
    # ------------------------------------------------------------------ #
    fraud_prob = np.where(
        fraud_signals >= 3,
        rng.uniform(0.70, 1.00, size=n_samples),
        rng.uniform(0.00, 0.08, size=n_samples),
    )

    threshold = np.percentile(fraud_prob, 95)
    is_fraud = (fraud_prob >= threshold).astype(int)

    # ------------------------------------------------------------------ #
    # 4. Overlay realistic fraud feature values for labelled fraud rows    #
    # ------------------------------------------------------------------ #
    fraud_mask = is_fraud == 1

    normalized_amount[fraud_mask] = rng.lognormal(mean=1.8, sigma=0.7, size=fraud_mask.sum())
    p_hours = [0.08, 0.08, 0.07, 0.06, 0.05, 0.04,
               0.02, 0.02, 0.02, 0.02, 0.02, 0.03,
               0.04, 0.04, 0.04, 0.04, 0.04, 0.05,
               0.06, 0.06, 0.06, 0.07, 0.08, 0.09]
    p_hours = np.array(p_hours) / sum(p_hours)
    fraud_hours = rng.choice(range(24), size=fraud_mask.sum(), p=p_hours)
    hour_of_day[fraud_mask] = fraud_hours / 24.0
    velocity_count[fraud_mask] = rng.integers(3, 11, size=fraud_mask.sum())
    amount_z_score[fraud_mask] = rng.normal(loc=2.5, scale=1.0, size=fraud_mask.sum())
    is_new_ip[fraud_mask] = rng.binomial(1, 0.65, size=fraud_mask.sum())
    is_new_city[fraud_mask] = rng.binomial(1, 0.55, size=fraud_mask.sum())
    mcc_bucket[fraud_mask] = rng.choice([0, 1, 2], size=fraud_mask.sum(), p=[0.20, 0.35, 0.45])

    # ------------------------------------------------------------------ #
    # 5. Assemble DataFrame                                                #
    # ------------------------------------------------------------------ #
    df = pd.DataFrame({
        "normalizedAmount": normalized_amount.astype(np.float32),
        "hourOfDay":        hour_of_day.astype(np.float32),
        "txTypeEncoded":    tx_type.astype(np.float32),
        "mccRiskBucket":    mcc_bucket.astype(np.float32),
        "isNewIp":          is_new_ip.astype(np.float32),
        "isNewCity":        is_new_city.astype(np.float32),
        "velocityCount":    velocity_count.astype(np.float32),
        "amountZScore":     amount_z_score.astype(np.float32),
        "is_fraud":         is_fraud,
    })

    return df


if __name__ == "__main__":
    import os

    df = generate_dataset(n_samples=10_000, random_seed=42)

    fraud_rate = df["is_fraud"].mean() * 100
    print(f"Dataset shape : {df.shape}")
    print(f"Fraud rate    : {fraud_rate:.2f}%")
    print(f"\nFeature stats:\n{df[FEATURE_COLUMNS].describe().round(4)}")

    os.makedirs("data", exist_ok=True)
    out_path = os.path.join("data", "transactions.csv")
    df.to_csv(out_path, index=False)
    print(f"\nSaved to {out_path}")
