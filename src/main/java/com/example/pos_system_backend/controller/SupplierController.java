package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController extends BaseController {

    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public SupplierController(SupplierRepository supplierRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.supplierRepository = supplierRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Supplier s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getName() != null ? s.getName() : "");
        map.put("phone", s.getPhone() != null ? s.getPhone() : "");
        map.put("email", s.getEmail() != null ? s.getEmail() : "");
        map.put("address", s.getAddress() != null ? s.getAddress() : "");
        try {
            if (s.getBranch() != null) {
                map.put("branch", Map.of("id", s.getBranch().getId(), "name", s.getBranch().getName()));
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
            List<Supplier> suppliers = effectiveBranchId != null
                ? supplierRepository.findByBranchId(effectiveBranchId)
                : supplierRepository.findAllWithBranch();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Supplier s : suppliers) result.add(toMap(s));
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
            Supplier supplier = new Supplier();
            supplier.setName((String) body.get("name"));
            supplier.setPhone((String) body.get("phone"));
            supplier.setEmail((String) body.get("email"));
            supplier.setAddress((String) body.get("address"));

            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(supplier::setBranch);
            } else if (effectiveBranchId != null) {
                branchRepository.findById(effectiveBranchId).ifPresent(supplier::setBranch);
            }

            Supplier saved = supplierRepository.save(supplier);
            Supplier reloaded = supplierRepository.findAllWithBranch().stream()
                .filter(s -> s.getId().equals(saved.getId()))
                .findFirst().orElse(saved);
            return ResponseEntity.ok(toMap(reloaded));
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
            Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
            if (body.get("name") != null) supplier.setName((String) body.get("name"));
            if (body.get("phone") != null) supplier.setPhone((String) body.get("phone"));
            if (body.get("email") != null) supplier.setEmail((String) body.get("email"));
            if (body.get("address") != null) supplier.setAddress((String) body.get("address"));
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(supplier::setBranch);
            }
            supplierRepository.save(supplier);
            Supplier reloaded = supplierRepository.findAllWithBranch().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(supplier);
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
            supplierRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
