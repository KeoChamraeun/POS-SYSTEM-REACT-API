package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.branch LEFT JOIN FETCH p.sale ORDER BY p.paymentDate DESC")
    List<Payment> findAllWithDetails();

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.branch LEFT JOIN FETCH p.sale WHERE p.branch.id = :branchId ORDER BY p.paymentDate DESC")
    List<Payment> findByBranchIdOrderByPaymentDateDesc(@Param("branchId") Long branchId);
}
