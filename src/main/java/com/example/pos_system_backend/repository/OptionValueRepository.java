package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.OptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptionValueRepository extends JpaRepository<OptionValue, Long> {

    List<OptionValue> findByOptionGroupIdAndIsActiveTrueOrderBySortOrderAsc(Long optionGroupId);
}