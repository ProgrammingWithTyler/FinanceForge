package com.programmingwithtyler.financeforge.controller;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.dto.*;
import com.programmingwithtyler.financeforge.service.AccountFilter;
import com.programmingwithtyler.financeforge.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * REST controller for account management operations.
 *
 * <p>Provides CRUD operations for financial accounts with proper HTTP semantics.
 * All endpoints return JSON responses and handle errors via global exception handlers.</p>
 *
 * <p>Base path: /api/accounts</p>
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * List all accounts with optional filters.
     *
     * @param active filter by active status (optional)
     * @param type filter by account type (optional)
     * @return list of accounts matching filters
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) AccountType type
    ) {
        AccountFilter filter = new AccountFilter(active, type, null, null, null);
        List<Account> accounts = accountService.listAccounts(filter);

        List<AccountResponse> responses = accounts.stream()
            .map(AccountResponse::from)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Create a new account.
     *
     * @param request account creation details
     * @return created account with 201 status and Location header
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
        @Valid @RequestBody CreateAccountRequest request
    ) {
        Account account = accountService.createAccount(
            request.name(),
            request.type(),
            request.startingBalance(),
            request.description()
        );

        URI location = URI.create("/api/accounts/" + account.getId());

        return ResponseEntity
            .created(location)
            .body(AccountResponse.from(account));
    }

    /**
     * Get account details by ID.
     *
     * @param id account identifier
     * @return account details
     * @throws com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException if account not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
        @PathVariable Long id
    ) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    /**
     * Update account details.
     *
     * <p>Supports partial updates - only provided fields are updated.
     * Null fields are ignored.</p>
     *
     * @param id account identifier
     * @param request update details
     * @return updated account
     * @throws com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException if account not found
     * @throws com.programmingwithtyler.financeforge.service.exception.DuplicateAccountNameException if name already exists
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAccountRequest request
    ) {
        Account account = accountService.updateAccount(
            id,
            request.name(),
            request.description(),
            request.isActive(),
            request.type()
        );
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    /**
     * Close an account.
     *
     * <p>Performs soft delete if account has transaction history,
     * hard delete otherwise.</p>
     *
     * @param id account identifier
     * @return 204 No Content if successful, 404 if already closed or not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
        @PathVariable Long id
    ) {
        boolean closed = accountService.closeAccount(id);
        if (!closed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Get account balance details.
     *
     * @param id account identifier
     * @return current balance, starting balance, and net change
     * @throws com.programmingwithtyler.financeforge.service.exception.AccountNotFoundException if account not found
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getAccountBalance(
        @PathVariable Long id
    ) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(BalanceResponse.from(account));
    }

    /**
     * Get account summary with aggregated statistics.
     *
     * <p>Returns total balances, balances by account type, account count,
     * and accounts below the specified balance threshold.</p>
     *
     * @param lowBalanceThreshold threshold for low balance accounts (default: $100)
     * @return aggregated account statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryResponse> getAccountSummary(
        @RequestParam(required = false, defaultValue = "100.00") BigDecimal lowBalanceThreshold
    ) {
        BigDecimal totalBalance = accountService.calculateTotalBalance();

        Map<AccountType, BigDecimal> totalByType = Map.of(
            AccountType.CHECKING, accountService.calculateTotalBalanceByType(AccountType.CHECKING),
            AccountType.SAVINGS, accountService.calculateTotalBalanceByType(AccountType.SAVINGS),
            AccountType.CREDIT_CARD, accountService.calculateTotalBalanceByType(AccountType.CREDIT_CARD),
            AccountType.INVESTMENT, accountService.calculateTotalBalanceByType(AccountType.INVESTMENT),
            AccountType.CASH, accountService.calculateTotalBalanceByType(AccountType.CASH)
        );

        List<Account> allAccounts = accountService.listAccounts(
            new AccountFilter(true, null, null, null, null)
        );
        int accountCount = allAccounts.size();

        List<AccountResponse> lowBalanceAccounts = allAccounts.stream()
            .filter(account -> account.getCurrentBalance().compareTo(lowBalanceThreshold) < 0)
            .map(AccountResponse::from)
            .toList();

        return ResponseEntity.ok(new AccountSummaryResponse(
            totalBalance,
            totalByType,
            accountCount,
            lowBalanceAccounts
        ));
    }
}