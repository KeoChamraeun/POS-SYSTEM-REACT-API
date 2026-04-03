package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.ProductOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductOptionGroupRepository extends JpaRepository<ProductOptionGroup, Long> {

    boolean existsByProductIdAndOptionGroupId(Long productId, Long optionGroupId);

    List<ProductOptionGroup> findByProductId(Long productId);

    @Query("SELECT p FROM ProductOptionGroup p WHERE p.product.id = :productId AND p.optionGroup.id = :optionGroupId")
    Optional<ProductOptionGroup> findByProductIdAndOptionGroupId(
            @Param("productId") Long productId,
            @Param("optionGroupId") Long optionGroupId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductOptionGroup p WHERE p.product.id = :productId AND p.optionGroup.id = :optionGroupId")
    void deleteByProductIdAndOptionGroupId(
            @Param("productId") Long productId,
            @Param("optionGroupId") Long optionGroupId);

    @Query("SELECT COUNT(p) FROM ProductOptionGroup p WHERE p.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
}