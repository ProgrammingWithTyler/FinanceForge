package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.DuplicateAccountNameException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for managing accounts.
 *
 * <p>Handles account lifecycle (create, update, close), balance inquiries,
 * and account-level reporting calculations.</p>
 *
 * <p><b>Important:</b> Account balances are updated exclusively through the
 * {@link TransactionService}. Direct balance adjustments are not permitted
 * to maintain ledger integrity.</p>
 *
 * @see TransactionService
 * @see Transaction
 */
public interface AccountService {

    // ========================================================================
    // Account Lifecycle
    // ========================================================================

    /**
     * Create a new account with a starting balance.
     *
     * <p>The starting balance represents the account's initial value and is used
     * to calculate net change over time. No synthetic "initial balance" transaction
     * is created.</p>
     *
     * @param name the account name (must be unique and non-blank)
     * @param type the account type (cannot be null)
     * @param startingBalance the initial balance (must be >= 0 for most account types)
     * @param description optional description (can be null)
     * @return the created Account entity
     * @throws IllegalArgumentException if name is blank, type is null, or balance is invalid
     * @throws DuplicateAccountNameException if an account with the same name already exists
     */
    Account createAccount(
        String name,
        AccountType type,
        BigDecimal startingBalance,
        String description
    );

    /**
     * Update an existing account's metadata.
     *
     * <p>Only non-null parameters are updated. This method cannot modify
     * the account balance (use {@link TransactionService} instead).</p>
     *
     * @param accountId the ID of the account to update
     * @param name new account name (optional, must be unique if provided)
     * @param description new description (optional)
     * @param isActive new active status (optional, use closeAccount for proper closure)
     * @param type new account type (optional)
     * @return the updated Account entity
     * @throws AccountNotFoundException if account with given ID does not exist
     * @throws DuplicateAccountNameException if updated name conflicts with an existing account
     * @throws IllegalArgumentException if attempting to activate a closed account with transactions
     */
    Account updateAccount(
        Long accountId,
        String name,
        String description,
        Boolean isActive,
        AccountType type
    );

    /**
     * Close an account (soft delete).
     *
     * <p>Accounts with transaction history are marked inactive rather than deleted.
     * Inactive accounts cannot be used for new transactions but remain visible for
     * reporting and historical analysis.</p>
     *
     * <p>Accounts without transaction history may be hard-deleted.</p>
     *
     * @param accountId the ID of the account to close
     * @return true if account was successfully closed, false if already inactive
     * @throws AccountNotFoundException if account does not exist
     */
    boolean closeAccount(Long accountId);

    // ========================================================================
    // Account Queries
    // ========================================================================

    /**
     * Retrieve a single account by ID.
     *
     * @param accountId the account ID
     * @return the Account entity
     * @throws AccountNotFoundException if account does not exist
     */
    Account getAccount(Long accountId);

    /**
     * Retrieve the current balance of a specific account.
     *
     * <p>The current balance reflects the starting balance plus all transaction
     * impacts (debits and credits).</p>
     *
     * @param accountId the ID of the account
     * @return the current balance
     * @throws AccountNotFoundException if account does not exist
     */
    BigDecimal getBalance(Long accountId);

    /**
     * List accounts based on filter criteria.
     *
     * <p>All filter fields are optional (null = no filter applied).</p>
     *
     * @param filter account filter criteria (active status, type, balance range, name substring)
     * @return a list of accounts matching the filter, ordered by name
     */
    List<Account> listAccounts(AccountFilter filter);

    // ========================================================================
    // Account Reporting & Analytics
    // ========================================================================

    /**
     * Calculate total balance across all active accounts.
     *
     * <p>This represents the user's total net worth as tracked in the system.
     * Inactive accounts are excluded.</p>
     *
     * @return the sum of all active account balances
     */
    BigDecimal calculateTotalBalance();

    /**
     * Calculate total balance across accounts of a specific type.
     *
     * <p>Useful for reporting (e.g., "Total Cash Accounts", "Total Credit Card Debt").</p>
     *
     * @param type the account type to sum (cannot be null)
     * @return the sum of balances for all active accounts of the specified type
     * @throws IllegalArgumentException if type is null
     */
    BigDecimal calculateTotalBalanceByType(AccountType type);

    /**
     * Sum balances for accounts below a certain threshold.
     *
     * <p>Useful for identifying low-balance accounts that may need attention.</p>
     *
     * @param threshold the balance threshold (must be >= 0)
     * @return total sum of balances for accounts below the threshold
     * @throws IllegalArgumentException if threshold is negative
     */
    BigDecimal sumBalancesBelowThreshold(BigDecimal threshold);

    /**
     * Calculate net change (current balance - starting balance) for an account.
     *
     * <p>Represents total gains/losses since account creation. Positive values
     * indicate net growth; negative values indicate net decline.</p>
     *
     * @param accountId the ID of the account
     * @return the net change amount
     * @throws AccountNotFoundException if account does not exist
     */
    BigDecimal calculateNetChange(Long accountId);
}
