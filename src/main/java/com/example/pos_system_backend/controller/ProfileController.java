package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.User;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ProfileController(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return null;
        try {
            return jwtUtils.extractUsername(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("fullName", u.getFullName() != null ? u.getFullName() : "");
        map.put("email", u.getEmail() != null ? u.getEmail() : "");
        map.put("isActive", u.getIsActive());
        map.put("createdAt", u.getCreatedAt());
        try {
            if (u.getRole() != null)
                map.put("role", Map.of("id", u.getRole().getId(), "name", u.getRole().getName()));
        } catch (Exception ignored) {
        }
        try {
            if (u.getBranch() != null)
                map.put("branch", Map.of("id", u.getBranch().getId(), "name", u.getBranch().getName()));
        } catch (Exception ignored) {
        }
        return map;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProfile(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        try {
            String username = extractUsername(auth);
            if (username == null)
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(toMap(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    @Transactional
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        try {
            String username = extractUsername(auth);
            if (username == null)
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (body.get("fullName") != null)
                user.setFullName((String) body.get("fullName"));
            if (body.get("email") != null)
                user.setEmail((String) body.get("email"));
            return ResponseEntity.ok(toMap(userRepository.save(user)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, String> body) {
        try {
            String username = extractUsername(auth);
            if (username == null)
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");
            String confirmPassword = body.get("confirmPassword");

            if (currentPassword == null || currentPassword.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "Current password is required"));

            // ✅ Frontend sends plain text → use matches() to verify against stored bcrypt
            // hash
            if (!passwordEncoder.matches(currentPassword, user.getPassword()))
                return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));

            if (newPassword == null || newPassword.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "New password is required"));
            if (newPassword.length() < 6)
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "New password must be at least 6 characters"));
            if (!newPassword.equals(confirmPassword))
                return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));

            // ✅ Always encode the plain new password before storing
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}