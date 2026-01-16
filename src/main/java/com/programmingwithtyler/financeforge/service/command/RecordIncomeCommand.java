package com.programmingwithtyler.financeforge.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command object for recording income transactions.
 *
 * <p>Income transactions credit the destination account and do not affect budgets.</p>
 */
public record RecordIncomeCommand(
    Long destinationAccountId,
    BigDecimal amount,
    LocalDate transactionDate,
    String description
) {
    public RecordIncomeCommand {
        if (destinationAccountId == null) {
            throw new IllegalArgumentException("Destination account ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
    }
}