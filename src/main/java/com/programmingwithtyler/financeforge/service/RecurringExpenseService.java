package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.RecurringExpenseNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing recurring expense templates and automated transaction generation.
 *
 * <p>This service manages the lifecycle of recurring expense templates and orchestrates the automatic
 * generation of scheduled expense transactions. It ensures idempotency (preventing duplicate transactions
 * for the same scheduled date) and properly advances schedules after generation.</p>
 *
 * <h2>Business Context</h2>
 * <p>Recurring expenses represent predictable monthly bills such as rent, subscriptions, insurance,
 * and utilities. Users create templates once, and the system automatically generates expense transactions
 * on schedule. This approach:</p>
 * <ul>
 *   <li>Reduces manual data entry burden</li>
 *   <li>Ensures bills aren't forgotten</li>
 *   <li>Provides predictable expense tracking</li>
 *   <li>Maintains financial integrity through idempotency guarantees</li>
 * </ul>
 *
 * <h2>Technical Context</h2>
 * <p>RecurringExpenseService coordinates between RecurringExpense templates and Transaction creation
 * via TransactionService. Per domain model rules: <strong>A recurring expense template can generate
 * at most one transaction per scheduled date.</strong></p>
 *
 * <p>The service enforces this rule through idempotency checks before generation and updates the
 * nextScheduledDate after successful generation using frequency-specific date arithmetic.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Template CRUD:</strong> Create, read, update, delete recurring expense templates</li>
 *   <li><strong>Activation Control:</strong> Enable/disable templates without deletion</li>
 *   <li><strong>Schedule Calculation:</strong> Automatic date advancement based on frequency</li>
 *   <li><strong>Transaction Generation:</strong> Idempotent creation of expense transactions</li>
 *   <li><strong>Batch Processing:</strong> Process all due expenses in a single operation</li>
 * </ul>
 *
 * @see RecurringExpense
 * @see TransactionService
 * @see TransactionFrequency
 * @since 1.0
 */
public interface RecurringExpenseService {

    /**
     * Creates a new recurring expense template.
     *
     * <p>This method creates a template that will automatically generate expense transactions
     * on the specified schedule. The template is created in an active state and can immediately
     * begin generating transactions when processScheduledExpenses is called.</p>
     *
     * <h3>Validation Rules</h3>
     * <ul>
     *   <li>All parameters must be non-null</li>
     *   <li>Amount must be positive (greater than zero)</li>
     *   <li>Source account must exist and be active</li>
     *   <li>Category must be a valid BudgetCategory enum value</li>
     *   <li>Description must not be blank</li>
     * </ul>
     *
     * @param frequency the recurrence frequency (DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY)
     * @param nextScheduledDate the date when the first transaction should be generated; must not be null
     * @param amount the fixed amount for each generated transaction; must be positive
     * @param category the budget category to assign to generated transactions
     * @param description a descriptive label for the recurring expense (e.g., "Netflix Subscription")
     * @param sourceAccount the account from which expenses will be debited
     * @return the newly created RecurringExpense template with active status set to true
     * @throws IllegalArgumentException if any validation rule is violated
     * @throws AccountNotFoundException if the source account does not exist
     * @throws IllegalStateException if the source account is not active
     */
    RecurringExpense createRecurringExpense(TransactionFrequency frequency,
                                            LocalDate nextScheduledDate,
                                            BigDecimal amount,
                                            BudgetCategory category,
                                            String description,
                                            Account sourceAccount);

    /**
     * Updates an existing recurring expense template.
     *
     * <p>This method supports partial updates—only non-null parameters will be updated.
     * Updates to frequency or nextScheduledDate will be validated to ensure they are consistent.
     * Updating a template does not affect transactions that have already been generated from it.</p>
     *
     * <h3>Update Behavior</h3>
     * <ul>
     *   <li>Null parameters are ignored (current value retained)</li>
     *   <li>Non-null parameters replace current values</li>
     *   <li>Changing frequency does not automatically recalculate nextScheduledDate</li>
     *   <li>Can activate/deactivate template via the active parameter</li>
     * </ul>
     *
     * <h3>Validation Rules</h3>
     * <ul>
     *   <li>Template with given ID must exist</li>
     *   <li>If amount is provided, it must be positive</li>
     *   <li>If frequency is changed, it must be a valid enum value</li>
     * </ul>
     *
     * @param id the ID of the recurring expense to update
     * @param frequency the new recurrence frequency, or null to keep current value
     * @param nextScheduledDate the new next scheduled date, or null to keep current value
     * @param amount the new fixed amount, or null to keep current value
     * @param category the new budget category, or null to keep current value
     * @param description the new description, or null to keep current value
     * @param active the new active status (true/false), or null to keep current value
     * @return the updated RecurringExpense template
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     * @throws IllegalArgumentException if provided amount is not positive
     */
    RecurringExpense updateRecurringExpense(Long id,
                                            TransactionFrequency frequency,
                                            LocalDate nextScheduledDate,
                                            BigDecimal amount,
                                            BudgetCategory category,
                                            String description,
                                            Boolean active);

