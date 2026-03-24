package com.example.pos_system_backend.model;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_item")
public class PurchaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;
    @Column(precision = 15, scale = 2)
    private BigDecimal quantity;
    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;
    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    public PurchaseItem() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase v) { this.purchase = v; }
    public Product getProduct() { return product; }
    public void setProduct(Product v) { this.product = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal v) { this.costPrice = v; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal v) { this.totalPrice = v; }
}
