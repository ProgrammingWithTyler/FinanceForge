package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when a budget with the specified ID cannot be found.
 */
public class BudgetNotFoundException extends ServiceException {
    private final Long budgetId;

    public BudgetNotFoundException(Long budgetId) {
        super("Budget not found with ID: " + budgetId);
        this.budgetId = budgetId;
    }

    public Long getBudgetId() {
        return budgetId;
    }
}