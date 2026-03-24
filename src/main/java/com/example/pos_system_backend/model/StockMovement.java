package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movement")
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "branch", "category", "brand", "unit"})
    private Product product;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "password", "role", "branch"})
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MovementType { IN, OUT, ADJUSTMENT }

    public StockMovement() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product v) { this.product = v; }
    public MovementType getType() { return type; }
    public void setType(MovementType v) { this.type = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String v) { this.referenceType = v; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long v) { this.referenceId = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public static StockMovementBuilder builder() { return new StockMovementBuilder(); }
    public static class StockMovementBuilder {
        private final StockMovement m = new StockMovement();
        public StockMovementBuilder product(Product v) { m.product=v; return this; }
        public StockMovementBuilder type(MovementType v) { m.type=v; return this; }
        public StockMovementBuilder quantity(BigDecimal v) { m.quantity=v; return this; }
        public StockMovementBuilder referenceType(String v) { m.referenceType=v; return this; }
        public StockMovementBuilder user(User v) { m.user=v; return this; }
        public StockMovementBuilder note(String v) { m.note=v; return this; }
        public StockMovement build() { return m; }
    }
}
