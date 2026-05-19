package com.fleetwise.api;

import com.fleetwise.api.ai.config.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenAiProperties.class)
public class FleetwiseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetwiseApiApplication.class, args);
    }
}