package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when attempting to create an account with a name that already exists.
 */
public class DuplicateAccountNameException extends ServiceException {
    private final String accountName;

    public DuplicateAccountNameException(String accountName) {
        super("An account with the name '" + accountName + "' already exists");
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }
}