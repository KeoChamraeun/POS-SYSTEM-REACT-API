package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.dto.ProductOptionGroupRequest;
import com.example.pos_system_backend.model.OptionGroup;
import com.example.pos_system_backend.model.Product;
import com.example.pos_system_backend.model.ProductOptionGroup;
import com.example.pos_system_backend.repository.OptionGroupRepository;
import com.example.pos_system_backend.repository.OptionValueRepository;
import com.example.pos_system_backend.repository.ProductRepository;
import com.example.pos_system_backend.service.ProductOptionGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/options/product")
@CrossOrigin(origins = "*")
public class ProductOptionGroupController {

    private final ProductOptionGroupService productOptionGroupService;
    private final ProductRepository productRepo;
    private final OptionGroupRepository optionGroupRepo;
    private final OptionValueRepository optionValueRepo;

    public ProductOptionGroupController(
            ProductOptionGroupService productOptionGroupService,
            ProductRepository productRepo,
            OptionGroupRepository optionGroupRepo,
            OptionValueRepository optionValueRepo) {
        this.productOptionGroupService = productOptionGroupService;
        this.productRepo = productRepo;
        this.optionGroupRepo = optionGroupRepo;
        this.optionValueRepo = optionValueRepo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ASSIGN option group to product
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{productId}")
    public ResponseEntity<?> assignOptionGroup(
            @PathVariable Long productId,
            @RequestBody ProductOptionGroupRequest request) {
        try {
            if (request.getOptionGroupId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "optionGroupId is required"));

            ProductOptionGroup result = productOptionGroupService.assignOptionGroup(productId, request);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", result.getOptionGroup().getId());
            response.put("optionGroupId", result.getOptionGroup().getId());
            response.put("linkId", result.getId());
            response.put("name", result.getOptionGroup().getName());
            response.put("sortOrder", result.getSortOrder());
            response.put("isRequired", result.getOptionGroup().getIsRequired());
            response.put("isMultiple", result.getOptionGroup().getIsMultiple());
            response.put("values", List.of());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET available option groups for a product, filtered by the product's branch
    //
    // - Finds the product → reads product.branch
    // - If product has a branch → returns branch-specific + shared groups
    // - If product has no branch → returns all active groups (admin / global
    // product)
    //
    // Also enriches each group with its values so the Options tab can render them.
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProductOptionGroups(@PathVariable Long productId) {
        try {
            // 1. Load the assigned option-group links for this product
            List<ProductOptionGroup> links = productOptionGroupService.getByProductId(productId);

            // 2. Build set of already-assigned group IDs (used by frontend to mark toggles)
            Set<Long> assignedIds = new HashSet<>();
            for (ProductOptionGroup link : links) {
                if (link.getOptionGroup() != null)
                    assignedIds.add(link.getOptionGroup().getId());
            }

            // 3. Resolve the product's branch
            Product product = productRepo.findById(productId).orElse(null);
            Long productBranchId = null;
            if (product != null && product.getBranch() != null) {
                try {
                    productBranchId = product.getBranch().getId();
                } catch (Exception ignored) {
                }
            }

            // 4. Load available option groups filtered by branch
            // - product has a branch → branch groups + shared (branch IS NULL)
            // - product has no branch → all active groups
            List<OptionGroup> availableGroups = productBranchId != null
                    ? optionGroupRepo.findActiveByBranchIdOrShared(productBranchId)
                    : optionGroupRepo.findByIsActiveTrueOrderBySortOrderAsc();

            // 5. Map to response, marking which ones are already assigned
            List<Map<String, Object>> result = new ArrayList<>();
            for (OptionGroup g : availableGroups) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", g.getId());
                item.put("optionGroupId", g.getId());
                item.put("name", g.getName() != null ? g.getName() : "");
                item.put("description", g.getDescription() != null ? g.getDescription() : "");
                item.put("isRequired", g.getIsRequired() != null ? g.getIsRequired() : true);
                item.put("isMultiple", g.getIsMultiple() != null ? g.getIsMultiple() : false);
                item.put("sortOrder", g.getSortOrder() != null ? g.getSortOrder() : 0);
                item.put("assigned", assignedIds.contains(g.getId()));

                // Branch info — lets frontend show 📍 / 🌐 badges
                try {
                    if (g.getBranch() != null)
                        item.put("branch", Map.of("id", g.getBranch().getId(), "name", g.getBranch().getName()));
                } catch (Exception ignored) {
                }

                // Include values so the Options tab can show value chips
                try {
                    var values = optionValueRepo
                            .findByOptionGroupIdAndIsActiveTrueOrderBySortOrderAsc(g.getId());
                    List<Map<String, Object>> valList = new ArrayList<>();
                    for (var v : values) {
                        Map<String, Object> vm = new LinkedHashMap<>();
                        vm.put("id", v.getId());
                        vm.put("name", v.getName() != null ? v.getName() : "");
                        vm.put("priceOverride", v.getPriceOverride());
                        valList.add(vm);
                    }
                    item.put("values", valList);
                } catch (Exception e) {
                    item.put("values", List.of());
                }

                result.add(item);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REMOVE option group from product
    // ══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{productId}/{optionGroupId}")
    public ResponseEntity<?> removeOptionGroup(
            @PathVariable Long productId,
            @PathVariable Long optionGroupId) {
        try {
            productOptionGroupService.removeOptionGroup(productId, optionGroupId);
            return ResponseEntity.ok(Map.of("message", "Option group removed successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }
}