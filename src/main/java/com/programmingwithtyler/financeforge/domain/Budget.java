package com.programmingwithtyler.financeforge.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private BudgetCategory category;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Positive
    @Column(name = "monthly_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyAllocationAmount;

    @Column(name = "current_spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentSpentAmount = BigDecimal.ZERO;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== JPA Lifecycle Callbacks =====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Constructors =====
    protected Budget() {
    }

    public Budget(BudgetCategory category, BigDecimal monthlyAllocationAmount,
                  LocalDate periodStart, LocalDate periodEnd) {
        this.category = category;
        this.monthlyAllocationAmount = monthlyAllocationAmount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // ===== Domain Behavior Methods =====
    public void spend(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currentSpentAmount.add(amount).compareTo(monthlyAllocationAmount) > 0) {
            throw new IllegalStateException("Budget exceeded for category: " + category);
        }
        currentSpentAmount = currentSpentAmount.add(amount);
    }

    public void resetPeriod(LocalDate newStart, LocalDate newEnd) {
        this.currentSpentAmount = BigDecimal.ZERO;
        this.periodStart = newStart;
        this.periodEnd = newEnd;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    // ===== Getters (No public setters for id/timestamps/currentSpentAmount) =====
    public Long getId() {
        return id;
    }

    public BudgetCategory getCategory() {
        return category;
    }

    public void setCategory(BudgetCategory category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getMonthlyAllocationAmount() {
        return monthlyAllocationAmount;
    }

    public void setMonthlyAllocationAmount(BigDecimal monthlyAllocationAmount) {
        this.monthlyAllocationAmount = monthlyAllocationAmount;
    }

    public BigDecimal getCurrentSpentAmount() {
        return currentSpentAmount;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ===== equals & hashCode based on ID =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget)) return false;
        Budget budget = (Budget) o;
        return Objects.equals(id, budget.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ===== toString =====
    @Override
    public String toString() {
        return "Budget{" +
            "id=" + id +
            ", category=" + category +
            ", active=" + active +
            ", monthlyAllocationAmount=" + monthlyAllocationAmount +
            ", currentSpentAmount=" + currentSpentAmount +
            ", periodStart=" + periodStart +
            ", periodEnd=" + periodEnd +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
