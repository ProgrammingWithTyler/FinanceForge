package com.programmingwithtyler.financeforge.api.dto.response;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for transaction details in API responses.
 *
 * <p>Provides complete transaction information including account summaries
 * (lightweight references) instead of full account entities to prevent over-fetching.</p>
 *
 * <p>This DTO is used in:</p>
 * <ul>
 *   <li>Transaction CRUD endpoints</li>
 *   <li>GeneratedTransactionResponse (for manual recurring expense generation)</li>
 *   <li>Transaction list/search results</li>
 * </ul>
 *
 * @param id Unique identifier of the transaction
 * @param type Transaction type (EXPENSE, INCOME, TRANSFER)
 * @param sourceAccount Source account summary (debit side)
 * @param destinationAccount Destination account summary (credit side, null for EXPENSE/INCOME)
 * @param category Budget category (null for TRANSFER)
 * @param amount Transaction amount (always positive)
 * @param transactionDate Date when transaction occurred
 * @param description User-provided description
 * @param isRecurring Whether this transaction was auto-generated from a recurring expense template
 * @param recurringExpenseId ID of the template that generated this (null if not recurring)
 * @param createdAt Timestamp when transaction was recorded
 */
public record TransactionResponse(
    Long id,
    TransactionType type,
    AccountSummary sourceAccount,
    AccountSummary destinationAccount,
    BudgetCategory category,
    BigDecimal amount,
    LocalDate transactionDate,
    String description,
    Boolean isRecurring,
    Long recurringExpenseId,
    LocalDateTime createdAt
) {
    /**
     * Factory method to convert Transaction entity to response DTO.
     *
     * <p>Extracts all fields from the transaction and creates lightweight
     * AccountSummary objects for source and destination accounts.</p>
     *
     * @param transaction The transaction entity to convert
     * @return TransactionResponse DTO with all fields populated
     * @throws IllegalArgumentException if transaction is null
     */
    public static TransactionResponse from(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        return new TransactionResponse(
            transaction.getId(),
            transaction.getType(),
            AccountSummary.from(transaction.getSourceAccount()),
            AccountSummary.from(transaction.getDestinationAccount()), // null for EXPENSE/INCOME
            transaction.getBudgetCategory(), // null for TRANSFER
            transaction.getAmount(),
            transaction.getTransactionDate(),
            transaction.getDescription(),
            transaction.isRecurring(),
            transaction.getRecurringExpenseId(), // null if not recurring
            transaction.getCreatedAt()
        );
    }
}