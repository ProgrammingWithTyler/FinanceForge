# FinanceForge Backend Architecture – Quick Reference

**Version**: 1.0 (MVP)
**Audience**: New hires, contractors, senior engineers
**Goal**: Immediate understanding of layers, boundaries, and invariants

For full architectural details, see [FinanceForge Backend Architecture](architecture.md)


---

## 1. Layer Responsibilities

| Layer          | Responsibility                                       | Lives Here                                         | Must Not Do                                          |
| -------------- | ---------------------------------------------------- | -------------------------------------------------- | ---------------------------------------------------- |
| **Controller** | HTTP handling, validation, DTO transformation        | `@RestController`, Request/Response DTOs, `@Valid` | Business logic, direct DB calls, expose entities     |
| **Service**    | Business rules, workflow orchestration, transactions | `@Service`, `@Transactional`, Commands/Queries     | HTTP concerns, raw persistence, DTO transformation   |
| **Domain**     | Business entities with behavior                      | JPA Entities, Value Objects, Enums                 | HTTP concerns, persistence logic, business workflows |
| **Repository** | Data access abstraction                              | Spring Data JPA, `@Query`                          | Business logic, transactions, DTOs, HTTP concerns    |

---

## 2. Data Flow Pattern

```
HTTP Request
   ↓
Request DTO (validation)
   ↓
Command/Query → Service → Domain Entity
   ↓
Domain Entity → Response DTO
   ↓
HTTP Response
```

**DTO Naming Conventions**:

* `CreateAccountRequest` → Controller input
* `CreateAccountCommand` → Service input
* `AccountResponse` → Controller output
* `Account` → Domain entity (never exposed)

---

## 3. Transaction & Persistence Rules

* **Transactions**: Only in services (`@Transactional`)
* **Propagation**: `REQUIRED`
* **Isolation**: `READ_COMMITTED`
* **Repositories**: Return domain entities or primitives only
* **N+1 Queries**: Bug. Use `@EntityGraph` or `JOIN FETCH`
* **Pagination**: Mandatory for all large result sets
* **Indexes**: All FK columns

---

## 4. Core Invariants

1. **Entities protect state**: Use behavior methods (`debit()`, `credit()`), no public setters
2. **No cross-layer dependencies**: Controller → Service → Repository → DB only
3. **Fail fast**: Validate at API boundary
4. **Services enforce business rules**: Controllers only validate/transform
5. **Entities never exposed in API**: Always return DTOs
6. **Testing**: Repository → unit, Service → mock repositories, Controller → mock services, Integration → Testcontainers

---

## 5. Core Entities

* **Account**: Checking, savings, credit, investment, cash
* **Budget**: Monthly category allocations
* **Transaction**: Income, expense, transfer
* **RecurringExpense**: Templates for predictable charges

---

## 6. Quick Tech Stack

| Layer       | Tech                                     |
| ----------- | ---------------------------------------- |
| API         | Spring Web (REST)                        |
| Validation  | Jakarta Validation (Hibernate Validator) |
| Service     | Spring Core (DI + AOP)                   |
| Persistence | Spring Data JPA (Hibernate)              |
| Database    | PostgreSQL 15+                           |
| Build       | Maven                                    |
| Testing     | JUnit 5 + Mockito + Testcontainers       |
| Migration   | Flyway                                   |

---

## 7. Deployment Cheats

**Local**: Docker PostgreSQL + Spring Boot DevTools
**Prod**: Single JAR, managed PostgreSQL, Nginx/Cloud LB
**Rollback**: Redeploy previous JAR; compensating Flyway migration for schema

---

## 8. When to Revisit Architecture

* > 100K users → caching, read replicas
* Multiple teams → microservices
* External integrations → event-driven architecture
* Complex reporting → CQRS
* Regulatory requirements → audit logs, encryption

---

✅ **Rule of Thumb**: Keep it simple, maintain clear boundaries, and enforce invariants. Layered monolith is sufficient until scaling or compliance demands change.
