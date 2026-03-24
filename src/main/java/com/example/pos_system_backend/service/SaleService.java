package com.example.pos_system_backend.service;

import com.example.pos_system_backend.dto.SaleRequest;
import com.example.pos_system_backend.model.*;
import com.example.pos_system_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
            StockMovementRepository stockMovementRepository, UserRepository userRepository,
            BranchRepository branchRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public Sale createSale(SaleRequest request, String username, Long branchId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Branch branch = branchId != null
            ? branchRepository.findById(branchId).orElse(null) : null;

        Sale sale = Sale.builder()
            .invoiceNo(generateInvoiceNo())
            .user(user).branch(branch)
            .paymentMethod(request.getPaymentMethod())
            .paidAmount(request.getPaidAmount())
            .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
            .note(request.getNote())
            .saleDate(LocalDateTime.now())
            .status(Sale.SaleStatus.COMPLETED).build();

        List<SaleItem> items = new ArrayList<>();
        for (SaleRequest.SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            if (product.getStockQty().compareTo(itemReq.getQuantity()) < 0)
                throw new RuntimeException("Insufficient stock: " + product.getName());

            product.setStockQty(product.getStockQty().subtract(itemReq.getQuantity()));
            productRepository.save(product);

            BigDecimal itemDiscount = itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO;
            BigDecimal totalPrice = product.getSalePrice().multiply(itemReq.getQuantity()).subtract(itemDiscount);

            StockMovement mv = StockMovement.builder()
                .product(product).type(StockMovement.MovementType.OUT)
                .quantity(itemReq.getQuantity()).referenceType("SALE")
                .user(user).note("Sale: " + sale.getInvoiceNo()).build();
            stockMovementRepository.save(mv);

            items.add(SaleItem.builder().sale(sale).product(product)
                .quantity(itemReq.getQuantity()).salePrice(product.getSalePrice())
                .discount(itemDiscount).totalPrice(totalPrice).build());
        }

        BigDecimal subTotal = items.stream().map(SaleItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.setSubTotal(subTotal);
        sale.setTotalAmount(subTotal.subtract(sale.getDiscount()));
        sale.setChangeAmount(sale.getPaidAmount().subtract(sale.getTotalAmount()));
        sale.setItems(items);
        return saleRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() { return saleRepository.findAllWithDetails(); }

    @Transactional(readOnly = true)
    public List<Sale> getSalesByBranch(Long branchId) { return saleRepository.findByBranchIdOrderBySaleDateDesc(branchId); }

    @Transactional(readOnly = true)
    public Sale getById(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    private String generateInvoiceNo() {
        String prefix = "INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + String.format("%04d", saleRepository.count() + 1);
    }

    public BigDecimal getTodaySales(Long branchId) {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        BigDecimal result = branchId != null
            ? saleRepository.sumSalesByBranch(branchId, start, end)
            : saleRepository.sumSalesBetween(start, end);
        return result != null ? result : BigDecimal.ZERO;
    }
}
