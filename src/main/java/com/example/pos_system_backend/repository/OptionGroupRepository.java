package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.OptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {

    /**
     * All active groups — no branch filter.
     * Used by admin (roleId = 1) when no branchId param is sent.
     */
    List<OptionGroup> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Active groups belonging to a specific branch only.
     * (Utility — not currently used by the controller but useful for reports.)
     */
    List<OptionGroup> findByBranchIdAndIsActiveTrueOrderBySortOrderAsc(Long branchId);

    /**
     * Active groups for a branch OR shared (branch_id IS NULL).
     * Used by branch staff — they see their own groups + global shared groups.
     */
    @Query("SELECT g FROM OptionGroup g WHERE g.isActive = true " +
            "AND (g.branch.id = :branchId OR g.branch IS NULL) " +
            "ORDER BY g.sortOrder ASC")
    List<OptionGroup> findActiveByBranchIdOrShared(@Param("branchId") Long branchId);
}