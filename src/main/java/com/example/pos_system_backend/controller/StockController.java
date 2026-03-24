package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController extends BaseController {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public StockController(StockAdjustmentRepository stockAdjustmentRepository,
            ProductRepository productRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/adjustments")
    public ResponseEntity<?> getAdjustments(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<StockAdjustment> list = effectiveBranchId != null
                ? stockAdjustmentRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId)
                : stockAdjustmentRepository.findAll();

            List<Map<String, Object>> result = new ArrayList<>();
            for (StockAdjustment a : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("adjustmentType", a.getAdjustmentType());
                map.put("quantity", a.getQuantity());
                map.put("reason", a.getReason());
                map.put("createdAt", a.getCreatedAt());
                if (a.getProduct() != null) {
                    map.put("product", Map.of(
                        "id", a.getProduct().getId(),
                        "name", a.getProduct().getName(),
                        "stockQty", a.getProduct().getStockQty()
                    ));
                }
                if (a.getBranch() != null) {
                    map.put("branch", Map.of("id", a.getBranch().getId(), "name", a.getBranch().getName()));
                }
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/products")
    public ResponseEntity<?> getStockProducts(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<Product> products = effectiveBranchId != null
                ? productRepository.findByBranchIdAndIsActiveTrue(effectiveBranchId)
                : productRepository.findByIsActiveTrue();

            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", p.getId());
                map.put("code", p.getCode() != null ? p.getCode() : "");
                map.put("name", p.getName());
                map.put("stockQty", p.getStockQty() != null ? p.getStockQty() : BigDecimal.ZERO);
                map.put("minStock", p.getMinStock() != null ? p.getMinStock() : BigDecimal.ZERO);
                map.put("salePrice", p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO);
                try {
                    if (p.getBranch() != null)
                        map.put("branch", Map.of("id", p.getBranch().getId(), "name", p.getBranch().getName()));
                } catch (Exception ignored) {}
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjust(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long productId = Long.valueOf(body.get("productId").toString());
            BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
            String type = (String) body.getOrDefault("adjustmentType", "ADD");
            String reason = (String) body.get("reason");

            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

            StockAdjustment adj = new StockAdjustment();
            adj.setProduct(product);
            adj.setQuantity(quantity);
            adj.setAdjustmentType(StockAdjustment.AdjustmentType.valueOf(type));
            adj.setReason(reason);

            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                branchRepository.findById(effectiveBranchId).ifPresent(adj::setBranch);
            }

            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    String username = jwtUtils.extractUsername(auth.substring(7));
                    userRepository.findByUsername(username).ifPresent(adj::setUser);
                } catch (Exception ignored) {}
            }

            switch (StockAdjustment.AdjustmentType.valueOf(type)) {
                case ADD -> product.setStockQty(product.getStockQty().add(quantity));
                case REMOVE -> {
                    if (product.getStockQty().compareTo(quantity) < 0)
                        throw new RuntimeException("Insufficient stock!");
                    product.setStockQty(product.getStockQty().subtract(quantity));
                }
                case SET -> product.setStockQty(quantity);
            }

            productRepository.save(product);
            stockAdjustmentRepository.save(adj);

            return ResponseEntity.ok(Map.of(
                "message", "Stock adjusted successfully",
                "productId", product.getId(),
                "productName", product.getName(),
                "newQty", product.getStockQty()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
