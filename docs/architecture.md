# FinanceForge Backend Architecture

**Version**: 1.0 (MVP)  
**Last Updated**: January 2026  
**Status**: Foundation Complete, Service Layer In Progress

---

## Executive Summary

FinanceForge is a personal finance management system built as a **Spring Boot monolith** using a **layered architecture**. The system tracks accounts, budgets, transactions, and recurring expenses with full transactional integrity and clear separation of concerns.

This document explains architectural decisions, layer responsibilities, and explicit boundaries. It is opinionated by design: simplicity and maintainability were prioritized over architectural novelty.

**Target Audience**: Senior engineers, architects, new hires, and contractors who need to understand system design, boundaries, and trade-offs.

---

## 1. Architectural Overview

### High-Level Structure

```
┌───────────────────────────────────────────────┐
│                REST API Layer                 │
│      (Controllers + DTOs + Validation)        │
└────────────────────┬──────────────────────────┘
                     │
┌────────────────────▼──────────────────────────┐
│                Service Layer                  │
│     (Business Logic + Transaction Boundaries) │
└────────────────────┬──────────────────────────┘
                     │
┌────────────────────▼──────────────────────────┐
│                Domain Layer                   │
│     (Entities + Value Objects + Enums)        │
└────────────────────┬──────────────────────────┘
                     │
┌────────────────────▼──────────────────────────┐
│               Repository Layer                │
│       (Spring Data JPA + Custom Queries)      │
└────────────────────┬──────────────────────────┘
                     │
┌────────────────────▼──────────────────────────┐
│                  PostgreSQL                   │
└───────────────────────────────────────────────┘
```

### Core Entities

- **Account**: Financial accounts (checking, savings, credit card, investment, cash)
- **Budget**: Monthly spending allocations per category
- **Transaction**: All financial movements (income, expense, transfer)
- **RecurringExpense**: Templates for predictable recurring expenses

### Key Design Principles

1. **Single Responsibility**: Each layer has one job
2. **Transaction Integrity**: Financial operations are ACID-compliant
3. **Domain-Driven Design Lite**: Rich domain model without tactical DDD patterns
4. **API-First**: REST endpoints define the contract
5. **Fail Fast**: Validation at boundaries; never persist invalid state

---

## 2. Layered Architecture Breakdown

### Controller Layer (API Boundary)

**Responsibility**: HTTP request/response handling, input validation, DTO transformation

**Lives Here**:
- REST controllers (`@RestController`)
- Request DTOs with validation (`@Valid`)
- Response DTOs (hide internal implementation)
- HTTP status code mapping
- Exception handlers (`@RestControllerAdvice`)

**Does NOT Live Here**:
- Business logic → services
- Direct repository access → violates layering
- Transaction management → services
- Domain entity exposure → use DTOs

**DTO Naming Conventions**:

FinanceForge uses a clear naming pattern to distinguish between external API contracts and internal service contracts:

| Type | Purpose | Example | Layer Boundary |
|------|---------|---------|----------------|
| **Request DTO** | External API input | `CreateAccountRequest` | Controller receives |
| **Command** | Internal service input | `CreateAccountCommand` | Service consumes |
| **Query** | Internal service query params | `AccountBalanceQuery` | Service consumes |
| **Response DTO** | External API output | `AccountResponse` | Controller returns |
| **Domain Entity** | Business model | `Account` | Service/Repository only |

**Pattern**:
```
HTTP Request → Request DTO → Command/Query → Service → Domain Entity
Domain Entity → Response DTO → HTTP Response
```

**Rationale**:
- **Request DTOs** expose only what the API needs (validation, documentation)
- **Commands/Queries** express business intent (immutable, testable)
- **Response DTOs** hide implementation details (no JPA annotations, lazy-loading issues)
- **Domain Entities** stay pure and framework-agnostic

