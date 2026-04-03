package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/options")
@CrossOrigin(origins = "*")
public class OptionController {

    private final OptionGroupRepository optionGroupRepo;
    private final OptionValueRepository optionValueRepo;
    private final ProductOptionGroupRepository productOptionGroupRepo;
    private final SaleItemOptionRepository saleItemOptionRepo;
    private final ProductRepository productRepo;

    public OptionController(
            OptionGroupRepository optionGroupRepo,
            OptionValueRepository optionValueRepo,
            ProductOptionGroupRepository productOptionGroupRepo,
            SaleItemOptionRepository saleItemOptionRepo,
            ProductRepository productRepo) {
        this.optionGroupRepo = optionGroupRepo;
        this.optionValueRepo = optionValueRepo;
        this.productOptionGroupRepo = productOptionGroupRepo;
        this.saleItemOptionRepo = saleItemOptionRepo;
        this.productRepo = productRepo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private Boolean bool(Map<String, Object> body, String key, Boolean def) {
        Object v = body.get(key);
        if (v == null)
            return def;
        if (v instanceof Boolean)
            return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private Integer intVal(Map<String, Object> body, String key, int def) {
        try {
            Object v = body.get(key);
            return v == null ? def : Integer.parseInt(v.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal decimal(Map<String, Object> body, String key) {
        try {
            Object v = body.get(key);
            if (v == null || v.toString().trim().isEmpty())
                return null;
            return new BigDecimal(v.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> valueToMap(OptionValue v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("name", v.getName() != null ? v.getName() : "");
        m.put("priceOverride", v.getPriceOverride());
        m.put("sortOrder", v.getSortOrder());
        m.put("isActive", v.getIsActive());
        return m;
    }

    private Map<String, Object> groupToMap(OptionGroup g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName() != null ? g.getName() : "");
        m.put("description", g.getDescription() != null ? g.getDescription() : "");
        m.put("isRequired", g.getIsRequired() != null ? g.getIsRequired() : true);
        m.put("isMultiple", g.getIsMultiple() != null ? g.getIsMultiple() : false);
        m.put("sortOrder", g.getSortOrder() != null ? g.getSortOrder() : 0);
        m.put("isActive", g.getIsActive() != null ? g.getIsActive() : true);

        try {
            if (g.getBranch() != null)
                m.put("branch", Map.of("id", g.getBranch().getId(), "name", g.getBranch().getName()));
        } catch (Exception ignored) {
        }

        try {
            List<OptionValue> values = optionValueRepo
                    .findByOptionGroupIdAndIsActiveTrueOrderBySortOrderAsc(g.getId());
            List<Map<String, Object>> valList = new ArrayList<>();
            for (OptionValue v : values)
                valList.add(valueToMap(v));
            m.put("values", valList);
        } catch (Exception e) {
            m.put("values", new ArrayList<>());
        }
        return m;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OPTION GROUPS — CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listGroups() {
        try {
            List<OptionGroup> groups = optionGroupRepo.findByIsActiveTrueOrderBySortOrderAsc();
            List<Map<String, Object>> result = new ArrayList<>();
            for (OptionGroup g : groups)
                result.add(groupToMap(g));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load option groups"));
        }
    }

    @PostMapping("/groups")
    @Transactional
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> body) {
        try {
            String name = str(body, "name");
            if (name.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "name is required"));

            OptionGroup g = new OptionGroup();
            g.setName(name);
            g.setDescription(str(body, "description"));
            g.setIsRequired(bool(body, "isRequired", true));
            g.setIsMultiple(bool(body, "isMultiple", false));
            g.setSortOrder(intVal(body, "sortOrder", 0));
            g.setIsActive(true);

            OptionGroup saved = optionGroupRepo.save(g);
            return ResponseEntity.ok(groupToMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create option group"));
        }
    }

    @PutMapping("/groups/{id}")
    @Transactional
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            OptionGroup g = optionGroupRepo.findById(id).orElse(null);
            if (g == null)
                return ResponseEntity.status(404).body(Map.of("error", "Option group not found"));

            String name = str(body, "name");
            if (!name.isEmpty())
                g.setName(name);
            if (body.containsKey("description"))
                g.setDescription(str(body, "description"));
            if (body.containsKey("isRequired"))
                g.setIsRequired(bool(body, "isRequired", g.getIsRequired()));
            if (body.containsKey("isMultiple"))
                g.setIsMultiple(bool(body, "isMultiple", g.getIsMultiple()));
            if (body.containsKey("sortOrder"))
                g.setSortOrder(intVal(body, "sortOrder", g.getSortOrder()));
            if (body.containsKey("isActive"))
                g.setIsActive(bool(body, "isActive", g.getIsActive()));

            OptionGroup saved = optionGroupRepo.save(g);
            return ResponseEntity.ok(groupToMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update option group"));
        }
    }

    @DeleteMapping("/groups/{id}")
    @Transactional
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        try {
            OptionGroup g = optionGroupRepo.findById(id).orElse(null);
            if (g == null)
                return ResponseEntity.status(404).body(Map.of("error", "Option group not found"));
            g.setIsActive(false);
            optionGroupRepo.save(g);
            return ResponseEntity.ok(Map.of("message", "Option group deactivated"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete option group"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OPTION VALUES — CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/groups/{gid}/values")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listValues(@PathVariable Long gid) {
        try {
            List<OptionValue> values = optionValueRepo
                    .findByOptionGroupIdAndIsActiveTrueOrderBySortOrderAsc(gid);
            List<Map<String, Object>> result = new ArrayList<>();
            for (OptionValue v : values)
                result.add(valueToMap(v));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load option values"));
        }
    }

    @PostMapping("/groups/{gid}/values")
    @Transactional
    public ResponseEntity<?> createValue(@PathVariable Long gid, @RequestBody Map<String, Object> body) {
        try {
            String name = str(body, "name");
            if (name.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("error", "name is required"));

            OptionGroup group = optionGroupRepo.findById(gid).orElse(null);
            if (group == null)
                return ResponseEntity.status(404).body(Map.of("error", "Option group not found"));

            OptionValue v = new OptionValue();
            v.setOptionGroup(group);
            v.setName(name);
            v.setPriceOverride(decimal(body, "priceOverride"));
            v.setSortOrder(intVal(body, "sortOrder", 0));
            v.setIsActive(true);

            OptionValue saved = optionValueRepo.save(v);
            return ResponseEntity.ok(valueToMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create option value"));
        }
    }

    @PutMapping("/values/{id}")
    @Transactional
    public ResponseEntity<?> updateValue(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            OptionValue v = optionValueRepo.findById(id).orElse(null);
            if (v == null)
                return ResponseEntity.status(404).body(Map.of("error", "Option value not found"));

            String name = str(body, "name");
            if (!name.isEmpty())
                v.setName(name);
            if (body.containsKey("priceOverride"))
                v.setPriceOverride(decimal(body, "priceOverride"));
            if (body.containsKey("sortOrder"))
                v.setSortOrder(intVal(body, "sortOrder", v.getSortOrder()));
            if (body.containsKey("isActive"))
                v.setIsActive(bool(body, "isActive", v.getIsActive()));

            OptionValue saved = optionValueRepo.save(v);
            return ResponseEntity.ok(valueToMap(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update option value"));
        }
    }

    @DeleteMapping("/values/{id}")
    @Transactional
    public ResponseEntity<?> deleteValue(@PathVariable Long id) {
        try {
            OptionValue v = optionValueRepo.findById(id).orElse(null);
            if (v == null)
                return ResponseEntity.status(404).body(Map.of("error", "Option value not found"));
            v.setIsActive(false);
            optionValueRepo.save(v);
            return ResponseEntity.ok(Map.of("message", "Option value deactivated"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete option value"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRODUCT ↔ OPTION GROUP MAPPING
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/product/{pid}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProductOptions(@PathVariable Long pid) {
        try {
            List<ProductOptionGroup> mappings = productOptionGroupRepo.findByProductId(pid);
            List<Map<String, Object>> result = new ArrayList<>();
            for (ProductOptionGroup pog : mappings) {
                OptionGroup g = pog.getOptionGroup();
                if (g != null && Boolean.TRUE.equals(g.getIsActive()))
                    result.add(groupToMap(g));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load product options"));
        }
    }

    @PostMapping("/product/{pid}")
    @Transactional
    public ResponseEntity<?> assignOptionToProduct(@PathVariable Long pid,
            @RequestBody Map<String, Object> body) {
        try {
            System.out.println("=== ASSIGN OPTION TO PRODUCT ===");
            System.out.println("Product ID: " + pid);
            System.out.println("Request Body: " + body);

            Object gidRaw = body.get("optionGroupId");
            if (gidRaw == null)
                return ResponseEntity.badRequest().body(Map.of("error", "optionGroupId is required"));

            Long groupId;
            try {
                groupId = Long.valueOf(gidRaw.toString());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "optionGroupId must be a valid number"));
            }

            Product product = productRepo.findById(pid).orElse(null);
            if (product == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Product not found with id: " + pid));

            OptionGroup group = optionGroupRepo.findById(groupId).orElse(null);
            if (group == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Option group not found with id: " + groupId));

            if (productOptionGroupRepo.existsByProductIdAndOptionGroupId(pid, groupId))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Option group already assigned to this product"));

            Integer sortOrder = intVal(body, "sortOrder", 0);

            ProductOptionGroup pog = new ProductOptionGroup(product, group, sortOrder);
            productOptionGroupRepo.save(pog);

            System.out.println("✅ SUCCESS: Assigned option group " + groupId + " to product " + pid);

            return ResponseEntity.ok(Map.of(
                    "message", "Option group assigned successfully",
                    "productId", pid,
                    "optionGroupId", groupId,
                    "sortOrder", sortOrder));

        } catch (Exception e) {
            System.err.println("🔥 ERROR in assignOptionToProduct:");
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to assign option group: " + e.getMessage()));
        }
    }

    @DeleteMapping("/product/{pid}/{gid}")
    @Transactional
    public ResponseEntity<?> removeOptionFromProduct(@PathVariable Long pid,
            @PathVariable Long gid) {
        try {
            System.out.println("=== REMOVE OPTION FROM PRODUCT ===");
            System.out.println("Product ID: " + pid + ", OptionGroup ID: " + gid);

            // ✅ FIXED: use renamed method
            Optional<ProductOptionGroup> existing = productOptionGroupRepo
                    .findByProductIdAndOptionGroupId(pid, gid);
            if (existing.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Option group mapping not found"));
            }

            productOptionGroupRepo.delete(existing.get());
            System.out.println("✅ SUCCESS: Removed option group " + gid + " from product " + pid);

            return ResponseEntity.ok(Map.of("message", "Option group removed successfully"));

        } catch (Exception e) {
            System.err.println("🔥 ERROR in removeOptionFromProduct:");
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to remove option group: " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SALE ITEM OPTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/sale-item/{saleItemId}")
    @Transactional
    public ResponseEntity<?> saveSaleItemOptions(@PathVariable Long saleItemId,
            @RequestBody List<Map<String, Object>> selections) {
        try {
            List<Map<String, Object>> saved = new ArrayList<>();
            for (Map<String, Object> sel : selections) {
                try {
                    Long groupId = Long.valueOf(sel.get("optionGroupId").toString());
                    Long valueId = Long.valueOf(sel.get("optionValueId").toString());

                    OptionGroup group = optionGroupRepo.findById(groupId).orElse(null);
                    OptionValue value = optionValueRepo.findById(valueId).orElse(null);
                    if (group == null || value == null)
                        continue;

                    BigDecimal snapshot = value.getPriceOverride() != null
                            ? value.getPriceOverride()
                            : BigDecimal.ZERO;

                    SaleItemOption sio = new SaleItemOption(saleItemId, group, value, snapshot);
                    SaleItemOption result = saleItemOptionRepo.save(sio);

                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", result.getId());
                    r.put("saleItemId", saleItemId);
                    r.put("optionGroup", Map.of("id", group.getId(), "name", group.getName()));
                    r.put("optionValue", Map.of("id", value.getId(), "name", value.getName()));
                    r.put("priceSnapshot", snapshot);
                    saved.add(r);
                } catch (Exception ignored) {
                }
            }
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save sale item options"));
        }
    }

    @GetMapping("/sale-item/{saleItemId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSaleItemOptions(@PathVariable Long saleItemId) {
        try {
            List<SaleItemOption> options = saleItemOptionRepo.findBySaleItemId(saleItemId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (SaleItemOption s : options) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("optionGroup", Map.of("id", s.getOptionGroup().getId(), "name", s.getOptionGroup().getName()));
                m.put("optionValue", Map.of("id", s.getOptionValue().getId(), "name", s.getOptionValue().getName()));
                m.put("priceSnapshot", s.getPriceSnapshot());
                result.add(m);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load sale item options"));
        }
    }
}