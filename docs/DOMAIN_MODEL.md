# FinanceForge Domain Model

**Version**: 1.0 (MVP)  
**Last Updated**: January 2026  
**Status**: Foundation Complete

---

## Executive Summary

This document describes the core domain model for FinanceForge, a personal finance management system built around four primary aggregates: **Account**, **Transaction**, **Budget**, and **RecurringExpense**. Financial integrity is enforced through explicit invariants, transactional boundaries, and behavior-driven entity design.

**Target Audience**: Backend engineers, architects, new hires, and technical reviewers evaluating domain quality and design decisions.

**Core Philosophy**: Transactions are sacred. Balances are derived. Budgets track intent. Recurring expenses are templates, not events.

---

## Domain at a Glance

| Aggregate            | Root Entity        | Key Invariants                                                                          | Primary Responsibilities                                                                     |
|----------------------|--------------------|-----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| **Account**          | `Account`          | Balance integrity (starting + transactions); Active status required for new operations  | Hold balances; Serve as source/destination for transactions; Manage lifecycle (open/close)   |
| **Transaction**      | `Transaction`      | Amount always positive; Account consistency by type; Immutable after creation           | Record all financial movements; Update account balances atomically; Serve as source of truth |
| **Budget**           | `Budget`           | Positive allocation; No overlapping periods per category; Valid period boundaries       | Define spending plans; Calculate utilization; Compare intent vs. actual spending             |
| **RecurringExpense** | `RecurringExpense` | Positive amount; Valid schedule when active; No duplicate generation per scheduled date | Template for predictable expenses; Generate transactions on schedule; Manage frequency       |

### Notation & Scope Notes

- Code blocks are illustrative and non-normative unless explicitly stated
- Entity methods express domain intent, not final APIs
- Persistence, DTOs, and infrastructure concerns are intentionally omitted

---

## Domain Philosophy

FinanceForge models personal finance as a **double-entry accounting system** with user-friendly abstractions. The domain prioritizes:

1. **Financial Integrity**: Balances are computed from transactions, not stored independently. Transactions are the single source of truth.
2. **Explicit Behavior**: Entities expose methods that enforce business rules. Direct field mutation via setters is prohibited.
3. **Immutability Where Appropriate**: Value objects and commands are immutable. Entities mutate only through controlled, named behavior methods.
4. **Clear Boundaries**: Each aggregate protects its own invariants. Cross-aggregate consistency is achieved through explicit coordination or eventual consistency patterns.

### Core Principles

- **Transactions Are Sacred**: Once recorded, transactions are rarely deleted. Financial corrections are made via compensating transactions (reversals).
- **Balances Are Derived**: Account balances are calculated from starting balance plus transaction history, never managed independently.
- **Budgets Track Intent**: Budgets represent planned spending; transactions represent actual spending. The difference between the two is actionable insight.
- **Recurring Expenses Are Templates**: They generate transactions on a schedule but are not transactions themselves.


<!-- 
═══════════════════════════════════════════════════════════════════════════════
MERMAID DIAGRAM PLACEHOLDER: Aggregate Relationship Overview
═══════════════════════════════════════════════════════════════════════════════
Type: Class Diagram
Purpose: Visualize relationships between core aggregates and enums
Key Elements:
  - Account (1) ←→ (*) Transaction [source/destination relationships]
  - Transaction (*) → (1) BudgetCategory [enum association]
  - Budget (*) → (1) BudgetCategory [enum association]
  - RecurringExpense (*) → (1) Account [source account reference]
  - RecurringExpense (1) → (*) Transaction [generated transactions link back]
Notes:
  - Show cardinality clearly
  - Indicate nullable relationships (source/destination in Transaction)
  - Distinguish composition vs. association
═══════════════════════════════════════════════════════════════════════════════
-->

---

## Core Aggregates

### 1. Account Aggregate

**Root Entity**: `Account` *(Mutable)*

