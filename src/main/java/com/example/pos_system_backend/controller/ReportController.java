package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.repository.*;
import com.example.pos_system_backend.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController extends BaseController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public ReportController(SaleRepository saleRepository, ProductRepository productRepository,
            CustomerRepository customerRepository, ExpenseRepository expenseRepository,
            PurchaseRepository purchaseRepository, PaymentRepository paymentRepository,
            UserRepository userRepository, JwtUtils jwtUtils) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.expenseRepository = expenseRepository;
        this.purchaseRepository = purchaseRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSummary(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);

            LocalDateTime startDate = from != null
                ? LocalDateTime.parse(from + "T00:00:00")
                : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endDate = to != null
                ? LocalDateTime.parse(to + "T23:59:59")
                : LocalDateTime.now();

            // Sales data
            BigDecimal totalSales = effectiveBranchId != null
                ? saleRepository.sumSalesByBranch(effectiveBranchId, startDate, endDate)
                : saleRepository.sumSalesBetween(startDate, endDate);

            // Sales by day
            List<Map<String, Object>> salesByDay = new ArrayList<>();
            LocalDateTime cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                LocalDateTime dayEnd = cursor.withHour(23).withMinute(59).withSecond(59);
                BigDecimal daySales = effectiveBranchId != null
                    ? saleRepository.sumSalesByBranch(effectiveBranchId, cursor, dayEnd)
                    : saleRepository.sumSalesBetween(cursor, dayEnd);
                Map<String, Object> day = new LinkedHashMap<>();
                day.put("date", cursor.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                day.put("day", cursor.format(DateTimeFormatter.ofPattern("EEE")));
                day.put("sales", daySales != null ? daySales : BigDecimal.ZERO);
                salesByDay.add(day);
                cursor = cursor.plusDays(1);
            }

            // Expenses
            BigDecimal totalExpenses = effectiveBranchId != null
                ? expenseRepository.sumByBranch(effectiveBranchId)
                : expenseRepository.sumAll();

            // Counts
            long totalProducts = effectiveBranchId != null
                ? productRepository.findByBranchIdAndIsActiveTrue(effectiveBranchId).size()
                : productRepository.findByIsActiveTrue().size();

            long totalCustomers = effectiveBranchId != null
                ? customerRepository.findByBranchId(effectiveBranchId).size()
                : customerRepository.count();

            long lowStock = effectiveBranchId != null
                ? productRepository.findLowStockByBranch(effectiveBranchId).size()
                : productRepository.findLowStockProducts().size();

            long totalPurchases = effectiveBranchId != null
                ? purchaseRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId).size()
                : purchaseRepository.count();

            BigDecimal profit = (totalSales != null ? totalSales : BigDecimal.ZERO)
                .subtract(totalExpenses != null ? totalExpenses : BigDecimal.ZERO);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
            result.put("totalExpenses", totalExpenses != null ? totalExpenses : BigDecimal.ZERO);
            result.put("profit", profit);
            result.put("totalProducts", totalProducts);
            result.put("totalCustomers", totalCustomers);
            result.put("lowStock", lowStock);
            result.put("totalPurchases", totalPurchases);
            result.put("salesByDay", salesByDay);
            result.put("filteredByBranch", effectiveBranchId != null);
            result.put("dateFrom", startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            result.put("dateTo", endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sales-detail")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSalesDetail(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            var sales = effectiveBranchId != null
                ? saleRepository.findByBranchIdOrderBySaleDateDesc(effectiveBranchId)
                : saleRepository.findAllWithDetails();

            List<Map<String, Object>> result = new ArrayList<>();
            for (var s : sales) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("invoiceNo", s.getInvoiceNo());
                map.put("date", s.getSaleDate());
                map.put("customer", s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in");
                map.put("branch", s.getBranch() != null ? s.getBranch().getName() : "-");
                map.put("total", s.getTotalAmount());
                map.put("paid", s.getPaidAmount());
                map.put("paymentMethod", s.getPaymentMethod());
                map.put("status", s.getStatus());
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/products-detail")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProductsDetail(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            var products = effectiveBranchId != null
                ? productRepository.findByBranchIdAndIsActiveTrue(effectiveBranchId)
                : productRepository.findByIsActiveTrue();

            List<Map<String, Object>> result = new ArrayList<>();
            for (var p : products) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("code", p.getCode() != null ? p.getCode() : "");
                map.put("name", p.getName());
                map.put("stockQty", p.getStockQty());
                map.put("minStock", p.getMinStock());
                map.put("costPrice", p.getCostPrice());
                map.put("salePrice", p.getSalePrice());
                try { map.put("branch", p.getBranch() != null ? p.getBranch().getName() : "-"); } catch (Exception ignored) {}
                try { map.put("category", p.getCategory() != null ? p.getCategory().getName() : "-"); } catch (Exception ignored) {}
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/expenses-detail")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getExpensesDetail(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        try {
            Long effectiveBranchId = getEffectiveBranchId(auth, branchId, jwtUtils, userRepository);
            var expenses = effectiveBranchId != null
                ? expenseRepository.findByBranchIdOrderByCreatedAtDesc(effectiveBranchId)
                : expenseRepository.findAllWithBranch();

            List<Map<String, Object>> result = new ArrayList<>();
            for (var e : expenses) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("title", e.getTitle());
                map.put("category", e.getCategory() != null ? e.getCategory() : "-");
                map.put("amount", e.getAmount());
                map.put("note", e.getNote() != null ? e.getNote() : "");
                map.put("date", e.getExpenseDate());
                try { map.put("branch", e.getBranch() != null ? e.getBranch().getName() : "-"); } catch (Exception ignored) {}
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
