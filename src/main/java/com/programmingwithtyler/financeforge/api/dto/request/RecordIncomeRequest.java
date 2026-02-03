package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.service.command.RecordIncomeCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording income transactions.
 *
 * Validates:
 * - Destination account exists
 * - Amount is positive
 * - Transaction date is not in the future
 *
 * @param destinationAccountId The account receiving the income
 * @param amount The income amount (must be positive)
 * @param transactionDate The date of the transaction (cannot be future)
 * @param description Optional description of the income source
 */
public record RecordIncomeRequest(
    @NotNull(message = "Destination account ID is required")
    Long destinationAccountId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    LocalDate transactionDate,

    String description
) {
    /**
     * Converts this request DTO to a domain command object.
     *
     * @return RecordIncomeCommand ready for service layer processing
     */
    public RecordIncomeCommand toCommand() {
        return new RecordIncomeCommand(
            destinationAccountId,
            amount,
            transactionDate,
            description
        );
    }
}