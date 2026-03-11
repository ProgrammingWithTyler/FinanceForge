package com.programmingwithtyler.financeforge.api.controller;

import com.programmingwithtyler.financeforge.api.dto.request.CreateRecurringExpenseRequest;
import com.programmingwithtyler.financeforge.api.dto.request.UpdateRecurringExpenseRequest;
import com.programmingwithtyler.financeforge.api.dto.response.GeneratedTransactionResponse;
import com.programmingwithtyler.financeforge.api.dto.response.RecurringExpenseResponse;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import com.programmingwithtyler.financeforge.service.AccountService;
import com.programmingwithtyler.financeforge.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for recurring expense template management.
 *
 * <p>Provides endpoints for CRUD operations on recurring expense templates,
 * manual transaction generation, and activation/deactivation toggles. Enables
 * users to manage predictable recurring expenses without repetitive data entry.</p>
 *
 * <h2>Key Endpoints</h2>
 * <ul>
 *   <li>POST /api/recurring-expenses - Create new template</li>
 *   <li>GET /api/recurring-expenses - List templates with optional filters</li>
 *   <li>GET /api/recurring-expenses/{id} - Get template details</li>
 *   <li>PUT /api/recurring-expenses/{id} - Update template (partial updates supported)</li>
 *   <li>DELETE /api/recurring-expenses/{id} - Delete template (preserves generated transactions)</li>
 *   <li>PATCH /api/recurring-expenses/{id}/toggle - Toggle active status</li>
 * </ul>
 *
 * <h2>Business Rules</h2>
 * <ul>
 *   <li>Source account must exist and be active to create template</li>
 *   <li>Templates can be activated/deactivated without deletion</li>
 *   <li>Deleting a template does NOT delete previously generated transactions</li>
 *   <li>All endpoints use RecurringExpenseResponse DTOs (not raw entities)</li>
 * </ul>
 *
 * @see RecurringExpenseService
 * @see CreateRecurringExpenseRequest
 * @see UpdateRecurringExpenseRequest
 * @see RecurringExpenseResponse
 */
