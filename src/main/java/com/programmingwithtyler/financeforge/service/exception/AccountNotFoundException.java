package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when an account with the specified ID cannot be found.
 */
public class AccountNotFoundException extends ServiceException {
    private final Long accountId;

    public AccountNotFoundException(Long accountId) {
        super("Account not found with ID: " + accountId);
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}