**Purpose**: Represents a financial account (checking, savings, credit card, investment, cash). Accounts hold balances and serve as the source or destination for all financial transactions.

**Mutability**: Mutable entity. State changes only occur through controlled behavior methods (`debit()`, `credit()`, `close()`). Direct field access via setters is not permitted.

**Key Attributes**:
- `id` *(Long)*: Unique identifier
- `name` *(String)*: User-defined name (e.g., "Chase Checking", "Emergency Fund")
- `type` *(AccountType enum)*: Account classification
- `startingBalance` *(BigDecimal)*: Initial balance when account was created (allows users to onboard existing accounts without synthetic transactions)
- `currentBalance` *(BigDecimal)*: Current balance, maintained by applying transaction deltas atomically
- `isActive` *(Boolean)*: Whether the account accepts new transactions
- `createdAt`, `updatedAt` *(Timestamp)*: Audit timestamps

**Behaviors** *(Illustrative - Non-Normative)*:

```java
// NOTE: Method signatures below are illustrative examples for design guidance.
// Actual implementations include additional validation, error handling, and framework integration.

public void debit(BigDecimal amount)
// Purpose: Decreases balance by specified amount
// Precondition: amount > 0
// Postcondition: currentBalance -= amount

public void credit(BigDecimal amount)
// Purpose: Increases balance by specified amount
// Precondition: amount > 0
// Postcondition: currentBalance += amount

public void close()
// Purpose: Marks account inactive; prevents new transactions
// Postcondition: isActive = false

public BigDecimal getNetChange()
// Purpose: Calculates total change since account creation
// Returns: currentBalance - startingBalance
```

**Invariants**:
1. **Balance Integrity**: `currentBalance` must always equal `startingBalance` plus the sum of all transaction impacts (debits subtract, credits add)
2. **Positive Amounts**: Debit and credit operations require positive amounts (direction is explicit in the method name, not the sign of the amount)
3. **No Orphaned Transactions**: Accounts with transaction history cannot be hard-deleted; they must be marked inactive (soft delete) or require cascade deletion with explicit user confirmation
4. **Active Status**: Inactive (closed) accounts cannot be used as source or destination for new transactions

**AccountType Enum** *(Illustrative)*:

```java
public enum AccountType {
    CHECKING,       // Day-to-day spending and bill payments
    SAVINGS,        // Long-term savings and emergency funds
    CREDIT_CARD,    // Revolving credit (negative balance represents debt)
    INVESTMENT,     // Brokerage accounts, retirement accounts (401k, IRA)
    CASH            // Physical cash tracking
}
```

**Design Rationale**:
- **Why `currentBalance` is stored**: Performance optimization. Calculating balance from transaction history on every read is computationally expensive and scales poorly. The balance is updated atomically with each transaction in a single database transaction, ensuring consistency.
- **Why `startingBalance` exists**: Allows users to initialize accounts with existing balances (e.g., importing from another system) without creating synthetic "initial balance" transactions. Simplifies onboarding and reporting.
- **Why enums for type**: Account types are stable, finite, and domain-specific. They change infrequently (years, not weeks) and do not require user customization in MVP scope. See "Why Enums" section for detailed rationale.

---

### 2. Transaction Aggregate

**Root Entity**: `Transaction` *(Immutable After Creation)*

**Purpose**: Represents a single financial movement (income, expense, or transfer). Transactions are the authoritative source of truth for all balance changes and budget spending calculations.

**Mutability**: Effectively immutable after creation. Transactions can be marked void or reversed via compensating transactions, but core fields (amount, date, accounts, category) are never edited. This ensures auditability and financial integrity.

**Key Attributes**:

