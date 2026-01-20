package com.programmingwithtyler.financeforge.service.command;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command object for recording refund transactions.
 *
 * <p>Refund transactions credit the source account (the account that originally paid).
 * If a budget category is provided, the refund amount is deducted from that category's
 * spending for the period.</p>
 */
public record RecordRefundCommand(
    Long sourceAccountId,
    BigDecimal amount,
    BudgetCategory category,  // Optional: null if refund should not affect budget
    LocalDate transactionDate,
    String description
) {
    public RecordRefundCommand {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source account ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }
    }
}