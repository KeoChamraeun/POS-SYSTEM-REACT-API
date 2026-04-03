package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import com.example.pos_system_backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController extends BaseController {

    private final ProductService productService;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, BranchRepository branchRepository,
            CategoryRepository categoryRepository, BrandRepository brandRepository,
            JwtUtils jwtUtils, UserRepository userRepository) {
        this.productService = productService;
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    // ─── Helper: safely get string from map ──────────────────────────────────
    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    // ─── Helper: safely get BigDecimal from map ───────────────────────────────
    private BigDecimal decimal(Map<String, Object> body, String key) {
        try {
            Object v = body.get(key);
            if (v == null || v.toString().trim().isEmpty())
                return BigDecimal.ZERO;
            return new BigDecimal(v.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ─── Helper: safely get Long from map ────────────────────────────────────
    private Long longVal(Map<String, Object> body, String key) {
        try {
            Object v = body.get(key);
            if (v == null || v.toString().trim().isEmpty())
                return null;
            return Long.valueOf(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Helper: safely get branch id from JWT ───────────────────────────────
    private Long safeBranchId(String auth, Long branchId) {
        try {
            return getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
        } catch (Exception e) {
            return branchId; // fallback to header value if JWT fails
        }
    }

    // ─── Map Product to response ──────────────────────────────────────────────
    private Map<String, Object> toMap(Product p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("code", p.getCode() != null ? p.getCode() : "");
        map.put("name", p.getName() != null ? p.getName() : "");
        map.put("description", p.getDescription() != null ? p.getDescription() : "");
        map.put("costPrice", p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
        map.put("salePrice", p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO);
        map.put("stockQty", p.getStockQty() != null ? p.getStockQty() : BigDecimal.ZERO);
        map.put("minStock", p.getMinStock() != null ? p.getMinStock() : BigDecimal.ZERO);
        map.put("isActive", p.getIsActive() != null ? p.getIsActive() : true);
        map.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        try {
            if (p.getCategory() != null)
                map.put("category", Map.of("id", p.getCategory().getId(), "name", p.getCategory().getName()));
        } catch (Exception ignored) {
        }
        try {
            if (p.getBrand() != null)
                map.put("brand", Map.of("id", p.getBrand().getId(), "name", p.getBrand().getName()));
        } catch (Exception ignored) {
        }
        try {
            if (p.getUnit() != null)
                map.put("unit", Map.of("id", p.getUnit().getId(), "name", p.getUnit().getName()));
        } catch (Exception ignored) {
        }
        try {
            if (p.getBranch() != null)
                map.put("branch", Map.of("id", p.getBranch().getId(), "name", p.getBranch().getName()));
        } catch (Exception ignored) {
        }
        return map;
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = safeBranchId(auth, branchId);
            List<Product> products = effectiveBranchId != null
                    ? productService.getByBranch(effectiveBranchId)
                    : productService.getAllProducts();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products)
                result.add(toMap(p));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toMap(productService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── SEARCH ───────────────────────────────────────────────────────────────
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = safeBranchId(auth, branchId);
            List<Product> products = effectiveBranchId != null
                    ? productService.searchByBranch(effectiveBranchId, q)
                    : productService.searchProducts(q);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products)
                result.add(toMap(p));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── LOW STOCK ────────────────────────────────────────────────────────────
    @GetMapping("/low-stock")
    @Transactional(readOnly = true)
    public ResponseEntity<?> lowStock(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = safeBranchId(auth, branchId);
            List<Product> products = effectiveBranchId != null
                    ? productService.getLowStockByBranch(effectiveBranchId)
                    : productService.getLowStockProducts();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products)
                result.add(toMap(p));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            // Validate required fields
            String name = str(body, "name");
            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
            }

            Product product = new Product();
            applyFields(product, body);

            // Set branch safely
            try {
                Long effectiveBranchId = safeBranchId(auth, branchId);
                if (effectiveBranchId != null) {
                    branchRepository.findById(effectiveBranchId).ifPresent(product::setBranch);
                }
            } catch (Exception ignored) {
                // Branch not critical — continue without it
            }

            Product saved = productService.save(product);
            return ResponseEntity.ok(toMap(saved));

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.contains("Duplicate entry") && msg.contains("code")) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Product code already exists. Use a different code or leave it empty."));
            }
            if (msg.contains("Duplicate entry")) {
                return ResponseEntity.status(400).body(Map.of("error", "Duplicate entry: " + msg));
            }
            return ResponseEntity.status(500).body(Map.of("error", msg));
        }
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            // Validate required fields
            String name = str(body, "name");
            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product name is required"));
            }

            Product existing = productService.getById(id);
            applyFields(existing, body);

            Product saved = productService.save(existing);
            return ResponseEntity.ok(toMap(saved));

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.contains("Duplicate entry") && msg.contains("code")) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Product code already exists. Use a different code or leave it empty."));
            }
            if (msg.contains("Duplicate entry")) {
                return ResponseEntity.status(400).body(Map.of("error", "Duplicate entry: " + msg));
            }
            return ResponseEntity.status(500).body(Map.of("error", msg));
        }
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ─── Apply fields to product (safe, null-checked) ─────────────────────────
    private void applyFields(Product p, Map<String, Object> body) {

        // Name
        String name = str(body, "name");
        if (!name.isEmpty())
            p.setName(name);

        // Code: store null instead of "" to avoid UNIQUE constraint violation
        if (body.containsKey("code")) {
            String code = str(body, "code");
            p.setCode(code.isEmpty() ? null : code);
        }

        // Description
        if (body.containsKey("description")) {
            p.setDescription(str(body, "description"));
        }

        // Image URL (TEXT column — safe for Base64)
        if (body.containsKey("imageUrl")) {
            String img = str(body, "imageUrl");
            p.setImageUrl(img.isEmpty() ? null : img);
        }

        // Prices & stock
        if (body.containsKey("salePrice"))
            p.setSalePrice(decimal(body, "salePrice"));
        if (body.containsKey("costPrice"))
            p.setCostPrice(decimal(body, "costPrice"));
        if (body.containsKey("stockQty"))
            p.setStockQty(decimal(body, "stockQty"));
        if (body.containsKey("minStock"))
            p.setMinStock(decimal(body, "minStock"));

        // Category
        Long catId = longVal(body, "categoryId");
        if (catId != null) {
            categoryRepository.findById(catId).ifPresent(p::setCategory);
        }

        // Brand
        Long brandId = longVal(body, "brandId");
        if (brandId != null) {
            brandRepository.findById(brandId).ifPresent(p::setBrand);
        }
    }
}