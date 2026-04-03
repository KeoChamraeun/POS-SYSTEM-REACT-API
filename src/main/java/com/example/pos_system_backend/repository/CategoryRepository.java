package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.Category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByBranchId(Long branchId);
}
