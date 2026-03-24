package com.example.pos_system_backend.service;

import com.example.pos_system_backend.model.Product;
import com.example.pos_system_backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public List<Product> getByBranch(Long branchId) {
        return productRepository.findByBranchIdAndIsActiveTrue(branchId);
    }

    public List<Product> searchProducts(String q) {
        return productRepository.searchProducts(q);
    }

    public List<Product> searchByBranch(Long branchId, String q) {
        return productRepository.searchByBranch(branchId, q);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public List<Product> getLowStockByBranch(Long branchId) {
        return productRepository.findLowStockByBranch(branchId);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Transactional
    public Product save(Product product) {
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product p = getById(id);
        p.setIsActive(false);
        productRepository.save(p);
    }
}
