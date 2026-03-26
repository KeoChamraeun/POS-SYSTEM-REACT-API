package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.Category;
import com.example.pos_system_backend.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    private Map<String, Object> toMap(Category c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName() != null ? c.getName() : "");
        map.put("description", c.getDescription() != null ? c.getDescription() : "");
        map.put("createdAt", c.getCreatedAt());
        map.put("updatedAt", c.getUpdatedAt());
        return map;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Category c : categoryRepository.findAll()) result.add(toMap(c));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
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
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Category c = new Category();
            c.setName((String) body.get("name"));
            c.setDescription((String) body.get("description"));
            return ResponseEntity.ok(toMap(categoryRepository.save(c)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
            if (body.get("name") != null) c.setName((String) body.get("name"));
            if (body.get("description") != null) c.setDescription((String) body.get("description"));
            c.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(toMap(categoryRepository.save(c)));
        } catch (Exception e) {
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
