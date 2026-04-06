package com.example.pos_system_backend.service;

import com.example.pos_system_backend.dto.LoginRequest;
import com.example.pos_system_backend.dto.LoginResponse;
import com.example.pos_system_backend.model.User;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("User is inactive");
        }

        // ✅ Correct BCrypt password check
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtils.generateToken(user.getUsername());

        Set<String> permissions = Set.of();
        if (user.getRole() != null && user.getRole().getPermissions() != null) {
            permissions = user.getRole().getPermissions().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toSet());
        }

        Long branchId = user.getBranch() != null ? user.getBranch().getId() : 1L;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : "Main Branch";

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getName() : "")
                .permissions(permissions)
                .branchId(branchId)
                .branchName(branchName)
                .build();
    }
}