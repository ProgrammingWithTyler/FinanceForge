package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.time.LocalDate;

/**
 * Filter criteria for budget queries.
 *
 * <p>All fields are optional (null = no filter applied).</p>
 */
public record BudgetFilter(
    BudgetCategory category,
    Boolean isActive,
    LocalDate periodStart,
    LocalDate periodEnd
) {}