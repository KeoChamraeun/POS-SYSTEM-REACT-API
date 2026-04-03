package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_item_option")
public class SaleItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Link back to the sale_item row.
     * Assumes your existing SaleItem entity has @Id Long id.
     */
    @Column(name = "sale_item_id", nullable = false)
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_group_id", nullable = false)
    @JsonIgnoreProperties({ "values", "hibernateLazyInitializer" })
    private OptionGroup optionGroup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_value_id", nullable = false)
    @JsonIgnoreProperties({ "optionGroup", "hibernateLazyInitializer" })
    private OptionValue optionValue;

    /**
     * Price locked at the moment of the sale.
     * Prevents future price edits from breaking old receipt totals.
     */
    @Column(name = "price_snapshot", precision = 15, scale = 2, nullable = false)
    private BigDecimal priceSnapshot = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────
    public SaleItemOption() {
    }

    public SaleItemOption(Long saleItemId, OptionGroup optionGroup,
            OptionValue optionValue, BigDecimal priceSnapshot) {
        this.saleItemId = saleItemId;
        this.optionGroup = optionGroup;
        this.optionValue = optionValue;
        this.priceSnapshot = priceSnapshot != null ? priceSnapshot : BigDecimal.ZERO;
    }

    // ── Getters & Setters ──────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(Long v) {
        this.saleItemId = v;
    }

    public OptionGroup getOptionGroup() {
        return optionGroup;
    }

    public void setOptionGroup(OptionGroup og) {
        this.optionGroup = og;
    }

    public OptionValue getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(OptionValue ov) {
        this.optionValue = ov;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal v) {
        this.priceSnapshot = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }
}