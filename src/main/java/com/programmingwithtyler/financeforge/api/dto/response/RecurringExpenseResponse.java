package com.programmingwithtyler.financeforge.api.dto.response;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for recurring expense templates.
 *
 * <p>Contains complete template information including source account summary
 * and last generation metadata for transparency. This DTO is returned by all
 * recurring expense API endpoints.</p>
 *
 * <p>Per FF-020 specification, this includes:</p>
 * <ul>
 *   <li>Template identification and configuration (id, frequency, nextScheduledDate, amount, category, description)</li>
 *   <li>Activation status (active)</li>
 *   <li>Source account summary (lightweight reference to prevent over-fetching)</li>
 *   <li>Generation tracking (lastGeneratedDate for transparency)</li>
 *   <li>Audit metadata (createdAt timestamp)</li>
 * </ul>
 *
 * @param id Unique identifier of the recurring expense template
 * @param frequency Recurrence frequency (DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY)
 * @param nextScheduledDate Next date when a transaction should be generated
 * @param amount Fixed amount for each generated transaction
 * @param category Budget category assigned to generated transactions
 * @param description Descriptive label for the expense (e.g., "Netflix Subscription")
 * @param active Whether this template is currently active for automatic processing
 * @param sourceAccount Lightweight summary of the source account (id, name, type only)
 * @param lastGeneratedDate Most recent date a transaction was generated (null if never generated)
 * @param createdAt Timestamp when this template was created
 */
public record RecurringExpenseResponse(
    Long id,
    TransactionFrequency frequency,
    LocalDate nextScheduledDate,
    BigDecimal amount,
    BudgetCategory category,
    String description,
    Boolean active,
    AccountSummary sourceAccount,
    LocalDate lastGeneratedDate,
    LocalDateTime createdAt
) {
    /**
     * Factory method to convert RecurringExpense entity to response DTO.
     *
     * <p>Extracts all fields from the entity and creates a lightweight
     * AccountSummary to prevent over-fetching account details in responses.</p>
     *
     * @param expense The recurring expense entity to convert
     * @return RecurringExpenseResponse DTO with all fields populated
     * @throws NullPointerException if expense is null
     */
    public static RecurringExpenseResponse from(RecurringExpense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("RecurringExpense cannot be null");
        }

        return new RecurringExpenseResponse(
            expense.getId(),
            expense.getFrequency(),
            expense.getNextScheduledDate(),
            expense.getAmount(),
            expense.getCategory(),
            expense.getDescription(),
            expense.isActive(),
            AccountSummary.from(expense.getSourceAccount()),
            expense.getLastGeneratedDate(),
            expense.getCreatedAt()
        );
    }
}