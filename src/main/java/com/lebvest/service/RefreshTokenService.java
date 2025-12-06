package com.lebvest.service;

import com.lebvest.model.entities.investor.User;
import com.lebvest.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RefreshTokenService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public RefreshTokenService(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, String> refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            Claims claims = jwtService.parseToken(refreshToken, "refresh");
            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);

            // Load user
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Validate refresh token
            if (!jwtService.validateToken(refreshToken, "refresh", userDetails)) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            // Generate new access token
            String newAccessToken = jwtService.generateToken(userDetails, "access", user.getId());
            
            // Optionally generate new refresh token (rotate refresh tokens)
            String newRefreshToken = jwtService.generateToken(userDetails, "refresh", user.getId());

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);
            tokens.put("tokenType", "Bearer");

            log.info("Token refreshed successfully for user: {}", email);
            return tokens;

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to refresh token: " + e.getMessage());
        }
    }
}

