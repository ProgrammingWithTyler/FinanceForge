package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transaction entity persistence operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Check if any transactions exist for the given account.
     * Used to determine if account can be hard-deleted.
     */
    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Transaction t
        WHERE t.sourceAccount.id = :accountId
           OR t.destinationAccount.id = :accountId
        """)
    boolean existsByAccountId(@Param("accountId") Long accountId);

    /**
     * Sum transaction amounts for a budget category within a date range.
     * Used to calculate budget spending.
     */
    @Query("""
        SELECT SUM(t.amount)
        FROM Transaction t
        WHERE t.budgetCategory = :category
          AND t.date BETWEEN :start AND :end
          AND t.type = 'EXPENSE'
        """)
    Optional<BigDecimal> sumByCategoryAndPeriod(
        @Param("category") BudgetCategory category,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    /**
     * Find transactions for an account within a date range.
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
          AND t.date BETWEEN :start AND :end
        ORDER BY t.date DESC
        """)
    List<Transaction> findByAccountIdAndDateBetween(
        @Param("accountId") Long accountId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );

    /**
     * Check if a transaction exists for a recurring expense on a specific date.
     * Used for idempotency in recurring expense generation.
     */
    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Transaction t
        WHERE t.recurringExpenseId = :recurringExpenseId
          AND t.date = :date
        """)
    boolean existsByRecurringExpenseAndDate(
        @Param("recurringExpenseId") Long recurringExpenseId,
        @Param("date") LocalDate date
    );

    /**
     * Find transactions with optional filters, excluding soft-deleted transactions.
     *
     * @param startDate filter by date >= startDate (optional)
     * @param endDate filter by date <= endDate (optional)
     * @param category filter by budget category (optional)
     * @param accountId filter by source or destination account (optional)
     * @param type filter by transaction type (optional)
     * @return list of transactions matching filters, ordered by date descending
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.isDeleted = false
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
          AND (:category IS NULL OR t.category = :category)
          AND (:accountId IS NULL OR t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
          AND (:type IS NULL OR t.type = :type)
        ORDER BY t.transactionDate DESC, t.id DESC
        """)
    List<Transaction> findWithFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("category") BudgetCategory category,
        @Param("accountId") Long accountId,
        @Param("type") TransactionType type
    );

    /**
     * Check if a reversal transaction exists for the given original transaction ID.
     *
     * @param originalTransactionId the ID of the original transaction
     * @return true if a reversal exists (description contains "REVERSAL" and original ID)
     */
    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM Transaction t
        WHERE t.description LIKE CONCAT('%Original: ', :originalId, '%')
          AND t.description LIKE 'REVERSAL:%'
        """)
    boolean existsReversalFor(@Param("originalId") Long originalTransactionId);
}