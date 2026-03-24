package com.example.pos_system_backend.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase")
public class Purchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reference_no", unique = true)
    private String referenceNo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.PENDING;
    private String note;
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PurchaseItem> items;

    public enum PurchaseStatus { PENDING, RECEIVED, CANCELLED }

    public Purchase() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String v) { this.referenceNo = v; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier v) { this.supplier = v; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch v) { this.branch = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus v) { this.status = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate v) { this.purchaseDate = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> v) { this.items = v; }
}
