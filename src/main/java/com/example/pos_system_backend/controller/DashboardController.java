package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController extends BaseController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRepository purchaseRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public DashboardController(SaleRepository saleRepository, ProductRepository productRepository,
            CustomerRepository customerRepository, ExpenseRepository expenseRepository,
            PurchaseRepository purchaseRepository, JwtUtils jwtUtils, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.expenseRepository = expenseRepository;
        this.purchaseRepository = purchaseRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {

        Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now();

        BigDecimal todaySales = effectiveBranchId != null
            ? saleRepository.sumSalesByBranch(effectiveBranchId, startOfDay, endOfDay)
            : saleRepository.sumSalesBetween(startOfDay, endOfDay);

        long totalProducts = effectiveBranchId != null
            ? productRepository.findByBranchIdAndIsActiveTrue(effectiveBranchId).size()
            : productRepository.findByIsActiveTrue().size();

        long totalCustomers = effectiveBranchId != null
            ? customerRepository.findByBranchId(effectiveBranchId).size()
            : customerRepository.count();

        long lowStock = effectiveBranchId != null
            ? productRepository.findLowStockByBranch(effectiveBranchId).size()
            : productRepository.findLowStockProducts().size();

        BigDecimal totalExpenses = effectiveBranchId != null
            ? expenseRepository.sumByBranch(effectiveBranchId) : BigDecimal.ZERO;

        long totalPurchases = effectiveBranchId != null
            ? purchaseRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId).size()
            : purchaseRepository.count();

        List<Map<String, Object>> salesChart = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = LocalDateTime.now().minusDays(i).withHour(23).withMinute(59).withSecond(59);
            BigDecimal daySales = effectiveBranchId != null
                ? saleRepository.sumSalesByBranch(effectiveBranchId, dayStart, dayEnd)
                : saleRepository.sumSalesBetween(dayStart, dayEnd);
            Map<String, Object> day = new HashMap<>();
            day.put("day", dayStart.getDayOfWeek().toString().substring(0, 3));
            day.put("sales", daySales != null ? daySales : BigDecimal.ZERO);
            salesChart.add(day);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("todaySales", todaySales != null ? todaySales : BigDecimal.ZERO);
        stats.put("totalProducts", totalProducts);
        stats.put("totalCustomers", totalCustomers);
        stats.put("lowStock", lowStock);
        stats.put("totalExpenses", totalExpenses != null ? totalExpenses : BigDecimal.ZERO);
        stats.put("totalPurchases", totalPurchases);
        stats.put("salesChart", salesChart);
        stats.put("filteredByBranch", effectiveBranchId != null);

        return ResponseEntity.ok(stats);
    }
}