**Example**:
```java
// Request DTO: External API contract
public record CreateAccountRequest(
    @NotBlank String name,
    @NotNull AccountType type,
    @NotNull @DecimalMin("0.0") BigDecimal startingBalance
) {
    // Transform to internal command
    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(name, type, startingBalance);
    }
}

// Command: Internal service contract
public record CreateAccountCommand(
    String name,
    AccountType type,
    BigDecimal startingBalance
) {}

// Controller: Orchestrate transformation
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
        @Valid @RequestBody CreateAccountRequest request
    ) {
        // Transform request → command → entity → response
        Account account = accountService.createAccount(request.toCommand());
        return ResponseEntity.status(CREATED)
            .body(AccountResponse.from(account));
    }
}

// Response DTO: External API output
public record AccountResponse(
    Long id,
    String name,
    AccountType type,
    BigDecimal currentBalance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getType(),
            account.getCurrentBalance()
        );
    }
}
```

**Invariants**:
- Controllers never contain business logic
- All inputs validated before service invocation
- DTOs transform to Commands/Queries at controller boundary
- HTTP concerns stay at HTTP boundary
- Domain entities never exposed in API responses

---

### Service Layer (Business Logic Boundary)

**Responsibility**: Orchestrate workflows, enforce rules, manage transactions

**Lives Here**:
- Business logic implementation
- Multi-entity coordination
- Transaction boundaries (`@Transactional`)
- Business rule validation
- Calculated/derived state

**Does NOT Live Here**:
- HTTP concerns → controllers
- SQL queries → repositories
- DTO transformation → controllers
- Raw persistence logic → repositories

**Example**:
```java
@Service
@Transactional
public class TransactionService {
    
    public Transaction recordExpense(RecordExpenseCommand cmd) {
        Account account = accountRepository.findById(cmd.accountId())
            .orElseThrow(() -> new AccountNotFoundException(cmd.accountId()));
        
        // Business rule: Check sufficient funds
        if (account.getBalance().compareTo(cmd.amount()) < 0) {
            throw new InsufficientFundsException(account.getId());
        }
        
        Transaction tx = Transaction.expense(
            account, cmd.amount(), cmd.category(), cmd.description()
        );
        
        account.debit(cmd.amount());
        budgetService.trackSpending(cmd.category(), cmd.amount());
        
        return transactionRepository.save(tx);
    }
}
```

**Transaction Strategy**:
- Default: `@Transactional` (read-write)
- Queries: `@Transactional(readOnly = true)`
- Propagation: `REQUIRED`
- Isolation: `READ_COMMITTED`

**Why `REQUIRED` Propagation?**
- Joins existing transaction if one exists (avoids nested transactions)
- Creates new transaction if none exists (ensures atomicity)
- Simplest model: one transaction per service method call
- Avoids distributed transaction complexity

**Why `READ_COMMITTED` Isolation?**
- PostgreSQL default; no configuration needed
- Prevents dirty reads (reading uncommitted data)
- Allows non-repeatable reads (acceptable for financial workflows)
- Balance between consistency and performance
- Stricter isolation (`SERIALIZABLE`) deferred until proven necessary

**Invariants**:
- Services own transaction boundaries
- No service-to-service transactions (avoid distributed transactions)
- Business rules enforced here, not controllers/repositories
- Services coordinate repositories but never bypass them

---

### Domain Layer (Business Concepts)

**Responsibility**: Model the business domain with behavior-focused entities

**Lives Here**:
- JPA entities (`@Entity`)
- Value objects (embedded or standalone)
- Enums (`AccountType`, `TransactionType`, `BudgetCategory`)
- Domain logic (balance calculations, validation)
- Relationships (`@OneToMany`, `@ManyToOne`)

**Does NOT Live Here**:
- HTTP concerns → controllers
- Database queries → repositories
- Business workflows → services
- Excessive framework annotations → minimize

**Example**:
```java
@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Enumerated(STRING)
    private AccountType type;
    
    private String name;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal startingBalance;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBalance;
    
    // Domain behavior: Encapsulate balance mutations
    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit must be positive");
        }
        this.currentBalance = this.currentBalance.subtract(amount);
    }
    
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit must be positive");
        }
        this.currentBalance = this.currentBalance.add(amount);
    }
}
```

