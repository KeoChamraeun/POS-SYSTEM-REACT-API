package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.dto.ProductOptionGroupRequest;
import com.example.pos_system_backend.model.ProductOptionGroup;
import com.example.pos_system_backend.service.ProductOptionGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/options/product")
@CrossOrigin(origins = "*")
public class ProductOptionGroupController {

    private final ProductOptionGroupService productOptionGroupService;

    public ProductOptionGroupController(ProductOptionGroupService productOptionGroupService) {
        this.productOptionGroupService = productOptionGroupService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> assignOptionGroup(
            @PathVariable Long productId,
            @RequestBody ProductOptionGroupRequest request) {
        try {
            if (request.getOptionGroupId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "optionGroupId is required"));
            }

            ProductOptionGroup result = productOptionGroupService.assignOptionGroup(productId, request);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", result.getOptionGroup().getId());
            response.put("optionGroupId", result.getOptionGroup().getId());
            response.put("linkId", result.getId());
            response.put("name", result.getOptionGroup().getName());
            response.put("sortOrder", result.getSortOrder());
            response.put("isRequired", result.getOptionGroup().getIsRequired());
            response.put("isMultiple", result.getOptionGroup().getIsMultiple());
            response.put("values", List.of()); // no values relation on OptionGroup

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductOptionGroups(@PathVariable Long productId) {
        try {
            List<ProductOptionGroup> links = productOptionGroupService.getByProductId(productId);

            List<Map<String, Object>> result = links.stream().map(link -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", link.getOptionGroup().getId());
                item.put("optionGroupId", link.getOptionGroup().getId());
                item.put("linkId", link.getId());
                item.put("name", link.getOptionGroup().getName());
                item.put("sortOrder", link.getSortOrder());
                item.put("isRequired", link.getOptionGroup().getIsRequired());
                item.put("isMultiple", link.getOptionGroup().getIsMultiple());
                item.put("values", List.of()); // no values relation on OptionGroup
                return item;
            }).toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @DeleteMapping("/{productId}/{optionGroupId}")
    public ResponseEntity<?> removeOptionGroup(
            @PathVariable Long productId,
            @PathVariable Long optionGroupId) {
        try {
            productOptionGroupService.removeOptionGroup(productId, optionGroupId);
            return ResponseEntity.ok(Map.of("message", "Option group removed successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }
}