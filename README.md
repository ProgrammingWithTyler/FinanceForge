# FinanceForge

Backend-first personal finance management API built with Spring Boot and Java 21. Demonstrates enterprise backend engineering practices, clean architecture, and disciplined Git workflows.

## Overview

FinanceForge is a **backend-only REST API** designed for personal finance management. This project showcases professional Spring Boot development patterns, production-grade architecture, and rigorous version control discipline suitable for enterprise environments.

**Note:** This is an API-only project. There is no frontend implementation.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.8
- **Build Tool:** Gradle with Kotlin DSL
- **Persistence:** JPA / Hibernate
- **Database:** PostgreSQL
- **Testing:** JUnit 5, Mockito, Testcontainers
- **API Style:** REST (JSON)

## Architecture

FinanceForge follows a layered architecture pattern:

- **Controller Layer:** REST endpoints and request handling
- **Service Layer:** Business logic and orchestration
- **Repository Layer:** Data access via JPA
- **Domain Layer:** Core business entities
- **DTO Layer:** API request/response models
- **Exception Layer:** Centralized error handling

## Local Setup

### Prerequisites

- Java 21 JDK
- Gradle 8.x
- PostgreSQL 15+
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/ProgrammingWithTyler/FinanceForge.git
cd FinanceForge
```

2. Configure PostgreSQL:
```bash
createdb financeforge_dev
```

3. Update `application.yml` with your local database credentials

4. Build the project:
```bash
./gradlew build
```

5. Run the application:
```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Development Workflow

### Branching Model

This project uses a strict Git workflow:

- **`main`:** Production-ready code only. All merges are squash-merged and tagged.
- **`develop`:** Integration branch. Base for all feature branches.
- **`feature/FF-###-description`:** Feature branches linked to GitHub issues.

### Workflow Steps

1. Create a GitHub issue for the feature/fix
2. Create a feature branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/FF-123-add-user-endpoint
   ```
3. Implement changes with conventional commits:
   ```
   feat(user): add user registration endpoint
   fix(account): resolve balance calculation bug
   docs(readme): update setup instructions
   ```
4. Push branch and open a Pull Request to `develop`
5. After review and approval, squash-merge to `develop`
6. Periodically merge `develop` to `main` with release tags

### Commit Message Convention

Follow the conventional commits format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:** feat, fix, docs, refactor, test, chore

## Project Status

This is a **solo-developer backend project** built to demonstrate:

- Enterprise-grade Spring Boot architecture
- Professional Git workflow discipline
- Production-ready code organization
- Comprehensive testing strategies
- Clean API design patterns

## Future Plans

- **v2.0:** Migration to RedQuery ORM (planned, not yet implemented)
- Expanded financial modeling capabilities
- Enhanced reporting endpoints
- Performance optimization for high-volume transactions

## Contributing

This is a solo-developer project for portfolio demonstration purposes. External contributions are not currently accepted.

## License

[Add license information]

## Contact

**GitHub:** [@ProgrammingWithTyler](https://github.com/ProgrammingWithTyler)