- `id` *(Long)*: Unique identifier
- `type` *(TransactionType enum)*: Classification as income, expense, or transfer
- `amount` *(BigDecimal)*: Absolute value (always positive; direction is determined by transaction type)
- `description` *(String)*: User-provided notes or memo
- `date` *(LocalDate)*: Transaction date (when the financial event occurred)
- `sourceAccount` *(Account)*: Account from which money is debited (nullable for income transactions)
- `destinationAccount` *(Account)*: Account to which money is credited (nullable for expense transactions)
- `budgetCategory` *(BudgetCategory enum)*: Category for budget tracking (required for expenses, null for income/transfers)
- `isRecurring` *(Boolean)*: Flag indicating if this transaction was auto-generated from a RecurringExpense template
- `recurringExpenseId` *(Long)*: Foreign key linking to the source RecurringExpense template (nullable for manual transactions)
- `createdAt`, `updatedAt` *(Timestamp)*: Audit timestamps

**Behaviors** *(Illustrative - Non-Normative)*:

```java
// NOTE: Factory method examples below illustrate the transaction creation pattern.
// Production implementations include additional validation, null checks, and persistence logic.

public static Transaction income(Account destination, BigDecimal amount, String description)
// Purpose: Creates an income transaction (credits destination account)
// Preconditions: destination not null, amount > 0
// Postcondition: Transaction with sourceAccount = null, destinationAccount = destination

public static Transaction expense(Account source, BigDecimal amount, 
                                   BudgetCategory category, String description)
// Purpose: Creates an expense transaction (debits source account)
// Preconditions: source not null, amount > 0, category not null
// Postcondition: Transaction with sourceAccount = source, destinationAccount = null

public static Transaction transfer(Account source, Account destination, 
                                    BigDecimal amount, String description)
// Purpose: Creates a transfer transaction (debits source, credits destination)
// Preconditions: source not null, destination not null, source != destination, amount > 0
// Postcondition: Transaction with both accounts populated, budgetCategory = null

public void applyToAccounts()
// Purpose: Updates associated account balances atomically
// Called by: Service layer after transaction creation
// Side effects: Modifies account.currentBalance for source/destination

public boolean isTransfer()
// Purpose: Checks if this is a transfer transaction
// Returns: true if both sourceAccount and destinationAccount are non-null
```

**Invariants**:

1. **Amount Positivity**: `amount` must always be positive. Direction is determined by transaction type and account assignment (source vs. destination), not by the sign of the amount value.
2. **Account Consistency by Type**:
    - **Income**: `destinationAccount` required (where money arrives), `sourceAccount` must be null
    - **Expense**: `sourceAccount` required (where money leaves), `destinationAccount` must be null
    - **Transfer**: Both `sourceAccount` and `destinationAccount` required, and they must be different accounts
3. **Budget Category Rules**: Expense transactions must have a `budgetCategory` assigned. Income and transfer transactions do not impact budgets and must have `budgetCategory = null`.
4. **Date Validity**: Transaction date cannot be in the future (enforced at service layer). Historical transactions are permitted.
5. **Immutability After Creation**: Once persisted, transactions cannot be edited. Corrections must be made via new compensating transactions (reversals or adjustments). This ensures an immutable audit trail.

**TransactionType Enum** *(Illustrative)*:

```java
public enum TransactionType {
    INCOME,     // Money coming in (salary, gifts, investment returns, refunds)
    EXPENSE,    // Money going out (bills, purchases, fees)
    TRANSFER    // Moving money between accounts (no budget impact, no net worth change)
}
```

**Design Rationale**:
- **Why separate `sourceAccount` and `destinationAccount`**: Transfers inherently involve two accounts, while income and expense involve only one. Using separate nullable fields handles all three transaction types elegantly without type-specific subclasses or discriminator logic.
- **Why `amount` is always positive**: Direction is made explicit through the transaction type and account assignment. This prevents sign confusion (is `-50` a debit or a credit?) and simplifies queries and calculations.
- **Why `isRecurring` flag**: Distinguishes manual user-initiated transactions from system-generated recurring transactions. Useful for auditing, reporting, and user experience (e.g., showing which transactions are automated).
- **Why immutability**: Financial transactions are audit records. Editing historical transactions violates accounting principles and regulatory requirements. Corrections must be transparent (new transactions), not silent (edits).