**JPA Strategy**:
- Identity generation: `IDENTITY` (PostgreSQL sequences)
- Lazy loading: Default for collections
- Cascade: Explicit only (no blanket `CascadeType.ALL`)
- Fetch strategies: Optimize per query, not per entity

**Invariants**:
- Entities are **not anemic**: Contain behavior, not just getters/setters
- Entities protect invariants (balance mutations, required fields)
- Relationships bidirectional where sensible
- Never reference DTOs or HTTP concepts

---

### Repository Layer (Persistence Boundary)

**Responsibility**: Abstract database access

**Lives Here**:
- Spring Data JPA repositories
- Custom query methods (`@Query`)
- Derived query methods
- Specifications for complex filters

**Does NOT Live Here**:
- Business logic → services
- Transaction management → services
- DTO transformation → controllers
- HTTP concerns → controllers

**Example**:
```java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByAccountIdAndDateBetween(
        Long accountId, LocalDate start, LocalDate end
    );
    
    @Query("""
        SELECT t FROM Transaction t 
        WHERE t.budgetCategory = :category 
          AND t.date BETWEEN :start AND :end
        """)
    List<Transaction> findByCategoryAndPeriod(
        @Param("category") BudgetCategory category,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
    
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) 
        FROM Transaction t 
        WHERE t.budgetCategory = :category 
          AND t.date >= :start
        """)
    BigDecimal sumByCategoryAfterDate(
        @Param("category") BudgetCategory category,
        @Param("start") LocalDate start
    );
}
```

**Query Strategy**:
- Prefer derived queries for simple cases
- Use `@Query` for complex queries
- Native SQL only when JPA insufficient (rare)
- Paginate large result sets (`Pageable`)

**Invariants**:
- Repositories never contain business logic
- Return domain entities or primitives
- No raw JDBC; JPA everywhere
- Injected only into services, never controllers

---

## 3. Architectural Decisions

### Why Monolith?

**NOT Microservices Because**:

1. **Complexity Budget**: Personal finance app, not distributed system. No need for network latency, distributed transactions, service discovery, API gateways.

2. **Team Size**: Single developer. Microservices optimize for independent team scaling. Not applicable.

3. **Data Consistency**: Financial transactions require ACID. Monolith gives ACID for free. Microservices require Saga patterns and eventual consistency.

4. **Deployment Simplicity**: One JAR, one database, one pipeline. No Kubernetes, service mesh, or tracing infrastructure.

5. **Development Velocity**: Change entity → update repository, service, controller in one commit. No cross-service coordination, versioned APIs, or backward compatibility.

**When to Split**:
- **Never** at current scale
- **Maybe** if traffic >100K users
- **Probably** if independent teams need subdomain ownership
- **Definitely** if regulatory concerns demand physical isolation

---

### Why Layered Architecture?

**NOT Hexagonal/Clean/Onion Because**:

1. **Pragmatism**: Spring Boot's `@Service` + `@Transactional` already provides good abstraction. No framework infection problem.

2. **Inversion Overkill**: Hexagonal adds ports and adapters for what? Swapping PostgreSQL? Not happening.

3. **Testing**: Spring Boot gives `@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest`. Can test each layer in isolation without ports/adapters.

4. **Cognitive Load**: Layered architecture is universally understood. Any Spring developer can contribute immediately.

**Tradeoff**:
- **Lose**: Theoretical decoupling from Spring Boot
- **Gain**: Simplicity, speed, maintainability

---

## 4. Deferred Concerns

| Feature | Status | Reason | Future Approach |
|---------|--------|--------|----------------|
| **Auth & AuthZ** | Not implemented | MVP focus | Spring Security + JWT/session |
| **Multi-Tenancy** | Not implemented | Single-user MVP | Add `userId` FK + row-level security |
| **Caching** | Not implemented | Premature optimization | `@Cacheable` after profiling |
| **Event-Driven** | Not implemented | No external integrations | `@EventListener` or Kafka if needed |
| **CQRS** | Not implemented | Single DB sufficient | Separate read models if writes slow reads |
| **RedQuery** | Deferred | JPA sufficient | Custom DSL if Criteria API unmanageable |
| **API Versioning** | Not implemented | No external consumers | `/api/v1/...` when backward compat required |
| **Distributed Tracing** | Not implemented | Monolith, simple logs | Sleuth + Zipkin if microservices |

