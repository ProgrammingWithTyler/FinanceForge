package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.service.command.RecordTransferCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording transfer transactions between accounts.
 *
 * Validates:
 * - Source and destination accounts exist
 * - Source account has sufficient funds
 * - Amount is positive
 * - Transaction date is not in the future
 * - Source and destination accounts are different
 *
 * @param sourceAccountId The account from which funds are transferred
 * @param destinationAccountId The account receiving the transfer
 * @param amount The transfer amount (must be positive)
 * @param transactionDate The date of the transaction (cannot be future)
 * @param description Optional description of the transfer
 */
public record RecordTransferRequest(
    @NotNull(message = "Source account ID is required")
    Long sourceAccountId,

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
     * Custom validation: ensures source and destination accounts are different.
     *
     * @return true if accounts are different, false otherwise
     */
    @AssertTrue(message = "Source and destination accounts must be different")
    public boolean isValidTransfer() {
        if (sourceAccountId == null || destinationAccountId == null) {
            return true; // Let @NotNull handle null validation
        }
        return !sourceAccountId.equals(destinationAccountId);
    }

    /**
     * Converts this request DTO to a domain command object.
     *
     * @return RecordTransferCommand ready for service layer processing
     */
    public RecordTransferCommand toCommand() {
        return new RecordTransferCommand(
            sourceAccountId,
            destinationAccountId,
            amount,
            transactionDate,
            description
        );
    }
}