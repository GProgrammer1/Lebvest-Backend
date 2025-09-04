package com.lebvest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("s3CleanupExecutor")
    public Executor s3CleanupExecutor() {
        return Executors.newFixedThreadPool(5);
    }

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(5);
    }
}
