package com.antigravity.fraud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudDetectionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UPI Fraud Detection API")
                        .description("Hybrid ML + rule-based fraud scoring for UPI/wallet transactions")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Antigravity")
                                .email("dev@antigravity.com")));
    }
}
