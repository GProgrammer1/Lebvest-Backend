package com.lebvest.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to verify and generate BCrypt password hashes.
 * This will verify if a hash matches "admin123" and generate a new one.
 */
public class VerifyPasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // The hash from the SQL file
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "admin123";
        
        System.out.println("=== Verifying existing hash ===");
        boolean matches = encoder.matches(password, existingHash);
        System.out.println("Password: " + password);
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches: " + matches);
        
        if (!matches) {
            System.out.println("\n❌ The hash does NOT match 'admin123'!");
            System.out.println("\n=== Generating NEW hash for 'admin123' ===");
            String newHash = encoder.encode(password);
            System.out.println("New BCrypt Hash: " + newHash);
            System.out.println("\nUse this hash in your SQL:");
            System.out.println("UPDATE users SET password = '" + newHash + "' WHERE email = 'admin@lebvest.com';");
        } else {
            System.out.println("\n✅ The hash matches 'admin123'!");
        }
        
        // Also generate a fresh hash for reference
        System.out.println("\n=== Fresh hash for 'admin123' (for reference) ===");
        String freshHash = encoder.encode(password);
        System.out.println("Fresh Hash: " + freshHash);
    }
}

