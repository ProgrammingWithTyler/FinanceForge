package com.programmingwithtyler.financeforge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary of budget performance for a specific period.
 *
 * @param year The year of the period
 * @param month The month of the period (1-12)
 * @param periodStart First day of the period
 * @param periodEnd Last day of the period
 * @param totalAllocated Total amount budgeted across all budgets
 * @param totalSpent Total amount spent across all transactions
 * @param utilization Percentage of budget used (spent/allocated * 100)
 * @param overBudgetCount Number of individual budgets that exceeded their allocation
 * @param totalBudgets Total number of budgets in the period
 */
public record PeriodSummary(
    int year,
    int month,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal totalAllocated,
    BigDecimal totalSpent,
    BigDecimal utilization,
    int overBudgetCount,
    int totalBudgets
) {
}