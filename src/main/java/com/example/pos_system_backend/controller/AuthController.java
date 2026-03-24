package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.dto.*;
import com.example.pos_system_backend.model.User;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import com.example.pos_system_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("message", "No token provided"));
            }

            String oldToken = authHeader.substring(7);

            if (!jwtUtils.isTokenValid(oldToken)) {
                return ResponseEntity.status(401).body(Map.of("message", "Token expired or invalid"));
            }

            String username = jwtUtils.extractUsername(oldToken);
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getIsActive()) {
                return ResponseEntity.status(403).body(Map.of("message", "User is inactive"));
            }

            String newToken = jwtUtils.generateToken(username);

            Set<String> permissions = Set.of();
            if (user.getRole() != null && user.getRole().getPermissions() != null) {
                permissions = user.getRole().getPermissions().stream()
                    .map(p -> p.getName()).collect(Collectors.toSet());
            }

            Long branchId = user.getBranch() != null ? user.getBranch().getId() : 1L;
            String branchName = user.getBranch() != null ? user.getBranch().getName() : "Main Branch";

            return ResponseEntity.ok(LoginResponse.builder()
                .token(newToken)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getName() : "")
                .permissions(permissions)
                .branchId(branchId)
                .branchName(branchName)
                .build());

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }
}
