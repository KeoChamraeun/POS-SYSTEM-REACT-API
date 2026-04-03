package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.OptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {

    /** All active groups (shared + branch-specific) */
    List<OptionGroup> findByIsActiveTrueOrderBySortOrderAsc();

    /** All active groups for a specific branch OR shared (branch_id = null) */
    List<OptionGroup> findByIsActiveTrueAndBranchIdIsNullOrIsActiveTrueAndBranchIdOrderBySortOrderAsc(
            Long branchId);
}