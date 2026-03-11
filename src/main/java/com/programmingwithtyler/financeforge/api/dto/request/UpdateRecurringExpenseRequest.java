package com.programmingwithtyler.financeforge.api.dto.request;

import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing recurring expense template.
 *
 * <p>Supports partial updates—all fields are optional (nullable). Only fields
 * with non-null values will be updated in the template. Fields with null values
 * retain their current values.</p>
 *
 * <h3>Update Behavior</h3>
 * <ul>
 *   <li>Null fields are ignored (current value retained)</li>
 *   <li>Non-null fields replace current values</li>
 *   <li>Changing frequency does not automatically recalculate nextScheduledDate</li>
 *   <li>Can activate/deactivate template via the active parameter</li>
 * </ul>
 *
 * <h3>Validation</h3>
 * <p>While all fields are nullable (to support partial updates), any non-null
 * values must still be valid:</p>
 * <ul>
 *   <li>Amount (if provided) must be positive</li>
 *   <li>Frequency (if provided) must be a valid enum value</li>
 *   <li>Description (if provided) must not be blank</li>
 * </ul>
 *
 * @param frequency New recurrence frequency, or null to keep current value
 * @param nextScheduledDate New next scheduled date, or null to keep current value
 * @param amount New fixed amount, or null to keep current value (must be positive if provided)
 * @param category New budget category, or null to keep current value
 * @param description New description, or null to keep current value (must not be blank if provided)
 * @param active New active status (true/false), or null to keep current value
 */
public record UpdateRecurringExpenseRequest(
    TransactionFrequency frequency,
    LocalDate nextScheduledDate,
    BigDecimal amount,
    BudgetCategory category,
    String description,
    Boolean active
) {
}