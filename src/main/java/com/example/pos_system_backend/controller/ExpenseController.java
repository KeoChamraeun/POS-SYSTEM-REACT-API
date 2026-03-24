package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController extends BaseController {

    private final ExpenseRepository expenseRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public ExpenseController(ExpenseRepository expenseRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.expenseRepository = expenseRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Expense e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("title", e.getTitle() != null ? e.getTitle() : "");
        map.put("amount", e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
        map.put("category", e.getCategory() != null ? e.getCategory() : "");
        map.put("note", e.getNote() != null ? e.getNote() : "");
        map.put("expenseDate", e.getExpenseDate());
        map.put("createdAt", e.getCreatedAt());
        try {
            if (e.getBranch() != null) {
                map.put("branch", Map.of("id", e.getBranch().getId(), "name", e.getBranch().getName()));
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
            List<Expense> list = effectiveBranchId != null
                ? expenseRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId)
                : expenseRepository.findAllWithBranch();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Expense e : list) result.add(toMap(e));
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
            Expense e = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
            return ResponseEntity.ok(toMap(e));
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
            Expense expense = new Expense();
            expense.setTitle((String) body.get("title"));
            expense.setAmount(new BigDecimal(body.get("amount").toString()));
            expense.setCategory((String) body.get("category"));
            expense.setNote((String) body.get("note"));
            expense.setExpenseDate(LocalDate.now());

            Long userBranchId = body.get("branchId") != null
                ? Long.valueOf(body.get("branchId").toString())
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (userBranchId != null) {
                branchRepository.findById(userBranchId).ifPresent(expense::setBranch);
            }

            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    String username = jwtUtils.extractUsername(auth.substring(7));
                    userRepository.findByUsername(username).ifPresent(expense::setUser);
                } catch (Exception ignored) {}
            }

            Expense saved = expenseRepository.save(expense);
            Expense reloaded = expenseRepository.findAllWithBranch().stream()
                .filter(ex -> ex.getId().equals(saved.getId()))
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
            Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
            if (body.get("title") != null) expense.setTitle((String) body.get("title"));
            if (body.get("amount") != null) expense.setAmount(new BigDecimal(body.get("amount").toString()));
            if (body.get("category") != null) expense.setCategory((String) body.get("category"));
            if (body.get("note") != null) expense.setNote((String) body.get("note"));
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(expense::setBranch);
            }
            expenseRepository.save(expense);
            Expense reloaded = expenseRepository.findAllWithBranch().stream()
                .filter(ex -> ex.getId().equals(id))
                .findFirst().orElse(expense);
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
            expenseRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/total")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTotal(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            BigDecimal total = effectiveBranchId != null
                ? expenseRepository.sumByBranch(effectiveBranchId)
                : expenseRepository.sumAll();
            return ResponseEntity.ok(Map.of("total", total != null ? total : BigDecimal.ZERO));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
