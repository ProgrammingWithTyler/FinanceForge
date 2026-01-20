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
    private boolean active = true; // defaulting new accounts to active

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "start_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingBalance;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

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

    public Account(String accountName, AccountType type,
                   boolean active,
                   String description, BigDecimal startingBalance,
                   BigDecimal currentBalance) {
        this.accountName = accountName;
        this.type = type;
        this.active = active;
        this.description = description;
        this.startingBalance = startingBalance;
        this.currentBalance = currentBalance;
    }

    // ===== Domain Behavior Methods =====
    /**
     * Debit (subtract from) the account balance.
     *
     * @param amount the amount to debit (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     */
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    /**
     * Credit (add to) the account balance.
     *
     * @param amount the amount to credit (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.currentBalance = this.currentBalance.add(amount);
    }

    /**
     * Close this account (mark as inactive).
     */
    public void close() {
        this.active = false;
    }

    /**
     * Calculate the net change since account creation.
     *
     * @return current balance minus starting balance
     */
    public BigDecimal getNetChange() {
        return this.currentBalance.subtract(this.startingBalance);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
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
            ", description='" + description + '\'' +
            ", startingBalance=" + startingBalance +
            ", currentBalance=" + currentBalance +
            '}';
    }
}