    /**
     * Permanently deletes a recurring expense template.
     *
     * <p><strong>Important:</strong> This is a hard delete operation that removes the template
     * from the database. However, this operation does NOT cascade delete transactions that were
     * previously generated from this template. Generated transactions are preserved to maintain
     * historical financial records.</p>
     *
     * <h3>Deletion Behavior</h3>
     * <ul>
     *   <li>Template is permanently removed from the database</li>
     *   <li>All generated transactions remain in the database</li>
     *   <li>Generated transactions retain their recurringExpenseId reference (becomes orphaned)</li>
     *   <li>No further transactions will be generated from this template</li>
     * </ul>
     *
     * @param recurringExpenseId the ID of the recurring expense template to delete
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     */
    void deleteRecurringExpense(Long recurringExpenseId);

    /**
     * Activates a recurring expense template.
     *
     * <p>Once activated, the template will begin generating transactions when
     * processScheduledExpenses is called and the nextScheduledDate is reached.</p>
     *
     * <h3>Activation Requirements</h3>
     * <ul>
     *   <li>Template must exist</li>
     *   <li>nextScheduledDate must not be null</li>
     *   <li>Template must not already be active</li>
     * </ul>
     *
     * @param recurringExpenseId the ID of the recurring expense to activate
     * @return true if the template was activated, false if already active
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     * @throws IllegalStateException if nextScheduledDate is null
     */
    boolean activateRecurringExpense(Long recurringExpenseId);

    /**
     * Deactivates a recurring expense template.
     *
     * <p>Deactivating a template stops it from generating new transactions but preserves
     * all configuration including the nextScheduledDate. The template can be reactivated
     * later without losing scheduling information.</p>
     *
     * <h3>Deactivation Behavior</h3>
     * <ul>
     *   <li>Template stops generating transactions</li>
     *   <li>nextScheduledDate is preserved</li>
     *   <li>All template data remains in the database</li>
     *   <li>Previously generated transactions are unaffected</li>
     * </ul>
     *
     * @param recurringExpenseId the ID of the recurring expense to deactivate
     * @return true if the template was deactivated, false if already inactive
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     */
    boolean deactivateRecurringExpense(Long recurringExpenseId);

    /**
     * Retrieves a recurring expense template by its unique identifier.
     *
     * @param id the ID of the recurring expense template to retrieve
     * @return the RecurringExpense template with the specified ID
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     */
    RecurringExpense getRecurringExpense(Long id);

    /**
     * Lists recurring expense templates with optional filtering.
     *
     * <p>All filter parameters are optional (can be null). When null, that filter is ignored.
     * Results are ordered by nextScheduledDate in ascending order, allowing clients to easily
     * identify which templates are due soonest.</p>
     *
     * <h3>Filter Behavior</h3>
     * <ul>
     *   <li><strong>active = null:</strong> Returns both active and inactive templates</li>
     *   <li><strong>active = true:</strong> Returns only active templates</li>
     *   <li><strong>active = false:</strong> Returns only inactive templates</li>
     *   <li><strong>sourceAccountId = null:</strong> Returns templates for all accounts</li>
     *   <li><strong>sourceAccountId = {id}:</strong> Returns templates for specified account only</li>
     *   <li><strong>frequency = null:</strong> Returns templates of all frequencies</li>
     *   <li><strong>frequency = {freq}:</strong> Returns templates with specified frequency only</li>
     * </ul>
     *
     * @param active filter by active status (null = no filter, true = active only, false = inactive only)
     * @param sourceAccountId filter by source account ID (null = no filter)
     * @param frequency filter by recurrence frequency (null = no filter)
     * @return list of recurring expense templates matching the filters, ordered by nextScheduledDate ASC
     */
    List<RecurringExpense> listRecurringExpenses(Boolean active,
                                                 Long sourceAccountId,
                                                 TransactionFrequency frequency);

