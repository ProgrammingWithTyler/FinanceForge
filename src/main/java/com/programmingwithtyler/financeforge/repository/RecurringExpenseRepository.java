package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for RecurringExpense entity persistence operations.
 */
@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    // ========================================================================
    // Basic Query Methods (your existing methods - can keep for backward compatibility)
    // ========================================================================

    /**
     * Find recurring expenses by active status (unordered).
     */
    List<RecurringExpense> findByActive(boolean active);

    /**
     * Find recurring expenses by exact next scheduled date (unordered).
     */
    List<RecurringExpense> findByNextScheduledDate(LocalDate nextDate);

    /**
     * Find recurring expenses with next scheduled date in range (unordered).
     */
    List<RecurringExpense> findByNextScheduledDateBetween(LocalDate start, LocalDate end);

    /**
     * Find active recurring expenses with next scheduled date in range (unordered).
     */
    List<RecurringExpense> findByActiveAndNextScheduledDateBetween(boolean active, LocalDate start, LocalDate end);

    /**
     * Find recurring expenses by frequency (unordered).
     */
    List<RecurringExpense> findByFrequency(TransactionFrequency frequency);

    /**
     * Find recurring expenses by source account (unordered).
     */
    List<RecurringExpense> findBySourceAccount(Account source);

    // ========================================================================
    // Ordered Query Methods (required by RecurringExpenseService)
    // ========================================================================

    /**
     * Find all recurring expenses ordered by next scheduled date ascending.
     * Used when listing all templates without filters.
     */
    List<RecurringExpense> findAllByOrderByNextScheduledDateAsc();

    /**
     * Find recurring expenses by active status ordered by next scheduled date.
     * Used when filtering by active/inactive status.
     */
    List<RecurringExpense> findByActiveOrderByNextScheduledDateAsc(Boolean active);

    /**
     * Find recurring expenses by source account ordered by next scheduled date.
     * Used when viewing all templates for a specific account.
     */
    List<RecurringExpense> findBySourceAccountOrderByNextScheduledDateAsc(Account sourceAccount);

    /**
     * Find recurring expenses by frequency ordered by next scheduled date.
     * Used when viewing all templates of a specific frequency (e.g., all MONTHLY).
     */
    List<RecurringExpense> findByFrequencyOrderByNextScheduledDateAsc(TransactionFrequency frequency);

    /**
     * Find recurring expenses by active status and source account ordered by next scheduled date.
     * Used when filtering active templates for a specific account.
     */
    List<RecurringExpense> findByActiveAndSourceAccountOrderByNextScheduledDateAsc(
        Boolean active,
        Account sourceAccount
    );

    /**
     * Find recurring expenses by active status and frequency ordered by next scheduled date.
     * Used when filtering active templates of a specific frequency.
     */
    List<RecurringExpense> findByActiveAndFrequencyOrderByNextScheduledDateAsc(
        Boolean active,
        TransactionFrequency frequency
    );

    /**
     * Find recurring expenses by source account and frequency ordered by next scheduled date.
     * Used when viewing templates for an account with a specific frequency.
     */
    List<RecurringExpense> findBySourceAccountAndFrequencyOrderByNextScheduledDateAsc(
        Account sourceAccount,
        TransactionFrequency frequency
    );

    /**
     * Find recurring expenses by all three filter criteria ordered by next scheduled date.
     * Used when applying all filters simultaneously.
     */
    List<RecurringExpense> findByActiveAndSourceAccountAndFrequencyOrderByNextScheduledDateAsc(
        Boolean active,
        Account sourceAccount,
        TransactionFrequency frequency
    );

    // ========================================================================
    // Custom Query for Batch Processing
    // ========================================================================

    /**
     * Finds all active recurring expenses that are due for processing.
     *
     * <p>This query is used by the batch processing job to find all templates
     * that should generate transactions as of the specified date. It returns
     * only active templates with nextScheduledDate on or before the current date.</p>
     *
     * <p><strong>Performance Note:</strong> This query uses the composite index on
     * (is_active, next_scheduled_date) for efficient filtering.</p>
     *
     * @param currentDate the date to check against (typically LocalDate.now())
     * @return list of due recurring expenses, ordered by nextScheduledDate (earliest first)
     */
    @Query("""
        SELECT re FROM RecurringExpense re
        WHERE re.active = true
          AND re.nextScheduledDate <= :currentDate
        ORDER BY re.nextScheduledDate ASC
        """)
    List<RecurringExpense> findDue(@Param("currentDate") LocalDate currentDate);
}