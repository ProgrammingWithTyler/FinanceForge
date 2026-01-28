package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import com.programmingwithtyler.financeforge.repository.RecurringExpenseRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.AccountService;
import com.programmingwithtyler.financeforge.service.RecurringExpenseService;
import com.programmingwithtyler.financeforge.service.TransactionService;
import com.programmingwithtyler.financeforge.service.command.RecordExpenseCommand;
import com.programmingwithtyler.financeforge.service.exception.InactiveAccountException;
import com.programmingwithtyler.financeforge.service.exception.RecurringExpenseNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of RecurringExpenseService interface.
 *
 * <p>Manages recurring expense templates and orchestrates automatic transaction generation
 * with idempotency guarantees, proper schedule advancement, and comprehensive error handling.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Idempotent transaction generation (safe to run multiple times)</li>
 *   <li>Automatic schedule advancement using date arithmetic</li>
 *   <li>Batch processing with per-template error isolation</li>
 *   <li>Comprehensive validation and business rule enforcement</li>
 * </ul>
 *
 * <p>All public methods are transactional with read-write by default.
 * Read-only methods are explicitly marked with @Transactional(readOnly = true)
 * for performance optimization.</p>
 */
@Service
@Transactional
public class RecurringExpenseServiceImpl implements RecurringExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringExpenseServiceImpl.class);

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AccountService accountService;

    public RecurringExpenseServiceImpl(
        RecurringExpenseRepository recurringExpenseRepository,
        TransactionRepository transactionRepository,
        TransactionService transactionService,
        AccountService accountService
    ) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // ========================================================================
    // Template CRUD Operations
    // ========================================================================

    @Override
    public RecurringExpense createRecurringExpense(
        TransactionFrequency frequency,
        LocalDate nextScheduledDate,
        BigDecimal amount,
        BudgetCategory category,
        String description,
        Account sourceAccount
    ) {
        // Validate all inputs
        validateFrequency(frequency);
        validateNextScheduledDate(nextScheduledDate);
        validateAmount(amount);
        validateCategory(category);
        validateDescription(description);
        validateSourceAccount(sourceAccount);

        // Verify source account exists and is active
        Account account = accountService.getAccount(sourceAccount.getId());
        if (!account.isActive()) {
            throw new InactiveAccountException(account.getId());
        }

        // Create recurring expense using domain constructor
        RecurringExpense recurringExpense = new RecurringExpense(
            frequency,
            nextScheduledDate,
            amount,
            category,
            description,
            true,  // active = true by default
            account
        );

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);

        logger.info("Created recurring expense: id={}, frequency={}, amount={}, category={}",
            saved.getId(), frequency, amount, category);

        return saved;
    }

    @Override
    public RecurringExpense updateRecurringExpense(
        Long id,
        TransactionFrequency frequency,
        LocalDate nextScheduledDate,
        BigDecimal amount,
        BudgetCategory category,
        String description,
        Boolean active
    ) {
        RecurringExpense recurringExpense = getRecurringExpense(id);

        // Update frequency if provided
        if (frequency != null) {
            validateFrequency(frequency);
            recurringExpense.setFrequency(frequency);
        }

        // Update nextScheduledDate if provided
        if (nextScheduledDate != null) {
            validateNextScheduledDate(nextScheduledDate);
            recurringExpense.setNextScheduledDate(nextScheduledDate);
        }

        // Update amount if provided
        if (amount != null) {
            validateAmount(amount);
            recurringExpense.setAmount(amount);
        }

        // Update category if provided
        if (category != null) {
            validateCategory(category);
            recurringExpense.setCategory(category);
        }

        // Update description if provided
        if (description != null) {
            validateDescription(description);
            recurringExpense.setDescription(description);
        }

        // Update active status if provided
        if (active != null) {
            recurringExpense.setActive(active);
        }

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);

        logger.info("Updated recurring expense: id={}", id);

        return saved;
    }

    @Override
    public void deleteRecurringExpense(Long recurringExpenseId) {
        RecurringExpense recurringExpense = getRecurringExpense(recurringExpenseId);

        // Hard delete - does NOT cascade delete generated transactions
        recurringExpenseRepository.delete(recurringExpense);

        logger.info("Deleted recurring expense template: id={} (generated transactions preserved)",
            recurringExpenseId);
    }

    // ========================================================================
    // Activation Control
    // ========================================================================

    @Override
    public boolean activateRecurringExpense(Long recurringExpenseId) {
        RecurringExpense recurringExpense = getRecurringExpense(recurringExpenseId);

        // Already active
        if (recurringExpense.isActive()) {
            return false;
        }

        // Validate nextScheduledDate is set
        if (recurringExpense.getNextScheduledDate() == null) {
            throw new IllegalStateException(
                "Cannot activate recurring expense with null nextScheduledDate. " +
                    "Please set a scheduled date first."
            );
        }

        recurringExpense.setActive(true);
        recurringExpenseRepository.save(recurringExpense);

        logger.info("Activated recurring expense: id={}, nextScheduledDate={}",
            recurringExpenseId, recurringExpense.getNextScheduledDate());

        return true;
    }

    @Override
    public boolean deactivateRecurringExpense(Long recurringExpenseId) {
        RecurringExpense recurringExpense = getRecurringExpense(recurringExpenseId);

        // Already inactive
        if (!recurringExpense.isActive()) {
            return false;
        }

        recurringExpense.setActive(false);
        recurringExpenseRepository.save(recurringExpense);

        logger.info("Deactivated recurring expense: id={} (nextScheduledDate preserved for reactivation)",
            recurringExpenseId);

        return true;
    }

    // ========================================================================
    // Queries
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public RecurringExpense getRecurringExpense(Long id) {
        return recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new RecurringExpenseNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringExpense> listRecurringExpenses(
        Boolean active,
        Long sourceAccountId,  // FIXED: Changed from Account to Long
        TransactionFrequency frequency
    ) {
        // Convert sourceAccountId to Account entity if provided
        Account sourceAccount = null;
        if (sourceAccountId != null) {
            sourceAccount = accountService.getAccount(sourceAccountId);
        }

        // Handle all 8 filter combinations
        // Pattern: active, account, frequency (each can be null or non-null)

        if (active == null && sourceAccount == null && frequency == null) {
            // No filters: return all
            return recurringExpenseRepository.findAllByOrderByNextScheduledDateAsc();
        }

        if (active != null && sourceAccount == null && frequency == null) {
            // Filter by active only
            return recurringExpenseRepository.findByActiveOrderByNextScheduledDateAsc(active);
        }

        if (active == null && sourceAccount != null && frequency == null) {
            // Filter by account only
            return recurringExpenseRepository.findBySourceAccountOrderByNextScheduledDateAsc(sourceAccount);
        }

        if (active == null && sourceAccount == null && frequency != null) {
            // Filter by frequency only
            return recurringExpenseRepository.findByFrequencyOrderByNextScheduledDateAsc(frequency);
        }

        if (active != null && sourceAccount != null && frequency == null) {
            // Filter by active and account
            return recurringExpenseRepository.findByActiveAndSourceAccountOrderByNextScheduledDateAsc(
                active, sourceAccount);
        }

        if (active != null && sourceAccount == null && frequency != null) {
            // Filter by active and frequency
            return recurringExpenseRepository.findByActiveAndFrequencyOrderByNextScheduledDateAsc(
                active, frequency);
        }

        if (active == null && sourceAccount != null && frequency != null) {
            // Filter by account and frequency
            return recurringExpenseRepository.findBySourceAccountAndFrequencyOrderByNextScheduledDateAsc(
                sourceAccount, frequency);
        }

        // All three filters provided
        return recurringExpenseRepository.findByActiveAndSourceAccountAndFrequencyOrderByNextScheduledDateAsc(
            active, sourceAccount, frequency);
    }

    // ========================================================================
    // Transaction Generation
    // ========================================================================

    @Override
    public void generateTransaction(Long recurringExpenseId) {
        RecurringExpense template = getRecurringExpense(recurringExpenseId);

        // Validate template is active
        if (!template.isActive()) {
            throw new IllegalStateException(
                "Cannot generate transaction from inactive recurring expense: " + recurringExpenseId
            );
        }

        // Validate source account is still active
        if (!template.getSourceAccount().isActive()) {
            throw new InactiveAccountException(template.getSourceAccount().getId());
        }

        // Idempotency check: has transaction already been generated for this scheduled date?
        LocalDate scheduledDate = template.getNextScheduledDate();
        boolean alreadyExists = transactionRepository.existsByRecurringExpenseAndDate(
            recurringExpenseId,
            scheduledDate
        );

        if (alreadyExists) {
            logger.info("Transaction already exists for recurring expense {} on date {}, skipping generation",
                recurringExpenseId, scheduledDate);
            return;  // Idempotent: no duplicate creation
        }

        // Generate the transaction
        Transaction generatedTransaction = createTransactionFromTemplate(template);

        // Update template: advance schedule and record generation
        LocalDate nextDate = calculateNextScheduledDate(
            template.getFrequency(),
            template.getNextScheduledDate()
        );

        template.setNextScheduledDate(nextDate);
        template.setLastGeneratedDate(generatedTransaction.getTransactionDate());
        recurringExpenseRepository.save(template);

        logger.info("Generated transaction from recurring expense: expenseId={}, transactionId={}, " +
                "amount={}, nextScheduledDate={}",
            recurringExpenseId, generatedTransaction.getId(),
            generatedTransaction.getAmount(), nextDate);
    }

    @Override
    public void processScheduledExpenses(LocalDate currentDate) {
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date cannot be null");
        }

        // Find all due expenses (active with nextScheduledDate <= currentDate)
        List<RecurringExpense> dueExpenses = recurringExpenseRepository.findDue(currentDate);

        if (dueExpenses.isEmpty()) {
            logger.info("No recurring expenses due for processing on {}", currentDate);
            return;
        }

        logger.info("Processing {} recurring expenses due on or before {}",
            dueExpenses.size(), currentDate);

        int successCount = 0;
        int failureCount = 0;

        // Process each expense with individual error handling
        for (RecurringExpense expense : dueExpenses) {
            try {
                generateTransaction(expense.getId());
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to generate transaction for recurring expense {}: {}",
                    expense.getId(), e.getMessage(), e);
                // Continue processing remaining expenses (don't abort batch)
            }
        }

        logger.info("Batch processing complete: {} successful, {} failed, {} total",
            successCount, failureCount, dueExpenses.size());
    }

    // ========================================================================
    // Schedule Calculation
    // ========================================================================

    @Override
    public LocalDate calculateNextScheduledDate(TransactionFrequency frequency, LocalDate currentDate) {
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date cannot be null");
        }

        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusDays(7);
            case BIWEEKLY -> currentDate.plusDays(14);
            case MONTHLY -> currentDate.plusMonths(1); // Handles variable month lengths
            case QUARTERLY -> currentDate.plusMonths(3);
            case YEARLY -> currentDate.plusYears(1); // Handles leap years
        };
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Creates a transaction from a recurring expense template.
     *
     * <p>This method generates the expense transaction and marks it as recurring
     * by linking it to the template. Since Transaction entities have immutable fields,
     * this method uses the repository to update the recurring metadata after creation.</p>
     *
     * @param template the recurring expense template
     * @return the generated transaction with recurring metadata set
     */
    private Transaction createTransactionFromTemplate(RecurringExpense template) {
        // Create expense command from template data
        RecordExpenseCommand command = new RecordExpenseCommand(
            template.getSourceAccount().getId(),
            template.getAmount(),
            template.getCategory(),
            template.getNextScheduledDate(),
            template.getDescription()
        );

        // Generate the transaction via TransactionService
        Transaction transaction = transactionService.recordExpense(command);

        // Mark as recurring and link to template
        // Note: Transaction fields are immutable per domain design (FF-004),
        // so we need to update these metadata fields directly via repository
        // This is acceptable as isRecurring and recurringExpenseId are metadata,
        // not core transactional data that affects balances
        transaction.markAsRecurring(template.getId());
        transactionRepository.save(transaction);

        return transaction;
    }

    /**
     * Validates source account is not null and has a valid ID.
     *
     * @param sourceAccount the account to validate
     * @throws IllegalArgumentException if account is null or has null ID
     */
    private void validateSourceAccount(Account sourceAccount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Source account cannot be null");
        }
        if (sourceAccount.getId() == null) {
            throw new IllegalArgumentException("Source account ID cannot be null");
        }
    }

    /**
     * Validates frequency is not null.
     *
     * @param frequency the frequency to validate
     * @throws IllegalArgumentException if frequency is null
     */
    private void validateFrequency(TransactionFrequency frequency) {
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
    }

    /**
     * Validates nextScheduledDate is not null.
     *
     * <p>Logs a warning if the date is in the past, though this is allowed
     * to support backdating scenarios.</p>
     *
     * @param nextScheduledDate the date to validate
     * @throws IllegalArgumentException if date is null
     */
    private void validateNextScheduledDate(LocalDate nextScheduledDate) {
        if (nextScheduledDate == null) {
            throw new IllegalArgumentException("Next scheduled date cannot be null");
        }
        // Warn if scheduling in the past (though this may be intentional for backdating)
        if (nextScheduledDate.isBefore(LocalDate.now())) {
            logger.warn("Scheduling recurring expense in the past: {}", nextScheduledDate);
        }
    }

    /**
     * Validates amount is positive (greater than zero).
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is null or not positive
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Amount must be positive, got: " + amount
            );
        }
    }

    /**
     * Validates budget category is not null.
     *
     * @param category the category to validate
     * @throws IllegalArgumentException if category is null
     */
    private void validateCategory(BudgetCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Budget category cannot be null");
        }
    }

    /**
     * Validates description is not null or blank.
     *
     * @param description the description to validate
     * @throws IllegalArgumentException if description is null or blank
     */
    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
    }
}