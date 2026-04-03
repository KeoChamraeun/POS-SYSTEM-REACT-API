package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.Category;
import com.example.pos_system_backend.repository.CategoryRepository;
import com.example.pos_system_backend.repository.BranchRepository;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController extends BaseController {

    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public CategoryController(CategoryRepository categoryRepository,
            BranchRepository branchRepository,
            UserRepository userRepository,
            JwtUtils jwtUtils) {
        this.categoryRepository = categoryRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Category c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName() != null ? c.getName() : "");
        map.put("description", c.getDescription() != null ? c.getDescription() : "");
        map.put("createdAt", c.getCreatedAt());
        map.put("updatedAt", c.getUpdatedAt());

        if (c.getBranch() != null) {
            map.put("branch", Map.of(
                    "id", c.getBranch().getId(),
                    "name", c.getBranch().getName()));
        }
        return map;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

            List<Category> categories;

            // === MAIN LOGIC: Admin (role_id == 1) sees ALL, others see by branch ===
            if (effectiveBranchId == null) {
                categories = categoryRepository.findAll(); // Admin: No branch filter
            } else {
                categories = categoryRepository.findByBranchId(effectiveBranchId); // Normal user
            }

            List<Map<String, Object>> result = categories.stream()
                    .map(this::toMap)
                    .toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Category c = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            return ResponseEntity.ok(toMap(c));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        try {
            Category category = new Category();
            category.setName((String) body.get("name"));
            category.setDescription((String) body.get("description"));

            // Branch Assignment
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

            if (effectiveBranchId != null) {
                // Normal user - force their branch
                branchRepository.findById(effectiveBranchId).ifPresent(category::setBranch);
            } else if (body.get("branchId") != null) {
                // Admin can choose branch
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                        .ifPresent(category::setBranch);
            }

            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(toMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (body.get("name") != null) {
                category.setName((String) body.get("name"));
            }
            if (body.get("description") != null) {
                category.setDescription((String) body.get("description"));
            }

            category.setUpdatedAt(LocalDateTime.now());

            // Only Admin can change branch
            Long effectiveBranchId = getEffectiveBranchId(auth, null, jwtUtils, userRepository);
            if (effectiveBranchId == null && body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                        .ifPresent(category::setBranch);
            }

            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(toMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Cannot delete — category may be in use"));
        }
    }
}