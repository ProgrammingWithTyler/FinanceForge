package com.programmingwithtyler.financeforge.service;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;
import com.programmingwithtyler.financeforge.service.command.RecordExpenseCommand;
import com.programmingwithtyler.financeforge.service.command.RecordIncomeCommand;
import com.programmingwithtyler.financeforge.service.command.RecordRefundCommand;
import com.programmingwithtyler.financeforge.service.command.RecordTransferCommand;
import com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.InactiveAccountException;
import com.programmingwithtyler.financeforge.service.exception.InsufficientFundsException;
import com.programmingwithtyler.financeforge.service.exception.TransactionNotFoundException;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing financial transactions.
 *
 * <p>This service handles all financial movements in the system: income, expenses,
 * transfers, and refunds. All transaction operations are atomic and update account
 * balances within the same transactional boundary.</p>
 *
 * <p><b>Design Note:</b> Transaction types have fundamentally different semantics
 * and are handled by separate methods rather than a single polymorphic method.
 * This follows the domain model's factory pattern and ensures type safety.</p>
 *
 * @see Transaction
 * @see Account
 * @see BudgetService
 */
public interface TransactionService {

    // ========================================================================
    // Transaction Creation (Follows Domain Model Factory Pattern)
    // ========================================================================

    /**
     * Record an income transaction (credits destination account).
     *
     * <p>Income transactions increase the balance of the destination account.
     * They do not affect budgets and have no source account.</p>
     *
     * <p><b>Transactional:</b> Account balance update and transaction persistence
     * occur atomically.</p>
     *
     * @param command income transaction details
     * @return the created Transaction entity
     * @throws AccountNotFoundException if destination account does not exist
     * @throws InactiveAccountException if destination account is closed
     * @throws IllegalArgumentException if command validation fails
     */
    Transaction recordIncome(RecordIncomeCommand command);

    /**
     * Record an expense transaction (debits source account, updates budget).
     *
     * <p>Expense transactions decrease the balance of the source account and
     * are tracked against the specified budget category.</p>
     *
     * <p><b>Transactional:</b> Account balance update, budget tracking, and
     * transaction persistence occur atomically.</p>
     *
     * @param command expense transaction details (includes required budget category)
     * @return the created Transaction entity
     * @throws AccountNotFoundException if source account does not exist
     * @throws InactiveAccountException if source account is closed
     * @throws InsufficientFundsException if account balance is insufficient
     * @throws IllegalArgumentException if command validation fails
     */
    Transaction recordExpense(RecordExpenseCommand command);

    /**
     * Record a transfer between two accounts (atomic debit/credit).
     *
     * <p>Transfer transactions move funds from source to destination account.
     * Both balances are updated atomically. Transfers do not affect budgets.</p>
     *
     * <p><b>Transactional:</b> Both account balance updates and transaction
     * persistence occur atomically. If either fails, both are rolled back.</p>
     *
     * @param command transfer details (source, destination, amount)
     * @return the created Transaction entity
     * @throws AccountNotFoundException if either account does not exist
     * @throws InactiveAccountException if either account is closed
     * @throws InsufficientFundsException if source account balance is insufficient
     * @throws IllegalArgumentException if source and destination are the same account
     */
    Transaction recordTransfer(RecordTransferCommand command);

    /**
     * Record a refund transaction (credits source account, may reverse budget).
     *
     * <p>Refund transactions increase the balance of the source account (the account
     * that originally paid). If a budget category is provided, the refund amount is
     * deducted from the category's spending for the period.</p>
     *
     * <p><b>Transactional:</b> Account balance update, optional budget adjustment,
     * and transaction persistence occur atomically.</p>
     *
     * @param command refund transaction details
     * @return the created Transaction entity
     * @throws AccountNotFoundException if source account does not exist
     * @throws InactiveAccountException if source account is closed
     * @throws IllegalArgumentException if command validation fails
     */
    Transaction recordRefund(RecordRefundCommand command);

    // ========================================================================
    // Transaction Metadata Updates (Limited Mutability)
    // ========================================================================

    /**
     * Update non-financial metadata of an existing transaction.
     *
     * <p>Only the following fields may be updated:</p>
     * <ul>
     *   <li>Transaction date (for correction purposes)</li>
     *   <li>Budget category (for EXPENSE and REFUND only)</li>
     *   <li>Description (free-form notes)</li>
     * </ul>
     *
     * <p><b>Immutability Constraint:</b> Transaction amount, type, and involved
     * accounts cannot be modified. To correct financial values, create a reversal
     * transaction and a new corrected transaction.</p>
     *
     * @param transactionId the ID of the transaction to update
     * @param newDate new transaction date (optional, null = no change)
     * @param newCategory new budget category (optional, null = no change, only for EXPENSE/REFUND)
     * @param newDescription new description (optional, null = no change)
     * @return the updated Transaction entity
     * @throws TransactionNotFoundException if transaction does not exist
     * @throws IllegalArgumentException if attempting to change immutable fields
     */
    Transaction updateTransactionMetadata(
        Long transactionId,
        LocalDate newDate,
        BudgetCategory newCategory,
        String newDescription
    );

    // ========================================================================
    // Transaction Deletion (Soft Delete)
    // ========================================================================

    /**
     * Soft-delete a transaction.
     *
     * <p>Marks the transaction as deleted without reversing account balances or
     * budget impacts. This preserves ledger integrity while hiding the transaction
     * from standard queries.</p>
     *
     * <p><b>Balance Impact:</b> Soft-deleted transactions do NOT reverse account
     * balances. To correct balances, create a reversal transaction instead.</p>
     *
     * @param transactionId the ID of the transaction to delete
     * @throws TransactionNotFoundException if transaction does not exist
     */
    void deleteTransaction(Long transactionId);

    // ========================================================================
    // Transaction Queries
    // ========================================================================

    /**
     * Retrieve a single transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return the Transaction entity
     * @throws TransactionNotFoundException if transaction does not exist
     */
    Transaction getTransaction(Long transactionId);

    /**
     * List transactions with optional filtering.
     *
     * <p>Soft-deleted transactions are excluded by default. All filter parameters
     * are optional (null = no filter applied).</p>
     *
     * @param startDate filter transactions on or after this date (inclusive)
     * @param endDate filter transactions on or before this date (inclusive)
     * @param category filter by budget category (EXPENSE and REFUND only)
     * @param accountId filter transactions involving this account (as source or destination)
     * @param type filter by transaction type (INCOME, EXPENSE, TRANSFER, REFUND)
     * @return list of matching transactions, ordered by date descending
     */
    List<Transaction> listTransactions(
        LocalDate startDate,
        LocalDate endDate,
        BudgetCategory category,
        Long accountId,
        TransactionType type
    );

    // ========================================================================
    // Transaction Reversals (For Corrections)
    // ========================================================================

    /**
     * Create a reversal transaction for a given transaction.
     *
     * <p>A reversal creates a new transaction with opposite financial impact,
     * effectively canceling the original transaction while preserving audit history.</p>
     *
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Reversing an EXPENSE creates a REFUND</li>
     *   <li>Reversing an INCOME creates an EXPENSE</li>
     *   <li>Reversing a TRANSFER creates a reverse TRANSFER</li>
     * </ul>
     *
     * @param transactionId the ID of the transaction to reverse
     * @param reversalDate the date of the reversal transaction
     * @param reason description explaining why the reversal was made
     * @return the created reversal Transaction entity
     * @throws TransactionNotFoundException if original transaction does not exist
     * @throws IllegalArgumentException if transaction is already reversed
     */
    Transaction reverseTransaction(Long transactionId, LocalDate reversalDate, String reason);
}