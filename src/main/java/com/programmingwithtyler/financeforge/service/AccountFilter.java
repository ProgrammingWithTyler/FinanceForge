package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.AccountType;
import java.math.BigDecimal;

/**
 * Filter criteria for account queries.
 *
 * <p>All fields are optional (null = no filter applied).</p>
 */
public record AccountFilter(
    Boolean isActive,
    AccountType type,
    BigDecimal minBalance,
    BigDecimal maxBalance,
    String nameSubstring
) {
}
