package com.programmingwithtyler.financeforge.controller;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.dto.*;
import com.programmingwithtyler.financeforge.service.BudgetFilter;
import com.programmingwithtyler.financeforge.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * REST controller for budget management endpoints.
 *
 * <p>Provides HTTP endpoints for budget CRUD operations, utilization reporting,
 * and period-based queries. All business logic is delegated to {@link BudgetService}.</p>
 */
@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * List budgets with optional filtering.
     *
     * <p>Supports filtering by category, active status, and period dates.
     * Returns budgets ordered by period start date descending.</p>
     *
     * @param category optional budget category filter
     * @param active optional active status filter
     * @param periodStart optional period start date filter
     * @param periodEnd optional period end date filter
     * @return list of matching budgets with calculations
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
        @RequestParam(required = false) BudgetCategory category,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) LocalDate periodStart,
        @RequestParam(required = false) LocalDate periodEnd
    ) {
        BudgetFilter filter = new BudgetFilter(category, active, periodStart, periodEnd);
        List<Budget> budgets = budgetService.listBudgets(filter);

        List<BudgetResponse> responses = budgets.stream()
            .map(budget -> budgetService.getBudgetResponse(budget.getId()))
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Create a new budget.
     *
     * <p>Validates that no overlapping budget exists for the same category and period.</p>
     *
     * @param request the budget creation request
     * @return created budget with 201 status and Location header
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetOverlapException
     *         if a budget already exists for the category and period
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
        @Valid @RequestBody CreateBudgetRequest request
    ) {
        Budget budget = budgetService.createBudget(
            request.category(),
            request.monthlyAllocationAmount(),
            request.periodStart(),
            request.periodEnd()
        );

        URI location = URI.create("/api/budgets/" + budget.getId());

        return ResponseEntity.created(location)
            .body(budgetService.getBudgetResponse(budget.getId()));
    }

    /**
     * Retrieve a single budget by ID.
     *
     * @param id the budget ID
     * @return the budget with calculated spending metrics
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetNotFoundException
     *         if budget does not exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(budgetService.getBudgetResponse(id));
    }

    /**
     * Update an existing budget.
     *
     * <p>Only non-null fields in the request are updated.
     * Period or category changes must not create overlaps with existing budgets.</p>
     *
     * @param id the budget ID to update
     * @param request the update request
     * @return the updated budget with recalculated metrics
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetNotFoundException
     *         if budget does not exist
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetOverlapException
     *         if update would create an overlap
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
        @PathVariable Long id,
        @Valid @RequestBody UpdateBudgetRequest request
    ) {
        budgetService.updateBudget(
            id,
            request.category(),
            request.monthlyAllocationAmount(),
            request.periodStart(),
            request.periodEnd(),
            request.isActive()
        );

        return ResponseEntity.ok(budgetService.getBudgetResponse(id));
    }

    /**
     * Close a budget (soft delete).
     *
     * <p>Marks the budget as inactive. Historical data is preserved.</p>
     *
     * @param id the budget ID to close
     * @return 204 No Content if successful, 404 if budget does not exist or is already inactive
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
        @PathVariable Long id
    ) {
        boolean closed = budgetService.closeBudget(id);
        if (!closed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieve all budgets for a specific month.
     *
     * <p>Returns budgets whose periods overlap with the specified month.
     * Useful for dashboard views and monthly reporting.</p>
     *
     * @param year the year (must be >= 2000)
     * @param month the month (1-12)
     * @return list of budgets active during the specified month
     * @throws IllegalArgumentException if year or month is invalid
     */
    @GetMapping("/period/{year}/{month}")
    public ResponseEntity<List<BudgetResponse>> getBudgetsForPeriod(
        @PathVariable int year,
        @PathVariable int month
    ) {
        // Validate year and month
        if (year < 2000) {
            throw new IllegalArgumentException("Year must be >= 2000");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        // Calculate period start and end dates
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        // Get budgets for this period from service
        List<Budget> budgets = budgetService.findBudgetsForPeriod(periodStart, periodEnd);

        // Convert to response DTOs
        List<BudgetResponse> responses = budgets.stream()
            .map(budget -> budgetService.getBudgetResponse(budget.getId()))
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieve detailed utilization metrics for a budget.
     *
     * <p>Returns spending data with utilization status:
     * <ul>
     *   <li>ON_TRACK: &lt; 80% utilized</li>
     *   <li>WARNING: 80-100% utilized</li>
     *   <li>OVER_BUDGET: &gt; 100% utilized</li>
     * </ul>
     * </p>
     *
     * @param id the budget ID
     * @return detailed utilization response with status indicator
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetNotFoundException
     *         if budget does not exist
     */
    @GetMapping("/{id}/utilization")
    public ResponseEntity<BudgetUtilizationResponse> getBudgetUtilization(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(budgetService.getBudgetUtilization(id));
    }

    /**
     * Retrieve all over-budget budgets.
     *
     * <p>Returns budgets where spending exceeds allocation, sorted by
     * overage amount descending (worst offenders first).</p>
     *
     * @return list of over-budget budgets
     */
    @GetMapping("/over-budget")
    public ResponseEntity<List<BudgetResponse>> getOverBudgets() {
        List<Budget> budgets = budgetService.findOverBudgets();
        List<BudgetResponse> responses = budgets.stream()
            .map(b -> budgetService.getBudgetResponse(b.getId()))
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Rollover budgets from one period to another.
     *
     * <p>Creates new budgets in the target period with the same allocation
     * amounts as the source period. Only active budgets are rolled over.</p>
     *
     * @param request the rollover request with source and target periods
     * @return list of newly created budgets with 201 status
     * @throws com.programmingwithtyler.financeforge.service.exception.BudgetOverlapException
     *         if any budget already exists in the target period
     * @throws IllegalArgumentException if target period is not after source period
     */
    @PostMapping("/rollover")
    public ResponseEntity<List<BudgetResponse>> rolloverBudgets(
        @Valid @RequestBody RolloverRequest request
    ) {
        // Calculate source period dates
        LocalDate sourcePeriodStart = LocalDate.of(request.sourceYear(), request.sourceMonth(), 1);
        LocalDate sourcePeriodEnd = YearMonth.of(request.sourceYear(), request.sourceMonth()).atEndOfMonth();

        // Calculate target period dates
        LocalDate targetPeriodStart = LocalDate.of(request.targetYear(), request.targetMonth(), 1);
        LocalDate targetPeriodEnd = YearMonth.of(request.targetYear(), request.targetMonth()).atEndOfMonth();

        // Validate target is after source
        if (!targetPeriodStart.isAfter(sourcePeriodEnd)) {
            throw new IllegalArgumentException("Target period must be after source period");
        }

        // Rollover budgets
        List<Budget> newBudgets = budgetService.rolloverBudgets(
            sourcePeriodStart,
            sourcePeriodEnd,
            targetPeriodStart,
            targetPeriodEnd
        );

        // Convert to responses
        List<BudgetResponse> responses = newBudgets.stream()
            .map(budget -> budgetService.getBudgetResponse(budget.getId()))
            .toList();

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}