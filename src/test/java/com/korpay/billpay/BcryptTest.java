package com.korpay.billpay;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        
        System.out.println("=== BCrypt Test ===");
        System.out.println("Password: " + password);
        System.out.println("Stored Hash: " + storedHash);
        System.out.println("Match: " + encoder.matches(password, storedHash));
        
        String newHash = encoder.encode(password);
        System.out.println("New Hash: " + newHash);
    }
}
