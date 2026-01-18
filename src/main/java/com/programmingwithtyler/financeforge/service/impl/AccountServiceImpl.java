package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.repository.AccountRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.AccountFilter;
import com.programmingwithtyler.financeforge.service.AccountService;
import com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.DuplicateAccountNameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation of AccountService interface.
 *
 * <p>Manages account lifecycle, balance inquiries, and reporting calculations
 * with full transactional integrity.</p>
 *
 * <p>All public methods are transactional with read-write by default.
 * Read-only methods are explicitly marked with @Transactional(readOnly = true)
 * for performance optimization.</p>
 */
@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountServiceImpl(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // ========================================================================
    // Account Lifecycle
    // ========================================================================

    @Override
    public Account createAccount(
        String name,
        AccountType type,
        BigDecimal startingBalance,
        String description
    ) {
        // Validate inputs
        validateAccountName(name);
        validateAccountType(type);
        validateStartingBalance(startingBalance, type);

        // Check for duplicate name
        if (accountRepository.existsByAccountName(name)) {
            throw new DuplicateAccountNameException(name);
        }

        // Create and persist account
        Account account = new Account();
        account.setAccountName(name);
        account.setType(type);
        account.setStartingBalance(startingBalance);
        account.setCurrentBalance(startingBalance); // Initialize with starting balance
        account.setDescription(description);
        account.setActive(true);

        return accountRepository.save(account);
    }

    @Override
    public Account updateAccount(
        Long accountId,
        String name,
        String description,
        Boolean isActive,
        AccountType type
    ) {
        Account account = getAccount(accountId);

        // Update name if provided and validate uniqueness
        if (name != null && !name.equals(account.getAccountName())) {
            validateAccountName(name);
            if (accountRepository.existsByAccountName(name)) {
                throw new DuplicateAccountNameException(name);
            }
            account.setAccountName(name);
        }

        // Update description if provided
        if (description != null) {
            account.setDescription(description);
        }

        // Update active status if provided
        if (isActive != null) {
            // Prevent reactivating closed accounts with transaction history
            if (isActive && !account.isActive()) {
                boolean hasTransactions = transactionRepository.existsByAccountId(accountId);
                if (hasTransactions) {
                    throw new IllegalArgumentException(
                        "Cannot reactivate account with transaction history. " +
                            "Create a new account instead."
                    );
                }
            }
            account.setActive(isActive);
        }

        // Update type if provided
        if (type != null) {
            account.setType(type);
        }

        return accountRepository.save(account);
    }

    @Override
    public boolean closeAccount(Long accountId) {
        Account account = getAccount(accountId);

        // Already inactive
        if (!account.isActive()) {
            return false;
        }

        // Check for transaction history
        boolean hasTransactions = transactionRepository.existsByAccountId(accountId);

        if (hasTransactions) {
            // Soft delete: mark inactive
            account.setActive(false);
            accountRepository.save(account);
        } else {
            // Hard delete: no history to preserve
            accountRepository.delete(account);
        }

        return true;
    }

    // ========================================================================
    // Account Queries
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        Account account = getAccount(accountId);
        return account.getCurrentBalance();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> listAccounts(AccountFilter filter) {
        if (filter == null) {
            // No filter: return all accounts ordered by name
            return accountRepository.findAllByOrderByAccountNameAsc();
        }

        // Build query based on filter criteria
        // This is a simplified implementation - production would use Specifications
        if (filter.isActive() != null && filter.type() != null) {
            return accountRepository.findByActiveAndTypeOrderByAccountNameAsc(
                filter.isActive(),
                filter.type()
            );
        } else if (filter.isActive() != null) {
            return accountRepository.findByActiveOrderByAccountNameAsc(filter.isActive());
        } else if (filter.type() != null) {
            return accountRepository.findByTypeOrderByAccountNameAsc(filter.type());
        }

        return accountRepository.findAllByOrderByAccountNameAsc();
    }

    // ========================================================================
    // Account Reporting & Analytics
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBalance() {
        return accountRepository.sumBalancesByActive(true)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBalanceByType(AccountType type) {
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }

        return accountRepository.sumBalancesByActiveAndType(true, type)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumBalancesBelowThreshold(BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }

        return accountRepository.sumBalancesBelowThreshold(threshold)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateNetChange(Long accountId) {
        Account account = getAccount(accountId);
        // Calculate: currentBalance - startingBalance
        return account.getCurrentBalance().subtract(account.getStartingBalance());
    }

    // ========================================================================
    // Validation Helpers
    // ========================================================================

    private void validateAccountName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be blank");
        }
    }

    private void validateAccountType(AccountType type) {
        if (type == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }
    }

    private void validateStartingBalance(BigDecimal balance, AccountType type) {
        if (balance == null) {
            throw new IllegalArgumentException("Starting balance cannot be null");
        }

        // Most account types require non-negative starting balance
        // Credit cards can have negative balance (representing debt)
        if (type != AccountType.CREDIT_CARD && balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                "Starting balance must be non-negative for " + type + " accounts"
            );
        }
    }
}