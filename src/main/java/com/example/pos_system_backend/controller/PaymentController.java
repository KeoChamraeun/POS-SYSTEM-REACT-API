package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController extends BaseController {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public PaymentController(PaymentRepository paymentRepository, SaleRepository saleRepository,
            BranchRepository branchRepository, UserRepository userRepository, JwtUtils jwtUtils) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Payment p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("amount", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
        map.put("paymentMethod", p.getPaymentMethod());
        map.put("referenceNo", p.getReferenceNo() != null ? p.getReferenceNo() : "");
        map.put("paymentDate", p.getPaymentDate());
        try {
            if (p.getSale() != null) {
                map.put("sale", Map.of(
                    "id", p.getSale().getId(),
                    "invoiceNo", p.getSale().getInvoiceNo() != null ? p.getSale().getInvoiceNo() : ""
                ));
            }
        } catch (Exception ignored) {}
        try {
            if (p.getBranch() != null) {
                map.put("branch", Map.of("id", p.getBranch().getId(), "name", p.getBranch().getName()));
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
            List<Payment> payments = effectiveBranchId != null
                ? paymentRepository.findByBranchIdOrderByPaymentDateDesc(effectiveBranchId)
                : paymentRepository.findAllWithDetails();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Payment p : payments) result.add(toMap(p));
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
            Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            return ResponseEntity.ok(toMap(p));
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
            Payment payment = new Payment();
            payment.setAmount(new BigDecimal(body.get("amount").toString()));
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(
                body.getOrDefault("paymentMethod", "CASH").toString()));
            payment.setReferenceNo((String) body.get("referenceNo"));

            if (body.get("saleId") != null) {
                saleRepository.findById(Long.valueOf(body.get("saleId").toString()))
                    .ifPresent(payment::setSale);
            }

            Long userBranchId = body.get("branchId") != null
                ? Long.valueOf(body.get("branchId").toString())
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (userBranchId != null) {
                branchRepository.findById(userBranchId).ifPresent(payment::setBranch);
            }

            Payment saved = paymentRepository.save(payment);
            Payment reloaded = paymentRepository.findAllWithDetails().stream()
                .filter(p -> p.getId().equals(saved.getId()))
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
            Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

            if (body.get("amount") != null)
                payment.setAmount(new BigDecimal(body.get("amount").toString()));
            if (body.get("paymentMethod") != null)
                payment.setPaymentMethod(Payment.PaymentMethod.valueOf(body.get("paymentMethod").toString()));
            if (body.get("referenceNo") != null)
                payment.setReferenceNo((String) body.get("referenceNo"));
            if (body.get("saleId") != null) {
                saleRepository.findById(Long.valueOf(body.get("saleId").toString()))
                    .ifPresent(payment::setSale);
            }
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(payment::setBranch);
            }

            paymentRepository.save(payment);
            Payment reloaded = paymentRepository.findAllWithDetails().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst().orElse(payment);
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
            paymentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
