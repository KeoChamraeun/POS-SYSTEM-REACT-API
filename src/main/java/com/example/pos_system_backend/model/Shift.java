package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "shift_code", unique = true, nullable = false, length = 30)
    private String shiftCode;

    @Column(name = "shift_name", nullable = false, length = 20)
    private String shiftName; // Changed to String

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "opened_by", nullable = false)
    private Long openedBy;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_float", precision = 12, scale = 2, nullable = false)
    private BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "closing_counted_cash", precision = 12, scale = 2)
    private BigDecimal closingCountedCash;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "cash_difference", precision = 12, scale = 2)
    private BigDecimal cashDifference;

    @Column(name = "total_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_cash_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(name = "total_non_cash_sales", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalNonCashSales = BigDecimal.ZERO;

    @Column(name = "total_expense", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 10)
    private String status = "OPEN"; // Changed to String

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ==================== GETTERS & SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public Long getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(Long openedBy) {
        this.openedBy = openedBy;
    }

    public Long getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(Long closedBy) {
        this.closedBy = closedBy;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningFloat() {
        return openingFloat;
    }

    public void setOpeningFloat(BigDecimal openingFloat) {
        this.openingFloat = openingFloat;
    }

    public BigDecimal getClosingCountedCash() {
        return closingCountedCash;
    }

    public void setClosingCountedCash(BigDecimal closingCountedCash) {
        this.closingCountedCash = closingCountedCash;
    }

    public BigDecimal getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(BigDecimal expectedCash) {
        this.expectedCash = expectedCash;
    }

    public BigDecimal getCashDifference() {
        return cashDifference;
    }

    public void setCashDifference(BigDecimal cashDifference) {
        this.cashDifference = cashDifference;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getTotalCashSales() {
        return totalCashSales;
    }

    public void setTotalCashSales(BigDecimal totalCashSales) {
        this.totalCashSales = totalCashSales;
    }

    public BigDecimal getTotalNonCashSales() {
        return totalNonCashSales;
    }

    public void setTotalNonCashSales(BigDecimal totalNonCashSales) {
        this.totalNonCashSales = totalNonCashSales;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}