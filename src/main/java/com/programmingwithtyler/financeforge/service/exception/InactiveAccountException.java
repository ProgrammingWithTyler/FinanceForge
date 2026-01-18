package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when attempting to use an inactive (closed) account for a transaction.
 */
public class InactiveAccountException extends ServiceException {

    private final Long accountId;

    public InactiveAccountException(Long accountId) {
        super(String.format("Account %d is inactive and cannot be used for transactions", accountId));
        this.accountId = accountId;
    }

    public InactiveAccountException(String message) {
        super(message);
        this.accountId = null;
    }

    public Long getAccountId() {
        return accountId;
    }
}