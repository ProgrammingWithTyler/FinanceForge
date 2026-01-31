package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.dto.PeriodSummary;
import com.programmingwithtyler.financeforge.repository.BudgetRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.BudgetService;
import com.programmingwithtyler.financeforge.service.MonthEndRolloverService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Implementation of month-end budget rollover operations.
 *
 * <p>This service orchestrates the complex workflow of closing budget periods,
 * initializing new periods, and generating performance analytics. It serves as
 * a facade over lower-level budget and transaction services, providing a
 * simplified API for month-end operations.</p>
 *
 * <p><strong>Architecture Decisions:</strong></p>
 * <ul>
 *   <li><strong>Date Conversion:</strong> Accepts year/month integers, converts to LocalDate
 *       ranges internally for data access layer compatibility</li>
 *   <li><strong>Dynamic Calculation:</strong> Spending is calculated on-demand from transactions
 *       rather than stored, ensuring single source of truth</li>
 *   <li><strong>Delegation:</strong> Leverages existing BudgetService.rolloverBudgets()
 *       to avoid code duplication</li>
 *   <li><strong>Transactional:</strong> All operations are wrapped in database transactions
 *       to ensure data consistency</li>
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong></p>
 * <ul>
 *   <li>Period summary generation may be expensive for periods with many transactions</li>
 *   <li>Spending calculations require aggregating transaction data in real-time</li>
 *   <li>Consider caching summary results for frequently-accessed historical periods</li>
 * </ul>
 *
 * @author Tyler (FinanceForge)
 * @since 1.0.0
 * @see MonthEndRolloverService
 */
@Service
@Transactional
public class MonthEndRolloverServiceImpl implements MonthEndRolloverService {

