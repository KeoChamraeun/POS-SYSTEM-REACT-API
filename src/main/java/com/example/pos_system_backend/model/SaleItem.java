package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_item")
public class SaleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnoreProperties({"items", "hibernateLazyInitializer"})
    private Sale sale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "branch", "category", "brand", "unit"})
    private Product product;

    @Column(precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    public SaleItem() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sale getSale() { return sale; }
    public void setSale(Sale v) { this.sale = v; }
    public Product getProduct() { return product; }
    public void setProduct(Product v) { this.product = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal v) { this.salePrice = v; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { this.discount = v; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal v) { this.totalPrice = v; }

    public static SaleItemBuilder builder() { return new SaleItemBuilder(); }
    public static class SaleItemBuilder {
        private final SaleItem i = new SaleItem();
        public SaleItemBuilder sale(Sale v) { i.sale=v; return this; }
        public SaleItemBuilder product(Product v) { i.product=v; return this; }
        public SaleItemBuilder quantity(BigDecimal v) { i.quantity=v; return this; }
        public SaleItemBuilder salePrice(BigDecimal v) { i.salePrice=v; return this; }
        public SaleItemBuilder discount(BigDecimal v) { i.discount=v; return this; }
        public SaleItemBuilder totalPrice(BigDecimal v) { i.totalPrice=v; return this; }
        public SaleItem build() { return i; }
    }
}
