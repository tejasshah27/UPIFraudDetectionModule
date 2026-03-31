# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**UPI Fraud Detection System** — A standalone POC service that analyses UPI/wallet transactions using a hybrid approach: an XGBoost ONNX model provides the primary risk score, while a rule-based Chain of Responsibility engine adds interpretable reason codes. A decoupled Angular dashboard visualises results.

---

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 21 + Spring Boot 4.0.5 |
| Build | Maven |
| Database | MongoDB |
| ML Runtime | ONNX Runtime for Java (`com.microsoft.onnxruntime:onnxruntime`) |
| ML Training | Python (XGBoost + skl2onnx / onnxmltools) in `/ml` dir |
| Frontend | Angular 17/18 (separate project, decoupled) |
| Auth | None (POC) |
| Logging | SLF4J structured logs |
| Test data | JavaFaker data generator (Spring Boot module) |

---

## Project Structure

```
upi-fraud-detection/           ← root repo
├── backend/                   ← Spring Boot service (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/antigravity/fraud/
│       └── main/resources/
│           ├── application.yml
│           ├── rules.yml
│           └── models/fraud_model.onnx
├── frontend/                  ← Angular 17/18 app
│   ├── package.json
│   ├── angular.json
│   └── src/app/
│       ├── core/
│       │   └── services/
│       │       └── transaction.service.ts
│       ├── features/
│       │   ├── dashboard/
│       │   │   ├── dashboard.component.ts
│       │   │   └── dashboard.component.html
│       │   └── transactions/
│       │       ├── transaction-list.component.ts
│       │       ├── transaction-list.component.html
│       │       └── rule-breakdown-modal.component.ts
│       ├── shared/
│       │   └── models/
│       │       └── transaction.model.ts
│       ├── app.routes.ts
│       └── environments/
│           ├── environment.ts           # apiBaseUrl: 'http://localhost:8080'
│           └── environment.prod.ts
└── ml/                        ← Python training pipeline
    ├── requirements.txt
    ├── data_generator.py
    ├── train.py
    ├── evaluate.py
    └── models/
        └── fraud_model.onnx
```

---

## Backend Package Structure

```
src/main/java/com/antigravity/fraud/
├── config/
│   ├── MongoConfig.java
│   ├── RuleConfig.java          # Loads rules.yml → RuleProperties beans
│   └── MLModelConfig.java       # Loads model path, auto-loads on startup
├── controller/
│   ├── TransactionController.java
│   ├── UserProfileController.java
│   ├── RuleConfigController.java
│   ├── MLModelController.java   # reload + info endpoints
│   └── DataGeneratorController.java
├── model/
│   ├── Transaction.java         # MongoDB @Document
│   ├── UserProfile.java         # MongoDB @Document — per-user baseline
│   └── FraudEvaluation.java     # MongoDB @Document — full audit record
├── repository/
│   ├── TransactionRepository.java
│   ├── UserProfileRepository.java
│   └── FraudEvaluationRepository.java
├── service/
│   ├── ml/
│   │   ├── MLModel.java              # Interface: predict(float[]) → MLPrediction
│   │   ├── OnnxMLModel.java          # Impl: loads .onnx via OrtEnvironment/OrtSession
│   │   ├── MLModelRegistry.java      # Holds active model instance; supports hot-swap
│   │   └── FeatureExtractor.java     # Transaction + UserProfile → float[8] feature vector
│   ├── detection/
│   │   ├── RuleContext.java          # Carries tx + profile + triggeredReasons + ruleBreakdown
│   │   ├── FraudRule.java            # Interface: annotate(RuleContext) → void  [no scoring]
│   │   ├── FraudAnnotationChain.java # Runs all rules for reason codes only
│   │   └── rules/
│   │       ├── AmountThresholdRule.java
│   │       ├── OddHourRule.java
│   │       ├── VelocityRule.java
│   │       └── LocationAnomalyRule.java
│   ├── UserProfileService.java       # Read/update per-user baseline stats
│   ├── TransactionService.java       # Orchestration: extract → ML score → annotate → persist
│   └── DataGeneratorService.java     # JavaFaker synthetic transactions
├── dto/
│   ├── TransactionRequest.java
│   └── FraudAnalysisResponse.java
└── UPIFraudDetectionApplication.java
```

