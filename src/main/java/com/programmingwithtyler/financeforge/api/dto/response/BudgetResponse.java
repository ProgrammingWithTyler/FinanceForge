package com.programmingwithtyler.financeforge.api.dto.response;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for budget summary information.
 *
 * <p>Provides core budget metadata including category, allocation, period, and status.</p>
 */
public record BudgetResponse(
    Long id,
    BudgetCategory category,
    BigDecimal monthlyAllocationAmount,
    LocalDate periodStart,
    LocalDate periodEnd,
    boolean active,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal utilization
) {
    /**
     Calculated fields (spent, remaining, utilization) may be null depending on endpoint usage.
     *
     * <p>Use this factory method when calculations are not needed (e.g., list views).
     * For full utilization details, use {@link BudgetService#getBudgetUtilization(Long)}.</p>
     *
     * @param budget the budget entity
     * @return a BudgetResponse with null calculation fields
     */
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
            budget.getId(),
            budget.getCategory(),
            budget.getMonthlyAllocationAmount(),
            budget.getPeriodStart(),
            budget.getPeriodEnd(),
            budget.isActive(),
            null,
            null,
            null
        );
    }
}