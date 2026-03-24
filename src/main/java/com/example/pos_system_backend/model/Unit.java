package com.example.pos_system_backend.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "unit")
public class Unit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;
    private String abbreviation;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Unit() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String v) { this.abbreviation = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
