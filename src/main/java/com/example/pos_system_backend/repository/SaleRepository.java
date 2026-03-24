package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.customer LEFT JOIN FETCH s.branch LEFT JOIN FETCH s.user ORDER BY s.saleDate DESC")
    List<Sale> findAllWithDetails();

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.customer LEFT JOIN FETCH s.branch LEFT JOIN FETCH s.user WHERE s.branch.id = :branchId ORDER BY s.saleDate DESC")
    List<Sale> findByBranchIdOrderBySaleDateDesc(@Param("branchId") Long branchId);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end AND s.status = 'COMPLETED' AND s.branch.id = :branchId")
    BigDecimal sumSalesByBranch(@Param("branchId") Long branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal sumSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end AND s.branch.id = :branchId")
    Long countSalesByBranch(@Param("branchId") Long branchId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