<!-- 
═══════════════════════════════════════════════════════════════════════════════
MERMAID DIAGRAM PLACEHOLDER: Transaction Creation Flow
═══════════════════════════════════════════════════════════════════════════════
Type: Sequence Diagram
Purpose: Illustrate the interaction flow for creating a transaction
Participants: Controller, Service, Transaction (entity), Account (entity), Repository
Key Steps:
  1. Controller receives HTTP request with transaction data
  2. Service validates business rules (sufficient funds, active accounts, etc.)
  3. Service creates Transaction entity via factory method
  4. Transaction.applyToAccounts() updates source/destination balances
  5. Service persists Transaction and Account(s) atomically within @Transactional boundary
  6. Service returns created transaction to Controller
  7. Controller transforms entity to DTO and returns HTTP response
Notes:
  - Emphasize atomicity: both transaction save and balance updates succeed or fail together
  - Show rollback on exception (insufficient funds, account inactive, etc.)
═══════════════════════════════════════════════════════════════════════════════
-->

---

### 3. Budget Aggregate

**Root Entity**: `Budget` *(Mutable)*

**Purpose**: Represents a spending allocation for a specific category over a defined time period (typically monthly). Budgets are prescriptive (planned spending) while transactions are descriptive (actual spending).

**Mutability**: Mutable entity. Users can adjust `allocatedAmount` or period boundaries through controlled service layer methods. Modifications are audited via `updatedAt` timestamp.

**Key Attributes**:
- `id` *(Long)*: Unique identifier
- `category` *(BudgetCategory enum)*: The spending category this budget tracks
- `allocatedAmount` *(BigDecimal)*: Planned spending limit for the period
- `periodStart` *(LocalDate)*: First day of the budget period (inclusive, typically 1st of month)
- `periodEnd` *(LocalDate)*: Last day of the budget period (inclusive, typically last day of month)
- `createdAt`, `updatedAt` *(Timestamp)*: Audit timestamps

**Behaviors** *(Illustrative - Non-Normative)*:

```java
// NOTE: These methods demonstrate domain behavior patterns for budget calculations.
// Actual implementations delegate to repository/service layers for transaction queries.

public BigDecimal calculateSpent()
// Purpose: Computes total spending for this category within the budget period
// Implementation: Queries transactionRepository.sumByCategoryAndPeriod(category, periodStart, periodEnd)
// Returns: Sum of all expense transaction amounts for this category in this period

public BigDecimal calculateRemaining()
// Purpose: Computes remaining budget allowance
// Returns: allocatedAmount - calculateSpent()
// Note: Negative value indicates over-budget condition

public BigDecimal getUtilizationPercentage()
// Purpose: Computes percentage of budget consumed
// Returns: (calculateSpent() / allocatedAmount) * 100
// Note: Values > 100 indicate over-budget condition

public boolean isOverBudget()
// Purpose: Checks if spending has exceeded allocated amount
// Returns: calculateSpent() > allocatedAmount

public boolean isWithinPeriod(LocalDate date)
// Purpose: Checks if a given date falls within this budget's period
// Returns: date >= periodStart && date <= periodEnd
```

**Invariants**:
1. **Period Validity**: `periodEnd` must be chronologically after `periodStart` (or equal for single-day budgets)
2. **Positive Allocation**: `allocatedAmount` must be positive (zero allocation is not permitted; delete the budget instead)
3. **No Overlapping Periods**: For any given category, only one budget may exist for overlapping time periods (enforced at service layer to prevent ambiguity)
4. **Category Required**: Every budget must have a `budgetCategory` assigned

