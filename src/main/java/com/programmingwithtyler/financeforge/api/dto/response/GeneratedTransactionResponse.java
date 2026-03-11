package com.programmingwithtyler.financeforge.api.dto.response;

import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.Transaction;

/**
 * Response wrapper for manual recurring expense transaction generation.
 *
 * <p>This DTO is returned by the POST /api/recurring-expenses/{id}/generate endpoint
 * when a transaction is successfully generated (or already exists due to idempotency).
 * It provides complete transparency about:</p>
 * <ul>
 *   <li>The generated transaction details</li>
 *   <li>The updated template state (with advanced nextScheduledDate)</li>
 *   <li>A human-readable message explaining the outcome</li>
 * </ul>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *   <li><strong>Testing Templates:</strong> Users can manually trigger generation to verify template configuration</li>
 *   <li><strong>Schedule Adjustments:</strong> Users can generate a transaction early if needed</li>
 *   <li><strong>Idempotency Feedback:</strong> Message indicates whether transaction was created or already existed</li>
 * </ul>
 *
 * <h3>Sample Response (Transaction Created - 201 CREATED)</h3>
 * <pre>
 * {
 *   "transaction": {
 *     "id": 42,
 *     "type": "EXPENSE",
 *     "sourceAccount": { "id": 1, "accountName": "Chase Checking", "type": "CHECKING" },
 *     "category": "HOUSING",
 *     "amount": 1500.00,
 *     "transactionDate": "2026-02-01",
 *     "description": "Monthly rent",
 *     "isRecurring": true
 *   },
 *   "template": {
 *     "id": 1,
 *     "frequency": "MONTHLY",
 *     "nextScheduledDate": "2026-03-01",
 *     "lastGeneratedDate": "2026-02-01",
 *     ...
 *   },
 *   "message": "Transaction generated successfully. Next scheduled date: 2026-03-01"
 * }
 * </pre>
 *
 * <h3>Sample Response (Already Generated - 200 OK)</h3>
 * <pre>
 * {
 *   "transaction": { ... existing transaction ... },
 *   "template": { ... current template state ... },
 *   "message": "Transaction already exists for this date: 2026-02-01"
 * }
 * </pre>
 *
 * @param transaction The generated (or existing) transaction details
 * @param template The recurring expense template with updated nextScheduledDate and lastGeneratedDate
 * @param message Human-readable message describing the outcome
 */
public record GeneratedTransactionResponse(
    TransactionResponse transaction,
    RecurringExpenseResponse template,
    String message
) {
    /**
     * Factory method for successful transaction generation (201 CREATED).
     *
     * <p>Creates a response indicating a new transaction was generated and the
     * template schedule was advanced.</p>
     *
     * @param transaction The newly generated transaction entity
     * @param template The recurring expense template (with updated nextScheduledDate)
     * @return GeneratedTransactionResponse with success message
     */
    public static GeneratedTransactionResponse created(Transaction transaction, RecurringExpense template) {
        if (transaction == null || template == null) {
            throw new IllegalArgumentException("Transaction and template cannot be null");
        }

        String message = String.format(
            "Transaction generated successfully. Next scheduled date: %s",
            template.getNextScheduledDate()
        );

        return new GeneratedTransactionResponse(
            TransactionResponse.from(transaction),
            RecurringExpenseResponse.from(template),
            message
        );
    }

    /**
     * Factory method for idempotent generation (200 OK - already exists).
     *
     * <p>Creates a response indicating the transaction already existed for this
     * scheduled date (idempotency guarantee). No new transaction was created.</p>
     *
     * @param existingTransaction The existing transaction that was found
     * @param template The recurring expense template (state unchanged)
     * @return GeneratedTransactionResponse with idempotency message
     */
    public static GeneratedTransactionResponse alreadyExists(Transaction existingTransaction, RecurringExpense template) {
        if (existingTransaction == null || template == null) {
            throw new IllegalArgumentException("Transaction and template cannot be null");
        }

        String message = String.format(
            "Transaction already exists for this date: %s",
            existingTransaction.getTransactionDate()
        );

        return new GeneratedTransactionResponse(
            TransactionResponse.from(existingTransaction),
            RecurringExpenseResponse.from(template),
            message
        );
    }
}