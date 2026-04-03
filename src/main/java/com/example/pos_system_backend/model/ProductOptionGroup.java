package com.example.pos_system_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_option_group", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id",
        "option_group_id" }))
public class ProductOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "option_group_id", nullable = false)
    private OptionGroup optionGroup;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public ProductOptionGroup() {
    }

    public ProductOptionGroup(Product product, OptionGroup optionGroup) {
        this.product = product;
        this.optionGroup = optionGroup;
        this.sortOrder = 0;
    }

    public ProductOptionGroup(Product product, OptionGroup optionGroup, Integer sortOrder) {
        this.product = product;
        this.optionGroup = optionGroup;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public OptionGroup getOptionGroup() {
        return optionGroup;
    }

    public void setOptionGroup(OptionGroup optionGroup) {
        this.optionGroup = optionGroup;
    }

    public Integer getSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}