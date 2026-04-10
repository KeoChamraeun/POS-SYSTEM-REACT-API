package com.example.pos_system_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", unique = true)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "branch" })
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "password", "role", "branch" })
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Branch branch;

    // ==================== FIXED SHIFT RELATIONSHIP ====================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false) // nullable=false is recommended
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Shift shift;

    @Column(name = "sub_total", precision = 15, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 15, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    private SaleStatus status = SaleStatus.COMPLETED;

    private String note;

    @Column(name = "sale_date")
    private LocalDateTime saleDate = LocalDateTime.now();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "sale" })
    private List<SaleItem> items;

    public enum PaymentMethod {
        CASH, CARD, QR, CREDIT
    }

    public enum SaleStatus {
        COMPLETED, REFUNDED, PENDING
    }

    public Sale() {
    }

    // ==================== GETTERS & SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public void setStatus(SaleStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    // Builder
    public static SaleBuilder builder() {
        return new SaleBuilder();
    }

    public static class SaleBuilder {
        private final Sale s = new Sale();

        public SaleBuilder invoiceNo(String v) {
            s.invoiceNo = v;
            return this;
        }

        public SaleBuilder user(User v) {
            s.user = v;
            return this;
        }

        public SaleBuilder branch(Branch v) {
            s.branch = v;
            return this;
        }

        public SaleBuilder customer(Customer v) {
            s.customer = v;
            return this;
        }

        public SaleBuilder paymentMethod(PaymentMethod v) {
            s.paymentMethod = v;
            return this;
        }

        public SaleBuilder paidAmount(BigDecimal v) {
            s.paidAmount = v;
            return this;
        }

        public SaleBuilder discount(BigDecimal v) {
            s.discount = v;
            return this;
        }

        public SaleBuilder note(String v) {
            s.note = v;
            return this;
        }

        public SaleBuilder saleDate(LocalDateTime v) {
            s.saleDate = v;
            return this;
        }

        public SaleBuilder status(SaleStatus v) {
            s.status = v;
            return this;
        }

        public SaleBuilder shift(Shift v) {
            s.shift = v;
            return this;
        }

        public Sale build() {
            return s;
        }
    }
}