package com.lebvest.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes.
 * Run this as a standalone Java application to generate password hashes for SQL inserts.
 * 
 * Usage: java PasswordHashGenerator <password>
 * Example: java PasswordHashGenerator admin123
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PasswordHashGenerator <password>");
            System.out.println("Example: java PasswordHashGenerator admin123");
            System.exit(1);
        }
        
        String password = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("\nUse this hash in your SQL INSERT statement:");
        System.out.println("INSERT INTO users (name, email, password, locked, enabled, created_at)");
        System.out.println("VALUES ('Admin User', 'admin@lebvest.com', '" + hash + "', false, true, NOW());");
    }
}

