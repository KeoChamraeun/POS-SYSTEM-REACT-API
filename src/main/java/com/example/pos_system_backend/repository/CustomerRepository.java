package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.branch ORDER BY c.name ASC")
    List<Customer> findAllWithBranch();

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.branch WHERE c.branch.id = :branchId ORDER BY c.name ASC")
    List<Customer> findByBranchId(@Param("branchId") Long branchId);
}
