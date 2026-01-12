package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.AccountType;
import java.math.BigDecimal;

public class AccountFilter {
    private Boolean active;              // null = ignore, true = only active, false = only inactive
    private AccountType type;            // null = ignore
    private BigDecimal minBalance;       // null = ignore
    private BigDecimal maxBalance;       // null = ignore
    private String nameContains;         // null = ignore

    // ===== Constructors =====
    public AccountFilter() {}

    // Optionally add a full constructor with all fields

    // ===== Getters and Setters =====
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public BigDecimal getMinBalance() { return minBalance; }
    public void setMinBalance(BigDecimal minBalance) { this.minBalance = minBalance; }

    public BigDecimal getMaxBalance() { return maxBalance; }
    public void setMaxBalance(BigDecimal maxBalance) { this.maxBalance = maxBalance; }

    public String getNameContains() { return nameContains; }
    public void setNameContains(String nameContains) { this.nameContains = nameContains; }
}
