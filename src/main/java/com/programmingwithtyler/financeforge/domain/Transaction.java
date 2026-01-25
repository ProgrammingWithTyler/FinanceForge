package com.programmingwithtyler.financeforge.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType type;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(name = "recurring_expense_id")
    private Long recurringExpenseId;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_category", length = 50)
    private BudgetCategory budgetCategory;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateInvariants();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateInvariants();
    }

    protected Transaction() {
    }

    private Transaction(
        TransactionType type,
        Account sourceAccount,
        Account destinationAccount,
        BudgetCategory budgetCategory,
        BigDecimal amount,
        String currency,
        LocalDate transactionDate
    ) {
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.budgetCategory = budgetCategory;
        this.amount = amount;
        this.currency = currency;
        this.transactionDate = transactionDate;
        this.isDeleted = false;
    }

    /**
     * Create an income transaction (credits destination account).
     *
     * @param destination the account receiving the income
     * @param amount the income amount (must be positive)
     * @param description transaction description
     * @return a new income Transaction
     */
    public static Transaction income(Account destination, BigDecimal amount, String description) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination account cannot be null for income");
        }
        validatePositiveAmount(amount);

        Transaction tx = new Transaction(
            TransactionType.INCOME,
            null,
            destination,
            null,
            amount,
            "USD",
            LocalDate.now()
        );
        tx.description = description;
        return tx;
    }

    /**
     * Create an expense transaction (debits source account, tracks against budget).
     *
     * @param source the account being debited
     * @param amount the expense amount (must be positive)
     * @param category the budget category (required)
     * @param description transaction description
     * @return a new expense Transaction
     */
    public static Transaction expense(Account source, BigDecimal amount,
                                      BudgetCategory category, String description) {
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null for expense");
        }
        if (category == null) {
            throw new IllegalArgumentException("Budget category is required for expenses");
        }
        validatePositiveAmount(amount);

        Transaction tx = new Transaction(
            TransactionType.EXPENSE,
            source,
            null,
            category,
            amount,
            "USD",
            LocalDate.now()
        );
        tx.description = description;
        return tx;
    }

    /**
     * Create a transfer transaction (debits source, credits destination).
     *
     * @param source the account being debited
     * @param destination the account being credited
     * @param amount the transfer amount (must be positive)
     * @param description transaction description
     * @return a new transfer Transaction
     */
    public static Transaction transfer(Account source, Account destination,
                                       BigDecimal amount, String description) {
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null for transfer");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination account cannot be null for transfer");
        }
        if (source.getId() != null && source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        validatePositiveAmount(amount);

        Transaction tx = new Transaction(
            TransactionType.TRANSFER,
            source,
            destination,
            null,
            amount,
            "USD",
            LocalDate.now()
        );
        tx.description = description;
        return tx;
    }

    /**
     * Create a refund transaction (credits source account, may reverse budget).
     *
     * @param source the account being credited (account that originally paid)
     * @param amount the refund amount (must be positive)
     * @param category the budget category (optional)
     * @param description transaction description
     * @return a new refund Transaction
     */
    public static Transaction refund(Account source, BigDecimal amount,
                                     BudgetCategory category, String description) {
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null for refund");
        }
        validatePositiveAmount(amount);

        Transaction tx = new Transaction(
            TransactionType.REFUND,
            source,
            null,
            category,
            amount,
            "USD",
            LocalDate.now()
        );
        tx.description = description;
        return tx;
    }

    /**
     * Mark transaction as linked to a recurring expense.
     */
    public void markAsRecurring(Long recurringExpenseId) {
        if (recurringExpenseId == null) {
            throw new IllegalArgumentException("Recurring expense ID cannot be null");
        }
        this.recurringExpenseId = recurringExpenseId;
        this.isRecurring = true;
    }

    /**
     * Soft delete this transaction.
     */
    public void delete() {
        this.isDeleted = true;
    }

    /**
     * Restore a soft-deleted transaction.
     */
    public void restore() {
        this.isDeleted = false;
    }

    /**
     * Update transaction details (amount, date, description).
     * Use carefully - changing amount may require recalculating account balances.
     */
    public void updateDetails(BigDecimal newAmount, LocalDate newDate, String newDescription) {
        validatePositiveAmount(newAmount);
        if (newDate == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        this.amount = newAmount;
        this.transactionDate = newDate;
        this.description = newDescription;
    }

    /**
     * Check if this is a transfer transaction.
     */
    public boolean isTransfer() {
        return type == TransactionType.TRANSFER;
    }

    /**
     * Check if this transaction affects a budget category.
     */
    public boolean affectsBudget() {
        return budgetCategory != null;
    }

    // ===== Validation =====

    private static void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void validateInvariants() {
        // Validate transaction type rules
        switch (type) {
            case INCOME:
                if (destinationAccount == null) {
                    throw new IllegalStateException("Income transaction must have a destination account");
                }
                if (sourceAccount != null) {
                    throw new IllegalStateException("Income transaction should not have a source account");
                }
                break;
            case EXPENSE:
                if (sourceAccount == null) {
                    throw new IllegalStateException("Expense transaction must have a source account");
                }
                if (budgetCategory == null) {
                    throw new IllegalStateException("Expense transaction must have a budget category");
                }
                break;
            case TRANSFER:
                if (sourceAccount == null || destinationAccount == null) {
                    throw new IllegalStateException("Transfer transaction must have both source and destination accounts");
                }
                break;
            case REFUND:
                if (sourceAccount == null) {
                    throw new IllegalStateException("Refund transaction must have a source account");
                }
                break;
        }
    }

    // ===== Getters (Limited Setters) =====

    public Long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public Long getRecurringExpenseId() {
        return recurringExpenseId;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public BudgetCategory getBudgetCategory() {
        return budgetCategory;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ===== Protected setters for JPA/framework use only =====


    public void setId(Long id) {
        this.id = id;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public void setDestinationAccount(Account destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public void setRecurringExpenseId(Long recurringExpenseId) {
        this.recurringExpenseId = recurringExpenseId;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public void setBudgetCategory(BudgetCategory budgetCategory) {
        this.budgetCategory = budgetCategory;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
            "id=" + id +
            ", type=" + type +
            ", sourceAccountId=" + (sourceAccount != null ? sourceAccount.getId() : null) +
            ", destinationAccountId=" + (destinationAccount != null ? destinationAccount.getId() : null) +
            ", budgetCategory=" + budgetCategory +
            ", transactionDate=" + transactionDate +
            ", amount=" + amount +
            ", currency='" + currency + '\'' +
            ", isDeleted=" + isDeleted +
            '}';
    }
}