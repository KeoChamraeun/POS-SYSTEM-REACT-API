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
        try { if (p.getCategory() != null) map.put("category", Map.of("id", p.getCategory().getId(), "name", p.getCategory().getName())); } catch (Exception ignored) {}
        try { if (p.getBrand() != null) map.put("brand", Map.of("id", p.getBrand().getId(), "name", p.getBrand().getName())); } catch (Exception ignored) {}
        try { if (p.getUnit() != null) map.put("unit", Map.of("id", p.getUnit().getId(), "name", p.getUnit().getName())); } catch (Exception ignored) {}
        try { if (p.getBranch() != null) map.put("branch", Map.of("id", p.getBranch().getId(), "name", p.getBranch().getName())); } catch (Exception ignored) {}
        return map;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<Product> products = effectiveBranchId != null
                ? productService.getByBranch(effectiveBranchId)
                : productService.getAllProducts();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products) result.add(toMap(p));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try { return ResponseEntity.ok(toMap(productService.getById(id))); }
        catch (Exception e) { return ResponseEntity.status(500).body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<Product> products = effectiveBranchId != null
                ? productService.searchByBranch(effectiveBranchId, q)
                : productService.searchProducts(q);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products) result.add(toMap(p));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/low-stock")
    @Transactional(readOnly = true)
    public ResponseEntity<?> lowStock(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            List<Product> products = effectiveBranchId != null
                ? productService.getLowStockByBranch(effectiveBranchId)
                : productService.getLowStockProducts();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Product p : products) result.add(toMap(p));
            return ResponseEntity.ok(result);
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
            Product product = new Product();
            setProductFields(product, body);
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (effectiveBranchId != null) {
                branchRepository.findById(effectiveBranchId).ifPresent(product::setBranch);
            }
            return ResponseEntity.ok(toMap(productService.save(product)));
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
            Product existing = productService.getById(id);
            setProductFields(existing, body);
            return ResponseEntity.ok(toMap(productService.save(existing)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private void setProductFields(Product p, Map<String, Object> body) {
        if (body.get("name") != null) p.setName((String) body.get("name"));
        if (body.get("code") != null) p.setCode((String) body.get("code"));
        if (body.get("description") != null) p.setDescription((String) body.get("description"));
        if (body.get("imageUrl") != null) p.setImageUrl((String) body.get("imageUrl"));
        if (body.get("salePrice") != null) p.setSalePrice(new BigDecimal(body.get("salePrice").toString()));
        if (body.get("costPrice") != null) p.setCostPrice(new BigDecimal(body.get("costPrice").toString()));
        if (body.get("stockQty") != null) p.setStockQty(new BigDecimal(body.get("stockQty").toString()));
        if (body.get("minStock") != null) p.setMinStock(new BigDecimal(body.get("minStock").toString()));

        // ✅ Set Category by ID
        if (body.get("categoryId") != null) {
            Long catId = Long.valueOf(body.get("categoryId").toString());
            categoryRepository.findById(catId).ifPresent(p::setCategory);
        }

        // ✅ Set Brand by ID
        if (body.get("brandId") != null) {
            Long brandId = Long.valueOf(body.get("brandId").toString());
            brandRepository.findById(brandId).ifPresent(p::setBrand);
        }
    }
}