    /**
     * Generates a transaction from a recurring expense template with idempotency guarantee.
     *
     * <p>This method creates a single expense transaction from the template for its current
     * nextScheduledDate. <strong>Idempotency is guaranteed:</strong> if a transaction already
     * exists for this template's nextScheduledDate, no new transaction is created.</p>
     *
     * <h3>Generation Workflow</h3>
     * <ol>
     *   <li>Retrieve the recurring expense template</li>
     *   <li>Check if transaction already exists for (recurringExpenseId, nextScheduledDate)</li>
     *   <li>If exists: return without creating duplicate (idempotent)</li>
     *   <li>If not exists:
     *     <ol>
     *       <li>Create RecordExpenseCommand from template data</li>
     *       <li>Call TransactionService.recordExpense to create transaction</li>
     *       <li>Set transaction.isRecurring = true</li>
     *       <li>Set transaction.recurringExpenseId = template.id</li>
     *       <li>Calculate next scheduled date using calculateNextScheduledDate</li>
     *       <li>Update template.nextScheduledDate and template.lastGeneratedDate</li>
     *       <li>Persist updated template</li>
     *     </ol>
     *   </li>
     * </ol>
     *
     * <h3>Idempotency Guarantee</h3>
     * <p>This method can be safely called multiple times for the same template on the same day
     * without creating duplicate transactions. This ensures financial data integrity even if
     * batch jobs are retried or run multiple times.</p>
     *
     * @param recurringExpenseId the ID of the recurring expense template to generate from
     * @throws RecurringExpenseNotFoundException if no template exists with the given ID
     * @throws IllegalStateException if template is inactive or source account is inactive
     */
    void generateTransaction(Long recurringExpenseId);

    /**
     * Processes all active recurring expenses that are due as of the specified date.
     *
     * <p>This method is designed for batch job invocation (e.g., nightly cron job) to automatically
     * generate all scheduled expense transactions. It queries for all active templates where
     * nextScheduledDate ≤ currentDate and generates transactions for each.</p>
     *
     * <h3>Batch Processing Behavior</h3>
     * <ul>
     *   <li>Queries active recurring expenses with nextScheduledDate ≤ currentDate</li>
     *   <li>For each template, calls generateTransaction with idempotency</li>
     *   <li>Individual failures do not abort the batch (error handling per-template)</li>
     *   <li>Continues processing remaining templates even if some fail</li>
     *   <li>Logs errors for failed generations</li>
     *   <li>Returns summary of successes and failures</li>
     * </ul>
     *
     * <h3>Idempotency for Batch Operations</h3>
     * <p>This method is safe to run multiple times on the same day. Due to idempotency checks
     * in generateTransaction, re-running the batch will not create duplicate transactions.</p>
     *
     * <h3>Typical Usage</h3>
     * <pre>
     * // In a scheduled component:
     * {@literal @}Scheduled(cron = "0 0 2 * * *") // 2 AM daily
     * public void dailyRecurringExpenseProcessing() {
     *     LocalDate today = LocalDate.now();
     *     recurringExpenseService.processScheduledExpenses(today);
     * }
     * </pre>
     *
     * @param currentDate the date to use for determining which expenses are due (typically LocalDate.now())
     */
    void processScheduledExpenses(LocalDate currentDate);

    /**
     * Calculates the next scheduled date based on frequency and current date.
     *
     * <p>This method advances the schedule by adding the appropriate time period to the current
     * date. It uses LocalDate's built-in date arithmetic which correctly handles edge cases like
     * varying month lengths, leap years, and daylight saving time (not applicable to LocalDate).</p>
     *
     * <h3>Frequency-Specific Logic</h3>
     * <ul>
     *   <li><strong>DAILY:</strong> currentDate + 1 day</li>
     *   <li><strong>WEEKLY:</strong> currentDate + 7 days</li>
     *   <li><strong>BIWEEKLY:</strong> currentDate + 14 days</li>
     *   <li><strong>MONTHLY:</strong> currentDate + 1 month (handles variable month lengths)</li>
     *   <li><strong>QUARTERLY:</strong> currentDate + 3 months</li>
     *   <li><strong>YEARLY:</strong> currentDate + 1 year (handles leap years)</li>
     * </ul>
     *
     * <h3>Edge Case Handling</h3>
     * <p><strong>Month-End Dates:</strong> LocalDate.plusMonths() automatically handles month-end
     * edge cases. For example:</p>
     * <ul>
     *   <li>January 31 + 1 month = February 28 (or 29 in leap years)</li>
     *   <li>August 31 + 1 month = September 30</li>
     * </ul>
     *
     * <p><strong>Leap Years:</strong> LocalDate.plusYears() correctly handles leap year transitions:</p>
     * <ul>
     *   <li>February 29, 2024 + 1 year = February 28, 2025 (2025 is not a leap year)</li>
     * </ul>
     *
     * <h3>Performance</h3>
     * <p>This method uses date arithmetic (O(1) complexity) rather than iteration, making it
     * efficient even for long-running schedules.</p>
     *
     * @param frequency the recurrence frequency to use for calculation
     * @param currentDate the current scheduled date from which to calculate the next date
     * @return the next scheduled date based on the frequency
     * @throws IllegalArgumentException if frequency is null
     */
    LocalDate calculateNextScheduledDate(TransactionFrequency frequency, LocalDate currentDate);
}