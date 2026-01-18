package com.programmingwithtyler.financeforge.service.exception;

import java.math.BigDecimal;

/**
 * Thrown when an operation requires more funds than are available in an account.
 */
public class InsufficientFundsException extends ServiceException {

    private final Long accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(Long accountId, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format(
            "Insufficient funds in account %d. Current balance: %s, Requested: %s",
            accountId,
            currentBalance,
            requestedAmount
        ));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}