**Design Rationale**:
- **Why `allocatedAmount` is stored**: Budgets represent intent (planning), while transactions represent reality (execution). Storing the allocation separates these concerns and allows historical analysis ("How did my budget change over time?").
- **Why periods are explicit**: Enables historical budget tracking, period-over-period comparisons, and year-end reporting. Without explicit periods, historical budgets would be lost.
- **Why spending is calculated dynamically**: Maintains single source of truth (transactions). If spending were stored in the Budget entity, it would require synchronization with transactions, creating potential for data inconsistency.
- **Why monthly periods for MVP**: Monthly budgeting is the most common pattern in personal finance. Future enhancements may support weekly, bi-weekly, quarterly, or custom period definitions.

---

### 4. RecurringExpense Aggregate

**Root Entity**: `RecurringExpense` *(Mutable)*

**Purpose**: Serves as a template for automatically generating predictable expense transactions (rent, subscriptions, insurance, utilities). RecurringExpense is not a transaction itself; it is a factory that creates transactions on a defined schedule.

**Mutability**: Mutable entity. Users can modify the schedule, amount, or active status through controlled service layer methods. Template modifications do not affect previously generated transactions.

**Key Attributes**:
- `id` *(Long)*: Unique identifier
- `name` *(String)*: User-friendly name for the recurring expense (e.g., "Netflix Subscription", "Monthly Rent")
- `amount` *(BigDecimal)*: Amount of each generated transaction
- `frequency` *(RecurringFrequency enum)*: How often transactions should be generated (weekly, monthly, etc.)
- `sourceAccount` *(Account)*: Account from which generated transactions will debit money
- `budgetCategory` *(BudgetCategory enum)*: Category assigned to all generated transactions (for budget tracking)
- `nextScheduledDate` *(LocalDate)*: The date when the next transaction should be generated
- `isActive` *(Boolean)*: Whether this template is enabled for automatic transaction generation
- `createdAt`, `updatedAt` *(Timestamp)*: Audit timestamps

**Behaviors** *(Illustrative - Non-Normative)*:

```java
// NOTE: These methods illustrate the template pattern for recurring transaction generation.
// Service layer orchestrates actual generation, validation, and persistence.

public Transaction generateTransaction()
// Purpose: Creates a new Transaction entity based on this template
// Preconditions: isActive = true, sourceAccount is active, nextScheduledDate <= today
// Postcondition: Returns Transaction with isRecurring = true, recurringExpenseId = this.id
// Side effect: Calls calculateNextScheduledDate() to advance schedule

public void calculateNextScheduledDate()
// Purpose: Advances nextScheduledDate based on frequency
// Examples: 
//   - WEEKLY: add 7 days
//   - MONTHLY: add 1 month (handles variable month lengths)
//   - YEARLY: add 1 year (handles leap years)

public void activate()
// Purpose: Enables automatic transaction generation
// Precondition: nextScheduledDate must be set
// Postcondition: isActive = true

public void deactivate()
// Purpose: Disables automatic transaction generation (pause)
// Postcondition: isActive = false
// Note: nextScheduledDate is preserved for future reactivation

public boolean isDue(LocalDate currentDate)
// Purpose: Checks if this template should generate a transaction today
// Returns: nextScheduledDate <= currentDate && isActive == true
```

**Invariants**:
1. **Positive Amount**: `amount` must be positive (zero-amount recurring expenses are not permitted)
2. **Valid Schedule When Active**: If `isActive = true`, then `nextScheduledDate` must not be null
3. **Account Required and Active**: `sourceAccount` must exist and must be active (cannot generate transactions from closed accounts)
4. **Category Required**: `budgetCategory` must be specified (all generated transactions are expenses and require budget tracking)
5. **No Duplicate Generation**: The same recurring expense cannot generate multiple transactions for the same scheduled date (enforced at service layer via idempotency checks)

**RecurringFrequency Enum** *(Illustrative)*:

```java
public enum RecurringFrequency {
    WEEKLY,      // Every 7 days
    BIWEEKLY,    // Every 14 days
    MONTHLY,     // Same day each month (e.g., 15th of every month)
    QUARTERLY,   // Every 3 months
    YEARLY       // Same day each year (e.g., annual insurance premium)
}
```

