package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.*;
import com.programmingwithtyler.financeforge.repository.AccountRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.BudgetService;
import com.programmingwithtyler.financeforge.service.TransactionService;
import com.programmingwithtyler.financeforge.service.command.*;
import com.programmingwithtyler.financeforge.service.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetService budgetService;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        BudgetService budgetService
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.budgetService = budgetService;
    }

    // ========================================================================
    // Transaction Creation
    // ========================================================================

    @Override
    public Transaction recordIncome(RecordIncomeCommand command) {
        // 1. Validate and retrieve account
        Account destination = accountRepository.findById(command.destinationAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.destinationAccountId()));

        if (!destination.isActive()) {
            throw new InactiveAccountException(destination.getId());
        }

        // 2. Create transaction entity via factory method
        Transaction tx = Transaction.income(
            destination,
            command.amount(),
            command.description()
        );

        // 3. Update transaction date if different from today
        if (command.transactionDate() != null && !command.transactionDate().equals(LocalDate.now())) {
            tx.updateDetails(command.amount(), command.transactionDate(), command.description());
        }

        // 4. Update account balance using domain method
        destination.credit(command.amount());

        // 5. Persist both entities atomically
        transactionRepository.save(tx);
        accountRepository.save(destination);

        return tx;
    }

    @Override
    public Transaction recordExpense(RecordExpenseCommand command) {
        // 1. Validate and retrieve account
        Account source = accountRepository.findById(command.sourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

        if (!source.isActive()) {
            throw new InactiveAccountException(source.getId());
        }

        // 2. Check sufficient funds (unless credit card account)
        if (source.getType() != AccountType.CREDIT_CARD) {
            if (!source.hasSufficientBalance(command.amount())) {
                throw new InsufficientFundsException(
                    source.getId(),
                    source.getCurrentBalance(),
                    command.amount()
                );
            }
        }

        // 3. Create transaction entity via factory method
        Transaction tx = Transaction.expense(
            source,
            command.amount(),
            command.category(),
            command.description()
        );

        // 4. Update transaction date if different from today
        if (command.transactionDate() != null && !command.transactionDate().equals(LocalDate.now())) {
            tx.updateDetails(command.amount(), command.transactionDate(), command.description());
        }

        // 5. Update account balance using domain method
        source.debit(command.amount());

        // 6. Persist transaction and account atomically
        transactionRepository.save(tx);
        accountRepository.save(source);

        // Note: Budget spending is calculated dynamically from transactions,
        // so no explicit budget update is needed here

        return tx;
    }

    @Override
    public Transaction recordTransfer(RecordTransferCommand command) {
        // 1. Validate and retrieve both accounts
        Account source = accountRepository.findById(command.sourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

        Account destination = accountRepository.findById(command.destinationAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.destinationAccountId()));

        if (!source.isActive()) {
            throw new InactiveAccountException(source.getId());
        }
        if (!destination.isActive()) {
            throw new InactiveAccountException(destination.getId());
        }

        // 2. Validate accounts are different (enforced in domain model)
        if (source.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        // 3. Check sufficient funds (unless credit card)
        if (source.getType() != AccountType.CREDIT_CARD) {
            if (!source.hasSufficientBalance(command.amount())) {
                throw new InsufficientFundsException(
                    source.getId(),
                    source.getCurrentBalance(),
                    command.amount()
                );
            }
        }

        // 4. Create transaction entity via factory method
        Transaction tx = Transaction.transfer(
            source,
            destination,
            command.amount(),
            command.description()
        );

        // 5. Update transaction date if different from today
        if (command.transactionDate() != null && !command.transactionDate().equals(LocalDate.now())) {
            tx.updateDetails(command.amount(), command.transactionDate(), command.description());
        }

        // 6. Update both account balances atomically using domain methods
        source.debit(command.amount());
        destination.credit(command.amount());

        // 7. Persist all entities atomically
        transactionRepository.save(tx);
        accountRepository.save(source);
        accountRepository.save(destination);

        return tx;
    }

    @Override
    public Transaction recordRefund(RecordRefundCommand command) {
        // 1. Validate and retrieve account
        Account source = accountRepository.findById(command.sourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceAccountId()));

        if (!source.isActive()) {
            throw new InactiveAccountException(source.getId());
        }

        // 2. Create transaction entity via factory method
        Transaction tx = Transaction.refund(
            source,
            command.amount(),
            command.category(),  // May be null
            command.description()
        );

        // 3. Update transaction date if different from today
        if (command.transactionDate() != null && !command.transactionDate().equals(LocalDate.now())) {
            tx.updateDetails(command.amount(), command.transactionDate(), command.description());
        }

        // 4. Update account balance using domain method (refund credits the account)
        source.credit(command.amount());

        // 5. Persist both entities atomically
        transactionRepository.save(tx);
        accountRepository.save(source);

        // Note: If category is provided, budget spending is automatically
        // adjusted because it's calculated from transactions dynamically

        return tx;
    }

    // ========================================================================
    // Transaction Metadata Updates
    // ========================================================================

    @Override
    public Transaction updateTransactionMetadata(
        Long transactionId,
        LocalDate newDate,
        BudgetCategory newCategory,
        String newDescription
    ) {
        // 1. Retrieve transaction
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // 2. Validate future dates
        if (newDate != null && newDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }

        // 3. Validate category changes
        if (newCategory != null) {
            if (tx.getType() != TransactionType.EXPENSE && tx.getType() != TransactionType.REFUND) {
                throw new IllegalArgumentException(
                    "Budget category can only be updated for EXPENSE and REFUND transactions"
                );
            }
        }

        // 4. Update using domain method
        LocalDate dateToUse = newDate != null ? newDate : tx.getTransactionDate();
        String descToUse = newDescription != null ? newDescription : tx.getDescription();

        tx.updateDetails(tx.getAmount(), dateToUse, descToUse);

        // 5. Update category separately if provided (no domain method for this)
        if (newCategory != null) {
            // Uses protected setter - ideally add updateCategory() domain method
            tx.setBudgetCategory(newCategory);
        }

        // 6. Persist updated transaction
        return transactionRepository.save(tx);
    }

    // ========================================================================
    // Transaction Deletion
    // ========================================================================

    @Override
    public void deleteTransaction(Long transactionId) {
        // 1. Retrieve transaction
        Transaction tx = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // 2. Soft delete using domain method
        tx.delete();

        // 3. Persist
        transactionRepository.save(tx);

        // Note: Account balances are NOT reversed on soft delete.
        // To correct balances, use reverseTransaction() instead.
    }

    // ========================================================================
    // Transaction Queries
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> listTransactions(
        LocalDate startDate,
        LocalDate endDate,
        BudgetCategory category,
        Long accountId,
        TransactionType type
    ) {
        // Delegate to repository with filtering logic
        // Repository method will handle:
        // - Excluding soft-deleted transactions (isDeleted = false)
        // - Applying all optional filters
        // - Ordering by date descending

        return transactionRepository.findWithFilters(
            startDate,
            endDate,
            category,
            accountId,
            type
        );
    }

    // ========================================================================
    // Transaction Reversals
    // ========================================================================

    @Override
    public Transaction reverseTransaction(Long transactionId, LocalDate reversalDate, String reason) {
        // 1. Retrieve original transaction
        Transaction original = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // 2. Check if already reversed
        boolean alreadyReversed = transactionRepository.existsReversalFor(transactionId);
        if (alreadyReversed) {
            throw new IllegalArgumentException(
                "Transaction " + transactionId + " has already been reversed"
            );
        }

        // 3. Create reversal based on transaction type
        Transaction reversal;
        String reversalDescription = "REVERSAL: " + reason + " (Original: " + transactionId + ")";

        switch (original.getType()) {
            case INCOME -> {
                // Reversing income = create expense (debit the account that was credited)
                reversal = Transaction.expense(
                    original.getDestinationAccount(),
                    original.getAmount(),
                    BudgetCategory.MISCELLANEOUS,  // No budget impact for reversals
                    reversalDescription
                );
                reversal.updateDetails(original.getAmount(), reversalDate, reversalDescription);
                original.getDestinationAccount().debit(original.getAmount());
            }
            case EXPENSE -> {
                // Reversing expense = create refund (credit the account that was debited)
                reversal = Transaction.refund(
                    original.getSourceAccount(),
                    original.getAmount(),
                    original.getBudgetCategory(),  // Reverse budget impact
                    reversalDescription
                );
                reversal.updateDetails(original.getAmount(), reversalDate, reversalDescription);
                original.getSourceAccount().credit(original.getAmount());
            }
            case TRANSFER -> {
                // Reversing transfer = create reverse transfer
                reversal = Transaction.transfer(
                    original.getDestinationAccount(),  // Swap source/destination
                    original.getSourceAccount(),
                    original.getAmount(),
                    reversalDescription
                );
                reversal.updateDetails(original.getAmount(), reversalDate, reversalDescription);
                original.getDestinationAccount().debit(original.getAmount());
                original.getSourceAccount().credit(original.getAmount());
            }
            case REFUND -> {
                // Reversing refund = create expense
                reversal = Transaction.expense(
                    original.getSourceAccount(),
                    original.getAmount(),
                    original.getBudgetCategory() != null
                        ? original.getBudgetCategory()
                        : BudgetCategory.MISCELLANEOUS,
                    reversalDescription
                );
                reversal.updateDetails(original.getAmount(), reversalDate, reversalDescription);
                original.getSourceAccount().debit(original.getAmount());
            }
            default -> throw new IllegalStateException("Unknown transaction type: " + original.getType());
        }

        // 4. Persist reversal transaction
        transactionRepository.save(reversal);

        // 5. Update account balances (already done above in switch)
        if (original.getSourceAccount() != null) {
            accountRepository.save(original.getSourceAccount());
        }
        if (original.getDestinationAccount() != null) {
            accountRepository.save(original.getDestinationAccount());
        }

        return reversal;
    }
}