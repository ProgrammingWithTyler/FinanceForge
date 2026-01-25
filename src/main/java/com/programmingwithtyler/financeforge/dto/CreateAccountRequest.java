package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
    @NotBlank
    String name,
    @NotNull
    AccountType type,
    @NotNull
    @DecimalMin("0.00")
    BigDecimal startingBalance,
    String description
    ) {
}
