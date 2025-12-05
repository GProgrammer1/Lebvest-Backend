package com.lebvest.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    /**
     *
     * @return: cors configuration that is applied to all routes
     */
    @Bean
    @Qualifier("corsFilter")
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        // Use setAllowedOriginPatterns when credentials are enabled (required in newer Spring versions)
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.setAllowCredentials(true);
        
        // Explicitly allow SSE-specific headers
        configuration.addExposedHeader("Cache-Control");
        configuration.addExposedHeader("Content-Type");
        configuration.addExposedHeader("Last-Event-ID");

        //Mapper between cors config and route
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
