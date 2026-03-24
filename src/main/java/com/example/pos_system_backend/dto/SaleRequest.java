package com.example.pos_system_backend.dto;
import com.example.pos_system_backend.model.Sale;
import java.math.BigDecimal;
import java.util.List;
public class SaleRequest {
    private Long customerId;
    private Sale.PaymentMethod paymentMethod;
    private BigDecimal paidAmount;
    private BigDecimal discount;
    private String note;
    private List<SaleItemRequest> items;
    public SaleRequest() {}
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long v) { this.customerId = v; }
    public Sale.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Sale.PaymentMethod v) { this.paymentMethod = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { this.discount = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public List<SaleItemRequest> getItems() { return items; }
    public void setItems(List<SaleItemRequest> v) { this.items = v; }
    public static class SaleItemRequest {
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal discount;
        public SaleItemRequest() {}
        public Long getProductId() { return productId; }
        public void setProductId(Long v) { this.productId = v; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal v) { this.quantity = v; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal v) { this.discount = v; }
    }
}