@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
//    private final AccountService accountService;

    public RecurringExpenseController(
        RecurringExpenseService recurringExpenseService,
        AccountService accountService
    ) {
        this.recurringExpenseService = recurringExpenseService;
//        this.accountService = accountService;
    }

    /**
     * Creates a new recurring expense template.
     *
     * <p>The template is created in an active state and will immediately begin
     * generating transactions when the batch processor runs.</p>
     *
     * @param request the template creation request with all required fields
     * @return 201 CREATED with RecurringExpenseResponse and Location header
     */
    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
        @Valid @RequestBody CreateRecurringExpenseRequest request
    ) {
        RecurringExpense recurringExpense = recurringExpenseService.createRecurringExpense(
            request.frequency(),
            request.nextScheduledDate(),
            request.amount(),
            request.category(),
            request.description(),
            request.sourceAccountId()
        );

        URI location = URI.create("/api/recurring-expenses/" + recurringExpense.getId());

        return ResponseEntity.created(location)
            .body(RecurringExpenseResponse.from(recurringExpense));
    }

    /**
     * Lists all recurring expense templates with optional filtering.
     *
     * <p>Query parameters allow filtering by active status, source account, and frequency.
     * All filters are optional. Results are ordered by nextScheduledDate ascending.</p>
     *
     * @param active filter by active status (null = all, true = active only, false = inactive only)
     * @param sourceAccountId filter by source account ID (null = all accounts)
     * @param frequency filter by recurrence frequency (null = all frequencies)
     * @return 200 OK with list of RecurringExpenseResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> listRecurringExpenses(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Long sourceAccountId,
        @RequestParam(required = false) TransactionFrequency frequency
    ) {
        List<RecurringExpense> expenses = recurringExpenseService.listRecurringExpenses(
            active,
            sourceAccountId,
            frequency
        );

        List<RecurringExpenseResponse> responses = expenses.stream()
            .map(RecurringExpenseResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a single recurring expense template by ID.
     *
     * @param id the template ID
     * @return 200 OK with RecurringExpenseResponse
     * @throws RecurringExpenseNotFoundException if template not found (404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpense(@PathVariable Long id) {
        RecurringExpense expense = recurringExpenseService.getRecurringExpense(id);
        return ResponseEntity.ok(RecurringExpenseResponse.from(expense));
    }

    /**
     * Updates an existing recurring expense template.
     *
     * <p>Supports partial updates—only non-null fields in the request will be updated.
     * Updating a template does not affect transactions that have already been generated.</p>
     *
     * @param id the template ID to update
     * @param request the update request with fields to change (all fields optional)
     * @return 200 OK with updated RecurringExpenseResponse
     * @throws RecurringExpenseNotFoundException if template not found (404)
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRecurringExpenseRequest request
    ) {
        RecurringExpense updated = recurringExpenseService.updateRecurringExpense(
            id,
            request.frequency(),
            request.nextScheduledDate(),
            request.amount(),
            request.category(),
            request.description(),
            request.active()
        );

        return ResponseEntity.ok(RecurringExpenseResponse.from(updated));
    }

    /**
     * Deletes a recurring expense template.
     *
     * <p><strong>Important:</strong> This is a hard delete operation that removes the template
     * from the database. However, previously generated transactions are preserved to maintain
     * historical financial records.</p>
     *
     * <p>This operation is idempotent—deleting a non-existent template returns 204.</p>
     *
     * @param id the template ID to delete
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles the active status of a recurring expense template.
     *
     * <p>If the template is currently active, it will be deactivated.
     * If the template is currently inactive, it will be activated.</p>
     *
     * <p>This operation is idempotent—calling it multiple times will toggle
     * the status each time.</p>
     *
     * @param id the template ID to toggle
     * @return 200 OK with updated RecurringExpenseResponse showing new active status
     * @throws RecurringExpenseNotFoundException if template not found (404)
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RecurringExpenseResponse> toggleRecurringExpense(@PathVariable Long id) {
        RecurringExpense expense = recurringExpenseService.getRecurringExpense(id);

        // Toggle active status
        if (expense.isActive()) {
            recurringExpenseService.deactivateRecurringExpense(id);
        } else {
            recurringExpenseService.activateRecurringExpense(id);
        }

        // Retrieve updated template
        RecurringExpense updated = recurringExpenseService.getRecurringExpense(id);
        return ResponseEntity.ok(RecurringExpenseResponse.from(updated));
    }

    /**
     * Manually generates a transaction from a recurring expense template.
     *
     * <p>This endpoint allows users to manually trigger transaction generation for testing
     * templates or handling schedule adjustments. It respects the same idempotency guarantees
     * as the automated batch processor.</p>
     *
     * <h3>Response Status Codes</h3>
     * <ul>
     *   <li><strong>201 CREATED:</strong> New transaction was generated successfully</li>
     *   <li><strong>200 OK:</strong> Transaction already exists for this date (idempotency)</li>
     * </ul>
     *
     * <h3>Idempotency</h3>
     * <p>Calling this endpoint multiple times for the same template on the same scheduled date
     * will not create duplicate transactions. The first call generates the transaction and
     * advances the schedule. Subsequent calls return the existing transaction with a 200 OK status.</p>
     *
     * @param id the template ID to generate from
     * @return 201 CREATED with GeneratedTransactionResponse if new transaction created,
     *         200 OK with GeneratedTransactionResponse if transaction already existed
     * @throws RecurringExpenseNotFoundException if template not found (404)
     * @throws IllegalStateException if template is inactive or source account is inactive (400)
     */
    @PostMapping("/{id}/generate")
    public ResponseEntity<GeneratedTransactionResponse> generateTransaction(@PathVariable Long id) {
        // Get template before generation to check if transaction already exists
        RecurringExpense templateBefore = recurringExpenseService.getRecurringExpense(id);
        LocalDate scheduledDate = templateBefore.getNextScheduledDate();

        // Generate transaction (idempotent - returns existing if already generated)
        Transaction transaction = recurringExpenseService.generateTransactionManually(id);

        // Get updated template after generation
        RecurringExpense templateAfter = recurringExpenseService.getRecurringExpense(id);

        // Determine if this was a new generation or idempotent return
        boolean wasNewlyGenerated = !scheduledDate.equals(templateAfter.getNextScheduledDate());

        GeneratedTransactionResponse response;
        if (wasNewlyGenerated) {
            // New transaction was created and schedule was advanced
            response = GeneratedTransactionResponse.created(transaction, templateAfter);
            return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId()))
                .body(response);
        } else {
            // Transaction already existed (idempotency)
            response = GeneratedTransactionResponse.alreadyExists(transaction, templateAfter);
            return ResponseEntity.ok(response);
        }
    }
}