package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.dto.SaleRequest;
import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.UserRepository;
import com.example.pos_system_backend.security.JwtUtils;
import com.example.pos_system_backend.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SaleController extends BaseController {

    private final SaleService saleService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public SaleController(SaleService saleService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.saleService = saleService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    private Map<String, Object> toMap(Sale s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("invoiceNo", s.getInvoiceNo());
        map.put("subTotal", s.getSubTotal());
        map.put("discount", s.getDiscount());
        map.put("totalAmount", s.getTotalAmount());
        map.put("paidAmount", s.getPaidAmount());
        map.put("changeAmount", s.getChangeAmount());
        map.put("paymentMethod", s.getPaymentMethod());
        map.put("status", s.getStatus());
        map.put("note", s.getNote());
        map.put("saleDate", s.getSaleDate());
        try {
            if (s.getCustomer() != null) map.put("customer", Map.of("id", s.getCustomer().getId(), "name", s.getCustomer().getName()));
        } catch (Exception ignored) {}
        try {
            if (s.getBranch() != null) map.put("branch", Map.of("id", s.getBranch().getId(), "name", s.getBranch().getName()));
        } catch (Exception ignored) {}
        try {
            if (s.getUser() != null) map.put("user", Map.of("id", s.getUser().getId(), "username", s.getUser().getUsername()));
        } catch (Exception ignored) {}
        try {
            if (s.getItems() != null) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (SaleItem item : s.getItems()) {
                    Map<String, Object> imap = new LinkedHashMap<>();
                    imap.put("id", item.getId());
                    imap.put("quantity", item.getQuantity());
                    imap.put("salePrice", item.getSalePrice());
                    imap.put("discount", item.getDiscount());
                    imap.put("totalPrice", item.getTotalPrice());
                    try {
                        if (item.getProduct() != null) {
                            imap.put("product", Map.of(
                                "id", item.getProduct().getId(),
                                "name", item.getProduct().getName(),
                                "code", item.getProduct().getCode() != null ? item.getProduct().getCode() : ""
                            ));
                        }
                    } catch (Exception ignored) {}
                    items.add(imap);
                }
                map.put("items", items);
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
            List<Sale> sales = effectiveBranchId != null
                ? saleService.getSalesByBranch(effectiveBranchId)
                : saleService.getAllSales();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Sale s : sales) result.add(toMap(s));
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
            return ResponseEntity.ok(toMap(saleService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createSale(
            @RequestBody SaleRequest request,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            String username = "admin";
            if (auth != null && auth.startsWith("Bearer ")) {
                try { username = jwtUtils.extractUsername(auth.substring(7)); } catch (Exception ignored) {}
            }
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            Sale sale = saleService.createSale(request, username, effectiveBranchId);
            return ResponseEntity.ok(toMap(sale));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/today-total")
    public ResponseEntity<?> getTodayTotal(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            return ResponseEntity.ok(saleService.getTodaySales(effectiveBranchId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