---

## ML Architecture

### Scoring Flow
```
TransactionRequest
  → UserProfileService (load/create profile)
  → FeatureExtractor.extract(transaction, profile) → float[8]
  → MLModelRegistry.getActiveModel().predict(features) → MLPrediction { score: 0.87 }
  → FraudAnnotationChain.annotate(RuleContext) → reasons[], ruleBreakdown[]
  → FraudAnalysisResponse { riskScore: 0.87, decision: REJECT, reasons: [...], ruleBreakdown: [...] }
```

**Rules do NOT contribute to score** — they only annotate with reason codes and detail strings. ML score is authoritative. Falls back to rules-only scoring if ML model unavailable (`ml.fallback-to-rules=true`).

### Feature Vector (8 features — must stay in sync between Java `FeatureExtractor` and Python `train.py`)

| Index | Feature | Derivation |
|---|---|---|
| 0 | `normalizedAmount` | amount / userProfile.avgAmount |
| 1 | `hourOfDay` | tx.timestamp.hour / 24.0 |
| 2 | `txTypeEncoded` | P2P=0, P2M=1, BILL_PAY=2, RECHARGE=3 |
| 3 | `mccRiskBucket` | 0=low, 1=medium, 2=high (gambling/jewellery) |
| 4 | `isNewIp` | 0/1 — IP not in userProfile.knownIpAddresses |
| 5 | `isNewCity` | 0/1 — city not in userProfile.knownCities |
| 6 | `velocityCount` | tx count last 10 min for sender |
| 7 | `amountZScore` | (amount - avgAmount) / stdDevAmount |

### MLModel Interface (Extensibility Contract)
```java
public interface MLModel {
    MLPrediction predict(float[] features);
    String getModelId();
    String getModelVersion();
    ModelMetadata getMetadata();   // feature schema, training date, algorithm
}
```
New algorithms implement this interface. `MLModelRegistry` swaps the active impl. REST API shape never changes.

### Hot-Swap
- Model path in `application.yml`: `ml.model-path: classpath:models/fraud_model.onnx`
- `POST /api/v1/model/reload?path=/path/to/new_model.onnx` — atomically replaces active `OrtSession`, no restart needed

---

## Config-Driven Rules (`rules.yml`)

Rules annotate only (no score weights). Thresholds are live-updatable via `PUT /api/v1/rules`.

```yaml
fraud:
  rules:
    amount-threshold:
      enabled: true
      day-max: 50000
      night-max: 30000
      high-risk-mcc: ["7995", "5944"]
    odd-hour:
      enabled: true
      risk-start-hour: 1
      risk-end-hour: 5
    velocity:
      enabled: true
      max-tx-in-window: 5
      window-minutes: 10
    location-anomaly:
      enabled: true
  ml:
    model-path: classpath:models/fraud_model.onnx
    fallback-to-rules: true
    decision-thresholds:
      accept: 0.30
      review: 0.70
```

---

## REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/transactions/analyze` | Analyze without persisting |
| `POST` | `/api/v1/transactions` | Submit, analyze, and persist |
| `GET` | `/api/v1/transactions/{id}` | Fetch tx + fraud result |
| `GET` | `/api/v1/transactions/user/{userId}` | Transaction history with risk data |
| `GET` | `/api/v1/users/{userId}/profile` | User behavioural profile |
| `GET` | `/api/v1/rules` | Current rule config |
| `PUT` | `/api/v1/rules` | Update thresholds without restart |
| `POST` | `/api/v1/model/reload` | Hot-swap ONNX model |
| `GET` | `/api/v1/model/info` | Active model metadata |
| `POST` | `/api/v1/data/generate` | Generate N synthetic transactions |

Swagger UI available at `http://localhost:8080/swagger-ui.html` (Springdoc OpenAPI).

---

## Key Response Shape

```json
{
  "txId": "TXN_uuid",
  "riskScore": 0.87,
  "decision": "REJECT",
  "scoringEngine": "ONNX_ML",
  "reasons": ["velocity_breach", "new_city"],
  "ruleBreakdown": [
    { "rule": "VelocityCheck", "triggered": true,  "detail": "6 tx in last 10 min" },
    { "rule": "LocationAnomaly", "triggered": true, "detail": "New city: Delhi (usual: Mumbai)" }
  ]
}
```

