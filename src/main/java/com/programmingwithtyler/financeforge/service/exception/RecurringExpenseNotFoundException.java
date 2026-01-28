package com.programmingwithtyler.financeforge.service.exception;

/**
 * Thrown when a recurring expense template with the specified ID cannot be found.
 */
public class RecurringExpenseNotFoundException extends ServiceException {
    private final Long recurringExpenseId;

    public RecurringExpenseNotFoundException(Long recurringExpenseId) {
        super("Recurring expense not found with ID: " + recurringExpenseId);
        this.recurringExpenseId = recurringExpenseId;
    }

    public Long getRecurringExpenseId() {
        return recurringExpenseId;
    }
}