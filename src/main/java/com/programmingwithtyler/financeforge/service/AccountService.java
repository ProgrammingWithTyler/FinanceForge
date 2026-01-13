package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.domain.BalanceAdjustType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for managing accounts.
 * Handles account lifecycle, balance adjustments, status updates,
 * and reporting calculations.
 */
public interface AccountService {

    /**
     * Create a new account with a starting balance.
     *
     * @param name the account name (must be unique and non-null)
     * @param type the account type (cannot be null)
     * @param startingBalance the initial balance (must be >= 0)
     * @param description optional description
     * @return the created Account entity
     * @throws IllegalArgumentException if any argument is invalid
     * @throws DuplicateAccountException if an account with the same name already exists
     */
    Account createAccount(String name, AccountType type, BigDecimal startingBalance, String description);

    /**
     * Update an existing account.
     * Only non-null fields are updated.
     *
     * @param accountId the ID of the account to update
     * @param name new account name (optional)
     * @param description new description (optional)
     * @param status new active status (optional)
     * @param type new account type (cannot be null)
     * @return the updated Account entity
     * @throws AccountNotFoundException if account with given ID does not exist
     * @throws DuplicateAccountException if updated name conflicts with an existing account
     */
    Account updateAccount(Long accountId, String name, String description, Boolean status, AccountType type);

    /**
     * Retrieve the current balance of a specific account.
     *
     * @param accountId the ID of the account
     * @return the current balance
     * @throws AccountNotFoundException if account does not exist
     */
    BigDecimal getBalance(Long accountId);

    /**
     * Adjust the account balance by a positive or negative amount.
     * Should be transactional.
     *
     * @param accountId the ID of the account
     * @param amount the adjustment amount (must be > 0)
     * @param adjustType the type of adjustment (DEPOSIT or WITHDRAWAL)
     * @return the updated Account entity
     * @throws AccountNotFoundException if account does not exist
     * @throws IllegalArgumentException if amount is invalid or insufficient funds
     */
    Account adjustBalance(Long accountId, BigDecimal amount, BalanceAdjustType adjustType);

    /**
     * Close the account.
     * Prevent closure if there are dependent transactions.
     *
     * @param accountId the ID of the account
     * @return true if account was successfully closed, false if already inactive
     * @throws AccountNotFoundException if account does not exist
     */
    boolean closeAccount(Long accountId);

    /**
     * List accounts based on filter criteria.
     *
     * @param filter account filter (optional fields: active status, type, balance range, name substring)
     * @return a list of accounts matching the filter
     */
    List<Account> listAccounts(AccountFilter filter);

    /**
     * Sum of balances for accounts below a certain threshold.
     *
     * @param threshold the balance threshold (must be >= 0)
     * @return total sum of balances below the threshold
     * @throws IllegalArgumentException if threshold is invalid
     */
    BigDecimal sumBalancesBelowThreshold(BigDecimal threshold);

    /**
     * Calculate total balance across all active accounts.
     *
     * @return the total balance
     */
    BigDecimal calculateTotalBalance();
}
