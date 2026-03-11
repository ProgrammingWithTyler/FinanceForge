package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecurringExpenseRequest(
    @NotNull
    TransactionFrequency frequency,

    @NotNull
    LocalDate nextScheduledDate,

    @NotNull
    @Positive
    BigDecimal amount,

    @NotNull
    BudgetCategory category,

    @NotBlank
    String description,

    @NotNull
    Long sourceAccountId
) {
}
