package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.User;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;

public abstract class BaseController {

    protected Long getEffectiveBranchId(String authHeader, Long branchId,
            JwtUtils jwtUtils, UserRepository userRepository) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return branchId;
            }

            String token = authHeader.substring(7);
            String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) return branchId;

            // role_id = 1 = ADMIN → return NULL = no filter = see ALL
            if (user.getRole() != null && user.getRole().getId() != null
                    && user.getRole().getId() == 1L) {
                return null;
            }

            // Other roles → filter by user's branch
            if (user.getBranch() != null) {
                return user.getBranch().getId();
            }

            return branchId;

        } catch (Exception e) {
            return branchId;
        }
    }

    protected boolean isAdmin(String authHeader, JwtUtils jwtUtils, UserRepository userRepository) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
            String username = jwtUtils.extractUsername(authHeader.substring(7));
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return false;
            return user.getRole() != null && user.getRole().getId() != null
                && user.getRole().getId() == 1L;
        } catch (Exception e) {
            return false;
        }
    }
}
