# Full Pre-Launch Wiring Checklist

## ML Layer — Architecture Explained

```
Python (offline, one-time)          Java (runtime, always running)
──────────────────────────          ──────────────────────────────
data_generator.py                   FeatureExtractor.java
  → synthetic training data    ───▶   same 8 features, same order
train.py                            OnnxMLModel.java
  → XGBoost model                     loads fraud_model.onnx
  → exports to .onnx           ───▶   runs inference via ONNX Runtime
fraud_model.onnx                    MLModelRegistry.java
  → file on disk               ───▶   holds the active model instance
```

The Python side is a **one-time offline step**. Once you have the `.onnx` file, Java takes over permanently.

---

## Step-by-Step: Training the ML Model

```bat
REM Navigate to the ml directory
cd ml

REM Step 1 — Install Python dependencies
pip install -r requirements.txt

REM Step 2 — Train the XGBoost model and export to ONNX
python train.py
REM ✔ Generates: ml/models/fraud_model.onnx
REM ✔ Prints ROC-AUC scores + verifies ONNX output shape

REM Step 3 — Copy the model to the backend resources
copy models\fraud_model.onnx ..\backend\src\main\resources\models\fraud_model.onnx

REM Step 4 — (Re)start the backend — MLModelRegistry loads it automatically on startup
```

> **Note:** With `fallback-to-rules: true` set in `application.yml`, the backend works
> without the model — it scores via rule-based heuristics and logs:
> `"ML model failed to load — falling back to rules-only scoring"`

---

## Pre-Launch Checklist

### MongoDB

| # | Check | Status | Notes |
|---|---|---|---|
| 1 | MongoDB running on `localhost:27017` | ✅ Done | Confirmed by user |
| 2 | Database `upi_fraud_db` created | ✅ Done | Confirmed by user |
| 3 | Collections + indexes created | ✅ Done | See indexes below |

**Collections & indexes:**
```js
use upi_fraud_db

db.transactions.createIndex({ txId: 1 }, { unique: true })
db.transactions.createIndex({ senderId: 1, timestamp: -1 })
db.fraud_evaluations.createIndex({ txId: 1 }, { unique: true })
db.user_profiles.createIndex({ userId: 1 }, { unique: true })
```

---

### ML Pipeline

| # | Check | Status | Notes |
|---|---|---|---|
| 4 | Python 3.10–3.12 installed | ❓ Verify | Required to run training |
| 5 | `pip install -r requirements.txt` run | ❌ Pending | Fixed `skl2onnx` (0.5.0→1.17.0), added `onnx==1.16.0` |
| 6 | `python train.py` run successfully | ❌ Pending | Fixed `use_label_encoder` param (removed, XGBoost 2.0+) |
| 7 | `fraud_model.onnx` copied to `backend/src/main/resources/models/` | ❌ Pending | Required for ML scoring mode |

---

### Backend

| # | Check | Status | Notes |
|---|---|---|---|
| 8 | Java 21 installed | ❓ Verify | `java -version` should show 21 |
| 9 | Maven installed | ❓ Verify | `mvn -version` |
| 10 | `FeatureExtractor.getMcc()` compile error | ✅ Fixed | → `getMccCode()` |
| 11 | `TransactionController` Optional fix | ✅ Fixed | `isPresent()` / `get()` |
| 12 | `rules.yml` imported in `application.yml` | ✅ Fixed | `spring.config.import: classpath:rules.yml` |
| 13 | `fallback-to-rules: true` configured | ✅ Set | Backend works without the ONNX model |
| 14 | MongoDB URI set to `localhost:27017/upi_fraud_db` | ✅ Set | In `application.yml` |

**Start the backend:**
```bat
cd backend
start.bat         REM kills any process on :8080, then runs mvn spring-boot:run
```

**Verify it's up:**
```
GET http://localhost:8080/swagger-ui.html   → Swagger UI
GET http://localhost:8080/api/v1/model/info → ML model status
GET http://localhost:8080/api/v1/rules      → Active rule config
```

---

### Frontend

| # | Check | Status | Notes |
|---|---|---|---|
| 15 | `npm install` completed | ✅ Done | Inside `frontend/` |
| 16 | Angular build passes (`ng build`) | ✅ Exit 0 | |
| 17 | API base URL points to `http://localhost:8080` | ✅ Set | `environments/environment.ts` |

**Start the frontend:**
```bat
cd frontend
start.bat         REM kills any process on :4200, then runs npm start (ng serve)
```

**Access the app:**
```
http://localhost:4200                → Dashboard
http://localhost:4200/transactions  → Transaction history lookup
http://localhost:4200/analyze       → Live transaction analyzer
```

---

## Launch Order

```
1. Start MongoDB          (must be running before backend)
2. Run ML training        (optional — backend works without it via fallback)
3. start backend/start.bat
4. start frontend/start.bat
5. Open http://localhost:4200
```

---

## Scoring Behaviour Summary

| Condition | Scoring Engine | Decision Source |
|---|---|---|
| `fraud_model.onnx` loaded successfully | `ONNX_ML` | XGBoost probability score |
| Model missing or failed to load | `RULES_ONLY` | Weighted heuristic on features |
| Rules always run (both modes) | — | Reason codes & rule breakdown only |

Decision thresholds (configurable in `rules.yml` / `application.yml`):
- `< 0.30` → **ACCEPT**
- `0.30 – 0.70` → **REVIEW**
- `> 0.70` → **REJECT**
