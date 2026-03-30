package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findByBranchId(Long branchId);

    List<Role> findByBranchIsNull();

    boolean existsByIdAndBranchId(Long id, Long branchId);

    // ==================== ADD THESE TWO METHODS ====================
    boolean existsByNameAndBranchIsNull(String name); // For Global roles

    boolean existsByNameAndBranchId(String name, Long branchId); // For branch-scoped roles
}