# Backend Code Review Report

## 1. Architectural Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CONTROLLERS (REST)                       │
│  TransactionController  UserProfileController  MLModelController│
│  RuleConfigController   DataGeneratorController                 │
└────────────────────────────────┬────────────────────────────────┘
                                 │ @Valid DTO
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                              │
│                                                                 │
│  TransactionService ◄── orchestrator (analyze / submit)         │
│       │                                                         │
│       ├──► UserProfileService  (get/create/update profiles)     │
│       ├──► FeatureExtractor    (tx+profile → float[8])          │
│       ├──► MLModelRegistry     (volatile MLModel ref)           │
│       │        └──► OnnxMLModel (ONNX Runtime session)          │
│       ├──► FraudAnnotationChain (chain of FraudRule impls)      │
│       │        ├── AmountThresholdRule                           │
│       │        ├── OddHourRule                                  │
│       │        ├── VelocityRule                                 │
│       │        └── LocationAnomalyRule                          │
│       └──► DataGeneratorService (synthetic data via Faker)      │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER (MongoDB)                   │
│  TransactionRepository  UserProfileRepository                   │
│  FraudEvaluationRepository                                      │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
                        ┌──────────────┐
                        │   MongoDB    │
                        │ upi_fraud_db │
                        └──────────────┘
```

---

## 2. Flow Hierarchy

```
POST /api/v1/transactions (submit)
  └─ TransactionController.submit()
       └─ TransactionService.submit()
            ├─ buildTransaction(request)         → Transaction entity
            ├─ txRepository.save(tx)             → persist PENDING
            ├─ profileService.getOrCreateProfile → UserProfile (or new default)
            ├─ countRecentTransactions()          → velocity count (DB query)
            ├─ featureExtractor.extract()         → float[8]
            ├─ modelRegistry.getActiveModel()
            │    ├─ [present] → model.predict()   → MLPrediction{score}
            │    └─ [empty]   → computeRuleBasedScore() (weighted fallback)
            ├─ FraudAnnotationChain.annotate()
            │    ├─ AmountThresholdRule.annotate()
            │    ├─ OddHourRule.annotate()
            │    ├─ VelocityRule.annotate()
            │    └─ LocationAnomalyRule.annotate()
            ├─ resolveDecision(score)             → ACCEPT|REVIEW|REJECT
            ├─ evalRepository.save(FraudEvaluation)
            ├─ profileService.updateProfile()     → EMA stats update
            └─ txRepository.save(tx)              → status COMPLETED|BLOCKED
```

`POST /analyze` = same minus all persistence and profile update steps.

---

## 3. Design Patterns

| Pattern | Where | Notes |
|---|---|---|
| **Strategy** | `MLModel` interface + `OnnxMLModel` impl | Swap scoring engines without touching orchestrator |
| **Chain of Responsibility** | `FraudAnnotationChain` iterates `List<FraudRule>` | Spring auto-collects all `@Component` impls |
| **Registry / Singleton** | `MLModelRegistry` holds `volatile MLModel` | Hot-swap via atomic ref replacement |
| **Template Method (implicit)** | `TransactionService.analyze()` vs `submit()` | Same pipeline, submit adds persistence |
| **DTO / Domain separation** | `TransactionRequest` → `Transaction`, `FraudEvaluation` → `FraudAnalysisResponse` | Clean boundary |
| **Factory Method** | `FraudAnalysisResponse.from(FraudEvaluation)` | Domain→DTO conversion |
| **Configuration Properties** | `RuleConfig`, `MLModelConfig` w/ `@ConfigurationProperties` | Externalized thresholds |

---

## 4. Gains

- **ML/Rule decoupling** — rules annotate only, ML scores authoritatively. Clean separation of concerns
- **Hot-swap** — model reload without restart via `POST /model/reload`, volatile ref swap
- **Graceful fallback** — `fallback-to-rules=true` keeps system functional when ONNX model unavailable
- **Config-driven rules** — thresholds updatable at runtime via REST, no redeploy
- **Feature vector contract** — explicit 8-feature schema enforced in both Java and Python training pipeline
- **EMA profile updates** — exponential moving average prevents profile drift from single outlier tx
- **Audit trail** — every submitted tx gets a `FraudEvaluation` record with full rule breakdown
- **Constructor injection** throughout — testable, no field injection
- **Validation** — `@Valid` on controller inputs with Jakarta constraints
- **Test coverage** — unit tests for orchestrator (mock ML + fallback paths), controller (MockMvc), and FeatureExtractor (edge cases incl. nulls)

---

## 5. Gaps / Flaws

### Critical

| # | Issue | Location | Impact |
|---|---|---|---|
| **G1** | **Duplicate MongoTemplate bean** — `MongoConfig` hardcodes `mongodb://127.0.0.1:27017` while `application.yml` sets `mongodb://localhost:27017`. Both create a `MongoTemplate`. May cause dual connections or wrong DB binding | `MongoConfig.java:15-17` vs `application.yml:11` | Runtime ambiguity |
| **G2** | **`OnnxMLModel.close()` calls `env.close()`** — `OrtEnvironment.getEnvironment()` returns a shared singleton. Closing it corrupts all subsequent models in the same JVM. Hot-swap will crash after first reload | `OnnxMLModel.java:86-91` | **Production crash on hot-swap** |
| **G3** | **`getAllTransactions` loads ALL docs then `.stream().limit()`** — `findAll(Sort)` fetches entire collection into memory before Java-side truncation | `TransactionService.java:206` | OOM at scale |
| **G4** | **No index on `senderId+timestamp`** — `findBySenderIdAndTimestampBetween` used on every request for velocity. Full collection scan without compound index | `TransactionRepository.java:29` | Slow velocity queries |
| **G5** | **Velocity query counts current tx** — tx is saved BEFORE velocity count in `submit()`. The just-saved tx falls within its own 10min window, inflating count by 1 | `TransactionService.java:116-120` | Off-by-one velocity |

