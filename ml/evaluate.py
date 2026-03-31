"""
Evaluate the trained ONNX fraud detection model.

Loads models/fraud_model.onnx, runs inference on a fresh 2 000-sample dataset,
and prints classification report, confusion matrix, ROC-AUC, and model I/O
binding info (so Java developers can verify input/output tensor names).
"""

from __future__ import annotations

import numpy as np
import onnxruntime as ort
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    roc_auc_score,
)

from data_generator import generate_dataset, FEATURE_COLUMNS

MODEL_PATH   = "models/fraud_model.onnx"
EVAL_SAMPLES = 2_000
EVAL_SEED    = 99
THRESHOLD    = 0.70   # matches Java decision-thresholds.review value


def evaluate() -> None:
    # ── 1. Fresh evaluation dataset ───────────────────────────────────────
    print(f"Generating {EVAL_SAMPLES} evaluation samples (seed={EVAL_SEED}) ...")
    df = generate_dataset(n_samples=EVAL_SAMPLES, random_seed=EVAL_SEED)

    X = df[FEATURE_COLUMNS].values.astype(np.float32)
    y_true = df["is_fraud"].values
    print(f"Fraud rate in eval set: {y_true.mean():.3f}")

    # ── 2. Load ONNX model and print I/O binding info ─────────────────────
    print(f"\nLoading ONNX model from: {MODEL_PATH}")
    sess = ort.InferenceSession(MODEL_PATH)

    print("\n── Model I/O Binding Info (for Java OnnxRuntime developers) ──")
    for inp in sess.get_inputs():
        print(f"  INPUT  name='{inp.name}'  shape={inp.shape}  dtype={inp.type}")
    for out in sess.get_outputs():
        print(f"  OUTPUT name='{out.name}'  shape={out.shape}  dtype={out.type}")

    # ── 3. Inference ──────────────────────────────────────────────────────
    input_name = sess.get_inputs()[0].name
    outputs    = sess.run(None, {input_name: X})

    prob_output = outputs[-1]
    if prob_output.ndim == 2 and prob_output.shape[1] == 2:
        y_prob = prob_output[:, 1]
    else:
        y_prob = prob_output.ravel()

    # ── 4. Metrics ────────────────────────────────────────────────────────
    y_pred = (y_prob >= THRESHOLD).astype(int)

    print("\n── Classification Report ──")
    print(classification_report(y_true, y_pred, target_names=["Legit", "Fraud"], digits=4))

    print("── Confusion Matrix ──")
    cm = confusion_matrix(y_true, y_pred)
    tn, fp, fn, tp = cm.ravel()
    print(f"  True Negatives  (legit correctly rejected) : {tn}")
    print(f"  False Positives (legit flagged as fraud)   : {fp}")
    print(f"  False Negatives (fraud missed)             : {fn}")
    print(f"  True Positives  (fraud correctly caught)   : {tp}")

    roc_auc = roc_auc_score(y_true, y_prob)
    print(f"\n── ROC-AUC Score : {roc_auc:.4f} ──")

    print("\n── Risk Score Distribution ──")
    print(f"  Legit  — mean={y_prob[y_true == 0].mean():.4f}  "
          f"std={y_prob[y_true == 0].std():.4f}")
    print(f"  Fraud  — mean={y_prob[y_true == 1].mean():.4f}  "
          f"std={y_prob[y_true == 1].std():.4f}")

    accept_pct = (y_prob <  0.30).mean() * 100
    review_pct = ((y_prob >= 0.30) & (y_prob < 0.70)).mean() * 100
    reject_pct = (y_prob >= 0.70).mean() * 100
    print(f"\n── Decision Bucket Distribution ──")
    print(f"  ACCEPT (< 0.30)    : {accept_pct:.1f}%")
    print(f"  REVIEW (0.30–0.70) : {review_pct:.1f}%")
    print(f"  REJECT (>= 0.70)   : {reject_pct:.1f}%")


if __name__ == "__main__":
    evaluate()
