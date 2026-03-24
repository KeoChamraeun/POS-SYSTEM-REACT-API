package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sale")
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", unique = true)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "branch"})
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "password", "role", "branch"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Branch branch;

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
    @JsonIgnoreProperties({"sale"})
    private List<SaleItem> items;

    public enum PaymentMethod { CASH, CARD, QR, CREDIT }
    public enum SaleStatus { COMPLETED, REFUNDED, PENDING }

    public Sale() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String v) { this.invoiceNo = v; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer v) { this.customer = v; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch v) { this.branch = v; }
    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal v) { this.subTotal = v; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { this.discount = v; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal v) { this.tax = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal v) { this.changeAmount = v; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod v) { this.paymentMethod = v; }
    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus v) { this.status = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime v) { this.saleDate = v; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> v) { this.items = v; }

    public static SaleBuilder builder() { return new SaleBuilder(); }
    public static class SaleBuilder {
        private final Sale s = new Sale();
        public SaleBuilder invoiceNo(String v) { s.invoiceNo=v; return this; }
        public SaleBuilder user(User v) { s.user=v; return this; }
        public SaleBuilder branch(Branch v) { s.branch=v; return this; }
        public SaleBuilder customer(Customer v) { s.customer=v; return this; }
        public SaleBuilder paymentMethod(PaymentMethod v) { s.paymentMethod=v; return this; }
        public SaleBuilder paidAmount(BigDecimal v) { s.paidAmount=v; return this; }
        public SaleBuilder discount(BigDecimal v) { s.discount=v; return this; }
        public SaleBuilder note(String v) { s.note=v; return this; }
        public SaleBuilder saleDate(LocalDateTime v) { s.saleDate=v; return this; }
        public SaleBuilder status(SaleStatus v) { s.status=v; return this; }
        public Sale build() { return s; }
    }
}
