package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.supplier LEFT JOIN FETCH p.branch ORDER BY p.createdAt DESC")
    List<Purchase> findAllWithDetails();

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.supplier LEFT JOIN FETCH p.branch WHERE p.branch.id = :branchId ORDER BY p.createdAt DESC")
    List<Purchase> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId);
}
