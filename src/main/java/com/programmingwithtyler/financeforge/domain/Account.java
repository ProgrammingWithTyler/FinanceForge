package com.programmingwithtyler.financeforge.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private AccountType type;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "start_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingBalance = BigDecimal.ZERO;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;

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

    public Account() {
    }

    /**
     * Primary constructor for creating new accounts.
     * Current balance is initialized to match starting balance.
     *
     * @param accountName the account name
     * @param type the account type
     * @param description optional description
     * @param startingBalance initial balance (defaults to zero if null)
     */
    public Account(String accountName, AccountType type, String description, BigDecimal startingBalance) {
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }

        this.accountName = accountName;
        this.type = type;
        this.description = description;
        this.startingBalance = startingBalance != null ? startingBalance : BigDecimal.ZERO;
        this.currentBalance = this.startingBalance;
        this.active = true;
    }

    /**
     * Convenience constructor for accounts starting at zero balance.
     */
    public Account(String accountName, AccountType type, String description) {
        this(accountName, type, description, BigDecimal.ZERO);
    }

    // ===== Domain Behavior Methods =====

    /**
     * Debit (subtract from) the account balance.
     *
     * @param amount the amount to debit (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     * @throws IllegalStateException if account is inactive
     */
    public void debit(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    /**
     * Credit (add to) the account balance.
     *
     * @param amount the amount to credit (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     * @throws IllegalStateException if account is inactive
     */
    public void credit(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        this.currentBalance = this.currentBalance.add(amount);
    }

    /**
     * Close this account (mark as inactive).
     * Cannot perform transactions on closed accounts.
     */
    public void close() {
        this.active = false;
    }

    /**
     * Reopen this account (mark as active).
     */
    public void reopen() {
        this.active = true;
    }

    /**
     * Update account name.
     */
    public void rename(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        this.accountName = newName;
    }

    /**
     * Update account description.
     */
    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    /**
     * Calculate the net change since account creation.
     *
     * @return current balance minus starting balance
     */
    public BigDecimal getNetChange() {
        return this.currentBalance.subtract(this.startingBalance);
    }

    /**
     * Check if account has sufficient balance for a debit.
     *
     * @param amount the amount to check
     * @return true if current balance >= amount
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return this.currentBalance.compareTo(amount) >= 0;
    }

    /**
     * Check if account is overdrawn (negative balance).
     *
     * @return true if current balance is negative
     */
    public boolean isOverdrawn() {
        return this.currentBalance.compareTo(BigDecimal.ZERO) < 0;
    }

    // ===== Validation =====

    private void validateActive() {
        if (!active) {
            throw new IllegalStateException("Cannot perform transactions on an inactive account");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    // ===== Getters (No Public Setters for Balances) =====

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public AccountType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ===== public setters for JPA/framework use only =====

    public void setId(Long id) {
        this.id = id;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
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
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Account{" +
            "id=" + id +
            ", accountName='" + accountName + '\'' +
            ", type=" + type +
            ", active=" + active +
            ", currentBalance=" + currentBalance +
            ", startingBalance=" + startingBalance +
            '}';
    }
}