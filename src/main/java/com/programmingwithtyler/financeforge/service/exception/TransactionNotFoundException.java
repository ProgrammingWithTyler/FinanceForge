package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when a requested transaction does not exist.
 */
public class TransactionNotFoundException extends ServiceException {

    private final Long transactionId;

    public TransactionNotFoundException(Long transactionId) {
        super(String.format("Transaction with ID %d not found", transactionId));
        this.transactionId = transactionId;
    }

    public Long getTransactionId() {
        return transactionId;
    }
}