**Design Rationale**:
- **Why separate from Transaction**: Recurring expenses are templates (intent), not events (execution). Deleting a template should not delete historical transactions generated from it. Conversely, editing a template should not retroactively modify past transactions.
- **Why `nextScheduledDate` is stored**: Makes batch processing efficient. A scheduled job can query all recurring expenses where `isDue(today)` returns true without recalculating dates for every row. Also allows users to "skip" a scheduled occurrence by manually advancing the date.
- **Why `isActive` flag**: Allows users to pause/resume recurring expenses (e.g., seasonal subscriptions, temporary suspension of rent) without losing the template configuration or schedule history.
- **Why link to generated transactions**: Creates an audit trail showing which transactions were auto-generated versus manually entered. Useful for reconciliation and troubleshooting.

<!-- 
═══════════════════════════════════════════════════════════════════════════════
MERMAID DIAGRAM PLACEHOLDER: Recurring Expense Generation Flow
═══════════════════════════════════════════════════════════════════════════════
Type: Flowchart
Purpose: Show the batch processing logic for generating recurring expense transactions
Key Steps:
  1. Scheduler triggers daily batch job (e.g., cron job at midnight)
  2. Service queries all RecurringExpense where isDue(today) = true
  3. For each due recurring expense:
     a. Check if transaction already exists for (recurringExpenseId, scheduledDate) - idempotency
     b. If not exists: call generateTransaction(), persist Transaction
     c. Call calculateNextScheduledDate(), update RecurringExpense.nextScheduledDate
     d. Persist RecurringExpense
  4. Commit all changes atomically (or per recurring expense transaction)
  5. Log success/failure for monitoring and alerting
Decision Points:
  - Diamond: "Transaction already generated?" (Yes → Skip, No → Generate)
  - Diamond: "Source account active?" (No → Skip with warning, Yes → Proceed)
Notes:
  - Emphasize idempotency (no duplicate transactions)
  - Show rollback handling if generation fails mid-batch
═══════════════════════════════════════════════════════════════════════════════
-->

---

### 5. BudgetCategory (Enum-Based for MVP)

**Type**: Java Enum *(Immutable)*

**Purpose**: Defines a fixed set of spending categories for budget tracking and expense classification. Users assign transactions to categories; budgets allocate amounts to categories.

**Mutability**: Immutable. Enum values are compile-time constants defined in code, not runtime data.

**Values** *(Illustrative - MVP Scope)*:
```java
public enum BudgetCategory {
    GROCERIES,          // Food and household supplies
    DINING_OUT,         // Restaurants, cafes, delivery
    TRANSPORTATION,     // Gas, public transit, rideshare, car maintenance
    UTILITIES,          // Electric, water, gas, internet, phone
    HOUSING,            // Rent, mortgage, property tax, HOA fees
    ENTERTAINMENT,      // Movies, concerts, streaming services, hobbies
    HEALTHCARE,         // Medical expenses, prescriptions, insurance copays
    INSURANCE,          // Health, auto, home, life insurance premiums
    DEBT_PAYMENT,       // Credit card payments, loan payments (principal + interest)
    SAVINGS_TRANSFER,   // Transfers to savings accounts (not an expense, but tracked)
    PERSONAL_CARE,      // Haircuts, cosmetics, gym memberships
    CLOTHING,           // Apparel, shoes, accessories
    EDUCATION,          // Tuition, books, courses, training
    GIFTS,              // Presents, donations, charitable contributions
    MISCELLANEOUS       // Uncategorized or one-off expenses
}
```

**Design Rationale**: See "Why Enums Were Chosen Over Lookup Tables" section below for detailed justification of enum-based categories versus database tables.

---

## Key Invariants and Rules

