package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.api.dto.PeriodSummary;

import java.util.List;

/**
 * Service interface for managing month-end budget rollover operations.
 *
 * <p>This service handles the critical end-of-period budget management workflow,
 * including closing completed periods, initializing new budget periods from templates,
 * and generating comprehensive period performance summaries.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li>Period Closure: Deactivating budgets for completed fiscal periods</li>
 *   <li>Period Initialization: Creating new budget periods based on historical templates</li>
 *   <li>Performance Analytics: Generating utilization and spending summaries</li>
 * </ul>
 *
 * <p><strong>Design Philosophy:</strong></p>
 * <p>The service operates on year/month integers rather than date ranges to provide
 * a simpler API for clients. Internally, these are converted to appropriate date
 * boundaries (first day of month to last day of month) for data access.</p>
 *
 * <p><strong>Workflow Integration:</strong></p>
 * <p>Typical month-end workflow:</p>
 * <ol>
 *   <li>Generate summary for current period ({@link #generatePeriodSummary(int, int)})</li>
 *   <li>Close current period ({@link #closeBudgetPeriod(int, int)})</li>
 *   <li>Initialize next period ({@link #initializeBudgetPeriod(int, int, int, int)})</li>
 * </ol>
 *
 * @author Tyler (FinanceForge)
 * @since 1.0.0
 * @see Budget
 * @see PeriodSummary
 */
public interface MonthEndRolloverService {

    /**
     * Closes all active budgets for the specified period by marking them inactive.
     *
     * <p>This operation effectively "locks" a budget period, preventing further
     * modifications and signaling that the period is complete. All budgets that
     * overlap with the specified month are deactivated.</p>
     *
     * <p><strong>Business Rules:</strong></p>
     * <ul>
     *   <li>Only active budgets are affected (already inactive budgets are ignored)</li>
     *   <li>Cannot close future periods (must be current month or earlier)</li>
     *   <li>Supports partial overlaps (budgets spanning multiple months)</li>
     *   <li>Operation is idempotent (safe to call multiple times)</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * // Close all budgets for January 2024
     * int closedCount = service.closeBudgetPeriod(2024, 1);
     * System.out.println("Closed " + closedCount + " budgets");
     * </pre>
     *
     * @param year The year of the period to close (e.g., 2024)
     * @param month The month of the period to close (1-12, where 1=January, 12=December)
     * @return The number of budgets successfully closed (deactivated)
     * @throws IllegalArgumentException if year is outside valid range (1900-2100)
     *                                  or month is outside valid range (1-12)
     * @throws IllegalStateException if attempting to close a future period
     */
    int closeBudgetPeriod(int year, int month);

    /**
     * Initializes budgets for a new period by copying configuration from a source period.
     *
     * <p>This method creates new budget entries for the target period using the same
     * categories and allocation amounts as the source period. This is the primary
     * mechanism for propagating budget structure across months while resetting
     * spending to zero.</p>
     *
     * <p><strong>What Gets Copied:</strong></p>
     * <ul>
     *   <li>Budget categories (e.g., GROCERIES, UTILITIES)</li>
     *   <li>Allocated amounts (monthly budget limits)</li>
     *   <li>Active status (all new budgets start active)</li>
     * </ul>
     *
     * <p><strong>What Gets Reset:</strong></p>
     * <ul>
     *   <li>Period dates (set to target year/month boundaries)</li>
     *   <li>Spent amounts (dynamically calculated from transactions, always $0 for new periods)</li>
     *   <li>Audit timestamps (createdAt, updatedAt)</li>
     * </ul>
     *
     * <p><strong>Business Rules:</strong></p>
     * <ul>
     *   <li>Source period must have at least one active budget</li>
     *   <li>Target period must not already have budgets (prevents accidental overwrite)</li>
     *   <li>Supports cross-year initialization (e.g., Dec 2024 → Jan 2025)</li>
     *   <li>Delegates to existing rollover logic for consistency</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * // Set up February 2024 budgets using January 2024 as template
     * List&lt;Budget&gt; newBudgets = service.initializeBudgetPeriod(
     *     2024, 2,  // Target: February 2024
     *     2024, 1   // Source: January 2024
     * );
     * System.out.println("Created " + newBudgets.size() + " budgets for Feb 2024");
     * </pre>
     *
     * @param targetYear The year to create budgets for (e.g., 2024)
     * @param targetMonth The month to create budgets for (1-12)
     * @param sourceYear The year to copy budgets from (e.g., 2024)
     * @param sourceMonth The month to copy budgets from (1-12)
     * @return List of newly created Budget entities for the target period
     * @throws IllegalArgumentException if any year or month parameter is invalid
     * @throws IllegalStateException if source period has no active budgets
     * @throws IllegalStateException if target period already has budgets
     */
    List<Budget> initializeBudgetPeriod(int targetYear, int targetMonth, int sourceYear, int sourceMonth);

    /**
     * Generates a comprehensive performance summary for the specified budget period.
     *
     * <p>This method aggregates all budget data for a given month to provide insights
     * into spending patterns, budget utilization, and overspending. The summary
     * includes both active and inactive budgets to maintain historical accuracy.</p>
     *
     * <p><strong>Calculated Metrics:</strong></p>
     * <ul>
     *   <li><strong>Total Allocated:</strong> Sum of all budget limits for the period</li>
     *   <li><strong>Total Spent:</strong> Sum of all expense transactions (dynamically calculated)</li>
     *   <li><strong>Utilization:</strong> (Total Spent ÷ Total Allocated) × 100, expressed as percentage</li>
     *   <li><strong>Over-Budget Count:</strong> Number of individual budgets exceeding their limits</li>
     *   <li><strong>Total Budgets:</strong> Count of all budgets in the period</li>
     * </ul>
     *
     * <p><strong>Data Sources:</strong></p>
     * <ul>
     *   <li>Budget allocations come from Budget entities</li>
     *   <li>Spending data comes from Transaction entities (real-time calculation)</li>
     *   <li>No cached or denormalized data is used (single source of truth)</li>
     * </ul>
     *
     * <p><strong>Special Cases:</strong></p>
     * <ul>
     *   <li>Zero allocation: Returns 0% utilization (prevents division by zero)</li>
     *   <li>Partial month: Includes all transactions within period boundaries</li>
     *   <li>Multi-month budgets: Only transactions within specified month are counted</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * // Generate January 2024 summary
     * PeriodSummary summary = service.generatePeriodSummary(2024, 1);
     *
     * System.out.println("Month: " + summary.year() + "-" + summary.month());
     * System.out.println("Budgeted: $" + summary.totalAllocated());
     * System.out.println("Spent: $" + summary.totalSpent());
     * System.out.println("Utilization: " + summary.utilization() + "%");
     * System.out.println("Over Budget: " + summary.overBudgetCount() +
     *                    " of " + summary.totalBudgets());
     * </pre>
     *
     * @param year The year of the period to summarize (e.g., 2024)
     * @param month The month of the period to summarize (1-12)
     * @return PeriodSummary containing aggregated metrics and analysis
     * @throws IllegalArgumentException if year or month is invalid
     * @throws IllegalStateException if no budgets exist for the specified period
     */
    PeriodSummary generatePeriodSummary(int year, int month);
}