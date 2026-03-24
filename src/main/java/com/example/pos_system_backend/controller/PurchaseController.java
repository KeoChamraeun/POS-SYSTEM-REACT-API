package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*")
public class PurchaseController extends BaseController {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public PurchaseController(PurchaseRepository purchaseRepository, ProductRepository productRepository,
            SupplierRepository supplierRepository, BranchRepository branchRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Map<String, Object> toMap(Purchase p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("referenceNo", p.getReferenceNo());
        map.put("totalAmount", p.getTotalAmount());
        map.put("paidAmount", p.getPaidAmount());
        map.put("status", p.getStatus());
        map.put("purchaseDate", p.getPurchaseDate());
        map.put("note", p.getNote());
        try {
            if (p.getSupplier() != null) {
                map.put("supplier", Map.of("id", p.getSupplier().getId(), "name", p.getSupplier().getName()));
            }
        } catch (Exception ignored) {}
        try {
            if (p.getBranch() != null) {
                map.put("branch", Map.of("id", p.getBranch().getId(), "name", p.getBranch().getName()));
            }
        } catch (Exception ignored) {}
        try {
            if (p.getItems() != null) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (PurchaseItem item : p.getItems()) {
                    Map<String, Object> imap = new LinkedHashMap<>();
                    imap.put("id", item.getId());
                    imap.put("quantity", item.getQuantity());
                    imap.put("costPrice", item.getCostPrice());
                    imap.put("totalPrice", item.getTotalPrice());
                    if (item.getProduct() != null) {
                        imap.put("product", Map.of(
                            "id", item.getProduct().getId(),
                            "name", item.getProduct().getName(),
                            "stockQty", item.getProduct().getStockQty()
                        ));
                    }
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
            List<Purchase> purchases = effectiveBranchId != null
                ? purchaseRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId)
                : purchaseRepository.findAllWithDetails();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Purchase p : purchases) result.add(toMap(p));
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
            Purchase p = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));
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
            Purchase purchase = new Purchase();
            String prefix = "PO" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            purchase.setReferenceNo(prefix + String.format("%04d", purchaseRepository.count() + 1));
            purchase.setPurchaseDate(LocalDate.now());
            purchase.setNote((String) body.get("note"));
            purchase.setStatus(Purchase.PurchaseStatus.RECEIVED);

            if (body.get("supplierId") != null) {
                supplierRepository.findById(Long.valueOf(body.get("supplierId").toString()))
                    .ifPresent(purchase::setSupplier);
            }

            Long userBranchId = body.get("branchId") != null
                ? Long.valueOf(body.get("branchId").toString())
                : getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            if (userBranchId != null) {
                branchRepository.findById(userBranchId).ifPresent(purchase::setBranch);
            }

            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    String username = jwtUtils.extractUsername(auth.substring(7));
                    userRepository.findByUsername(username).ifPresent(purchase::setUser);
                } catch (Exception ignored) {}
            }

            List<PurchaseItem> items = new ArrayList<>();
            if (body.get("items") != null) {
                List<Map<String, Object>> itemList = (List<Map<String, Object>>) body.get("items");
                for (Map<String, Object> itemData : itemList) {
                    if (itemData.get("productId") == null || itemData.get("quantity") == null) continue;
                    Long productId = Long.valueOf(itemData.get("productId").toString());
                    BigDecimal qty = new BigDecimal(itemData.get("quantity").toString());
                    BigDecimal price = new BigDecimal(itemData.getOrDefault("costPrice", "0").toString());

                    Product product = productRepository.findById(productId).orElse(null);
                    if (product != null) {
                        PurchaseItem item = new PurchaseItem();
                        item.setPurchase(purchase);
                        item.setProduct(product);
                        item.setQuantity(qty);
                        item.setCostPrice(price);
                        item.setTotalPrice(qty.multiply(price));
                        items.add(item);

                        product.setStockQty(product.getStockQty().add(qty));
                        productRepository.save(product);
                    }
                }
            }

            BigDecimal total = items.stream()
                .map(PurchaseItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            purchase.setTotalAmount(total);
            purchase.setPaidAmount(body.get("paidAmount") != null
                ? new BigDecimal(body.get("paidAmount").toString()) : BigDecimal.ZERO);
            purchase.setItems(items);

            Purchase saved = purchaseRepository.save(purchase);
            return ResponseEntity.ok(toMap(saved));
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
            Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));
            if (body.get("note") != null) purchase.setNote((String) body.get("note"));
            if (body.get("paidAmount") != null) purchase.setPaidAmount(new BigDecimal(body.get("paidAmount").toString()));
            if (body.get("status") != null) purchase.setStatus(Purchase.PurchaseStatus.valueOf((String) body.get("status")));
            if (body.get("supplierId") != null) {
                supplierRepository.findById(Long.valueOf(body.get("supplierId").toString()))
                    .ifPresent(purchase::setSupplier);
            }
            if (body.get("branchId") != null) {
                branchRepository.findById(Long.valueOf(body.get("branchId").toString()))
                    .ifPresent(purchase::setBranch);
            }
            return ResponseEntity.ok(toMap(purchaseRepository.save(purchase)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            purchaseRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
