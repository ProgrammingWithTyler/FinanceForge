package com.programmingwithtyler.financeforge.domain;

import jakarta.persistence.*;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private BudgetCategory category;

    @Column(name = "monthly_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal monthlyAllocationAmount;

    @Column(name = "current_spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentSpentAmount;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Budget() {
    }

    public Budget(Long id, BudgetCategory category, BigDecimal monthlyAllocationAmount,
                  BigDecimal currentSpentAmount, LocalDate periodStart, LocalDate periodEnd, LocalDateTime createdAt,
                  LocalDateTime updatedAt) {
        this.id = id;
        this.category = category;
        this.monthlyAllocationAmount = monthlyAllocationAmount;
        this.currentSpentAmount = currentSpentAmount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BudgetCategory getBudgetCategory() {
        return category;
    }

    public void setBudgetCategory(BudgetCategory budgetCategory) {
        this.category = budgetCategory;
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

    public void setCurrentSpentAmount(BigDecimal currentSpentAmount) {
        this.currentSpentAmount = currentSpentAmount;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return Objects.equals(id, budget.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Budget {" +
            "\nid=" + id +
            ", \nbudgetCategory=" + category +
            ", \nmonthlyAllocationAmount=" + monthlyAllocationAmount +
            ", \ncurrentSpentAmount=" + currentSpentAmount +
            ", \nperiodStart=" + periodStart +
            ", \nperiodEnd=" + periodEnd +
            ", \ncreatedAt=" + createdAt +
            ", \nupdatedAt=" + updatedAt +
            "\n}";
    }
}
