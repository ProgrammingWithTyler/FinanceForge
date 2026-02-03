package com.programmingwithtyler.financeforge.api.controller;

import com.programmingwithtyler.financeforge.api.dto.request.RecordExpenseRequest;
import com.programmingwithtyler.financeforge.api.dto.request.RecordIncomeRequest;
import com.programmingwithtyler.financeforge.api.dto.request.RecordTransferRequest;
import com.programmingwithtyler.financeforge.api.dto.response.TransactionResponse;
import com.programmingwithtyler.financeforge.service.exception.InsufficientFundsException;
import com.programmingwithtyler.financeforge.service.exception.TransactionNotFoundException;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;
import com.programmingwithtyler.financeforge.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for transaction management.
 *
 * Provides endpoints for:
 * - Recording income, expenses, and transfers (separate endpoints per type)
 * - Listing transactions with multi-criteria filtering
 * - Retrieving individual transaction details
 * - Soft-deleting transactions (preserves audit trail)
 *
 * Architecture notes:
 * - Mirrors service layer command pattern (separate POST endpoints per transaction type)
 * - No business logic in controller (validation, conversion, delegation only)
 * - Account summaries in responses (not full accounts) to minimize payload size
 *
 * @see TransactionService for business logic implementation
 */
@RestController
@RequestMapping("/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Record a new income transaction.
     *
     * POST /api/transactions/income
     *
     * @param request Income transaction details (validated)
     * @return 201 CREATED with TransactionResponse and Location header
     */
    @PostMapping("/income")
    public ResponseEntity<TransactionResponse> recordIncome(@Valid @RequestBody RecordIncomeRequest request) {
        Transaction transaction = transactionService.recordIncome(request.toCommand());
        TransactionResponse response = TransactionResponse.from(transaction);

        return ResponseEntity
            .created(URI.create("/transactions/" + transaction.getId()))
            .body(response);
    }

    /**
     * Record a new expense transaction.
     *
     * POST /api/transactions/expense
     *
     * @param request Expense transaction details (validated)
     * @return 201 CREATED with TransactionResponse and Location header
     * @throws InsufficientFundsException
     *         if source account balance is insufficient (409 CONFLICT)
     */
    @PostMapping("/expense")
    public ResponseEntity<TransactionResponse> recordExpense(@Valid @RequestBody RecordExpenseRequest request) {
        Transaction transaction = transactionService.recordExpense(request.toCommand());
        TransactionResponse response = TransactionResponse.from(transaction);

        return ResponseEntity
            .created(URI.create("/transactions/" + transaction.getId()))
            .body(response);
    }

    /**
     * Record a new transfer transaction between accounts.
     *
     * POST /api/transactions/transfer
     *
     * @param request Transfer transaction details (validated, includes same-account check)
     * @return 201 CREATED with TransactionResponse and Location header
     * @throws InsufficientFundsException
     *         if source account balance is insufficient (409 CONFLICT)
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> recordTransfer(@Valid @RequestBody RecordTransferRequest request) {
        Transaction transaction = transactionService.recordTransfer(request.toCommand());
        TransactionResponse response = TransactionResponse.from(transaction);

        return ResponseEntity
            .created(URI.create("/transactions/" + transaction.getId()))
            .body(response);
    }

    /**
     * List transactions with filtering.
     *
     * GET /api/transactions?dateFrom=2026-01-01&dateTo=2026-01-31&accountId=1&category=GROCERIES&type=EXPENSE
     *
     * Query parameters (all optional):
     * - dateFrom: Start date for transaction date range (ISO-8601: yyyy-MM-dd)
     * - dateTo: End date for transaction date range (ISO-8601: yyyy-MM-dd)
     * - accountId: Filter by account (matches either source or destination)
     * - category: Filter by budget category (EXPENSE transactions only)
     * - type: Filter by transaction type (INCOME, EXPENSE, TRANSFER, REFUND)
     *
     * Note: This implementation returns all matching transactions without pagination.
     * For production use with large datasets, consider adding pagination support.
     *
     * @return 200 OK with List<TransactionResponse>
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> listTransactions(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,

        @RequestParam(required = false)
        Long accountId,

        @RequestParam(required = false)
        BudgetCategory category,

        @RequestParam(required = false)
        TransactionType type
    ) {
        // Validate date range if both provided
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be before or equal to dateTo");
        }

        // Delegate to service layer with filter criteria
        List<Transaction> transactions = transactionService.listTransactions(
            dateFrom, dateTo, category, accountId, type
        );

        // Convert to response DTOs
        List<TransactionResponse> response = transactions.stream()
            .map(TransactionResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get details of a specific transaction by ID.
     *
     * GET /api/transactions/{id}
     *
     * @param id Transaction identifier
     * @return 200 OK with TransactionResponse
     * @throws TransactionNotFoundException
     *         if transaction not found (404 NOT FOUND)
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransaction(id);
        TransactionResponse response = TransactionResponse.from(transaction);

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-delete a transaction.
     *
     * DELETE /api/transactions/{id}
     *
     * IMPORTANT: Soft delete marks the transaction as deleted but does NOT reverse
     * the balance changes. To reverse a transaction, use the reversal endpoint or
     * create an offsetting transaction.
     *
     * Idempotent: Returns 204 even if transaction is already deleted.
     *
     * @param id Transaction identifier
     * @return 204 NO CONTENT
     * @throws TransactionNotFoundException
     *         if transaction not found (404 NOT FOUND)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}