package com.programmingwithtyler.financeforge.dto;

import com.programmingwithtyler.financeforge.domain.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AccountSummaryResponse(
    BigDecimal totalBalance,
    Map<AccountType, BigDecimal> totalByType,
    int accountCount,
    List<AccountResponse> lowBalanceAccounts
) {
}
