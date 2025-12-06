package com.lebvest.config;

import com.lebvest.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource source;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Qualifier("corsFilter") CorsConfigurationSource source, JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.source = source;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(source))
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/auth/**").permitAll()
                                .requestMatchers("/investments/featured").permitAll()
                                .requestMatchers("/investments").permitAll()
                                .requestMatchers(HttpMethod.GET, "/investments/*").permitAll() // Public investment details and updates
                                .requestMatchers("/investments/*/watchlist").authenticated()
                                .requestMatchers("/investments/*/invest").authenticated() // Make investment requires auth
                                .requestMatchers("/companies/me/**").authenticated() // All company endpoints require authentication (must come before /companies/*)
                                .requestMatchers(HttpMethod.GET, "/companies").permitAll() // Public company list
                                .requestMatchers(HttpMethod.GET, "/companies/*").permitAll() // Public company profiles
                                .requestMatchers(HttpMethod.GET, "/investors/*").permitAll() // Public investor profiles
                                .requestMatchers("/investors/me/**").authenticated() // All investor endpoints require authentication
                                .requestMatchers(HttpMethod.POST, "/investors/me/profile-image").authenticated() // Explicitly allow profile image upload
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/sse/**").permitAll() // SSE endpoints validate token in controller (EventSource can't send headers)
                                .requestMatchers("/ws/**").permitAll() // WebSocket endpoint
                                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() // Allow public access to uploaded files
                                                .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll() // Allow access to files (paths are UUID-based, hard to guess)
                                                .requestMatchers(HttpMethod.POST, "/payments/stripe/webhook").permitAll() // Stripe webhook (validated by signature)
                                                .requestMatchers("/api/user-activity/**").authenticated()
                                                .anyRequest().authenticated()
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }


}
