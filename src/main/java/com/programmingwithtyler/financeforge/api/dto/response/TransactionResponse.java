package com.programmingwithtyler.financeforge.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for transaction data.
 *
 * Includes account summaries (not full accounts) to minimize payload size.
 * Distinguishes between manual and auto-generated recurring transactions.
 *
 * Design decisions:
 * - sourceAccount is null for INCOME transactions
 * - destinationAccount is null for EXPENSE transactions
 * - category is null for INCOME and TRANSFER transactions
 * - All monetary amounts formatted to 2 decimal places
 * - Dates serialized as yyyy-MM-dd (ISO-8601)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
    Long id,
    TransactionType type,
    AccountSummary sourceAccount,
    AccountSummary destinationAccount,
    BudgetCategory budgetCategory,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate transactionDate,

    BigDecimal amount,
    String description,
    boolean isRecurring
) {
    /**
     * Factory method to create TransactionResponse from Transaction entity.
     *
     * Populates account summaries based on transaction type:
     * - INCOME: only destinationAccount populated
     * - EXPENSE: only sourceAccount populated
     * - TRANSFER: both accounts populated
     * - REFUND: only sourceAccount populated
     *
     * @param transaction The domain transaction entity
     * @return Complete transaction response with account summaries
     */
    public static TransactionResponse from(Transaction transaction) {
        AccountSummary sourceAccount = null;
        AccountSummary destinationAccount = null;

        switch (transaction.getType()) {
            case INCOME:
                destinationAccount = AccountSummary.from(transaction.getDestinationAccount());
                break;
            case EXPENSE:
                sourceAccount = AccountSummary.from(transaction.getSourceAccount());
                break;
            case TRANSFER:
                sourceAccount = AccountSummary.from(transaction.getSourceAccount());
                destinationAccount = AccountSummary.from(transaction.getDestinationAccount());
                break;
            case REFUND:
                sourceAccount = AccountSummary.from(transaction.getSourceAccount());
                break;
        }

        return new TransactionResponse(
            transaction.getId(),
            transaction.getType(),
            sourceAccount,
            destinationAccount,
            transaction.getBudgetCategory(),
            transaction.getTransactionDate(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.isRecurring()
        );
    }
}