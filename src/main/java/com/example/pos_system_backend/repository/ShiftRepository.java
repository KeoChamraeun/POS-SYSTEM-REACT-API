package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

        // ==================== FINDERS ====================

        boolean existsByBranchIdAndStatus(Long branchId, String status);

        Optional<Shift> findByBranchIdAndStatus(Long branchId, String status);

        List<Shift> findByBranchIdOrderByOpenedAtDesc(Long branchId);

        List<Shift> findByBranchIdAndBusinessDateOrderByOpenedAtDesc(Long branchId, LocalDate businessDate);

        // ✅ Admin: get all shifts regardless of branch
        List<Shift> findAllByOrderByOpenedAtDesc();

        // ✅ Admin: get all shifts filtered by date only
        List<Shift> findByBusinessDateOrderByOpenedAtDesc(LocalDate businessDate);

        // ==================== CASH SALES ====================

        @Query("SELECT COALESCE(SUM(p.amount), 0) " +
                        "FROM Payment p JOIN p.sale s " +
                        "WHERE s.shift.id = :shiftId " +
                        "AND p.paymentMethod = 'CASH' " +
                        "AND s.status = 'COMPLETED'")
        BigDecimal getTotalCashSalesByShift(@Param("shiftId") Long shiftId);

        @Query("SELECT COALESCE(SUM(p.amount), 0) " +
                        "FROM Payment p JOIN p.sale s " +
                        "WHERE s.shift.id = :shiftId " +
                        "AND p.paymentMethod != 'CASH' " +
                        "AND s.status = 'COMPLETED'")
        BigDecimal getTotalNonCashSalesByShift(@Param("shiftId") Long shiftId);

        // ==================== EXPENSES ====================

        @Query("SELECT COALESCE(SUM(e.amount), 0) " +
                        "FROM Expense e " +
                        "WHERE e.shift.id = :shiftId AND e.paidMethod = 'CASH'")
        BigDecimal getTotalCashExpenseByShift(@Param("shiftId") Long shiftId);

        // ==================== CASH MOVEMENTS ====================

        @Query("SELECT COALESCE(SUM(m.amount), 0) " +
                        "FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type IN ('CASH_IN', 'FLOAT_ADD')")
        BigDecimal getTotalCashInByShift(@Param("shiftId") Long shiftId);

        @Query("SELECT COALESCE(SUM(m.amount), 0) " +
                        "FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type = 'PAID_OUT'")
        BigDecimal getTotalPaidOutByShift(@Param("shiftId") Long shiftId);

        @Query("SELECT COALESCE(SUM(m.amount), 0) " +
                        "FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type IN ('CASH_OUT', 'SAFE_DROP')")
        BigDecimal getTotalCashRemovedByShift(@Param("shiftId") Long shiftId);
}