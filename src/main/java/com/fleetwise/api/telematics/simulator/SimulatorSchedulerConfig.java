package com.fleetwise.api.telematics.simulator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SimulatorSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler simulatorTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("telematics-simulator-");
        scheduler.initialize();
        return scheduler;
    }
}