---

## 5. Boundaries & Invariants

### Layer Boundaries

| Boundary | What Crosses | Cannot Cross |
|----------|-------------|--------------|
| **Controller ↔ Service** | Commands, Queries, DTOs | Domain Entities, HTTP Concerns |
| **Service ↔ Repository** | Domain Entities, IDs, Filters | DTOs, HTTP Concerns, Business Logic |
| **Service ↔ Domain** | Entities, Value Objects | Persistence Logic, DTOs |
| **Repository ↔ Database** | JPA Entities, Queries | Business Logic, DTOs |

---

### Architectural Invariants (Non-Negotiable)

#### 1. Transactions Are Service-Owned
- **Rule**: Only services use `@Transactional`
- **Why**: Controllers are request-scoped; repositories are data-scoped; only services understand workflows
- ❌ Controller calling `@Transactional` repository
- ✅ Controller → Service (with `@Transactional`) → Repository

#### 2. Entities Never Exposed via API
- **Rule**: Controllers return DTOs, never domain entities
- **Why**: Domain models change for business reasons; API contracts for consumer reasons
- ❌ `ResponseEntity<Account> getAccount()`
- ✅ `ResponseEntity<AccountResponse> getAccount()`

#### 3. Business Logic Lives in Services
- **Rule**: Controllers validate/transform. Services enforce rules.
- **Why**: Business logic in controllers = untestable. In repositories = violates SRP.
- ❌ Controller checking `if (balance > amount)`
- ✅ Service method `transferFunds()` checking sufficient funds

#### 4. Domain Entities Protect Invariants
- **Rule**: Entities expose behavior methods, not raw setters
- **Why**: Prevents invalid state (negative balances, null fields)
- ❌ `account.setBalance(account.getBalance().subtract(amount))`
- ✅ `account.debit(amount)`

#### 5. No Cross-Layer Dependencies
- **Rule**: Each layer depends only on layer below
- **Why**: Prevents circular dependencies, keeps architecture testable
- ❌ Repository calling Service
- ✅ Controller → Service → Repository

#### 6. Fail Fast at Boundaries
- **Rule**: Validate inputs at API boundary. Throw immediately.
- **Why**: Invalid data should never reach service/database
- ❌ Service checking `@NotNull` constraints
- ✅ Controller DTO with `@Valid @NotNull` → Service assumes valid

---

### Performance Invariants

1. **N+1 Queries Are Bugs**: Use `@EntityGraph` or `JOIN FETCH`
2. **Pagination Mandatory**: No unbounded result sets
3. **Index All Foreign Keys**: Every `@ManyToOne` gets index
4. **Query Timeout**: Queries <100ms or log warning

---

### Testing Invariants

1. **Repository Tests**: In-memory DB (fast, isolated)
2. **Service Tests**: Mock repositories (unit tests)
3. **Controller Tests**: Mock services (test HTTP layer)
4. **Integration Tests**: Testcontainers (real PostgreSQL, real workflows)

---

## 6. Technology Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| **API** | Spring Web (REST) | Industry standard REST framework; minimal boilerplate; excellent documentation; broad ecosystem support |
| **Validation** | Jakarta Validation (Hibernate Validator) | Declarative constraint validation; annotation-based; seamless Spring integration; extensible custom validators |
| **Service** | Spring Core (DI + AOP) | Dependency injection for testability; AOP for transaction management; mature, battle-tested framework |
| **Persistence** | Spring Data JPA (Hibernate) | Abstracts JDBC boilerplate; supports derived queries and custom JPQL; reduces manual SQL while allowing native queries when needed |
| **Database** | PostgreSQL 15+ | ACID compliance for financial integrity; JSON support for flexible schemas; excellent performance; open-source; mature tooling |
| **Build** | Maven | Dependency management; plugin ecosystem; standardized build lifecycle; widely adopted in enterprise Java |
| **Testing** | JUnit 5 + Mockito + Testcontainers | Modern test framework (JUnit 5); powerful mocking (Mockito); real database integration tests (Testcontainers) |
| **Migration** | Flyway | Version-controlled schema migrations; idempotent; supports rollback via versioned SQL; integrates with CI/CD |