### Moderate

| # | Issue | Location |
|---|---|---|
| **G6** | **`double` for monetary amounts** — floating-point rounding errors. Should use `BigDecimal` | `Transaction.java:17`, `TransactionRequest.java:19` |
| **G7** | **No `@Transactional` / atomicity** — submit does save→evaluate→save with no rollback. Partial failure leaves orphan records | `TransactionService.submit()` |
| **G8** | **Rule update via `PUT /rules` mutates live singleton** — no validation, no thread safety, no audit. Caller can set `dayMax=-1` | `RuleConfigController.java:23-29` |
| **G9** | **`/model/reload` takes arbitrary filesystem path** — path traversal risk. No auth, no whitelist | `MLModelController.java:30-31` |
| **G10** | **`analyze()` and `submit()` 80% duplicated code** — extract-score-annotate-decide logic copy-pasted | `TransactionService.java:76-106` vs `111-164` |
| **G11** | **`RuleBreakdownItem` duplicated** — identical inner class in both `FraudEvaluation` and `FraudAnalysisResponse` with manual mapping loop | `FraudAnalysisResponse.java:131-185` |
| **G12** | **No `@Indexed` on `txId`, `userId`** — `findByTxId`, `findByUserId` do collection scans | All repositories |
| **G13** | **`LocationAnomalyRule` adds multiple breakdown items** — new_city + new_ip = 2 separate entries for one rule, inconsistent with other rules that add exactly 1 | `LocationAnomalyRule.java:38-49` |
| **G14** | **MCC risk sets differ** between `FeatureExtractor` and `RuleConfig` — FeatureExtractor hardcodes `5813,9754` as high-risk, `AmountThresholdRule` reads from config `7995,5944`. Inconsistent | `FeatureExtractor.java:20-21` vs `rules.yml:7-9` |

### Minor

| # | Issue | Location |
|---|---|---|
| **G15** | **Debug endpoint in prod** — `GET /transactions/debug/dbname` leaks DB name | `TransactionController.java:27-29` |
| **G16** | **TransactionController injects `MongoTemplate`** — controller should not hold DB infra ref. Only used by debug endpoint | `TransactionController.java:19-24` |
| **G17** | **No pagination** — `getUserTransactions` returns unbounded list | `TransactionService.java:188` |
| **G18** | **`OddHourRule` uses `<=` for end hour** — config says `risk-end-hour: 5`, so hour=5 (5:00-5:59) triggers. May be intentional but conflicts with `AmountThresholdRule` which uses `hour < 6` for night | `OddHourRule.java:36` vs `AmountThresholdRule.java:38` |
| **G19** | **No global exception handler** — uncaught RuntimeExceptions return Spring default 500 with stack trace | All controllers |
| **G20** | **Test coverage gaps** — no tests for rules, DataGeneratorService, UserProfileService, or the fallback scoring formula edge cases | `src/test/` |

---

## 6. How to Improve

| Priority | Action | Addresses |
|---|---|---|
| **P0** | Remove `env.close()` from `OnnxMLModel.close()` — only close `session` | G2 |
| **P0** | Remove or profile-gate `MongoConfig.java` — let Spring Boot auto-config from `application.yml` handle it. Or remove the `spring.data.mongodb.uri` and keep only the explicit bean | G1 |
| **P1** | Add `@Indexed` on `Transaction.txId`, `Transaction.senderId`, `UserProfile.userId`, `FraudEvaluation.txId`. Add compound index `{senderId, timestamp}` | G4, G12 |
| **P1** | Use `PageRequest` in `getAllTransactions` — `txRepository.findAll(PageRequest.of(0, limit, sort))` instead of loading everything | G3 |
| **P1** | Move velocity count BEFORE `txRepository.save()` in `submit()`, or subtract 1 from count | G5 |
| **P1** | Add `@ControllerAdvice` global exception handler returning structured error JSON | G19 |
| **P1** | Whitelist/validate model path in reload endpoint. Add Spring Security or at minimum a path prefix check | G9 |
| **P2** | Extract shared scoring pipeline into private method called by both `analyze()` and `submit()` | G10 |
| **P2** | Unify `RuleBreakdownItem` — single shared class or use MapStruct/record | G11 |
| **P2** | Use `BigDecimal` for `amount` fields, or at least document precision trade-off | G6 |
| **P2** | Validate `RuleConfig` input in `PUT /rules` — reject negative thresholds, add synchronization | G8 |
| **P2** | Align MCC risk sets — either both read from config or both hardcode the same set | G14 |
| **P3** | Remove debug endpoint or gate behind profile/actuator | G15, G16 |
| **P3** | Add pagination support (`Pageable`) to list endpoints | G17 |
| **P3** | Add rule unit tests, integration tests with embedded MongoDB (Flapdoodle/Testcontainers) | G20 |
| **P3** | Harmonize night-hour boundary between `AmountThresholdRule` and `OddHourRule` | G18 |
