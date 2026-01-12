package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import com.programmingwithtyler.financeforge.domain.BalanceAdjustType;
import com.programmingwithtyler.financeforge.domain.TransactionType;
import com.programmingwithtyler.financeforge.repository.AccountRepository;
import com.programmingwithtyler.financeforge.service.AccountFilter;
import com.programmingwithtyler.financeforge.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account createAccount(String name, AccountType type, BigDecimal startingBalance, String description) {

        Account account;

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }

        if (startingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Starting balance must not be negative");
        }

        if (accountRepository.existsByAccountName(name)) {
            throw new IllegalArgumentException("Account name must be unique");
        }

        account = new Account(name, type, true, description, startingBalance, startingBalance);
        accountRepository.save(account);
        return account;
    }

    @Override
    public Account updateAccount(Long accountId, String name, String description, Boolean status, AccountType type) {

        Account updating = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account does not " +
            "exist"));

        if (type == null) {
            throw new IllegalArgumentException("Account type must not be null");
        }

        updating.setType(type);

        if (name != null && !name.isBlank()) {
            boolean nameTaken = accountRepository.existsByAccountName(name) && !name.equals(updating.getAccountName());

            if (nameTaken) {
                throw new IllegalArgumentException("Account name is already taken");
            }
                updating.setAccountName(name);
        }

        if (description != null && !description.isBlank()) {
            updating.setDescription(description);
        }

        if (status != null) {
            updating.setActive(status);
        }

        accountRepository.save(updating);
        return updating;
    }

    @Override
    public BigDecimal getBalance(Long accountId) {

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account " +
            "does not exist"));

        return account.getCurrentBalance();
    }

    @Override
    public Account adjustBalance(Long accountId, BigDecimal amount, BalanceAdjustType adjustType) {

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account " +
            "does not exist"));

        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (adjustType == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        if (adjustType == BalanceAdjustType.DEPOSIT) {
            BigDecimal newBalance = account.getCurrentBalance().add(amount);
            account.setCurrentBalance(newBalance);
        } else if (adjustType == BalanceAdjustType.WITHDRAWAL) {
            if (amount.compareTo(account.getCurrentBalance()) > 0) {
                throw new IllegalArgumentException("Insufficient balance for withdrawal");
            }
            BigDecimal newBalance = account.getCurrentBalance().subtract(amount);
            account.setCurrentBalance(newBalance);
        }

        accountRepository.save(account);
        return account;
    }

    @Override
    public boolean closeAccount(Long accountId) {

        Account account = accountRepository.findById(accountId).orElseThrow(() -> new IllegalArgumentException("Account " +
            "does not exist"));

        if (!account.isActive()) {
            return false;
        }

        // TODO: validate no pending/uncleared transactions once TransactionService exists

        account.setActive(false);
        accountRepository.save(account);
        return true;
    }

    @Override
    public List<Account> listAccounts(AccountFilter filter) {
        List<Account> allAccounts = accountRepository.findAll();

        return allAccounts.stream()
            .filter(a -> filter.getActive() == null || a.isActive() == filter.getActive())
            .filter(a -> filter.getType() == null || a.getType() == filter.getType())
            .filter(a -> filter.getMinBalance() == null || a.getCurrentBalance().compareTo(filter.getMinBalance()) >= 0)
            .filter(a -> filter.getMaxBalance() == null || a.getCurrentBalance().compareTo(filter.getMaxBalance()) <= 0)
            .filter(a -> filter.getNameContains() == null || a.getAccountName().contains(filter.getNameContains()))
            .toList();
    }


    @Override
    public BigDecimal sumBalancesBelowThreshold(BigDecimal threshold) {

        if (threshold == null) {
            throw new IllegalArgumentException("Threshold must not be null");
        }

        if (threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        return accountRepository.findAll().stream()
            .filter(a -> a.isActive() && a.getCurrentBalance().compareTo(threshold) < 0)
            .map(Account::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalBalance() {
        return accountRepository.findAll().stream()
            .filter(Account::isActive)
            .map(Account::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
