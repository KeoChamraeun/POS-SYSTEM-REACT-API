package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.SaleItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SaleItemOptionRepository extends JpaRepository<SaleItemOption, Long> {

    List<SaleItemOption> findBySaleItemId(Long saleItemId);
}