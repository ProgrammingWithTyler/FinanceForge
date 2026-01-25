package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.Account;

import java.math.BigDecimal;

public record BalanceResponse(
    BigDecimal currentBalance,
    BigDecimal startingBalance,
    BigDecimal netChange
) {
    public static BalanceResponse from(Account account) {
        return new BalanceResponse(
            account.getCurrentBalance(),
            account.getStartingBalance(),
            account.getNetChange()
        );
    }
}