---

## 7. Deployment & Release Strategy

### Local Development
- **Database**: PostgreSQL running in Docker
- **Application**: Spring Boot DevTools + hot reload
- **Ports**: 8080 (app), 5432 (PostgreSQL)
- **IDE**: IntelliJ IDEA or VS Code with Java extensions

### Production (Future)
- **Database**: Managed PostgreSQL (AWS RDS, Azure Database, Google Cloud SQL)
- **Application**: Single JAR deployment (VM or containerized)
- **Reverse Proxy**: Nginx or cloud load balancer
- **Monitoring**: Actuator endpoints + Prometheus + Grafana

---

### Versioning Strategy

**Semantic Versioning**: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking API changes, architectural rewrites
- **MINOR**: New features, backward-compatible API additions
- **PATCH**: Bug fixes, dependency updates, performance improvements

**Release Notes**: Maintained in `CHANGELOG.md` with each version increment

**API Versioning** (when needed):
- Not implemented for MVP (single consumer, rapid iteration)
- Future: URL-based versioning (`/api/v1/accounts`, `/api/v2/accounts`)
- Trigger: External consumers, backward compatibility requirements

**Schema Versioning**:
- Flyway migrations track all schema changes
- Migration naming: `V{version}__{description}.sql` (e.g., `V1__initial_schema.sql`)
- Rollback: Forward-only migrations; rollback via new migration if needed

---

### Rollback & Recovery Strategy

**Application Rollback**:
1. Redeploy previous JAR version
2. No code changes required; JAR is self-contained
3. Rollback window: <5 minutes (restart application)

**Database Rollback**:
- **Flyway Strategy**: Forward-only migrations by default
- **Rollback Process**:
    1. Create compensating migration (e.g., `V3__revert_feature.sql`)
    2. Apply compensating migration via Flyway
    3. Redeploy previous application version
- **Why Not Automatic Rollback?**: Schema rollbacks are dangerous (data loss risk); explicit compensating migrations force intentionality

**Disaster Recovery**:
- **Database Backups**: Automated daily snapshots (managed PostgreSQL provider)
- **Point-in-Time Recovery**: Restore to any point within retention period (7-30 days)
- **Application State**: Stateless; no local state to recover

**Rollback Testing**:
- Rollback procedures tested in staging environment before production
- Compensating migrations validated against prod-like data volumes

---

## 8. When to Revisit This Architecture

| Trigger | Consideration |
|---------|--------------|
| **Traffic >100K Users** | Caching, read replicas, horizontal scaling |
| **Multiple Teams** | Microservices with bounded contexts |
| **External Integrations** | Event-driven architecture with message queues |
| **Complex Reporting** | CQRS with separate read models |
| **Regulatory Requirements** | Audit logs, encryption at rest, compliance frameworks |

### What We'd Change (If We Had To)

- **Microservices**: Only if independent deployment or team scaling required
- **Event Sourcing**: Only if audit trail legally required or time-travel queries needed
- **GraphQL**: Only if API consumers need flexible querying (unlikely)
- **NoSQL**: Never. Relational data demands relational databases.

---

## 9. Conclusion

FinanceForge uses a **layered monolith** because it solves the problem with minimal complexity. The architecture prioritizes:

1. **Simplicity**: Four layers, clear boundaries
2. **Maintainability**: Any Spring Boot developer can contribute immediately
3. **Correctness**: ACID transactions, input validation, domain invariants
4. **Pragmatism**: No over-engineering, no premature optimization

This architecture scales to **tens of thousands of users** before needing revision. When that day comes, we'll have real data to guide decisions—not speculation.

**Until then, we build. We don't over-architect.**

---

**Document Owner**: FinanceForge Engineering  
**Last Review**: January 2026  
**Next Review**: After Phase 8 completion or 6 months, whichever comes first