package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.dto.BudgetResponse;
import com.programmingwithtyler.financeforge.dto.BudgetUtilizationResponse;
import com.programmingwithtyler.financeforge.service.exception.BudgetNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.BudgetOverlapException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing budgets.
 *
 * <p>Handles budget lifecycle (create, update, close), spending calculations,
 * period management, and budget utilization reporting.</p>
 *
 * <p><b>Important:</b> Budget spending is calculated dynamically from transactions,
 * not stored in the Budget entity. This maintains a single source of truth and
 * prevents data inconsistency.</p>
 *
 * @see com.programmingwithtyler.financeforge.domain.Transaction
 * @see TransactionService
 */
public interface BudgetService {

    // ========================================================================
    // Budget Lifecycle
    // ========================================================================

    /**
     * Create a new budget for a given category and time period.
     *
     * <p>Budgets define spending allocations for specific categories (e.g., Groceries,
     * Entertainment) over a defined period (typically monthly).</p>
     *
     * @param category the budget category (cannot be null)
     * @param allocatedAmount the allocated budget amount for the period (must be positive)
     * @param periodStart the start date of the budget period (inclusive)
     * @param periodEnd the end date of the budget period (inclusive)
     * @return the created Budget entity
     * @throws IllegalArgumentException if category is null, amount is non-positive, or period is invalid
     * @throws BudgetOverlapException if a budget already exists for the same category and overlapping period
     */
    Budget createBudget(
        BudgetCategory category,
        BigDecimal allocatedAmount,
        LocalDate periodStart,
        LocalDate periodEnd
    );

    /**
     * Update an existing budget's metadata.
     *
     * <p>Only non-null parameters are updated. Period changes must not create
     * overlaps with other budgets for the same category.</p>
     *
     * @param budgetId the ID of the budget to update
     * @param category the new category (optional)
     * @param allocatedAmount the new allocation amount (optional)
     * @param periodStart the new start date (optional)
     * @param periodEnd the new end date (optional)
     * @param isActive whether the budget is active (optional)
     * @return the updated Budget entity
     * @throws BudgetNotFoundException if budget with given ID does not exist
     * @throws BudgetOverlapException if updated period/category conflicts with an existing budget
     * @throws IllegalArgumentException if validation fails (e.g., negative amount, invalid period)
     */
    Budget updateBudget(
        Long budgetId,
        BudgetCategory category,
        BigDecimal allocatedAmount,
        LocalDate periodStart,
        LocalDate periodEnd,
        Boolean isActive
    );

    /**
     * Close a budget (soft delete).
     *
     * <p>Closed budgets are marked inactive and excluded from active budget queries.
     * Historical spending data is preserved for reporting.</p>
     *
     * @param budgetId the ID of the budget to close
     * @return true if budget was successfully closed, false if already inactive
     * @throws BudgetNotFoundException if budget does not exist
     */
    boolean closeBudget(Long budgetId);

    // ========================================================================
    // Budget Queries
    // ========================================================================

    /**
     * Retrieve a single budget by ID.
     *
     * @param budgetId the budget ID
     * @return the Budget entity
     * @throws BudgetNotFoundException if budget does not exist
     */
    Budget getBudget(Long budgetId);

    /**
     * List budgets with optional filtering.
     *
     * <p>Filters by category, active status, and/or period dates.
     * All filter fields are optional (null = no filter applied).</p>
     *
     * @param filter filtering criteria (category, isActive, periodStart, periodEnd)
     * @return list of budgets matching the filters, ordered by period start descending
     */
    List<Budget> listBudgets(BudgetFilter filter);

    /**
     * Find the active budget for a given category and date.
     *
     * <p>Returns the budget whose period contains the specified date.</p>
     *
     * @param category the budget category (cannot be null)
     * @param date the date to check (cannot be null)
     * @return the active budget for this category and date, or null if none exists
     * @throws IllegalArgumentException if category or date is null
     */
    Budget findActiveBudgetForCategoryAndDate(BudgetCategory category, LocalDate date);

    // ========================================================================
    // Budget Spending Calculations (Derived from Transactions)
    // ========================================================================

    /**
     * Calculate the total amount spent against a budget.
     *
     * <p>Spending is calculated by summing all expense transactions for the budget's
     * category within its period. The value is not stored in the Budget entity.</p>
     *
     * @param budgetId the ID of the budget
     * @return the total amount spent (derived from transactions)
     * @throws BudgetNotFoundException if budget does not exist
     */
    BigDecimal calculateSpent(Long budgetId);

    /**
     * Calculate the remaining budget allowance.
     *
     * <p>Formula: allocatedAmount - calculateSpent()</p>
     *
     * <p>Negative values indicate over-budget condition.</p>
     *
     * @param budgetId the ID of the budget
     * @return the remaining budget amount (may be negative if over-budget)
     * @throws BudgetNotFoundException if budget does not exist
     */
    BigDecimal calculateRemaining(Long budgetId);

