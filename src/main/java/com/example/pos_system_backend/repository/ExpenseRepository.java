package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.branch ORDER BY e.createdAt DESC")
    List<Expense> findAllWithBranch();

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.branch WHERE e.branch.id = :branchId ORDER BY e.createdAt DESC")
    List<Expense> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.branch.id = :branchId")
    BigDecimal sumByBranch(@Param("branchId") Long branchId);

    @Query("SELECT SUM(e.amount) FROM Expense e")
    BigDecimal sumAll();
}
