package com.antigravity.fraud;

import com.antigravity.fraud.repository.FraudEvaluationRepository;
import com.antigravity.fraud.repository.TransactionRepository;
import com.antigravity.fraud.repository.UserProfileRepository;
import com.antigravity.fraud.service.ml.MLModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring context smoke test.
 *
 * The "test" profile is active, which means:
 *   - MongoConfig (@Profile("!test")) is excluded, so @EnableMongoAuditing
 *     does not register and no live MongoDB is required.
 *   - MongoDB auto-configuration is excluded via spring.autoconfigure.exclude.
 *   - All MongoDB repositories are mocked via @MockBean.
 *   - MLModelRegistry is mocked so its @PostConstruct ONNX load is bypassed.
 */
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
        }
)
@ActiveProfiles("test")
class UPIFraudDetectionApplicationTests {

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private FraudEvaluationRepository fraudEvaluationRepository;

    @MockitoBean
    private MLModelRegistry mlModelRegistry;

    @Test
    void contextLoads() {
    }
}
