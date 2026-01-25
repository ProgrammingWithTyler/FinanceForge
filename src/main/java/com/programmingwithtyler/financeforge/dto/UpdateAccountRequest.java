package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.AccountType;

public record UpdateAccountRequest(
    String name,
    String description,
    Boolean isActive,
    AccountType type
) {
}
