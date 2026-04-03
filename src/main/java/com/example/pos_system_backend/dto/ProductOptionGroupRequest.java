package com.example.pos_system_backend.dto;

public class ProductOptionGroupRequest {

    private Long optionGroupId;
    private Integer sortOrder;

    public ProductOptionGroupRequest() {
    }

    public Long getOptionGroupId() {
        return optionGroupId;
    }

    public void setOptionGroupId(Long optionGroupId) {
        this.optionGroupId = optionGroupId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}