package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing budgets.
 * Handles budget lifecycle, spending tracking, period management,
 * and reporting/utilization calculations.
 */
public interface BudgetService {

    /**
     * Create a new budget for a given category and period.
     *
     * @param category the budget category (cannot be null)
     * @param monthlyAllocationAmount the allocated budget amount for the period (must be positive)
     * @param periodStart the start date of the budget period
     * @param periodEnd the end date of the budget period
     * @return the created Budget entity
     * @throws IllegalArgumentException if any argument is invalid
     * @throws DuplicateBudgetException if a budget already exists for the same category and period
     */
    Budget createBudget(BudgetCategory category, BigDecimal monthlyAllocationAmount,
                        LocalDate periodStart, LocalDate periodEnd);

    /**
     * Update an existing budget.
     * Only fields provided (non-null) are updated.
     *
     * @param budgetId the ID of the budget to update
     * @param category the new category (optional)
     * @param monthlyAllocationAmount the new allocation amount (optional)
     * @param periodStart the new start date (optional)
     * @param periodEnd the new end date (optional)
     * @param active whether the budget is active (optional)
     * @return the updated Budget entity
     * @throws BudgetNotFoundException if budget with given ID does not exist
     * @throws DuplicateBudgetException if updated period/category conflicts with an existing budget
     */
    Budget updateBudget(Long budgetId, BudgetCategory category, BigDecimal monthlyAllocationAmount,
                        LocalDate periodStart, LocalDate periodEnd, Boolean active);

    /**
     * Retrieve a budget by its ID.
     *
     * @param budgetId the ID of the budget
     * @return the Budget entity
     * @throws BudgetNotFoundException if budget with given ID does not exist
     */
    Budget getBudget(Long budgetId);

    /**
     * List budgets optionally filtered by category, active status, and period.
     *
     * @param category filter by budget category (optional)
     * @param active filter by active status (optional)
     * @param periodStart filter budgets starting on or after this date (optional)
     * @param periodEnd filter budgets ending on or before this date (optional)
     * @return a list of budgets matching the filters
     */
    List<Budget> listBudgets(BudgetCategory category, Boolean active, LocalDate periodStart, LocalDate periodEnd);

    /**
     * Record spending against a budget.
     *
     * @param budgetId the ID of the budget
     * @param amount the amount to spend (must be positive)
     * @throws BudgetNotFoundException if budget does not exist
     * @throws IllegalArgumentException if amount is <= 0
     * @throws IllegalStateException if spending exceeds the allocated budget
     */
    void spend(Long budgetId, BigDecimal amount);

    /**
     * Reset a budget's period and spent amount.
     *
     * @param budgetId the ID of the budget
     * @param newStart the new start date of the period
     * @param newEnd the new end date of the period
     * @throws BudgetNotFoundException if budget does not exist
     */
    void resetPeriod(Long budgetId, LocalDate newStart, LocalDate newEnd);

    /**
     * Calculate the utilization percentage of a budget.
     * Formula: (currentSpentAmount / monthlyAllocationAmount) * 100
     *
     * @param budgetId the ID of the budget
     * @return utilization percentage (0-100+)
     * @throws BudgetNotFoundException if budget does not exist
     */
    BigDecimal calculateUtilization(Long budgetId);

    /**
     * Retrieve all budgets where utilization exceeds a given threshold.
     *
     * @param utilizationThresholdPercent the utilization threshold percentage (e.g., 80 for 80%)
     * @return a list of budgets exceeding the threshold
     */
    List<Budget> budgetsExceedingThreshold(BigDecimal utilizationThresholdPercent);
}
