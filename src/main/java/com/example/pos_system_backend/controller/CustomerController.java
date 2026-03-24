package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController extends BaseController {

    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public CustomerController(CustomerRepository customerRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Customer c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName() != null ? c.getName() : "");
        map.put("phone", c.getPhone() != null ? c.getPhone() : "");
        map.put("email", c.getEmail() != null ? c.getEmail() : "");
        map.put("address", c.getAddress() != null ? c.getAddress() : "");
        map.put("loyaltyPoints", c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0);
        try {
            if (c.getBranch() != null) {
                map.put("branch", Map.of("id", c.getBranch().getId(), "name", c.getBranch().getName()));
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
            List<Customer> list = effectiveBranchId != null
                ? customerRepository.findByBranchId(effectiveBranchId)
                : customerRepository.findAllWithBranch();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Customer c : list) result.add(toMap(c));
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
            Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
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
            Customer c = new Customer();
            c.setName((String) body.get("name"));
            c.setPhone((String) body.get("phone"));
            c.setEmail((String) body.get("email"));
            c.setAddress((String) body.get("address"));

            Long userBranchId = body.get("branchId") != null
                ? Long.valueOf(body.get("branchId").toString())
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (userBranchId != null) {
                branchRepository.findById(userBranchId).ifPresent(c::setBranch);
            }

            Customer saved = customerRepository.save(c);
            Customer reloaded = customerRepository.findAllWithBranch().stream()
                .filter(cu -> cu.getId().equals(saved.getId()))
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
            Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
            if (body.get("name") != null) c.setName((String) body.get("name"));
            if (body.get("phone") != null) c.setPhone((String) body.get("phone"));
            if (body.get("email") != null) c.setEmail((String) body.get("email"));
            if (body.get("address") != null) c.setAddress((String) body.get("address"));
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(c::setBranch);
            }
            customerRepository.save(c);
            Customer reloaded = customerRepository.findAllWithBranch().stream()
                .filter(cu -> cu.getId().equals(id))
                .findFirst().orElse(c);
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
            customerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
