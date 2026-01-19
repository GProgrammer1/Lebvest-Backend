package com.lebvest.util;

import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.create-admin", havingValue = "true")
@RequiredArgsConstructor
public class CreateAdminUser implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = args.length > 0 ? args[0] : "admin@lebvest.com";
        String password = args.length > 1 ? args[1] : "admin123";
        String name = args.length > 2 ? args[2] : "Admin User";

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            Set<Role> roles = user.getRoles();
            if (roles == null) {
                roles = new HashSet<>();
            } else {
                roles = new HashSet<>(roles);
            }
            
            if (roles.contains(Role.ADMIN)) {
                log.info("User {} already exists with ADMIN role", email);
                return;
            }
            
            roles.add(Role.ADMIN);
            user.setRoles(roles);
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            user.setLocked(false);
            userRepository.save(user);
            log.info("Updated existing user {} with ADMIN role", email);
            return;
        }

        Set<Role> roles = new HashSet<>();
        roles.add(Role.ADMIN);

        User adminUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .roles(roles)
                .enabled(true)
                .locked(false)
                .build();

        userRepository.save(adminUser);
        log.info("Admin user created successfully!");
        log.info("Email: {}", email);
        log.info("Password: {}", password);
        log.info("Name: {}", name);
    }
}

