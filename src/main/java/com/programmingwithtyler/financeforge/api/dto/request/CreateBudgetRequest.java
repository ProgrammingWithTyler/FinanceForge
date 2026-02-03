package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequest(
    @NotNull BudgetCategory category,
    @NotNull @Positive BigDecimal monthlyAllocationAmount,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd
) {
}
