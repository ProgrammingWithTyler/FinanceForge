package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;

/**
 * Response DTO for budget utilization details.
 *
 * <p>Provides a comprehensive view of budget status including allocated amounts,
 * spending, remaining balance, utilization percentage, and status indicator.</p>
 */
public record BudgetUtilizationResponse(
    Long budgetId,
    BudgetCategory category,
    BigDecimal allocated,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal utilizationPercent,
    UtilizationStatus status
) {
    /**
     * Utilization status based on spending percentage.
     */
    public enum UtilizationStatus {
        /** Utilization is below 80% - budget is on track */
        ON_TRACK,

        /** Utilization is between 80-100% - approaching limit */
        WARNING,

        /** Utilization exceeds 100% - over budget */
        OVER_BUDGET
    }
}