    /**
     * Calculate the utilization percentage of a budget.
     *
     * <p>Formula: (calculateSpent() / allocatedAmount) * 100</p>
     *
     * <p>Values over 100% indicate over-budget condition.</p>
     *
     * @param budgetId the ID of the budget
     * @return utilization percentage (0-100+)
     * @throws BudgetNotFoundException if budget does not exist
     * @throws ArithmeticException if allocated amount is zero
     */
    BigDecimal calculateUtilization(Long budgetId);

    // ========================================================================
    // DTO Conversion & Response Building
    // ========================================================================

    /**
     * Build a complete budget response with all calculated fields.
     *
     * <p>This method retrieves the budget entity and enriches it with
     * calculated spending, remaining, and utilization data.</p>
     *
     * @param budgetId the ID of the budget
     * @return a BudgetResponse with all fields populated
     * @throws BudgetNotFoundException if budget does not exist
     */
    BudgetResponse getBudgetResponse(Long budgetId);

    /**
     * Build a detailed utilization response for a budget.
     *
     * <p>Calculates spending metrics and determines utilization status:
     * <ul>
     *   <li>ON_TRACK: utilization &lt; 80%</li>
     *   <li>WARNING: utilization between 80-100%</li>
     *   <li>OVER_BUDGET: utilization &gt; 100%</li>
     * </ul>
     * </p>
     *
     * @param budgetId the ID of the budget
     * @return a BudgetUtilizationResponse with status indicator
     * @throws BudgetNotFoundException if budget does not exist
     * @throws ArithmeticException if allocated amount is zero
     */
    BudgetUtilizationResponse getBudgetUtilization(Long budgetId);

    // ========================================================================
    // Budget Reporting & Analytics
    // ========================================================================

    /**
     * Retrieve all budgets where utilization exceeds a given threshold.
     *
     * <p>Useful for identifying budgets that need attention (e.g., 80% spent).</p>
     *
     * @param utilizationThresholdPercent the utilization threshold percentage (e.g., 80 for 80%)
     * @return a list of budgets exceeding the threshold, ordered by utilization descending
     * @throws IllegalArgumentException if threshold is negative
     */
    List<Budget> findBudgetsExceedingThreshold(BigDecimal utilizationThresholdPercent);

    /**
     * Retrieve all budgets that are currently over budget (spent > allocated).
     *
     * @return a list of over-budget budgets, ordered by overage amount descending
     */
    List<Budget> findOverBudgets();

    /**
     * Calculate total allocated budget across all active budgets for a given period.
     *
     * <p>Useful for understanding overall spending plan for a month or year.</p>
     *
     * @param periodStart the start of the period (inclusive)
     * @param periodEnd the end of the period (inclusive)
     * @return the sum of allocated amounts for all active budgets in the period
     * @throws IllegalArgumentException if period is invalid
     */
    BigDecimal calculateTotalAllocatedForPeriod(LocalDate periodStart, LocalDate periodEnd);

    /**
     * Calculate total spending across all budgets for a given period.
     *
     * <p>Derived by summing spending for each active budget in the period.</p>
     *
     * @param periodStart the start of the period (inclusive)
     * @param periodEnd the end of the period (inclusive)
     * @return the total spending amount (derived from transactions)
     * @throws IllegalArgumentException if period is invalid
     */
    BigDecimal calculateTotalSpentForPeriod(LocalDate periodStart, LocalDate periodEnd);

    // ========================================================================
    // Period Management
    // ========================================================================

    /**
     * Create budgets for a new period based on previous period's allocations.
     *
     * <p>Useful for rolling over monthly budgets. Copies allocation amounts from
     * the previous period to the new period for all active categories.</p>
     *
     * @param sourcePeriodStart the start date of the period to copy from
     * @param sourcePeriodEnd the end date of the period to copy from
     * @param newPeriodStart the start date of the new period
     * @param newPeriodEnd the end date of the new period
     * @return list of newly created budgets
     * @throws IllegalArgumentException if periods are invalid
     * @throws BudgetOverlapException if new period overlaps with existing budgets
     */
    List<Budget> rolloverBudgets(
        LocalDate sourcePeriodStart,
        LocalDate sourcePeriodEnd,
        LocalDate newPeriodStart,
        LocalDate newPeriodEnd
    );

    /**
     * Find all active budgets that overlap with the given period.
     *
     * <p>Returns budgets where the budget's period (periodStart to periodEnd)
     * overlaps with the specified date range. A budget overlaps if its period
     * intersects with the query period in any way.</p>
     *
     * <p>Example: For period 2026-02-01 to 2026-02-28, this returns:
     * <ul>
     *   <li>Budgets that start and end within February</li>
     *   <li>Budgets that start before February but end during February</li>
     *   <li>Budgets that start during February but end after</li>
     *   <li>Budgets that span across the entire February period</li>
     * </ul>
     * </p>
     *
     * @param periodStart the start date of the period to query (inclusive)
     * @param periodEnd the end date of the period to query (inclusive)
     * @return list of active budgets overlapping the period, empty list if none found
     * @throws IllegalArgumentException if periodStart or periodEnd is null
     * @throws IllegalArgumentException if periodEnd is before periodStart
     */
    List<Budget> findBudgetsForPeriod(LocalDate periodStart, LocalDate periodEnd);
}
