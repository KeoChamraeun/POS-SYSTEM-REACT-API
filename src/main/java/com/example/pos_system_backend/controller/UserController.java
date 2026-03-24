package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
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

    public UserController(UserRepository userRepository, RoleRepository roleRepository,
            BranchRepository branchRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("fullName", u.getFullName());
        map.put("email", u.getEmail());
        map.put("isActive", u.getIsActive());
        if (u.getRole() != null) map.put("role", Map.of("id", u.getRole().getId(), "name", u.getRole().getName()));
        if (u.getBranch() != null) map.put("branch", Map.of("id", u.getBranch().getId(), "name", u.getBranch().getName()));
        return map;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
        List<User> users = effectiveBranchId != null
            ? userRepository.findByBranchId(effectiveBranchId)
            : userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) result.add(toMap(u));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toMap(userRepository.findById(id).orElseThrow()));
    }

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
        user.setPassword((String) body.get("password"));
        user.setFullName((String) body.get("fullName"));
        user.setEmail((String) body.get("email"));
        user.setIsActive(true);

        if (body.get("roleId") != null) {
            roleRepository.findById(Long.valueOf(body.get("roleId").toString())).ifPresent(user::setRole);
        }

        Long userBranchId = body.get("branchId") != null
            ? Long.valueOf(body.get("branchId").toString())
            : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
        if (userBranchId != null) {
            branchRepository.findById(userBranchId).ifPresent(user::setBranch);
        }
        return ResponseEntity.ok(toMap(userRepository.save(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id).orElseThrow();
        if (body.get("fullName") != null) user.setFullName((String) body.get("fullName"));
        if (body.get("email") != null) user.setEmail((String) body.get("email"));
        if (body.get("password") != null && !((String) body.get("password")).isEmpty()) {
            user.setPassword((String) body.get("password"));
        }
        if (body.get("isActive") != null) user.setIsActive((Boolean) body.get("isActive"));
        if (body.get("roleId") != null) {
            roleRepository.findById(Long.valueOf(body.get("roleId").toString())).ifPresent(user::setRole);
        }
        if (body.get("branchId") != null) {
            branchRepository.findById(Long.valueOf(body.get("branchId").toString())).ifPresent(user::setBranch);
        }
        return ResponseEntity.ok(toMap(userRepository.save(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Deactivated"));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }
}
