package com.antigravity.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class UPIFraudDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(UPIFraudDetectionApplication.class, args);
    }
}
