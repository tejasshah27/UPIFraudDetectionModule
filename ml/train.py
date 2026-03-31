"""
Train an XGBoost binary classifier on synthetic UPI transaction data and
export the model to ONNX format (opset 12) compatible with
com.microsoft.onnxruntime:onnxruntime:1.18.0.

Output: models/fraud_model.onnx
"""

from __future__ import annotations

import os
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score
from xgboost import XGBClassifier
import onnxmltools
from onnxmltools.convert.common.data_types import FloatTensorType

from data_generator import generate_dataset, FEATURE_COLUMNS

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────
N_SAMPLES       = 10_000
RANDOM_SEED     = 42
TEST_SIZE       = 0.20
ONNX_OPSET      = 12
MODEL_OUT_PATH  = os.path.join("models", "fraud_model.onnx")

XGB_PARAMS: dict = {
    "n_estimators":      200,
    "max_depth":         5,
    "learning_rate":     0.05,
    "subsample":         0.8,
    "colsample_bytree":  0.8,
    "scale_pos_weight":  19,
    "eval_metric":       "logloss",
    "use_label_encoder": False,
    "random_state":      RANDOM_SEED,
}


def train() -> None:
    # ── 1. Data ────────────────────────────────────────────────────────────
    print("Generating dataset ...")
    df = generate_dataset(n_samples=N_SAMPLES, random_seed=RANDOM_SEED)

    X = df[FEATURE_COLUMNS].values.astype(np.float32)
    y = df["is_fraud"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=TEST_SIZE,
        stratify=y,
        random_state=RANDOM_SEED,
    )
    print(f"Train size: {X_train.shape[0]}  |  Test size: {X_test.shape[0]}")
    print(f"Fraud rate — train: {y_train.mean():.3f}  test: {y_test.mean():.3f}")

    # ── 2. Train ───────────────────────────────────────────────────────────
    print("\nTraining XGBoost ...")
    model = XGBClassifier(**XGB_PARAMS)
    model.fit(
        X_train, y_train,
        eval_set=[(X_test, y_test)],
        verbose=False,
    )

    # ── 3. Evaluate ────────────────────────────────────────────────────────
    train_prob = model.predict_proba(X_train)[:, 1]
    test_prob  = model.predict_proba(X_test)[:, 1]

    train_auc = roc_auc_score(y_train, train_prob)
    test_auc  = roc_auc_score(y_test,  test_prob)
    print(f"\nROC-AUC  — train: {train_auc:.4f}  |  test: {test_auc:.4f}")

    # ── 4. Export to ONNX ─────────────────────────────────────────────────
    print(f"\nExporting ONNX model (opset={ONNX_OPSET}) ...")

    initial_types = [("float_input", FloatTensorType([None, len(FEATURE_COLUMNS)]))]

    onnx_model = onnxmltools.convert_xgboost(
        model,
        initial_types=initial_types,
        target_opset=ONNX_OPSET,
    )

    _strip_zipmap(onnx_model)

    os.makedirs("models", exist_ok=True)
    onnxmltools.utils.save_model(onnx_model, MODEL_OUT_PATH)
    print(f"Model saved to: {MODEL_OUT_PATH}")

    _verify_onnx(MODEL_OUT_PATH, X_test[:5])


def _strip_zipmap(onnx_model) -> None:
    """Remove ZipMap nodes so probability output is a plain float32 tensor.

    The Java OnnxRuntime binding expects a raw OnnxTensor, not a sequence map.
    """
    import onnx

    graph = onnx_model.graph
    zipmap_nodes = [n for n in graph.node if n.op_type == "ZipMap"]

    for zm_node in zipmap_nodes:
        zm_input_name  = zm_node.input[0]
        zm_output_name = zm_node.output[0]

        for node in graph.node:
            for i, inp in enumerate(node.input):
                if inp == zm_output_name:
                    node.input[i] = zm_input_name

        for out in graph.output:
            if out.name == zm_output_name:
                out.name = zm_input_name
                out.type.CopyFrom(
                    onnx.helper.make_tensor_type_proto(
                        onnx.TensorProto.FLOAT, [None, 2]
                    )
                )

        graph.node.remove(zm_node)


def _verify_onnx(model_path: str, X_sample: np.ndarray) -> None:
    """Quick smoke-test via onnxruntime."""
    import onnxruntime as ort

    sess = ort.InferenceSession(model_path)
    input_name = sess.get_inputs()[0].name

    print("\nONNX model verification:")
    print(f"  Input  : name='{input_name}'  shape={sess.get_inputs()[0].shape}"
          f"  dtype={sess.get_inputs()[0].type}")
    for out in sess.get_outputs():
        print(f"  Output : name='{out.name}'  shape={out.shape}  dtype={out.type}")

    result = sess.run(None, {input_name: X_sample.astype(np.float32)})
    print(f"  Sample probabilities (first 5 rows): {result[-1][:, 1]}")
    print("ONNX verification passed.")


if __name__ == "__main__":
    train()
