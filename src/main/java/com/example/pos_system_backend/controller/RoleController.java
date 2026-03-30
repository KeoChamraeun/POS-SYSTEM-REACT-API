package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.Permission;
import com.example.pos_system_backend.model.Role;
import com.example.pos_system_backend.model.User;
import com.example.pos_system_backend.repository.BranchRepository;
import com.example.pos_system_backend.repository.PermissionRepository;
import com.example.pos_system_backend.repository.RoleRepository;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController extends BaseController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public RoleController(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            JwtUtils jwtUtils) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private boolean isAdmin(String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            return false;
        try {
            String token = auth.substring(7);
            String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            return user != null && user.getRole() != null && user.getRole().getId() == 1L;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== CREATE ROLE - FIXED ====================
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        String name = ((String) body.get("name")).trim().toUpperCase();
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role name is required"));
        }

        // You may want to allow same name in different branches later
        if (roleRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role with this name already exists"));
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription((String) body.get("description"));

        // Handle permissions
        if (body.get("permissionIds") != null) {
            List<Long> ids = ((List<?>) body.get("permissionIds")).stream()
                    .map(o -> Long.valueOf(o.toString()))
                    .collect(Collectors.toList());
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(perms);
        }

        // ==================== FIXED BRANCH ASSIGNMENT ====================
        Long branchIdToSet = null;

        // Extract branchId from frontend (can be number, string, or null)
        if (body.get("branchId") != null) {
            Object branchObj = body.get("branchId");
            if (branchObj instanceof Number) {
                branchIdToSet = ((Number) branchObj).longValue();
            } else if (branchObj instanceof String) {
                String str = ((String) branchObj).trim();
                if (!str.isEmpty() && !str.equals("null")) {
                    branchIdToSet = Long.valueOf(str);
                }
            }
        }

        // Non-admin: Force assign their own branch (ignore frontend choice)
        if (!isAdmin(auth)) {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                branchIdToSet = effectiveBranchId;
            }
        }
        // Admin: Can choose any branch or leave as Global (null)

        // Set branch if provided
        if (branchIdToSet != null) {
            branchRepository.findById(branchIdToSet)
                    .ifPresent(role::setBranch);
        }
        // If branchIdToSet == null → Global role (branch remains null)

        Role saved = roleRepository.save(role);
        return ResponseEntity.ok(saved);
    }

    // ==================== UPDATE ROLE - FIXED ====================
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Security: Non-admin can only update their own branch roles
        if (!isAdmin(auth)) {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                boolean belongsToBranch = role.getBranch() != null &&
                        role.getBranch().getId().equals(effectiveBranchId);
                if (!belongsToBranch) {
                    return ResponseEntity.status(403)
                            .body(Map.of("message", "You can only update roles in your branch"));
                }
            }
        }

        // Update fields
        if (body.get("name") != null) {
            role.setName(((String) body.get("name")).trim().toUpperCase());
        }
        if (body.get("description") != null) {
            role.setDescription((String) body.get("description"));
        }

        // Update permissions
        if (body.get("permissionIds") != null) {
            List<Long> ids = ((List<?>) body.get("permissionIds")).stream()
                    .map(o -> Long.valueOf(o.toString()))
                    .collect(Collectors.toList());
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(perms);
        }

        // Allow Admin to change branch
        if (body.containsKey("branchId") && isAdmin(auth)) {
            Object branchObj = body.get("branchId");
            Long newBranchId = null;

            if (branchObj instanceof Number) {
                newBranchId = ((Number) branchObj).longValue();
            } else if (branchObj instanceof String) {
                String str = ((String) branchObj).trim();
                if (!str.isEmpty() && !str.equals("null")) {
                    newBranchId = Long.valueOf(str);
                }
            }

            if (newBranchId != null) {
                branchRepository.findById(newBranchId)
                        .ifPresent(role::setBranch);
            } else {
                role.setBranch(null); // Global
            }
        }

        Role updated = roleRepository.save(role);
        return ResponseEntity.ok(updated);
    }

    // GET all roles
    @GetMapping
    public ResponseEntity<List<Role>> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        if (isAdmin(auth)) {
            return ResponseEntity.ok(roleRepository.findAll());
        }

        Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
        if (effectiveBranchId != null) {
            return ResponseEntity.ok(roleRepository.findByBranchId(effectiveBranchId));
        }
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (!isAdmin(auth)) {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                boolean belongsToBranch = role.getBranch() != null &&
                        role.getBranch().getId().equals(effectiveBranchId);
                if (!belongsToBranch) {
                    return ResponseEntity.status(403)
                            .body(Map.of("message", "You can only delete roles in your branch"));
                }
            }
        }

        roleRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Role deleted"));
    }
}