    private final BudgetService budgetService;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Constructs the rollover service with required dependencies.
     *
     * @param budgetService Service for budget lifecycle operations
     * @param budgetRepository Direct repository access for optimized queries
     * @param transactionRepository Direct repository access for spending calculations
     */
    public MonthEndRolloverServiceImpl(
        BudgetService budgetService,
        BudgetRepository budgetRepository,
        TransactionRepository transactionRepository
    ) {
        this.budgetService = budgetService;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ol>
     *   <li>Validates input parameters (year/month in valid ranges)</li>
     *   <li>Ensures period is not in the future (business rule enforcement)</li>
     *   <li>Converts year/month to date range (first to last day of month)</li>
     *   <li>Queries for all active budgets overlapping the period</li>
     *   <li>Deactivates each budget using domain method</li>
     *   <li>Persists changes and returns count</li>
     * </ol>
     *
     * <p><strong>Why This Design:</strong></p>
     * <p>We use {@code findByActiveAndPeriodOverlap} instead of a hypothetical
     * {@code findByYearAndMonth} because budgets in this system use date ranges
     * (periodStart/periodEnd) rather than discrete month fields. This supports
     * multi-month budgets (e.g., quarterly budgets) and provides more flexibility.</p>
     */
    @Override
    public int closeBudgetPeriod(int year, int month) {
        // Validate inputs first to fail fast
        validateYearMonthParameters(year, month);
        validatePeriodIsNotInFuture(year, month);

        // Convert year/month to date boundaries for repository queries
        LocalDate periodStart = calculatePeriodStart(year, month);
        LocalDate periodEnd = calculatePeriodEnd(year, month);

        // Find all active budgets that overlap with this period
        // Note: We only close active budgets; inactive ones are already closed
        List<Budget> activeBudgets = budgetRepository.findByActiveAndPeriodOverlap(
            true,
            periodStart,
            periodEnd
        );

        // Edge case: No budgets to close (valid scenario, not an error)
        if (activeBudgets.isEmpty()) {
            return 0;
        }

        // Deactivate each budget using domain method (encapsulates business logic)
        // This is preferred over direct field manipulation for maintainability
        activeBudgets.forEach(budget -> {
            budget.deactivate();
            budgetRepository.save(budget);
        });

        return activeBudgets.size();
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ol>
     *   <li>Validates all input parameters</li>
     *   <li>Converts both source and target year/month to date ranges</li>
     *   <li>Verifies source period has active budgets (prevents copying from empty period)</li>
     *   <li>Verifies target period is empty (prevents accidental overwrite)</li>
     *   <li>Delegates to BudgetService.rolloverBudgets() for actual copying logic</li>
     *   <li>Returns newly created budget entities</li>
     * </ol>
     *
     * <p><strong>Why Delegate to BudgetService:</strong></p>
     * <p>The {@code BudgetService.rolloverBudgets()} method already implements
     * the complex logic for copying budgets, checking for overlaps, and handling
     * edge cases. Rather than duplicating this code, we reuse it here. This
     * follows the DRY principle and ensures consistency across the codebase.</p>
     *
     * <p><strong>Why Pre-check Source and Target:</strong></p>
     * <p>While {@code rolloverBudgets()} performs its own validation, we check
     * earlier to provide clearer, more specific error messages to API consumers.
     * This improves developer experience when debugging issues.</p>
     */
    @Override
    public List<Budget> initializeBudgetPeriod(int targetYear, int targetMonth, int sourceYear, int sourceMonth) {
        // Validate all parameters
        validateYearMonthParameters(targetYear, targetMonth);
        validateYearMonthParameters(sourceYear, sourceMonth);

        // Convert to date ranges for repository access
        LocalDate sourcePeriodStart = calculatePeriodStart(sourceYear, sourceMonth);
        LocalDate sourcePeriodEnd = calculatePeriodEnd(sourceYear, sourceMonth);
        LocalDate targetPeriodStart = calculatePeriodStart(targetYear, targetMonth);
        LocalDate targetPeriodEnd = calculatePeriodEnd(targetYear, targetMonth);

        // Pre-validation: Ensure source period has budgets to copy
        List<Budget> sourceBudgets = budgetRepository.findByActiveAndPeriodOverlap(
            true,
            sourcePeriodStart,
            sourcePeriodEnd
        );

        if (sourceBudgets.isEmpty()) {
            throw new IllegalStateException(
                String.format("No active budgets found for source period %d-%02d. " +
                        "Cannot initialize target period from an empty source.",
                    sourceYear, sourceMonth)
            );
        }

        // Pre-validation: Ensure target period doesn't already have budgets
        List<Budget> existingTargetBudgets = budgetRepository.findByActiveAndPeriodOverlap(
            true,
            targetPeriodStart,
            targetPeriodEnd
        );

        if (!existingTargetBudgets.isEmpty()) {
            throw new IllegalStateException(
                String.format("Target period %d-%02d already has %d active budget(s). " +
                        "Cannot overwrite existing budgets. Close or delete them first.",
                    targetYear, targetMonth, existingTargetBudgets.size())
            );
        }

        // Delegate to existing rollover method to avoid code duplication
        // This method handles budget copying, allocation preservation, and overlap checking
        return budgetService.rolloverBudgets(
            sourcePeriodStart,
            sourcePeriodEnd,
            targetPeriodStart,
            targetPeriodEnd
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ol>
     *   <li>Validates input parameters</li>
     *   <li>Converts year/month to date range</li>
     *   <li>Queries ALL budgets (active + inactive) for historical accuracy</li>
     *   <li>Aggregates allocated amounts from budget entities</li>
     *   <li>Calculates spent amounts from transaction entities (real-time)</li>
     *   <li>Computes utilization percentage with proper rounding</li>
     *   <li>Counts over-budget items by comparing spent vs. allocated</li>
     *   <li>Constructs and returns PeriodSummary DTO</li>
     * </ol>
     *
     * <p><strong>Why Include Inactive Budgets:</strong></p>
     * <p>We use {@code findByPeriodOverlap} (all budgets) rather than
     * {@code findByActiveAndPeriodOverlap} (active only) because summaries
     * should reflect the complete historical picture. A budget may have been
     * active during the period but deactivated later. Including it ensures
     * accurate reporting.</p>
     *
     * <p><strong>Why Real-Time Calculation:</strong></p>
     * <p>Spending is calculated dynamically from transactions rather than stored
     * in Budget entities. This ensures:</p>
     * <ul>
     *   <li>Single source of truth (transactions)</li>
     *   <li>No data synchronization issues</li>
     *   <li>Ability to recalculate historical periods accurately</li>
     * </ul>
     *
     * <p><strong>Performance Note:</strong></p>
     * <p>For periods with many transactions, summary generation may be slow.
     * Consider caching results for closed/historical periods.</p>
     */
    @Override
    public PeriodSummary generatePeriodSummary(int year, int month) {
        // Validate inputs
        validateYearMonthParameters(year, month);

        // Convert to date range
        LocalDate periodStart = calculatePeriodStart(year, month);
        LocalDate periodEnd = calculatePeriodEnd(year, month);

        // Find ALL budgets (active and inactive) for complete historical picture
        List<Budget> budgets = budgetRepository.findByPeriodOverlap(periodStart, periodEnd);

        // Fail fast if no budgets exist for this period
        if (budgets.isEmpty()) {
            throw new IllegalStateException(
                String.format("No budgets found for period %d-%02d. " +
                        "Cannot generate summary for a period without budgets.",
                    year, month)
            );
        }

        // Aggregate total allocated amounts across all budgets
        BigDecimal totalAllocated = calculateTotalAllocated(budgets);

        // Calculate total spending from transactions (real-time calculation)
        BigDecimal totalSpent = calculateTotalSpent(budgets, periodStart, periodEnd);

        // Compute utilization percentage with proper decimal handling
        BigDecimal utilization = calculateUtilizationPercentage(totalSpent, totalAllocated);

        // Count budgets exceeding their allocation
        int overBudgetCount = countOverBudgetItems(budgets, periodStart, periodEnd);

        // Construct summary DTO
        return new PeriodSummary(
            year,
            month,
            periodStart,
            periodEnd,
            totalAllocated,
            totalSpent,
            utilization,
            overBudgetCount,
            budgets.size()
        );
    }

    // ========================================================================
    // Private Helper Methods - Date Calculations
    // ========================================================================

    /**
     * Calculates the first day of the month for the given year/month.
     *
     * <p>This is a pure function that converts integer parameters to a LocalDate
     * representing the start of the period boundary.</p>
     *
     * <p><strong>Why Extract This:</strong> Date boundary calculation is used in
     * multiple methods. Extracting it improves testability and maintainability.</p>
     *
     * @param year The year (e.g., 2024)
     * @param month The month (1-12)
     * @return LocalDate representing the first day of the month
     */
    private LocalDate calculatePeriodStart(int year, int month) {
        return YearMonth.of(year, month).atDay(1);
    }

    /**
     * Calculates the last day of the month for the given year/month.
     *
     * <p>Handles variable month lengths (28/29/30/31 days) and leap years
     * automatically via {@link YearMonth#atEndOfMonth()}.</p>
     *
     * <p><strong>Why Use YearMonth:</strong> Rather than manual date arithmetic
     * (which is error-prone), we leverage Java's built-in time API which
     * correctly handles edge cases.</p>
     *
     * @param year The year (e.g., 2024)
     * @param month The month (1-12)
     * @return LocalDate representing the last day of the month
     */
    private LocalDate calculatePeriodEnd(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    // ========================================================================
    // Private Helper Methods - Validation
    // ========================================================================

    /**
     * Validates that year and month parameters are within acceptable ranges.
     *
     * <p><strong>Validation Rules:</strong></p>
     * <ul>
     *   <li>Year must be between 1900 and 2100 (prevents date arithmetic overflow)</li>
     *   <li>Month must be between 1 and 12 (standard calendar months)</li>
     * </ul>
     *
     * <p><strong>Why These Ranges:</strong></p>
     * <ul>
     *   <li>1900-2100: Covers reasonable business timeframe, prevents edge cases</li>
     *   <li>1-12: Standard month validation, prevents invalid LocalDate construction</li>
     * </ul>
     *
     * <p><strong>Why Fail Fast:</strong> Validating at service entry point provides
     * clear error messages and prevents cascading failures in repository layer.</p>
     *
     * @param year The year to validate
     * @param month The month to validate
     * @throws IllegalArgumentException if parameters are outside valid ranges
     */
    private void validateYearMonthParameters(int year, int month) {
        if (year < 1900 || year > 2100) {
            throw new IllegalArgumentException(
                String.format("Year must be between 1900 and 2100. Provided: %d", year)
            );
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                String.format("Month must be between 1 (January) and 12 (December). Provided: %d", month)
            );
        }
    }

    /**
     * Validates that the specified period is not in the future.
     *
     * <p><strong>Business Rule:</strong> You cannot close a budget period that
     * hasn't occurred yet. This prevents premature period closure and maintains
     * data integrity.</p>
     *
     * <p><strong>Implementation:</strong> Compares the requested period against
     * the current system time using {@link YearMonth#now()}. Note that this uses
     * the server's system clock, which should be properly configured.</p>
     *
     * <p><strong>Edge Case:</strong> Closing the current month is allowed (month
     * in progress). Only future months are rejected.</p>
     *
     * @param year The year to check
     * @param month The month to check
     * @throws IllegalStateException if the specified period is in the future
     */
    private void validatePeriodIsNotInFuture(int year, int month) {
        YearMonth requestedPeriod = YearMonth.of(year, month);
        YearMonth currentPeriod = YearMonth.now();

        if (requestedPeriod.isAfter(currentPeriod)) {
            throw new IllegalStateException(
                String.format("Cannot close future period %d-%02d. Current period is %s.",
                    year, month, currentPeriod)
            );
        }
    }

    // ========================================================================
    // Private Helper Methods - Calculations
    // ========================================================================

    /**
     * Calculates the total allocated amount across all provided budgets.
     *
     * <p>Simple aggregation using Stream API. This is a pure function with no
     * side effects.</p>
     *
     * <p><strong>Why Extract This:</strong> Improves readability of
     * {@link #generatePeriodSummary} and makes the calculation testable in isolation.</p>
     *
     * @param budgets List of budgets to aggregate
     * @return Sum of all monthly allocation amounts
     */
    private BigDecimal calculateTotalAllocated(List<Budget> budgets) {
        return budgets.stream()
            .map(Budget::getMonthlyAllocationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total spending across all budget categories for the given period.
     *
     * <p><strong>How It Works:</strong></p>
     * <ol>
     *   <li>For each budget, query TransactionRepository for category spending</li>
     *   <li>Sum spending amounts using reduce operation</li>
     *   <li>Return total across all categories</li>
     * </ol>
     *
     * <p><strong>Why Per-Category:</strong> The transaction repository provides
     * a {@code sumByCategoryAndPeriod} method which is optimized for category-based
     * aggregation. We leverage this rather than writing a custom query.</p>
     *
     * <p><strong>Performance Note:</strong> This makes one query per budget category.
     * For periods with many categories, consider a single aggregation query.</p>
     *
     * @param budgets List of budgets whose categories to aggregate
     * @param periodStart Start of the period (inclusive)
     * @param periodEnd End of the period (inclusive)
     * @return Total spending across all categories in the period
     */
    private BigDecimal calculateTotalSpent(List<Budget> budgets, LocalDate periodStart, LocalDate periodEnd) {
        return budgets.stream()
            .map(budget -> transactionRepository.sumByCategoryAndPeriod(
                budget.getCategory(),
                periodStart,
                periodEnd
            ).orElse(BigDecimal.ZERO))  // Handle Optional returned by repository
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates utilization percentage: (spent ÷ allocated) × 100.
     *
     * <p><strong>Formula:</strong> {@code (totalSpent / totalAllocated) * 100}</p>
     *
     * <p><strong>Special Cases:</strong></p>
     * <ul>
     *   <li>Zero allocation: Returns 0% (prevents division by zero)</li>
     *   <li>Over 100%: Allowed (indicates overspending)</li>
     *   <li>Negative spending: Theoretically possible with refunds, handled correctly</li>
     * </ul>
     *
     * <p><strong>Precision Handling:</strong></p>
     * <ul>
     *   <li>Division uses 4 decimal places for intermediate calculation</li>
     *   <li>Final result rounded to 2 decimal places (e.g., 85.73%)</li>
     *   <li>Uses HALF_UP rounding mode (standard financial rounding)</li>
     * </ul>
     *
     * <p><strong>Why These Precision Settings:</strong> Financial calculations
     * require exact decimal arithmetic. 4 decimals during division prevents
     * precision loss, 2 decimals in result matches standard percentage display.</p>
     *
     * @param totalSpent Total amount spent in the period
     * @param totalAllocated Total amount budgeted for the period
     * @return Utilization percentage (0-100+) with 2 decimal places
     * @throws IllegalArgumentException if either parameter is null
     */
    private BigDecimal calculateUtilizationPercentage(BigDecimal totalSpent, BigDecimal totalAllocated) {
        if (totalSpent == null) {
            throw new IllegalArgumentException("Total spent cannot be null");
        }

        if (totalAllocated == null) {
            throw new IllegalArgumentException("Total allocated cannot be null");
        }

        // Edge case: avoid division by zero
        if (totalAllocated.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate: (spent / allocated) * 100, with proper decimal precision
        return totalSpent
            .divide(totalAllocated, 4, RoundingMode.HALF_UP)  // 4 decimal intermediate precision
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);  // 2 decimal final precision
    }

    /**
     * Counts how many budgets exceeded their allocated amount during the period.
     *
     * <p><strong>Logic:</strong> For each budget, compare actual spending against
     * the allocated amount. Count those where spending exceeds allocation.</p>
     *
     * <p><strong>Why Per-Budget Calculation:</strong> We need individual budget
     * spending to make the comparison. This requires querying transactions for
     * each budget category separately.</p>
     *
     * <p><strong>Performance Note:</strong> Like {@link #calculateTotalSpent},
     * this makes one query per budget. Consider optimization if performance
     * becomes an issue.</p>
     *
     * @param budgets List of budgets to check
     * @param periodStart Start of the period (inclusive)
     * @param periodEnd End of the period (inclusive)
     * @return Count of budgets where spending > allocation
     */
    private int countOverBudgetItems(List<Budget> budgets, LocalDate periodStart, LocalDate periodEnd) {
        return (int) budgets.stream()
            .filter(budget -> {
                BigDecimal spent = transactionRepository.sumByCategoryAndPeriod(
                    budget.getCategory(),
                    periodStart,
                    periodEnd
                ).orElse(BigDecimal.ZERO);  // Handle Optional
                return spent.compareTo(budget.getMonthlyAllocationAmount()) > 0;
            })
            .count();
    }
}