package com.programmingwithtyler.financeforge.service.impl;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.repository.BudgetRepository;
import com.programmingwithtyler.financeforge.service.BudgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetServiceImpl(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    public Budget createBudget(BudgetCategory category, BigDecimal monthlyAllocationAmount, LocalDate periodStart,
                               LocalDate periodEnd) {
        Budget budget;

        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }

        if (monthlyAllocationAmount == null || monthlyAllocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly allocation must be positive");
        }

        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invalid period dates");
        }

        // Check for duplicate budget for same category + period
        if (budgetRepository.existsByCategoryAndPeriodStartAndPeriodEnd(category, periodStart, periodEnd)) {
            throw new IllegalArgumentException("Budget already exists for this category and month");
        }

        budget = new Budget(category, monthlyAllocationAmount, periodStart, periodEnd);

        budgetRepository.save(budget);
        return budget;
    }

    @Override
    public Budget updateBudget(Long budgetId, BudgetCategory category, BigDecimal monthlyAllocationAmount,
                               LocalDate periodStart, LocalDate periodEnd, Boolean active) {
        Budget updating = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new IllegalArgumentException("Budget does not exist"));

        // Validate period if both dates are provided
        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invalid period dates");
        }

        // Validate allocation if provided
        if (monthlyAllocationAmount != null && monthlyAllocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly allocation must be positive");
        }

        if (category != null && periodStart != null && periodEnd != null) {
            throw new IllegalArgumentException("Budget already exists for this category and period");
        }

        if (category != null) updating.setCategory(category);
        if (monthlyAllocationAmount != null) updating.setMonthlyAllocationAmount(monthlyAllocationAmount);
        if (periodStart != null) updating.setPeriodStart(periodStart);
        if (periodEnd != null) updating.setPeriodEnd(periodEnd);
        if (active != null) updating.setActive(active);

        budgetRepository.save(updating);
        return updating;
    }

    @Override
    @Transactional(readOnly = true)
    public Budget getBudget(Long budgetId) {
        return budgetRepository.findById(budgetId)
            .orElseThrow(() -> new IllegalArgumentException("Budget does not exist"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> listBudgets(BudgetCategory category, Boolean active, LocalDate periodStart, LocalDate periodEnd) {
        return budgetRepository.findAll().stream()
            .filter(b -> category == null || b.getCategory() == category)
            .filter(b -> active == null || b.isActive() == active)
            .filter(b -> periodStart == null || !b.getPeriodStart().isBefore(periodStart)) // periodStart <= budget start
            .filter(b -> periodEnd == null || !b.getPeriodEnd().isAfter(periodEnd))       // periodEnd >= budget end
            .toList();
    }

    @Override
    public void spend(Long budgetId, BigDecimal amount) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new IllegalArgumentException("Budget does not exist"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        var exceedingAllocation =
            budget.getCurrentSpentAmount().add(amount).compareTo(budget.getMonthlyAllocationAmount()) > 0;

        if (exceedingAllocation) {
            throw new IllegalArgumentException("Exceeded monthly budget allocation");
        }

        budget.setCurrentSpentAmount(budget.getCurrentSpentAmount().add(amount));

        budgetRepository.save(budget);
    }

    @Override
    public void resetPeriod(Long budgetId, LocalDate newStart, LocalDate newEnd) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new IllegalArgumentException("Budget does not exist"));

        if (!budget.isActive()) {
            throw new IllegalArgumentException("Cannot reset period of inactive budget");
        }

        if (newStart == null || newEnd == null || newEnd.isBefore(newStart)) {
            throw new IllegalArgumentException("Invalid budget period dates");
        }

        budget.setCurrentSpentAmount(BigDecimal.ZERO);
        budget.setPeriodStart(newStart);
        budget.setPeriodEnd(newEnd);

        budgetRepository.save(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateUtilization(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new IllegalArgumentException("Budget does not exist"));

        if (budget.getMonthlyAllocationAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly allocation amount must be greater than zero");
        }
        return budget.getCurrentSpentAmount()
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getMonthlyAllocationAmount(), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> budgetsExceedingThreshold(BigDecimal utilizationThresholdPercent) {

        if (utilizationThresholdPercent == null || utilizationThresholdPercent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Utilization threshold percentage must be greater than zero");
        }

        return budgetRepository.findByActive(true).stream()
            .filter(b -> {
                if (b.getMonthlyAllocationAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    return false;
                }

                BigDecimal utilization = b.getCurrentSpentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(b.getMonthlyAllocationAmount(), 2, RoundingMode.HALF_UP);
                return utilization.compareTo(utilizationThresholdPercent) >= 0;
            })
            .toList();
    }
}