Decision thresholds: `riskScore < 0.30` → ACCEPT, `0.30–0.70` → REVIEW, `> 0.70` → REJECT.

---

## Development Commands

### Backend
```bash
cd backend
mvn spring-boot:run                          # start service on :8080
mvn test                                     # run all tests
mvn test -Dtest=TransactionServiceTest       # run single test class
mvn compile                                  # compile only
```

### ML Pipeline
```bash
cd ml
pip install -r requirements.txt
python train.py                              # train XGBoost → outputs models/fraud_model.onnx
python evaluate.py                           # print classification report
# Copy output to backend:
cp models/fraud_model.onnx ../backend/src/main/resources/models/
```

### Frontend
```bash
cd frontend
npm install
ng serve                                     # dev server on :4200
ng build --configuration production
```

---

## Implementation Order

1. Bootstrap Spring Boot 4 Maven project + MongoDB + `application.yml` / `rules.yml`
2. Domain models + MongoDB repositories (`Transaction`, `UserProfile`, `FraudEvaluation`)
3. ML pipeline — `data_generator.py` + `train.py` → `fraud_model.onnx`
4. ONNX integration — `MLModel` interface, `OnnxMLModel`, `MLModelRegistry`, `FeatureExtractor`
5. Rule engine — `FraudRule` interface (annotation-only), 4 rule impls, `FraudAnnotationChain`
6. Services — `UserProfileService`, `TransactionService` (orchestration + fallback)
7. Controllers + DTOs + Springdoc OpenAPI
8. ML hot-swap — `MLModelController`
9. Data generator — `DataGeneratorService` + controller
10. Angular — scaffold, HTTP service, dashboard + transaction views, CORS

---

## Agents

Agents map implementation steps into parallelisable work units. Each agent owns a well-defined slice of the codebase and declares its dependencies on other agents.

### Agent Definitions

| Agent | Steps | What it builds | Runs after |
|---|---|---|---|
| **SetupAgent** | 1 | Project scaffold, `pom.xml`, `application.yml`, `rules.yml`, folder structure, `MongoConfig`, `RuleConfig`, `MLModelConfig` | — (first) |
| **MLPipelineAgent** | 3 | Python `data_generator.py`, `train.py`, `evaluate.py`, `requirements.txt`, produces `fraud_model.onnx` | — (independent, parallel with SetupAgent) |
| **DomainAgent** | 2 | `Transaction`, `UserProfile`, `FraudEvaluation` models + all 3 MongoDB repositories + DTOs (`TransactionRequest`, `FraudAnalysisResponse`) | SetupAgent |
| **MLIntegrationAgent** | 4 | `MLModel` interface, `OnnxMLModel`, `MLModelRegistry`, `FeatureExtractor`, `MLPrediction`, `ModelMetadata` | DomainAgent, MLPipelineAgent |
| **RuleEngineAgent** | 5 | `FraudRule` interface, `RuleContext`, `FraudAnnotationChain`, 4 rule impls (Amount, OddHour, Velocity, Location) | DomainAgent |
| **ServiceAgent** | 6, 9 | `UserProfileService`, `TransactionService` (orchestration + fallback), `DataGeneratorService` | MLIntegrationAgent, RuleEngineAgent |
| **ControllerAgent** | 7, 8 | All 5 controllers + Springdoc OpenAPI config + CORS config + ML hot-swap endpoints | ServiceAgent |
| **FrontendAgent** | 10 | Angular scaffold, `transaction.service.ts`, dashboard + transaction-list components, routing, environments | ControllerAgent (API shape must be final) |
| **TestingAgent** | 11 | Backend (JUnit + Mockito) and Frontend (Jasmine/Karma) comprehensive test suites | FrontendAgent |

### Execution Waves

```
Wave 1 (parallel):  SetupAgent  ║  MLPipelineAgent
Wave 2:             DomainAgent
Wave 3 (parallel):  MLIntegrationAgent  ║  RuleEngineAgent
Wave 4:             ServiceAgent
Wave 5:             ControllerAgent
Wave 6:             FrontendAgent
Wave 7 (parallel):  BackendTestingAgent ║ FrontendTestingAgent
```
