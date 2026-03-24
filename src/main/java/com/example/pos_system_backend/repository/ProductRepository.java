package com.example.pos_system_backend.repository;
import com.example.pos_system_backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
    List<Product> findByIsActiveTrue();
    List<Product> findByBranchIdAndIsActiveTrue(Long branchId);

    @Query("SELECT p FROM Product p WHERE p.branch.id = :branchId AND p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Product> searchByBranch(@Param("branchId") Long branchId, @Param("q") String query);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Product> searchProducts(@Param("q") String query);

    @Query("SELECT p FROM Product p WHERE p.stockQty <= p.minStock AND p.isActive = true AND p.branch.id = :branchId")
    List<Product> findLowStockByBranch(@Param("branchId") Long branchId);

    @Query("SELECT p FROM Product p WHERE p.stockQty <= p.minStock AND p.isActive = true")
    List<Product> findLowStockProducts();
}
