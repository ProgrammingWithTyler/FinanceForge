package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBudgetRequest(
    BudgetCategory category,
    BigDecimal monthlyAllocationAmount,
    LocalDate periodStart,
    LocalDate periodEnd,
    Boolean isActive
) {
}
