package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.repository.BudgetRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.BudgetService;
import com.programmingwithtyler.financeforge.service.exception.BudgetNotFoundException;
import com.programmingwithtyler.financeforge.service.exception.BudgetOverlapException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of BudgetService interface.
 *
 * <p>Manages budget lifecycle, period management, and spending calculations
 * derived from transaction data.</p>
 *
 * <p>Budget spending is calculated dynamically from transactions to maintain
 * a single source of truth and prevent data inconsistency.</p>
 */
@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(
        BudgetRepository budgetRepository,
        TransactionRepository transactionRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    // ========================================================================
    // Budget Lifecycle
    // ========================================================================

    @Override
    public Budget createBudget(
        BudgetCategory category,
        BigDecimal allocatedAmount,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        // Validate inputs
        validateCategory(category);
        validateAllocatedAmount(allocatedAmount);
        validatePeriod(periodStart, periodEnd);

        // Check for overlapping budgets
        if (hasOverlappingBudget(category, periodStart, periodEnd, null)) {
            throw new BudgetOverlapException(
                category,
                periodStart,
                periodEnd,
                "A budget already exists for this category and overlapping period"
            );
        }

        // Create and persist budget
        Budget budget = new Budget();
        budget.setCategory(category);
        budget.setMonthlyAllocationAmount(allocatedAmount);
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(periodEnd);
        budget.setActive(true);

        return budgetRepository.save(budget);
    }

    @Override
    public Budget updateBudget(
        Long budgetId,
        BudgetCategory category,
        BigDecimal allocatedAmount,
        LocalDate periodStart,
        LocalDate periodEnd,
        Boolean isActive
    ) {
        Budget budget = getBudget(budgetId);

        // Track if period or category changed (affects overlap validation)
        boolean periodOrCategoryChanged = false;

        // Update category if provided
        if (category != null && category != budget.getCategory()) {
            validateCategory(category);
            budget.setCategory(category);
            periodOrCategoryChanged = true;
        }

        // Update allocated amount if provided
        if (allocatedAmount != null) {
            validateAllocatedAmount(allocatedAmount);
            budget.setMonthlyAllocationAmount(allocatedAmount);
        }

        // Update period if provided
        if (periodStart != null || periodEnd != null) {
            LocalDate newStart = periodStart != null ? periodStart : budget.getPeriodStart();
            LocalDate newEnd = periodEnd != null ? periodEnd : budget.getPeriodEnd();
            validatePeriod(newStart, newEnd);
            budget.setPeriodStart(newStart);
            budget.setPeriodEnd(newEnd);
            periodOrCategoryChanged = true;
        }

        // Check for overlapping budgets if period or category changed
        if (periodOrCategoryChanged) {
            if (hasOverlappingBudget(
                budget.getCategory(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                budgetId
            )) {
                throw new BudgetOverlapException(
                    budget.getCategory(),
                    budget.getPeriodStart(),
                    budget.getPeriodEnd(),
                    "Updated budget would overlap with an existing budget"
                );
            }
        }

        // Update active status if provided
        if (isActive != null) {
            budget.setActive(isActive);
        }

        return budgetRepository.save(budget);
    }

    @Override
    public boolean closeBudget(Long budgetId) {
        Budget budget = getBudget(budgetId);

        // Already inactive
        if (!budget.isActive()) {
            return false;
        }

        budget.setActive(false);
        budgetRepository.save(budget);
        return true;
    }

    // ========================================================================
    // Budget Queries
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
            .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> listBudgets(
        BudgetCategory category,
        Boolean isActive,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        // Build query based on provided filters
        // This is a simplified implementation - production would use Specifications

        if (category != null && isActive != null) {
            return budgetRepository.findByCategoryAndActiveOrderByPeriodStartDesc(
                category,
                isActive
            );
        } else if (category != null) {
            return budgetRepository.findByCategoryOrderByPeriodStartDesc(category);
        } else if (isActive != null) {
            return budgetRepository.findByActiveOrderByPeriodStartDesc(isActive);
        }

        return budgetRepository.findAllByOrderByPeriodStartDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Budget findActiveBudgetForCategoryAndDate(BudgetCategory category, LocalDate date) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        return budgetRepository.findByCategoryAndDateWithinPeriod(category, date)
            .orElse(null);
    }

    // ========================================================================
    // Budget Spending Calculations (Derived from Transactions)
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSpent(Long budgetId) {
        Budget budget = getBudget(budgetId);

        return transactionRepository.sumByCategoryAndPeriod(
            budget.getCategory(),
            budget.getPeriodStart(),
            budget.getPeriodEnd()
        ).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRemaining(Long budgetId) {
        Budget budget = getBudget(budgetId);
        BigDecimal spent = calculateSpent(budgetId);

        return budget.getMonthlyAllocationAmount().subtract(spent);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateUtilization(Long budgetId) {
        Budget budget = getBudget(budgetId);

        if (budget.getMonthlyAllocationAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException(
                "Cannot calculate utilization: allocated amount is zero"
            );
        }

        BigDecimal spent = calculateSpent(budgetId);

        return spent
            .divide(budget.getMonthlyAllocationAmount(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // Budget Reporting & Analytics
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Budget> findBudgetsExceedingThreshold(BigDecimal utilizationThresholdPercent) {
        if (utilizationThresholdPercent == null ||
            utilizationThresholdPercent.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }

        List<Budget> allBudgets = budgetRepository.findByActiveOrderByPeriodStartDesc(true);
        List<Budget> exceedingBudgets = new ArrayList<>();

        for (Budget budget : allBudgets) {
            try {
                BigDecimal utilization = calculateUtilization(budget.getId());
                if (utilization.compareTo(utilizationThresholdPercent) >= 0) {
                    exceedingBudgets.add(budget);
                }
            } catch (ArithmeticException e) {
                // Skip budgets with zero allocation
                continue;
            }
        }

        // Sort by utilization descending (highest first)
        exceedingBudgets.sort((b1, b2) -> {
            try {
                BigDecimal u1 = calculateUtilization(b1.getId());
                BigDecimal u2 = calculateUtilization(b2.getId());
                return u2.compareTo(u1); // Descending order
            } catch (ArithmeticException e) {
                return 0;
            }
        });

        return exceedingBudgets;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> findOverBudgets() {
        List<Budget> allBudgets = budgetRepository.findByActiveOrderByPeriodStartDesc(true);
        List<Budget> overBudgets = new ArrayList<>();

        for (Budget budget : allBudgets) {
            BigDecimal spent = calculateSpent(budget.getId());
            if (spent.compareTo(budget.getMonthlyAllocationAmount()) > 0) {
                overBudgets.add(budget);
            }
        }

        // Sort by overage amount descending
        overBudgets.sort((b1, b2) -> {
            BigDecimal overage1 = calculateSpent(b1.getId())
                .subtract(b1.getMonthlyAllocationAmount());
            BigDecimal overage2 = calculateSpent(b2.getId())
                .subtract(b2.getMonthlyAllocationAmount());
            return overage2.compareTo(overage1); // Descending order
        });

        return overBudgets;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAllocatedForPeriod(
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        validatePeriod(periodStart, periodEnd);

        List<Budget> budgets = budgetRepository
            .findByActiveAndPeriodOverlap(true, periodStart, periodEnd);

        return budgets.stream()
            .map(Budget::getMonthlyAllocationAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSpentForPeriod(
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        validatePeriod(periodStart, periodEnd);

        List<Budget> budgets = budgetRepository
            .findByActiveAndPeriodOverlap(true, periodStart, periodEnd);

        return budgets.stream()
            .map(budget -> calculateSpent(budget.getId()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========================================================================
    // Period Management
    // ========================================================================

    @Override
    public List<Budget> rolloverBudgets(
        LocalDate sourcePeriodStart,
        LocalDate sourcePeriodEnd,
        LocalDate newPeriodStart,
        LocalDate newPeriodEnd
    ) {
        validatePeriod(sourcePeriodStart, sourcePeriodEnd);
        validatePeriod(newPeriodStart, newPeriodEnd);

        // Find all active budgets in source period
        List<Budget> sourceBudgets = budgetRepository
            .findByActiveAndPeriodOverlap(true, sourcePeriodStart, sourcePeriodEnd);

        List<Budget> newBudgets = new ArrayList<>();

        for (Budget sourceBudget : sourceBudgets) {
            // Check if budget already exists for this category in new period
            if (hasOverlappingBudget(
                sourceBudget.getCategory(),
                newPeriodStart,
                newPeriodEnd,
                null
            )) {
                throw new BudgetOverlapException(
                    sourceBudget.getCategory(),
                    newPeriodStart,
                    newPeriodEnd,
                    "Cannot rollover: budget already exists for " +
                        sourceBudget.getCategory() + " in new period"
                );
            }

            // Create new budget with same allocation
            Budget newBudget = new Budget();
            newBudget.setCategory(sourceBudget.getCategory());
            newBudget.setMonthlyAllocationAmount(sourceBudget.getMonthlyAllocationAmount());
            newBudget.setPeriodStart(newPeriodStart);
            newBudget.setPeriodEnd(newPeriodEnd);
            newBudget.setActive(true);

            newBudgets.add(budgetRepository.save(newBudget));
        }

        return newBudgets;
    }

    // ========================================================================
    // Validation Helpers
    // ========================================================================

    private void validateCategory(BudgetCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Budget category cannot be null");
        }
    }

    private void validateAllocatedAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocated amount must be positive");
        }
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new IllegalArgumentException("Period start date cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("Period end date cannot be null");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                "Period end date must be on or after start date"
            );
        }
    }

    /**
     * Check if a budget with overlapping period exists for the given category.
     *
     * @param category the budget category
     * @param start period start date
     * @param end period end date
     * @param excludeBudgetId budget ID to exclude from check (for updates)
     * @return true if overlapping budget exists, false otherwise
     */
    private boolean hasOverlappingBudget(
        BudgetCategory category,
        LocalDate start,
        LocalDate end,
        Long excludeBudgetId
    ) {
        List<Budget> overlapping = budgetRepository
            .findByCategoryAndPeriodOverlap(category, start, end);

        if (excludeBudgetId == null) {
            return !overlapping.isEmpty();
        }

        // Exclude the budget being updated
        return overlapping.stream()
            .anyMatch(b -> !b.getId().equals(excludeBudgetId));
    }
}