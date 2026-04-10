package com.example.pos_system_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "paid_method")
    private String paidMethod;

    private String note;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Expense() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        this.title = v;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal v) {
        this.amount = v;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String v) {
        this.category = v;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User v) {
        this.user = v;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch v) {
        this.branch = v;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift v) {
        this.shift = v;
    }

    public String getPaidMethod() {
        return paidMethod;
    }

    public void setPaidMethod(String v) {
        this.paidMethod = v;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String v) {
        this.note = v;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate v) {
        this.expenseDate = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }
}