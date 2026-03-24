package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.Brand;
import com.example.pos_system_backend.repository.BranchRepository;
import com.example.pos_system_backend.repository.BrandRepository;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*")
public class BrandController extends BaseController {

    private final BrandRepository brandRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public BrandController(BrandRepository brandRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.brandRepository = brandRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Brand b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("name", b.getName() != null ? b.getName() : "");
        map.put("description", b.getDescription() != null ? b.getDescription() : "");
        try {
            if (b.getBranch() != null) {
                map.put("branch", Map.of(
                    "id", b.getBranch().getId(),
                    "name", b.getBranch().getName()
                ));
            }
        } catch (Exception ignored) {}
        return map;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<Brand> brands = effectiveBranchId != null
                ? brandRepository.findByBranchId(effectiveBranchId)
                : brandRepository.findAllWithBranch();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Brand b : brands) result.add(toMap(b));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
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
            Brand brand = new Brand();
            brand.setName((String) body.get("name"));
            brand.setDescription((String) body.get("description"));
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                branchRepository.findById(effectiveBranchId).ifPresent(brand::setBranch);
            }
            Brand saved = brandRepository.save(brand);
            // reload with branch
            Brand reloaded = brandRepository.findAllWithBranch().stream()
                .filter(b -> b.getId().equals(saved.getId()))
                .findFirst().orElse(saved);
            return ResponseEntity.ok(toMap(reloaded));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
            if (body.get("name") != null) brand.setName((String) body.get("name"));
            if (body.get("description") != null) brand.setDescription((String) body.get("description"));
            brandRepository.save(brand);
            Brand reloaded = brandRepository.findAllWithBranch().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst().orElse(brand);
            return ResponseEntity.ok(toMap(reloaded));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            brandRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
