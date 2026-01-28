package com.programmingwithtyler.financeforge.service.exception;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;

import java.time.LocalDate;

/**
 * Thrown when attempting to create/update a budget with overlapping period.
 */
public class BudgetOverlapException extends ServiceException {
    private final BudgetCategory category;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    /**
     * Creates exception with default message format.
     */
    public BudgetOverlapException(
        BudgetCategory category,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        super(String.format(
            "Budget overlap detected for category %s in period %s to %s",
            category, periodStart, periodEnd
        ));
        this.category = category;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    /**
     * Creates exception with custom message.
     */
    public BudgetOverlapException(
        BudgetCategory category,
        LocalDate periodStart,
        LocalDate periodEnd,
        String message
    ) {
        super(String.format(
            "%s (category=%s, period=%s to %s)",
            message, category, periodStart, periodEnd
        ));
        this.category = category;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public BudgetCategory getCategory() {
        return category;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
}