This section documents cross-aggregate business rules that coordinate behavior across multiple entities. Each rule includes a formal statement, enforcement mechanism, and illustrative code pattern.

### Cross-Aggregate Rules

#### 1. Balance Consistency Rule

**Statement**: An account's `currentBalance` must always equal `startingBalance` plus the sum of all transaction impacts (credits add, debits subtract).

**Enforcement Mechanism**:
- Transaction creation and account balance updates occur atomically within a single database transaction boundary
- `@Transactional` annotation at service layer ensures ACID guarantees via Spring's transaction management
- If transaction persistence fails, account balance updates automatically roll back (all-or-nothing semantics)

**Code Pattern** *(Illustrative - Non-Normative)*:
```java
// NOTE: This example demonstrates the transactional coordination pattern.
// Production code includes comprehensive error handling, validation, and logging.

@Transactional
public Transaction recordExpense(RecordExpenseCommand cmd) {
    // Retrieve account (throws exception if not found)
    Account account = accountRepository.findById(cmd.accountId())
        .orElseThrow(() -> new AccountNotFoundException(cmd.accountId()));
    
    // Create transaction entity (factory method enforces invariants)
    Transaction tx = Transaction.expense(
        account, 
        cmd.amount(), 
        cmd.category(), 
        cmd.description()
    );
    
    // Update account balance (behavior method enforces positive amount)
    account.debit(cmd.amount());
    
    // Persist both entities within single transaction boundary
    // If either save fails, both are rolled back
    transactionRepository.save(tx);
    accountRepository.save(account);
    
    return tx;
}
```

---

#### 2. Sufficient Funds Rule

**Statement**: An account cannot be debited below its current balance (overdraft protection). Transactions that would result in negative balances are rejected.

**Enforcement Mechanism**:
- Service layer validates account balance before creating expense or transfer transactions
- `InsufficientFundsException` is thrown if `account.currentBalance < transaction.amount`
- No transaction entity is created if validation fails (fail fast)

**Exception**: Credit card accounts may allow negative balances. A negative balance on a credit card account represents outstanding debt owed. This is business logic that can be encoded in `AccountType.CREDIT_CARD` behavior.

**Code Pattern** *(Illustrative - Non-Normative)*:

```java
// Service layer validation before transaction creation
if (account.getCurrentBalance().compareTo(amount) < 0) {
    throw new InsufficientFundsException(
        account.getId(), 
        account.getCurrentBalance(), 
        amount
    );
}
```

---

#### 3. Budget Tracking Rule

**Statement**: All expense transactions must be assigned to a budget category. A budget's spending is calculated by summing all expense transaction amounts for that category within the budget's time period.

**Enforcement Mechanism**:
- Expense transaction creation requires a non-null `budgetCategory` (validated at entity factory method)
- Budget spending is queried dynamically from transactions, not stored in the Budget entity
- Budget utilization is calculated on-demand when requested by the API or UI

**Code Pattern** *(Illustrative - Non-Normative)*:
```java
// Budget entity delegates spending calculation to repository
public BigDecimal calculateSpent() {
    return transactionRepository.sumByCategoryAndPeriod(
        this.category, 
        this.periodStart, 
        this.periodEnd
    );
}

// Repository query method (Spring Data JPA)
@Query("""
    SELECT COALESCE(SUM(t.amount), 0) 
    FROM Transaction t 
    WHERE t.budgetCategory = :category 
      AND t.date BETWEEN :start AND :end
    """)
BigDecimal sumByCategoryAndPeriod(
    @Param("category") BudgetCategory category,
    @Param("start") LocalDate start,
    @Param("end") LocalDate end
);
```

---

#### 4. Transfer Atomicity Rule

**Statement**: Transfers between accounts must debit the source account and credit the destination account atomically. If either operation fails, both must fail (no partial transfers).

**Enforcement Mechanism**:
- Single `@Transactional` boundary wraps both account balance updates
- Database transaction ensures atomicity via commit/rollback semantics
- Any exception during transfer processing triggers automatic rollback of all changes

