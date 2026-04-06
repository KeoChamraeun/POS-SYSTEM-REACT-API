package com.example.pos_system_backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        // Create BCrypt encoder
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Your raw password
        String rawPassword = "Krr2903@RN";

        // Generate hash
        String hash = encoder.encode(rawPassword);

        // Print output
        System.out.println("Raw password: " + rawPassword);
        System.out.println("BCrypt hash: " + hash);
    }
}