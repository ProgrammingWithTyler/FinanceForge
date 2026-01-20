package com.programmingwithtyler.financeforge.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command object for recording transfer transactions.
 *
 * <p>Transfer transactions move funds from source to destination account atomically.
 * They do not affect budgets.</p>
 */
public record RecordTransferCommand(
    Long sourceAccountId,
    Long destinationAccountId,
    BigDecimal amount,
    LocalDate transactionDate,
    String description
) {
    public RecordTransferCommand {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source account ID is required");
        }
        if (destinationAccountId == null) {
            throw new IllegalArgumentException("Destination account ID is required");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
    }
}