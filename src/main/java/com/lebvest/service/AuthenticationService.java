package com.lebvest.service;

import com.lebvest.config.VarsConfig;
import com.lebvest.model.dto.AuthenticationRequest;
import com.lebvest.model.dto.ForgotPasswordRequest;
import com.lebvest.model.dto.ResetPasswordRequest;
import com.lebvest.model.dto.ResponsePayload;
import com.lebvest.model.entities.ForgotPassToken;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.CompanyRepository;
import com.lebvest.repository.InvestorRepository;
import com.lebvest.repository.TokenRepository;
import com.lebvest.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthenticationService {

    private static final long EXPIRATION_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final MailService mailService;
    private final VarsConfig varsConfig;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService, VarsConfig varsConfig) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.varsConfig = varsConfig;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, String> login(AuthenticationRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmail(), req.getPassword()
                )
        );
        String role = req.getRole().toString();
        boolean exists = false;
        User user = (User) auth.getPrincipal(); // avoid 2nd DB hit


        switch (role) {
            case "Admin":
                if (user.getRoles().contains(Role.ADMIN)) {
                    exists = true;
                }
                break;

            case "Company":
                if (user.getRoles().contains(Role.COMPANY)) {
                    exists = true;
                }
                break;

            case "Investor":
                if (user.getRoles().contains(Role.INVESTOR)) {
                    exists = true;
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid role");
        }
        if (exists) {
            String token = jwtService.generateToken(user, "access", user.getId());
            return Map.of("token", token,
                    "role", role);
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    public ResponsePayload forgotPassword(ForgotPasswordRequest req) {
        String email = req.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String token = UUID.randomUUID().toString();
        ForgotPassToken dbToken = ForgotPassToken
                .builder()
                .token(token)
                .user(user)
                .build();
        tokenRepository.save(dbToken);
        String link = varsConfig.getResetLink(token);
//        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> mailService.loadAndFormatEmailTemplate(Map.of(
                        "name", user.getName(),
                        "email", email,
                        "resetLink", varsConfig.getResetLink(token)
                ), "ResetPassword"))
                        .thenAccept((html) -> mailService.sendHtmlMail(email, "Reset Your password", html));//);

        return ResponsePayload
                .builder()
                .message("Reset link sent successfully")
                .status(200)
                .data(Map.of("link", link))
                .build();
    }

    public ResponsePayload resetPassword(ResetPasswordRequest req) {
        ForgotPassToken existingToken = tokenRepository.findByToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        LocalDateTime expirationTime = existingToken.getCreatedAt().plusMinutes(EXPIRATION_MINUTES);
        if (expirationTime.isBefore(LocalDateTime.now())) {
            tokenRepository.delete(existingToken); // Clean up expired token
            throw new IllegalArgumentException("Token expired");
        }

        User user = existingToken.getUser();
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);

        tokenRepository.delete(existingToken); // Invalidate after use

        return ResponsePayload.builder()
                .message("Password reset successfully")
                .status(200)
                .build();
    }

}
