package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    Long id,
    String accountName,
    AccountType type,
    BigDecimal currentBalance,
    BigDecimal startingBalance,
    Boolean active,
    String description,
    LocalDateTime createdAt,
    BigDecimal netChange
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountName(),
            account.getType(),
            account.getCurrentBalance(),
            account.getStartingBalance(),
            account.isActive(),
            account.getDescription(),
            account.getCreatedAt(),
            account.getNetChange()
        );
    }
}
