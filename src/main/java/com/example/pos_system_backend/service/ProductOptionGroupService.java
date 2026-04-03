package com.example.pos_system_backend.service;

import com.example.pos_system_backend.dto.ProductOptionGroupRequest;
import com.example.pos_system_backend.model.OptionGroup;
import com.example.pos_system_backend.model.Product;
import com.example.pos_system_backend.model.ProductOptionGroup;
import com.example.pos_system_backend.repository.OptionGroupRepository;
import com.example.pos_system_backend.repository.ProductOptionGroupRepository;
import com.example.pos_system_backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductOptionGroupService {

    private final ProductOptionGroupRepository productOptionGroupRepository;
    private final ProductRepository productRepository;
    private final OptionGroupRepository optionGroupRepository;

    public ProductOptionGroupService(
            ProductOptionGroupRepository productOptionGroupRepository,
            ProductRepository productRepository,
            OptionGroupRepository optionGroupRepository) {
        this.productOptionGroupRepository = productOptionGroupRepository;
        this.productRepository = productRepository;
        this.optionGroupRepository = optionGroupRepository;
    }

    @Transactional
    public ProductOptionGroup assignOptionGroup(Long productId, ProductOptionGroupRequest request) {
        System.out.println(
                "Service: Assigning optionGroupId=" + request.getOptionGroupId() + " to productId=" + productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        OptionGroup optionGroup = optionGroupRepository.findById(request.getOptionGroupId())
                .orElseThrow(
                        () -> new RuntimeException("Option group not found with id: " + request.getOptionGroupId()));

        if (productOptionGroupRepository.existsByProductIdAndOptionGroupId(productId, request.getOptionGroupId())) {
            throw new RuntimeException("Option group already assigned to this product");
        }

        Integer sortOrder = request.getSortOrder() != null
                ? request.getSortOrder()
                : (int) productOptionGroupRepository.countByProductId(productId);

        ProductOptionGroup link = new ProductOptionGroup(product, optionGroup, sortOrder);
        ProductOptionGroup saved = productOptionGroupRepository.save(link);

        System.out.println("Successfully saved ProductOptionGroup ID: " + saved.getId());
        return saved;
    }

    public List<ProductOptionGroup> getByProductId(Long productId) {
        return productOptionGroupRepository.findByProductId(productId);
    }

    @Transactional
    public void removeOptionGroup(Long productId, Long optionGroupId) {
        productOptionGroupRepository.deleteByProductIdAndOptionGroupId(productId, optionGroupId);
    }
}