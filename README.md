Here is exactly how the UPI Fraud Detection hybrid-scoring system works, including a breakdown of the ML model versus the strictly rule-based engine, and how you can update them in the future.

Overview: The Hybrid Engine Strategy
Your system uses a Hybrid Scoring System designed for high accuracy and human interpretability.

The Core ML Model (XGBoost) determines the single authoritative riskScore (0.0 to 1.0).
The Business Rules (Chain of Responsibility) run after the ML scores the transaction. The rules do not change the score; they only attach human-readable "Reason Strings" and metadata so the dashboard can explain why a transaction was deemed risky.
(Note: The system falls back to calculating a manual score via the business rules only if the ML model crashes or goes offline).

1. The ML Logic (XGBoost via ONNX)
The Python ML model takes the incoming Transaction and the sender's UserProfile (a database record tracking their historical averages) and extracts 8 normalized mathematical features to predict risk:

Normalized Amount: tx.amount / user.avgAmount
Hour of Day: Values from 0 to 23 mathematically scaled.
Transaction Type: Encoded (e.g., P2P, P2M, BILL_PAY)
MCC Risk Bucket: Merchant Category Code assigned 0 (low), 1 (medium), or 2 (high-risk like jewellery/gambling).
New IP Flag: 0 or 1 (Is the IP address missing from their known historic IPs?).
New City Flag: 0 or 1.
Velocity Count: Number of transactions attempted by this user in the last 10 minutes.
Amount Z-Score: Mathematical deviation stringency (amount - avg) / standard_deviation.
2. The Business Rules (Interpretability)
Once the ML assigns a score (e.g. 0.87), it passes through the pure Java Rule Annotation Chain. These rules are defined in rules.yml and provide explicit reason codes:

Amount Threshold Rule: Checks if the amount strictly breaches daily variables (day-max: 50,000 or night-max: 30,000).
Odd Hour Rule: Did this happen during risk-start-hour: 1 to risk-end-hour: 5 (1 AM - 5 AM)?
Velocity Rule: Over 5 transactions in a 10-minute trailing window?
Location Anomaly Rule: Checking for sudden geographical jumps.
How to Update or Add New Risk Logic
Depending on what you want to achieve, there are two separate workflows:

Workflow A: Tweak an existing Business Rule
If you simply want to make the Amount Threshold stricter or change the Odd Hour window without touching code:

You can call the REST API PUT /api/v1/rules with a new JSON payload or manually edit backend/src/main/resources/rules.yml.
The rules dynamically update in real-time. No restart or ML training is required.
Workflow B: Adding a completely new Business Rule (Reason Code)
If you want to create a brand new rule (e.g., Device Anomaly):

Add a new DeviceAnomalyRule.java class in the backend that implements the FraudRule interface.
Inside the class, write your logic to add "foreign_device" to the reason codes.
Restart the Spring Boot backend (mvn spring-boot:run).
Workflow C: Adding a new Feature to the core ML Model
If you discover a new datapoint (e.g., deviceAgeDays) and want the actual ML model to start calculating scores based on this new data:

Update Python Pipeline: Add the data point logic to ml/data_generator.py and modify ml/train.py to train on 9 features instead of 8.
Train the new Model: Run python train.py inside the ml/ folder. This will output a new, smarter fraud_model_v2.onnx file.
Update Java Extractor: Go to backend/.../service/ml/FeatureExtractor.java and increase the float array size from 8 to 9, mapping your new deviceAgeDays variable to the 9th index!
Hot-Swap The Model: You do not need to restart the server. Place the new fraud_model_v2.onnx file in the resources folder and call POST /api/v1/model/reload?path=models/fraud_model_v2.onnx. The backend seamlessly replaces the AI brain in memory holding zero downtime!
