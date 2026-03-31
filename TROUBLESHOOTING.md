# TROUBLESHOOTING GUIDE

This document catalogs errors encountered during the backend, frontend, and machine learning integration phases for the UPI Fraud Detection system, alongside their step-by-step resolutions.

---

### 1. Java Compile Error: `FeatureExtractor.java`
**Error:** `cannot find symbol: method getMcc()`
**Context:** During the Spring Boot application build, the ML `FeatureExtractor.java` attempted to call `tx.getMcc()`, which did not exist on the `Transaction` entity.
**Resolution:**
- Modified `FeatureExtractor.java` line 37.
- Replaced `tx.getMcc()` with `tx.getMccCode()` to accurately map to the existing field on the `Transaction` model.

### 2. Python Package Installation Error (Wheels / Build Dependencies)
**Error:** `Failed to build 'scikit-learn' when installing build dependencies [...] subprocess-exited-with-error`
**Context:** When attempting to run `pip install -r requirements.txt`, packages like `scikit-learn` and `numpy` fell back to building from source and failed. This occurred because the global environment was using **Python 3.13**, which is too new to have pre-built stable `.whl` files for the pinned older library versions.
**Resolution:**
- Installed **Python 3.11**, completely isolating this pipeline.
- Created a virtual environment (`py -3.11 -m venv .venv`).
- Activated `.venv` and re-ran `pip install` using standard stable wheels.

### 3. ML Dataset Generation: Probability Array Sum
**Error:** `ValueError: probabilities do not sum to 1`
**Context:** While executing `train.py`, `numpy.random.choice` aborted in `data_generator.py`. The synthetic fraud generator assigns a temporal distribution over 24 hours, but the hardcoded probabilities actually summed to `1.18` rather than a perfect `1.0`.
**Resolution:**
- Updated `data_generator.py` surrounding line 100.
- Intercepted the probability distribution list and dynamically normalized it: `p_hours = np.array(p_hours) / sum(p_hours)`.
- Passed the normalized probabilities into the random generator.

### 4. ONNX Model Export Failure: Strict Schema Type Checks
**Error:** `TypeError: Field onnx.AttributeProto.ints: Expected an int, got a boolean.`
**Context:** After the dataset generated and XGBoost finished training, `onnxmltools.convert_xgboost()` crashed. Newer versions of `onnx` (1.14+) actively validate against strict schema definitions, causing it to reject XGBoost's internal initialization booleans (like `use_label_encoder`) because the schema expects an integer array.
**Resolution:**
- Downgraded `xgboost` from `2.0.3` to the highly-stable `1.7.6`.
- Temporarily reverted `use_label_encoder: False` inside `train.py`'s `XGB_PARAMS` to suppress sklearn warnings specific to that XGB version.
- Explicitly downgraded `onnx` tracking from `1.16.x` to the relaxed `onnx==1.13.1` parser rule, allowing the conversion pipeline to complete successfully and export `models/fraud_model.onnx`.

### 5. Spring Boot 4.0.0 Testing Annotations
**Error:** `The import org.springframework.boot.test.mock.mockito cannot be resolved`, `MockBean cannot be resolved to a type`
**Context:** When attempting to run the Java backend test suite on Spring Boot `4.0.0` (Spring Framework 6.2+), the classic `@MockBean` annotation failed to compile. 
**Resolution:**
- Modern Spring iterations deprecate and remove `@MockBean` entirely from its original package.
- It has been replaced natively by `@MockitoBean`. 
- Updated `TransactionControllerTest.java` to import `org.springframework.test.context.bean.override.mockito.MockitoBean` and completely bypassed flawed auto-configuration layers by wrapping the controller in `MockMvcBuilders.standaloneSetup()`.

### 6. MockMvc HTTP 400 Validation Error
**Error:** `java.lang.AssertionError: Status expected:<201> but was:<400>`
**Context:** During `TransactionControllerTest.java` execution, `mockMvc.perform(post(...))` returned a 400 Bad Request instead of the expected 201 Created. This was caused by the test method passing a `TransactionRequest` payload that was missing multiple fields mathematically required by `@NotNull` or `@NotBlank` entity validations (like `currency`, `txType`, `mccCode`).
**Resolution:**
- Populated all required parameters inside the mocked `TransactionRequest` object prior to JSON serialization.
- Test successfully mapped to the controller and asserted `201 Created`.

---
*Created for the UPI Fraud Detection Environment.*
