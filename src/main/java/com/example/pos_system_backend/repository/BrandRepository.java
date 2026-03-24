package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    @Query("SELECT b FROM Brand b LEFT JOIN FETCH b.branch WHERE b.branch.id = :branchId")
    List<Brand> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT b FROM Brand b LEFT JOIN FETCH b.branch")
    List<Brand> findAllWithBranch();
}
