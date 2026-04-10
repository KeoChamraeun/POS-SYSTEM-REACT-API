package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.ShiftCashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface ShiftCashMovementRepository extends JpaRepository<ShiftCashMovement, Long> {

        // ==================== FINDERS ====================

        List<ShiftCashMovement> findByShiftIdOrderByCreatedAtDesc(Long shiftId);

        List<ShiftCashMovement> findByShiftId(Long shiftId);

        // ==================== AGGREGATES ====================
        // These are also available directly from ShiftRepository queries.
        // Kept here for convenience if needed elsewhere.

        @Query("SELECT COALESCE(SUM(m.amount), 0) FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type IN ('CASH_IN', 'FLOAT_ADD')")
        BigDecimal getTotalCashIn(@Param("shiftId") Long shiftId);

        @Query("SELECT COALESCE(SUM(m.amount), 0) FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type = 'PAID_OUT'")
        BigDecimal getTotalPaidOut(@Param("shiftId") Long shiftId);

        @Query("SELECT COALESCE(SUM(m.amount), 0) FROM ShiftCashMovement m " +
                        "WHERE m.shiftId = :shiftId AND m.type IN ('CASH_OUT', 'SAFE_DROP')")
        BigDecimal getTotalCashRemoved(@Param("shiftId") Long shiftId);
}