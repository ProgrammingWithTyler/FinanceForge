# Development Notes

## Phase 3: Service Layer Implementation

### Design Decisions

#### FF-014: TransactionService Implementation Pattern
**Decision**: Used separate methods (`recordIncome`, `recordExpense`, `recordTransfer`) instead of a single `createTransaction(type, ...)` method.

**Rationale**:
- Aligns with domain model's factory pattern
- Provides type safety (no nullable parameters)
- Follows command pattern from architecture docs
- Each transaction type has fundamentally different semantics

**Impact**:
- Created 4 command objects instead of passing primitives
- Each method is focused and testable
- Interface clearly expresses intent

#### AccountService: Removed `adjustBalance()`
**Rationale**: Violates domain invariant that balances change only through transactions.

**Alternative**: Users call `TransactionService.recordIncome()` or `recordExpense()` directly.

#### BudgetService: Spending is Calculated, Not Stored
**Rationale**: Single source of truth (transactions). Avoids synchronization issues.

**Implementation**: `calculateSpent()` queries `TransactionRepository.sumByCategoryAndPeriod()`.