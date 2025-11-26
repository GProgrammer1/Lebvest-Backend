package com.lebvest.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for loading .env file.
 * spring-dotenv library automatically loads .env file from the root directory.
 * The library uses Spring Boot's auto-configuration, so no additional setup is needed.
 */
@Configuration
public class DotenvConfig {
    // spring-dotenv 4.0.0 uses auto-configuration
    // The .env file in the root directory will be automatically loaded
}

