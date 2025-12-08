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
        // Explicitly list common development origins
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
        ));
        configuration.setAllowCredentials(true);
        // Set max age for preflight cache
        configuration.setMaxAge(3600L);
        
        // Explicitly allow SSE-specific headers (for admin notifications)
        configuration.addExposedHeader("Cache-Control");
        configuration.addExposedHeader("Content-Type");
        configuration.addExposedHeader("Content-Length");
        configuration.addExposedHeader("Last-Event-ID");
        configuration.addExposedHeader("X-Accel-Buffering"); // For nginx SSE buffering
        // Explicitly expose Authorization header (for file uploads)
        configuration.addExposedHeader("Authorization");
        
        // Allow SSE-specific request headers
        configuration.addAllowedHeader("Cache-Control");
        configuration.addAllowedHeader("Last-Event-ID");
        configuration.addAllowedHeader("Accept");
        configuration.addAllowedHeader("Accept-Language");

        // Create a more permissive configuration specifically for SSE endpoints
        CorsConfiguration sseConfiguration = new CorsConfiguration();
        sseConfiguration.addAllowedHeader("*");
        sseConfiguration.addAllowedMethod("*");
        sseConfiguration.setAllowedOriginPatterns(java.util.Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        sseConfiguration.setAllowCredentials(true);
        sseConfiguration.setMaxAge(3600L);
        sseConfiguration.addExposedHeader("*"); // Expose all headers for SSE
        
        //Mapper between cors config and route
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/sse/**", sseConfiguration); // More permissive for SSE
        source.registerCorsConfiguration("/**", configuration); // Standard config for other routes
        return source;
    }
}
