package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.service.command.RecordExpenseCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording expense transactions.
 *
 * Validates:
 * - Source account exists and has sufficient funds
 * - Amount is positive
 * - Category is valid enum value
 * - Transaction date is not in the future
 *
 * @param sourceAccountId The account from which funds are withdrawn
 * @param amount The expense amount (must be positive)
 * @param category The budget category for expense tracking
 * @param transactionDate The date of the transaction (cannot be future)
 * @param description Optional description of the expense
 */
public record RecordExpenseRequest(
    @NotNull(message = "Source account ID is required")
    Long sourceAccountId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Category is required")
    BudgetCategory category,

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDate transactionDate,

    String description
) {
    /**
     * Converts this request DTO to a domain command object.
     *
     * @return RecordExpenseCommand ready for service layer processing
     */
    public RecordExpenseCommand toCommand() {
        return new RecordExpenseCommand(
            sourceAccountId,
            amount,
            category,
            transactionDate,
            description
        );
    }
}