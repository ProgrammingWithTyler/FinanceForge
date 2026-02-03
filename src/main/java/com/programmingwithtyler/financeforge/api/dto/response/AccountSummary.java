package com.programmingwithtyler.financeforge.api.dto.response;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;

/**
 * Lightweight account summary for transaction responses.
 *
 * Prevents over-fetching by including only essential account information
 * in transaction response payloads. Full account details (balance, description, etc.)
 * are excluded to minimize response size.
 *
 * @param id Account identifier
 * @param accountName Account display name
 * @param type Account type (CHECKING, SAVINGS, CREDIT_CARD, etc.)
 */
public record AccountSummary(
    Long id,
    String accountName,
    AccountType type
) {
    /**
     * Factory method to create AccountSummary from Account entity.
     *
     * @param account The full account entity
     * @return Lightweight summary containing only id, accountName, and type
     */
    public static AccountSummary from(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountSummary(
            account.getId(),
            account.getAccountName(),
            account.getType()
        );
    }
}