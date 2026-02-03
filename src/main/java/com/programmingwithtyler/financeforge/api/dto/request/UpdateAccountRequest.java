package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.domain.AccountType;

public record UpdateAccountRequest(
    String name,
    String description,
    Boolean isActive,
    AccountType type
) {
}
