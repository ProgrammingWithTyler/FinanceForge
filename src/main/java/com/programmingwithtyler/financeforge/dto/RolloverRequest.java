package com.programmingwithtyler.financeforge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for rolling over budgets to a new period.
 *
 * <p>Copies all active budgets from the source period to the target period
 * with the same allocation amounts.</p>
 */
public record RolloverRequest(
    @NotNull @Min(2000) Integer sourceYear,
    @NotNull @Min(1) @Max(12) Integer sourceMonth,
    @NotNull @Min(2000) Integer targetYear,
    @NotNull @Min(1) @Max(12) Integer targetMonth
) {
}