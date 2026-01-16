package com.programmingwithtyler.financeforge.service.command;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command object for recording expense transactions.
 *
 * <p>Expense transactions debit the source account and are tracked against
 * the specified budget category.</p>
 */
public record RecordExpenseCommand(
    Long sourceAccountId,
    BigDecimal amount,
    BudgetCategory category,
    LocalDate transactionDate,
    String description
) {
    public RecordExpenseCommand {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source account ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (category == null) {
            throw new IllegalArgumentException("Budget category is required for expenses");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
    }
}