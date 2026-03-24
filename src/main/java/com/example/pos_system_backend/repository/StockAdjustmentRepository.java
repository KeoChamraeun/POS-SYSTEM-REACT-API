package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    List<StockAdjustment> findByProductIdOrderByCreatedAtDesc(Long productId);
}
