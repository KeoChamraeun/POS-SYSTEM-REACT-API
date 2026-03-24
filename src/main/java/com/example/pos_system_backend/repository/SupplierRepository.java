package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("SELECT s FROM Supplier s LEFT JOIN FETCH s.branch WHERE s.branch.id = :branchId")
    List<Supplier> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT s FROM Supplier s LEFT JOIN FETCH s.branch")
    List<Supplier> findAllWithBranch();
}
