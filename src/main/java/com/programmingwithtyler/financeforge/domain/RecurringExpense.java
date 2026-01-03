package com.programmingwithtyler.financeforge.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "recurring_transactions")
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frequency", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionFrequency frequency;

    @Column(name = "next_scheduled_date", nullable = false)
    private LocalDate nextScheduledDate;

    // ===== Template Information (Transaction Blueprint) =====
    @Positive
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank
    @Column(name = "description", nullable = false)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "budget_category", nullable = false)
    private BudgetCategory category;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RecurringStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @Column(name = "last_generated_date")
    private LocalDate lastGeneratedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle callbacks
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
    public RecurringExpense(TransactionFrequency frequency,
                            LocalDate nextScheduledDate,
                            BigDecimal amount,
                            BudgetCategory category,
                            String description,
                            RecurringStatus status,
                            Account sourceAccount,
                            LocalDate lastGeneratedDate) {
        this.frequency = frequency;
        this.nextScheduledDate = nextScheduledDate;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.status = status;
        this.sourceAccount = sourceAccount;
        this.lastGeneratedDate = lastGeneratedDate; // can be null until first generated
    }

    public RecurringExpense(TransactionFrequency frequency,
                            LocalDate nextScheduledDate,
                            BigDecimal amount,
                            BudgetCategory category,
                            String description,
                            RecurringStatus status,
                            Account sourceAccount) {
        this(frequency, nextScheduledDate, amount, category, description, status, sourceAccount, null);
    }

    // No-args constructor for JPA
    public RecurringExpense() {}

    // ===== Getters & Setters =====
    public Long getId() { return id; }

    public TransactionFrequency getFrequency() { return frequency; }
    public void setFrequency(TransactionFrequency frequency) { this.frequency = frequency; }

    public LocalDate getNextScheduledDate() { return nextScheduledDate; }
    public void setNextScheduledDate(LocalDate nextScheduledDate) { this.nextScheduledDate = nextScheduledDate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BudgetCategory getCategory() { return category; }
    public void setCategory(BudgetCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RecurringStatus getStatus() { return status; }
    public void setStatus(RecurringStatus status) { this.status = status; }

    public Account getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(Account sourceAccount) { this.sourceAccount = sourceAccount; }

    public LocalDate getLastGeneratedDate() { return lastGeneratedDate; }
    public void setLastGeneratedDate(LocalDate lastGeneratedDate) { this.lastGeneratedDate = lastGeneratedDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ===== equals, hashCode, toString =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurringExpense that = (RecurringExpense) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RecurringExpense{" +
            "id=" + id +
            ", frequency=" + frequency +
            ", nextScheduledDate=" + nextScheduledDate +
            ", amount=" + amount +
            ", category=" + category +
            ", description='" + description + '\'' +
            ", status=" + status +
            ", sourceAccountId=" + (sourceAccount != null ? sourceAccount.getId() : null) +
            ", lastGeneratedDate=" + lastGeneratedDate +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
