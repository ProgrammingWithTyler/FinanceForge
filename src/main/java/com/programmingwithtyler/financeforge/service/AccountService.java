package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.domain.BalanceAdjustType;
import com.programmingwithtyler.financeforge.domain.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    /**
     * Create a new account with a starting balance.
     */
    Account createAccount(String name, AccountType type, BigDecimal startingBalance, String description);

    /**
     * Update an existing account. Any field can be null if it should remain unchanged.
     */
    Account updateAccount(Long accountId, String name, String description, Boolean status, AccountType type);

    /**
     * Retrieve the current balance of a specific account.
     */
    BigDecimal getBalance(Long accountId);

    /**
     * Adjust the account balance by a positive or negative amount.
     * Should be transactional.
     */
    public Account adjustBalance(Long accountId, BigDecimal amount, BalanceAdjustType adjustType);

    /**
     * Close the account. Should prevent closure if there are dependent transactions.
     * Returns true if closed successfully.
     */
    boolean closeAccount(Long accountId);

    /**
     * List accounts based on filter criteria.
     */
    List<Account> listAccounts(AccountFilter filter);

    /**
     * Sum of balances for accounts below a certain threshold.
     */
    BigDecimal sumBalancesBelowThreshold(BigDecimal threshold);

    /**
     * Calculate total balance across all accounts.
     */
    BigDecimal calculateTotalBalance();
}