**Code Pattern** *(Illustrative - Non-Normative)*:
```java
// NOTE: Demonstrates atomic transfer with rollback semantics for failure cases.

@Transactional
public Transaction recordTransfer(TransferCommand cmd) {
    // Retrieve both accounts
    Account source = accountRepository.findById(cmd.sourceId())
        .orElseThrow(() -> new AccountNotFoundException(cmd.sourceId()));
    Account destination = accountRepository.findById(cmd.destId())
        .orElseThrow(() -> new AccountNotFoundException(cmd.destId()));
    
    // Validate sufficient funds in source account
    if (source.getCurrentBalance().compareTo(cmd.amount()) < 0) {
        throw new InsufficientFundsException(
            source.getId(), 
            source.getCurrentBalance(), 
            cmd.amount()
        );
    }
    
    // Create transfer transaction
    Transaction tx = Transaction.transfer(
        source, 
        destination, 
        cmd.amount(), 
        cmd.description()
    );
    
    // Update both account balances (atomic within transaction boundary)
    source.debit(cmd.amount());
    destination.credit(cmd.amount());
    
    // Persist all entities (transaction + 2 accounts)
    // If any save fails, entire operation rolls back
    transactionRepository.save(tx);
    accountRepository.save(source);
    accountRepository.save(destination);
    
    return tx;
}
```

---

#### 5. Recurring Transaction Generation Rule

**Statement**: A recurring expense template can generate at most one transaction per scheduled date. Generated transactions must link back to their source template for audit purposes.

**Enforcement Mechanism**:
- Service layer checks for existing transactions with matching `(recurringExpenseId, scheduledDate)` before generation
- `RecurringExpense.generateTransaction()` updates `nextScheduledDate` after successful generation (advances schedule)
- Generated transactions are flagged with `isRecurring = true` and `recurringExpenseId = {template_id}`

**Code Pattern** *(Illustrative - Non-Normative)*:

```java
// NOTE: Example batch processing logic for scheduled expense generation.
// Production implementations include error handling, retry logic, and monitoring.

@Transactional
public void processScheduledExpenses(LocalDate today) {
    // Query all recurring expenses due today
    List<RecurringExpense> dueExpenses = recurringExpenseRepository.findDue(today);
    
    for (RecurringExpense expense : dueExpenses) {
        // Idempotency check: prevent duplicate generation
        boolean alreadyGenerated = transactionRepository
            .existsByRecurringExpenseAndDate(
                expense.getId(), 
                expense.getNextScheduledDate()
            );
        
        if (!alreadyGenerated) {
            // Generate transaction from template
            Transaction tx = expense.generateTransaction();
            transactionRepository.save(tx);
            
            // Advance schedule to next occurrence
            expense.calculateNextScheduledDate();
            recurringExpenseRepository.save(expense);
        }
    }
}
```

---

#### 6. Account Closure Rule

**Statement**: An account with transaction history cannot be hard-deleted from the database. Instead, it must be marked inactive (soft delete). Accounts without transaction history may be hard-deleted.

**Enforcement Mechanism**:

- Service layer checks for existing transactions referencing the account before deletion
- If transactions exist: set `account.isActive = false` (soft delete)
- If no transactions exist: execute `accountRepository.delete(account)` (hard delete)
- Inactive accounts are excluded from account selection dropdowns and cannot be used for new transactions

**Code Pattern** *(Illustrative - Non-Normative)*:
```java
public void closeAccount(Long accountId) {
    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
    
    // Check for transaction history
    boolean hasTransactions = transactionRepository.existsByAccountId(accountId);
    
    if (hasTransactions) {
        // Soft delete: mark inactive, preserve history
        account.close(); // sets isActive = false
        accountRepository.save(account);
    } else {
        // Hard delete: no history to preserve
        accountRepository.delete(account);
    }
}
```
