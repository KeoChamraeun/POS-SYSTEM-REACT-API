package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController extends BaseController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository,
            RoleRepository roleRepository,
            BranchRepository branchRepository,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.jwtUtils = jwtUtils;
    }

    // ✅ Map User entity to response map
    private Map<String, Object> toMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("fullName", u.getFullName());
        map.put("email", u.getEmail());
        map.put("isActive", u.getIsActive());
        if (u.getRole() != null)
            map.put("role", Map.of("id", u.getRole().getId(), "name", u.getRole().getName()));
        if (u.getBranch() != null)
            map.put("branch", Map.of("id", u.getBranch().getId(), "name", u.getBranch().getName()));
        return map;
    }

    // ✅ Check if the requesting user is admin (role_id = 1) → skip branch check
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

    // ✅ Helper: check if a string is already a bcrypt hash
    private boolean isBcryptHash(String value) {
        return value != null && value.startsWith("$2");
    }

    // ✅ GET current logged-in user info
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        try {
            String token = auth.substring(7);
            String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }
            if (!user.getIsActive()) {
                return ResponseEntity.status(403).body(Map.of("message", "User is inactive"));
            }
            return ResponseEntity.ok(toMap(user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid token"));
        }
    }

    // ✅ GET all users — admin sees all, others see only their branch users
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        Long effectiveBranchId = isAdmin(auth)
                ? null
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

        List<User> users = effectiveBranchId != null
                ? userRepository.findByBranchId(effectiveBranchId)
                : userRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users)
            result.add(toMap(u));
        return ResponseEntity.ok(result);
    }

    // ✅ GET single user by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(userRepository.findById(id).orElseThrow()));
    }

    // ✅ CREATE user — hash password before saving
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        String username = (String) body.get("username");
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        User user = new User();
        user.setUsername(username);

        // ✅ Hash password — if frontend already hashed it, store as-is (it's valid
        // bcrypt)
        String rawPassword = (String) body.get("password");
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(isBcryptHash(rawPassword)
                    ? rawPassword
                    : passwordEncoder.encode(rawPassword));
        }

        user.setFullName((String) body.get("fullName"));
        user.setEmail((String) body.get("email"));
        user.setIsActive(true);

        // ✅ Resolve branch for the new user
        Long userBranchId = body.get("branchId") != null
                ? Long.valueOf(body.get("branchId").toString())
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

        if (userBranchId != null) {
            branchRepository.findById(userBranchId).ifPresent(user::setBranch);
        }

        // ✅ Validate and assign role
        if (body.get("roleId") != null) {
            Long roleId = Long.valueOf(body.get("roleId").toString());
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Role not found"));
            }

            if (!isAdmin(auth) && userBranchId != null) {
                if (role.getBranch() == null || !role.getBranch().getId().equals(userBranchId)) {
                    return ResponseEntity.status(403)
                            .body(Map.of("message", "Selected role does not belong to your branch"));
                }
            }
            user.setRole(role);
        }

        return ResponseEntity.ok(toMap(userRepository.save(user)));
    }

    // ✅ UPDATE user — hash new password before saving
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        User user = userRepository.findById(id).orElseThrow();

        if (body.get("fullName") != null)
            user.setFullName((String) body.get("fullName"));
        if (body.get("email") != null)
            user.setEmail((String) body.get("email"));

        // ✅ Hash password if provided
        if (body.get("password") != null && !((String) body.get("password")).isEmpty()) {
            String rawPassword = (String) body.get("password");
            user.setPassword(isBcryptHash(rawPassword)
                    ? rawPassword
                    : passwordEncoder.encode(rawPassword));
        }

        if (body.get("isActive") != null)
            user.setIsActive((Boolean) body.get("isActive"));

        // ✅ Update branch if provided
        if (body.get("branchId") != null) {
            branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(user::setBranch);
        }

        // ✅ Validate and assign role
        if (body.get("roleId") != null) {
            Long roleId = Long.valueOf(body.get("roleId").toString());
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Role not found"));
            }

            Long effectiveBranchId = user.getBranch() != null
                    ? user.getBranch().getId()
                    : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

            if (!isAdmin(auth) && effectiveBranchId != null) {
                if (role.getBranch() == null || !role.getBranch().getId().equals(effectiveBranchId)) {
                    return ResponseEntity.status(403)
                            .body(Map.of("message", "Selected role does not belong to the user's branch"));
                }
            }
            user.setRole(role);
        }

        return ResponseEntity.ok(toMap(userRepository.save(user)));
    }

    // ✅ SOFT DELETE — deactivate user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Deactivated"));
    }

    // ✅ TOGGLE active status
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }
}