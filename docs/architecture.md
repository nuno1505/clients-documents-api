# Architecture — Clients API

Architecture document for the **Clients API** exercise: a Java + Spring REST API for managing Clients and their Documents, with full CRUD operations.

## Audited implementation status

This note describes the current code and takes precedence over historical examples and decisions in later sections where they differ. The implementation uses Java 21 and Spring Boot 3.5.16, and separates HTTP (`client.api`), services (`client.service`), repositories, entities, DTOs, and mappers. Services define transaction boundaries; controllers delegate and do not expose JPA entities. `Client.documents` uses `cascade = ALL` and `orphanRemoval = true`; `Document.client` owns the foreign key, and the `addDocument`/`removeDocument` methods maintain the bidirectional association.

Flyway applies `V1__create_clients.sql` and `V2__create_documents.sql`; Hibernate uses `ddl-auto: validate`. The application also uses in-memory H2 in the `prod` profile, so data does not survive a restart. There is no Actuator dependency or `/actuator/health` endpoint. Authentication is stateless and uses a single in-memory user, BCrypt, and JWT HS256 with a default lifetime of 900 seconds. The HS256 secret must be at least 32 bytes. Authentication properties come from the environment in the `dev` and `prod` profiles; the BCrypt format is not explicitly validated at startup.

Springdoc is enabled in `dev` and disabled in the base configuration and in `prod`. There are 10 controller happy-path unit tests for the 10 business endpoints, 15 full-context integration tests with JWT/H2/Flyway, and six security tests. The audit on 2026-10-30 verified 57 tests with no failures, errors, or skipped tests. The Docker image is multi-stage and runs as an unprivileged user.

The decision and configuration tables below preserve the exercise history; passages describing Actuator, health checks, or BCrypt format validation are historical intentions/documentation, not implemented behavior.

The project was developed using the **OpenCode → Ollama → Qwen3** stack, with agents used throughout implementation.

## Legend

| Label | Meaning |
|---|---|
| `[CONFIRMED]` | Requirement explicitly stated in the assignment. |
| `[ASSUMPTION]` | Our inference or simplification, **not** required by the assignment and not yet validated. |
| `[DECISION]` | Settled technical or scope choice, with rationale. |
| `[OPTIONAL]` | Nice-to-have in the assignment; OPT-01, OPT-02, and OPT-03 were promoted into scope by decision (Q-04/Q-08). |

> Note: this document adds no requirements. Anything not in the assignment is identified as `[ASSUMPTION]` or `[DECISION]`. Questions Q-01..Q-10 and the seven previously open items were resolved by the author and are consolidated in section 11 (including the “Resolved items” record).

---

## 1. Requirements

### 1.1 Functional requirements `[CONFIRMED]`

| ID | Requirement |
|---|---|
| RF-01 | Java + Spring REST API for managing Clients. |
| RF-02 | CRUD operations for Clients (Create, Read, Update, Delete). |
| RF-03 | `Client` has the fields Id, FirstName, LastName, Tax Identifier, Email, PhoneNumber, and Documents (one-to-many relationship). |
| RF-04 | `Document` has the fields Id, Number, Description, and Expiration Date. |
| RF-05 | Manage Documents associated with each Client (the assignment requests full CRUD for Clients and Documents). |
| RF-06 | POST, GET, PUT, and DELETE endpoints for Clients. |
| RF-07 | Login API, if JWT is implemented. |

### 1.2 Non-functional requirements `[CONFIRMED]`

| ID | Requirement |
|---|---|
| RNF-01 | In-memory H2 database (another database is allowed). |
| RNF-02 | Client and its Documents persisted in a single transaction (atomicity). |
| RNF-03 | Schema created automatically, or a script/configuration provided in `application.yaml`. |
| RNF-04 | Build with Maven or Gradle. |
| RNF-05 | At least one unit test (happy path) for each API endpoint. |
| RNF-06 | OpenCode → Ollama → Qwen3 development stack, with agents used during implementation. |

> Implementation note (RNF-03): the requirement allows either an automatically created schema **or** scripts/configuration. The chosen approach is **Flyway** — versioned SQL migrations in `src/main/resources/db/migration` — with `spring.jpa.hibernate.ddl-auto: validate`; runtime auto-DDL is no longer used to create the schema (DA-07/DA-17, section 10).

### 1.3 Deliverables `[CONFIRMED]`

| ID | Deliverable |
|---|---|
| ENT-01 | API source code. |
| ENT-02 | Instructions for running the project. |
| ENT-03 | Tests for each API (at least happy-path tests). |
| ENT-04 | Additional scripts and configuration files (for example, `Dockerfile` and database scripts). |

### 1.4 Nice-to-have `[OPTIONAL]`

| ID | Feature |
|---|---|
| OPT-01 | API documentation with Swagger. |
| OPT-02 | Containerization with Docker. |
| OPT-03 | JWT authentication: sign-in API to obtain a token and protect the remaining APIs with that token. |

> By decision of the assignment author, OPT-01, OPT-02, and OPT-03 are included in scope (Q-04/Q-08). The `[OPTIONAL]` label is retained only to identify their origin in the assignment.

### 1.5 Remaining assumptions `[ASSUMPTION]`

These points are **not** in the assignment and remain assumptions to allow a complete and consistent implementation. Assumptions already validated were reclassified as `[DECISION]` (see section 11).

| ID | Assumption | Rationale |
|---|---|---|
| P-02 | `Client` is the aggregate root, and a `Document` cannot exist without a `Client`. | Consistent with the one-to-many relationship and required atomicity. |
| P-03 | IDs use `Long` and are generated by the database. | Not specified in the assignment. |
| P-04 | `taxIdentifier` is unique. | Prevents duplicate taxpayer identifiers; not specified in the assignment. |
| P-06 | Both entities have `createdAt`/`updatedAt` audit fields and a `version` field (optimistic locking). | Robustness and concurrency; not required. |
| P-07 | Responses use DTOs (records) and never expose JPA entities. | Good decoupling practice; not required. |
| P-08 | Errors follow RFC 9457 (`ProblemDetail`, `application/problem+json`). | Consistent error format; not required. |
| P-09 | Dates use ISO-8601 (`LocalDate` for `expirationDate`, `Instant` for auditing). | Interoperability; not required. |

Assumptions reclassified as confirmed decisions:

| Original ID | Topic | Reclassified as |
|---|---|---|
| P-01 | Nested Document endpoints | `[DECISION]` Q-06 (section 6.2) |
| P-05 | Uniqueness of `Document.number` | `[DECISION]` Q-02 (global, section 5.3) |
| P-10 | Unpaginated GET for Clients | `[DECISION]` Q-07 |
| P-11 | JWT HS256, in-memory user, and BCrypt | `[DECISION]` Q-04 (required scope) |
| P-12 | Swagger and Docker included | `[DECISION]` Q-08 |

> Note: in the original text, P-04 referred to `taxIdentifier` (not `email`); `email` uniqueness became decision Q-01, now with normalization (`trim` + `lowercase(Locale.ROOT)`) and the `uk_clients_email` constraint. P-04 remains an assumption only for `taxIdentifier`.
>
> Important distinction about P-04: the **name** of the `uk_clients_tax_identifier` constraint is `[DECISION]` (settled by the author), but the **uniqueness rule** for `taxIdentifier` remains `[ASSUMPTION]` (P-04), because it is not in the assignment. The constraint is implemented with that name and a `UNIQUE` effect, but the business rule still needs confirmation; if uniqueness is not required by the assignment, remove the constraint and validation.
>
> The schema is managed by **Flyway** (versioned migrations in `src/main/resources/db/migration`) and validated by Hibernate with `ddl-auto: validate`, following the author's later decision revising Q-09 (DA-07/DA-17; sections 2, 9, and 10). The migrations are the sole source of the schema and must match the JPA entities for `validate` to pass (DA-25).

---

## 2. Architecture decisions `[DECISION]`

| ID | Decision | Alternatives | Rationale |
|---|---|---|---|
| DA-01 | Maven as the build tool. | Gradle | The assignment allows either; Maven is common in Spring interviews and has a predictable structure. |
| DA-02 | Java 21 + Spring Boot 3.5.16. | Spring Boot 4.x | Version confirmed by the assignment author (Q-05): Java 21 is stable; Boot 3.5.16 is the latest patch in the 3.5 line, with a mature ecosystem (springdoc 2.x, jjwt 0.12.x) and good compatibility with LLM code generation. |
| DA-03 | Layered architecture (controller → service → repository). | Hexagonal/CQRS | Simple, sufficient for the domain, and easy to test; avoids unnecessary complexity. |
| DA-04 | `Client` as the aggregate root; `Document` as a child entity. | Documents as an independent aggregate | Ensures atomicity and a shared lifecycle. |
| DA-05 | Feature-based package organization (`client`, `security`). | Strictly layer-based organization | Preserves domain cohesion and reduces cross-dependencies. |
| DA-06 | DTOs as records; manual mappers. | Lombok/MapStruct | Fewer dependencies and generated code; explicit, testable mapping. |
| DA-07 | Schema managed by **Flyway** (versioned SQL migrations) on H2; Hibernate uses `ddl-auto: validate`. | Auto-DDL (`update`/`create`) or `schema.sql` | Revised decision (Q-09/DA-17): Flyway is the sole source of the schema, with migrations in `src/main/resources/db/migration` (`V1__create_clients.sql`, `V2__create_documents.sql`). `validate` fails at startup if the JPA entities differ from the schema, ensuring alignment; `update` is no longer used at runtime. |
| DA-08 | `@Transactional` on the service layer. | Transactions in controllers | Keeps the HTTP layer free of logic and transaction boundaries in the domain layer. |
| DA-09 | `spring.jpa.open-in-view=false`. | Open Session in View | Prevents implicit queries during serialization and requires explicit fetching. |
| DA-10 | Stateless JWT with a custom filter, **included in scope**. | Sessions/form login | Suitable for REST APIs; the assignment author confirmed that login/JWT must be implemented (Q-04), so it is no longer a nice-to-have. |
| DA-11 | Multi-stage Dockerfile. | Docker Maven plugin | Transparent and adds no dependencies to `pom.xml`; Docker was confirmed in scope (Q-08). |
| DA-12 | Swagger through springdoc. | springfox | springdoc is the current standard for Spring Boot 3; Swagger was confirmed in scope (Q-08). |
| DA-13 | Normalize `email` (`trim` + `lowercase`) and apply the `uk_clients_email` constraint; check for duplicates in the service. | Non-unique / unnormalized email | Decision Q-01: prevents duplicate accounts/contacts; the pre-check provides a friendly message and the constraint is the final guarantee; duplicates return `409 Conflict` (DA-19). |
| DA-14 | Global `uk_documents_document_number` constraint on `Document.number`. | Uniqueness per Client `(client_id, document_number)` | Decision Q-02: a document number is a global identifier; duplicates across Clients return `409 Conflict`. |
| DA-15 | `expirationDate` is informational; past dates are accepted. | `@Future` / reject expired dates | Decision Q-03: the API does not validate the date against the present; the field is informational. |
| DA-16 | JWT HS256, in-memory user, and BCrypt; `/auth/login` is public and the remaining endpoints are protected. | No security / optional JWT | Decision Q-04: authentication becomes a scoped requirement, with a simplification suitable for the exercise. |
| DA-17 | Reversal of Q-09: `ddl-auto: update` is replaced by **Flyway + `validate`**. | Keep auto-DDL (`update`) or use `schema.sql` | The author revised the earlier decision: the schema is now versioned and reproducible, and schema↔JPA alignment is checked at startup (`validate`). Risks: migrations must track entity changes (any mismatch fails startup), each change adds another file, and the `clients` → `documents` order must be maintained. This removes `update`'s silent drift (obsolete columns/constraints) and its unsuitability for production. In-memory H2 data does not persist (Q-10), so Flyway runs against an empty schema at every startup. |
| DA-18 | Naming conventions: plural `snake_case` tables (`clients`, `documents`), `snake_case` columns, PK `id`, FK `client_id`; explicit names for constraints and indexes. | Implicit names generated by Hibernate | Stable, readable names shared by migrations and JPA annotations; prevents `validate` mismatches caused by generated names and makes H2 diagnostics easier. Names used: `uk_clients_email`, `uk_clients_tax_identifier`, `uk_documents_document_number`, `fk_documents_client`, `idx_documents_client_id`. The name `uk_clients_tax_identifier` is `[DECISION]`; the corresponding **uniqueness** of `taxIdentifier` remains `[ASSUMPTION]` (P-04), not a confirmed requirement. |
| DA-19 | Normalize `email` (`trim()` + `lowercase(Locale.ROOT)`) before checking for duplicates and persisting; uniqueness is enforced by `uk_clients_email`. | Store email exactly as received | Prevents duplicates caused by case or whitespace. The DTO remains validated with `@Email`; the service pre-check provides a friendly message and the constraint is the final guarantee. Concurrent collisions (`DataIntegrityViolationException`) are translated to `409 Conflict`. |
| DA-20 | Profile-based security policy (partially implemented): `/auth/login` is public and `/api/**` is protected by JWT; Swagger/OpenAPI only in dev (DA-21); H2 Console only in dev; JWT secret required in dev/prod. Actuator/health is not implemented (DA-24). | One configuration for all environments | Reduces the production attack surface without hindering development. Effective configuration is in the `application-*.yaml` files. |
| DA-21 | Swagger/OpenAPI (springdoc) is **disabled by default in production** and **enabled in development**: `springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled` are `false` in base/prod and `true` in dev. | Keep Swagger active in prod (protected by token) or disable only by convention | Resolves the open choice: documentation is not exposed in prod and remains available for use and validation in dev. This does not depend on `SecurityConfig` protecting documentation paths. |
| DA-22 | `JWT_SECRET`, `AUTH_USERNAME`, and `AUTH_PASSWORD_HASH` are provided externally in dev/prod; missing/empty values fail startup and the HS256 secret must be at least 256 bits. BCrypt hash format is not validated at startup. | Versioned secrets or known defaults | Prevents reusable credentials and detects missing configuration. |
| DA-23 | Integration tests use **Flyway + `ddl-auto=validate`**, with the same migrations as runtime; **`create-drop` is not a substitute** for migrations. | `create-drop`/auto-DDL in tests | Tests also validate schema↔JPA alignment: if migrations differ from entities, the test context fails at startup. This keeps tests and production aligned and avoids hiding drift. |
| DA-24 | Actuator in production exposes **only `health`**, with `management.endpoint.health.show-details: never`; `info` is **not** exposed in prod (it may be exposed in dev). | Expose `info`/health details in prod | Minimizes disclosed operational information. `health` remains available for Docker/operations probes, without internal details. |
| DA-25 | Migrations↔JPA alignment criteria: types, columns, nullability, constraints (`uk_*`, `fk_*`, `idx_*`), and entity names must match for `spring.jpa.hibernate.ddl-auto=validate` to pass, both at runtime and in tests. | Rely only on manual inspection or accept drift | Makes alignment verifiable and blocking: any mismatch fails startup. Complements DA-17/DA-18 without duplicating the core decision (Flyway as the schema source). |

---

## 3. Maven dependencies `[DECISION]`

Versions are managed by `spring-boot-starter-parent` 3.5.16, except where noted.

| Dependency | Coordinates | Scope | Required | Purpose |
|---|---|---|---|---|
| Spring Web MVC | `org.springframework.boot:spring-boot-starter-web` | compile | Yes | REST controllers, Jackson, embedded Tomcat. |
| Spring Data JPA | `org.springframework.boot:spring-boot-starter-data-jpa` | compile | Yes | Persistence, repositories, transactions. |
| Bean Validation | `org.springframework.boot:spring-boot-starter-validation` | compile | Yes | DTO validation (`@NotBlank`, `@Email`, etc.). |
| Spring Security | `org.springframework.boot:spring-boot-starter-security` | compile | Yes (JWT in scope) | Filter chain and authentication. |
| Spring Actuator | — | — | Not included | No Actuator/health endpoint is implemented in this version. |
| H2 | `com.h2database:h2` | runtime | Yes | In-memory database. |
| Flyway | `org.flywaydb:flyway-core` | compile | Yes | Versioned migrations; schema managed by Flyway with `ddl-auto: validate` (DA-07/DA-17). |
| Springdoc OpenAPI UI | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.9.1` | compile | Yes (in scope) | Swagger UI and OpenAPI JSON (Q-08). |
| JJWT API | `io.jsonwebtoken:jjwt-api:0.12.7` | compile | Yes (JWT in scope) | API for creating/validating tokens. |
| JJWT Impl | `io.jsonwebtoken:jjwt-impl:0.12.7` | runtime | Yes (JWT in scope) | Implementation. |
| JJWT Jackson | `io.jsonwebtoken:jjwt-jackson:0.12.7` | runtime | Yes (JWT in scope) | Claims serialization. |
| Spring Boot Test | `org.springframework.boot:spring-boot-starter-test` | test | Yes | JUnit 5, Mockito, AssertJ, MockMvc. |
| Spring Security Test | `org.springframework.security:spring-security-test` | test | Yes (JWT in scope) | Security testing support. |
| Spring Boot Maven Plugin | `org.springframework.boot:spring-boot-maven-plugin` | build | Yes | Executable JAR. |

Dependency versions are managed by `spring-boot-starter-parent` 3.5.16 and are not pinned manually, except where noted in the table. Deliberately **omitted** dependencies: Lombok, MapStruct, Testcontainers, and Docker Maven plugins (see the rationale in section 9).

> Flyway and H2 note `[DECISION]`: Spring Boot 3.5.16 manages Flyway **11.7.2** and, in that line, H2 support is included in `flyway-core`. Use **only** the `org.flywaydb:flyway-core` dependency. The `org.flywaydb:flyway-database-h2` artifact is **not** added (it is not among the coordinates managed by this Spring Boot version). No conditional check or additional H2 module is planned.

---

## 4. Package structure `[DECISION]`

Base package: `com.example.clientsapi`.

```text
com.example.clientsapi
├── ClientsApiApplication.java
├── common
│   └── error
│       ├── ApiExceptionHandler.java
│       ├── ResourceNotFoundException.java
│       └── DuplicateResourceException.java
├── config
│   └── OpenApiConfig.java
├── client
│   ├── api
│   │   ├── ClientController.java
│   │   └── DocumentController.java
│   ├── dto
│   │   ├── ClientCreateRequest.java
│   │   ├── ClientUpdateRequest.java
│   │   ├── ClientResponse.java
│   │   ├── ClientSummaryResponse.java
│   │   ├── DocumentCreateRequest.java
│   │   ├── DocumentUpdateRequest.java
│   │   └── DocumentResponse.java
│   ├── domain
│   │   ├── Client.java
│   │   └── Document.java
│   ├── mapper
│   │   ├── ClientMapper.java
│   │   └── DocumentMapper.java
│   ├── repository
│   │   ├── ClientRepository.java
│   │   └── DocumentRepository.java
│   └── service
│       ├── ClientService.java
│       └── DocumentService.java
└── security
    ├── api
    │   ├── AuthController.java
    │   └── dto
    │       ├── LoginRequest.java
    │       └── LoginResponse.java
    ├── AuthService.java
    ├── JwtAuthenticationFilter.java
    ├── JwtService.java
    ├── JwtProperties.java
    ├── SecurityProblemWriter.java
    ├── SecurityConfig.java
    └── SecurityProperties.java
```

Resources:

```text
src/main/resources
├── application.yaml
├── application-dev.yaml
├── application-prod.yaml
└── db
    └── migration
        ├── V1__create_clients.sql
        └── V2__create_documents.sql
```

- The schema is defined by Flyway migrations in `db/migration`; Hibernate validates it with `ddl-auto: validate`. There are no Hibernate-generated scripts or schema-specific configuration classes.
- `application-dev.yaml` and `application-prod.yaml` configure Swagger, H2 Console, and JWT/auth properties. Actuator is not included. The test profile uses `src/test/resources/application-test.yaml` with the same migrations and `validate` (section 8).
- `OpenApiConfig` (springdoc) and `SecurityConfig` (Spring Security/JWT) are the only planned configuration classes.

### Dependency rules

| Layer | May depend on | Must not depend on |
|---|---|---|
| `api` (controllers) | services, DTOs, error handler | repositories, entidades JPA |
| `service` | repositories, domain, mappers, DTOs | controllers, tipos HTTP |
| `repository` | domain | services, DTOs |
| `domain` | JPA/validation annotations | services, controllers, DTOs |
| `mapper` | domain, DTOs | repositories, services |
| `security` | services, properties, domain | controllers |

Overall direction: `api → service → repository/domain`, with no cycles. Controllers validate, delegate, and set status/Location; they contain no business rules.

---

## 5. Data model and relationships

### 5.1 Relationship

```text
Client (1) ──────< Document (N)
        owns
```

- One-to-many relationship `[CONFIRMED]`.
- `Client` is the inverse side: `@OneToMany(mappedBy = "client", cascade = ALL, orphanRemoval = true, fetch = LAZY)`.
- `Document` is the owning side: `@ManyToOne(fetch = LAZY, optional = false)` with `@JoinColumn(name = "client_id", nullable = false)`.
- `cascade = ALL` + `orphanRemoval = true` `[ASSUMPTION]`: cascading persistence and deletion.
- Helper methods `addDocument`/`removeDocument` keep both sides consistent.

### 5.2 `clients` table

| Field | Java type | Column | Constraint/validation | Origin |
|---|---|---|---|---|
| `id` | `Long` | `id` (PK, IDENTITY) | Not null | `[CONFIRMED]` (field) / `[ASSUMPTION]` (type) |
| `firstName` | `String` | `first_name` | `nullable=false`, max 100, `@NotBlank` | `[CONFIRMED]` (field) / `[ASSUMPTION]` (rules) |
| `lastName` | `String` | `last_name` | `nullable=false`, max 100, `@NotBlank` | `[CONFIRMED]` (field) / `[ASSUMPTION]` (rules) |
| `taxIdentifier` | `String` | `tax_identifier` | `nullable=false`, max 32, `@NotBlank`, alphanumeric pattern; `uk_clients_tax_identifier` constraint `[DECISION]` (name), with a **unique** effect that enforces the `[ASSUMPTION]` (P-04) uniqueness assumption | `[CONFIRMED]` (field) / `[DECISION]` (constraint name) / `[ASSUMPTION]` (P-04 uniqueness) |
| `email` | `String` | `email` | `nullable=false`, max 254, `@Email`; **normalized** (`trim` + `lowercase(Locale.ROOT)`) and **unique** (`uk_clients_email`) | `[CONFIRMED]` (field) / `[DECISION]` (Q-01 uniqueness, DA-19 normalization) |
| `phoneNumber` | `String` | `phone_number` | `nullable=false`, max 20, permissive pattern | `[CONFIRMED]` (field) / `[ASSUMPTION]` (rules) |
| `documents` | `List<Document>` | — | one-to-many, LAZY | `[CONFIRMED]` |
| `createdAt` | `Instant` | `created_at` | `@CreationTimestamp`, `updatable=false` | `[ASSUMPTION]` |
| `updatedAt` | `Instant` | `updated_at` | `@UpdateTimestamp` | `[ASSUMPTION]` |
| `version` | `Long` | `version` | `@Version` | `[ASSUMPTION]` |

### 5.3 `documents` table

| Field | Java type | Column | Constraint/validation | Origin |
|---|---|---|---|---|
| `id` | `Long` | `id` (PK, IDENTITY) | Not null | `[CONFIRMED]` (field) / `[ASSUMPTION]` (type) |
| `number` | `String` | **`document_number`** | `nullable=false`, max 50, `@NotBlank`; **globally unique** (`uk_documents_document_number`) | `[CONFIRMED]` (field) / `[DECISION]` (Q-02 global uniqueness) |
| `description` | `String` | `description` | max 255, optional | `[CONFIRMED]` (field) / `[ASSUMPTION]` (rules) |
| `expirationDate` | `LocalDate` | `expiration_date` | `nullable=false`, `@NotNull`; past dates allowed (no `@Future`) | `[CONFIRMED]` (field) / `[DECISION]` (Q-03 no rejection) |
| `client` | `Client` | `client_id` (FK `fk_documents_client`) | `nullable=false`, LAZY, owning side; index `idx_documents_client_id` | `[ASSUMPTION]` |
| `createdAt` | `Instant` | `created_at` | `@CreationTimestamp` | `[ASSUMPTION]` |
| `updatedAt` | `Instant` | `updated_at` | `@UpdateTimestamp` | `[ASSUMPTION]` |
| `version` | `Long` | `version` | `@Version` | `[ASSUMPTION]` |

Notes:

- The `number` column maps to `document_number` because `NUMBER` is a reserved word/type in H2 and can cause DDL errors.
- The `uk_documents_document_number` constraint is global `[DECISION]` (Q-02); there is no `UNIQUE (client_id, document_number)` constraint.
- `clients.email` is normalized (`trim` + `lowercase(Locale.ROOT)`) and has the `uk_clients_email` constraint `[DECISION]` (Q-01/DA-19).
- `tax_identifier` has a constraint named `uk_clients_tax_identifier` `[DECISION]`, but **uniqueness itself is `[ASSUMPTION]` (P-04)** and requires confirmation against the assignment. It must not be presented as a confirmed requirement; if uniqueness is not required, remove the constraint and its pre-check.
- The schema is defined by Flyway migrations (`V1__create_clients.sql`, `V2__create_documents.sql`), not by Hibernate. JPA annotations explicitly mirror the same names (`@Table(uniqueConstraints = @UniqueConstraint(name = "..."))`, `@JoinColumn(name = "client_id", foreignKey = @ForeignKey(name = "fk_documents_client"))`); `ddl-auto: validate` fails at startup if there is a mismatch.
- The `fk_documents_client` foreign key is supported by the `idx_documents_client_id` index created in migration `V2`.
- `expirationDate` has no future-date rule; past dates are accepted `[DECISION]` (Q-03).
- `equals`/`hashCode` are based on `id` and exclude collections and mutable fields.

### 5.4 Naming conventions and constraints `[DECISION]` (DA-18)

| Element | Convention | Examples |
|---|---|---|
| Tables | plural `snake_case` | `clients`, `documents` |
| Columns | `snake_case` | `first_name`, `document_number`, `expiration_date` |
| PK | `id` | `clients.id`, `documents.id` |
| FK | `<entity>_id` | `documents.client_id` |
| Unique | `uk_<table>_<column(s)>` | `uk_clients_email`, `uk_clients_tax_identifier`, `uk_documents_document_number` |
| Foreign key | `fk_<table>_<entity>` | `fk_documents_client` |
| Index | `idx_<table>_<column(s)>` | `idx_documents_client_id` |

- Migrations are the source of truth; the names above are repeated explicitly in JPA annotations so `validate` compares the same schema (DA-25).
- `uk_clients_tax_identifier` is the decided name for the `tax_identifier` constraint; the existence of the uniqueness rule remains `[ASSUMPTION]` (P-04), separate from the name.
- `document_number` is used instead of `number` because the latter is a reserved word/type in H2.
- The `idx_documents_client_id` index supports the foreign key and queries by Client.

---

## 6. REST endpoints

Base path: `/api/v1`. Request and response bodies use JSON. Errors use `application/problem+json`.

### 6.1 Clients `[CONFIRMED]`

| # | Method | Path | Request body | Response | Status |
|---|---|---|---|---|---|
| 1 | `POST` | `/clients` | `ClientCreateRequest` (optional nested Documents) | `ClientResponse` + `Location` | `201` |
| 2 | `GET` | `/clients` | — | `List<ClientSummaryResponse>` | `200` |
| 3 | `GET` | `/clients/{clientId}` | — | `ClientResponse` with Documents | `200` |
| 4 | `PUT` | `/clients/{clientId}` | `ClientUpdateRequest` | `ClientResponse` | `200` |
| 5 | `DELETE` | `/clients/{clientId}` | — | empty | `204` |

### 6.2 Nested Documents, confirmed `[DECISION]` (Q-06)

| # | Method | Path | Request body | Response | Status |
|---|---|---|---|---|---|
| 6 | `GET` | `/clients/{clientId}/documents` | — | `List<DocumentResponse>` | `200` |
| 7 | `POST` | `/clients/{clientId}/documents` | `DocumentCreateRequest` | `DocumentResponse` + `Location` | `201` |
| 8 | `GET` | `/clients/{clientId}/documents/{documentId}` | — | `DocumentResponse` | `200` |
| 9 | `PUT` | `/clients/{clientId}/documents/{documentId}` | `DocumentUpdateRequest` | `DocumentResponse` | `200` |
| 10 | `DELETE` | `/clients/{clientId}/documents/{documentId}` | — | empty | `204` |

The Document's existence is always checked within the Client's scope; a Document belonging to another Client returns `404`.

### 6.3 Login and authentication `[DECISION]` (Q-04)

| # | Method | Path | Request body | Response | Status |
|---|---|---|---|---|---|
| 11 | `POST` | `/auth/login` | `{ "username", "password" }` | `{ "accessToken", "tokenType": "Bearer", "expiresInSeconds" }` | `200` / `401` |

- `/auth/login` is public (`permitAll`); business endpoints under `/api/**` require `Authorization: Bearer <token>`.
- JWT HS256, in-memory user, and BCrypt password; a missing/invalid token returns `401`, while a valid token without permission returns `403`.
- Login/JWT is in scope (Q-04) and cannot be removed without changing the API's expected behavior.
- Profile policy: Swagger/OpenAPI is enabled in development and disabled in production; H2 Console is available only in development. Actuator is not included.
- Fallback: if decision Q-04 changes and JWT is not implemented, authentication must use HTTP Basic/session or the requirement must be explicitly marked as unmet. This document's primary scenario is the implemented JWT flow.

### 6.4 Example — create a Client with Documents

`POST /api/v1/clients`

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "taxIdentifier": "PT123456789",
  "email": "ada@example.com",
  "phoneNumber": "+351912345678",
  "documents": [
    {
      "number": "DOC-001",
      "description": "Passport",
      "expirationDate": "2030-12-31"
    }
  ]
}
```

`201 Created` with `Location: /api/v1/clients/1`

```json
{
  "id": 1,
  "firstName": "Ada",
  "lastName": "Lovelace",
  "taxIdentifier": "PT123456789",
  "email": "ada@example.com",
  "phoneNumber": "+351912345678",
  "documents": [
    {
      "id": 10,
      "number": "DOC-001",
      "description": "Passport",
      "expirationDate": "2030-12-31",
      "createdAt": "2026-10-30T12:00:00Z",
      "updatedAt": "2026-10-30T12:00:00Z",
      "version": 0
    }
  ],
  "createdAt": "2026-10-30T12:00:00Z",
  "updatedAt": "2026-10-30T12:00:00Z",
  "version": 0
}
```

### 6.5 Error codes `[ASSUMPTION]`

| Scenario | Status |
|---|---|
| Field validation / invalid JSON | `400 Bad Request` |
| Client or Document not found | `404 Not Found` |
| Duplicate `taxIdentifier` (`uk_clients_tax_identifier`) — behavior depends on `[ASSUMPTION]` P-04 uniqueness assumption | `409 Conflict` |
| Duplicate `email` after normalization (`uk_clients_email`) | `409 Conflict` |
| Duplicate `document_number` (global, same or different Client; `uk_documents_document_number`) | `409 Conflict` |
| Concurrent uniqueness violation (`DataIntegrityViolationException`) | `409 Conflict` |
| Optimistic locking conflict | `409 Conflict` |
| Invalid credentials / unauthenticated | `401 Unauthorized` |
| Authenticated but not authorized | `403 Forbidden` |
| Unexpected error | `500 Internal Server Error` |

> Past dates are not validation errors: `expirationDate` is informational (Q-03).
>
> The service duplicate pre-check provides a friendly message; the database constraint is the final guarantee. A violation of `uk_clients_email`, `uk_clients_tax_identifier`, or `uk_documents_document_number` (including a race between requests) is translated to `409`/`ProblemDetail`, never `500`.

Error format (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/clients",
  "errors": {
    "email": "must be a well-formed email address",
    "documents[0].number": "must not be blank"
  }
}
```

---

## 7. Transaction strategy

| Operation | Annotation | Rationale |
|---|---|---|
| `ClientService.create` | `@Transactional` | Persists Client + Documents atomically; normalizes `email` and validates unique `email`/`taxIdentifier`. |
| `ClientService.update` | `@Transactional` | Loads, normalizes, validates uniqueness (`email` and `taxIdentifier`), then updates. |
| `ClientService.delete` | `@Transactional` | Cascading deletion of Documents. |
| `ClientService.list/get` | `@Transactional(readOnly = true)` | Read optimization; prevents accidental writes. |
| `DocumentService.add` | `@Transactional` | Adds a Document through Client and persists it; validates globally unique `number`. |
| `DocumentService.update/delete` | `@Transactional` | Updates/deletes and validates globally unique `number`. |
| `DocumentService.list/get` | `@Transactional(readOnly = true)` | Read-only operation. |
| Controllers | none | HTTP layer only. |

Rules:

1. **Atomicity (RNF-02):** `create` builds a `Client`, adds Documents with `client.addDocument(...)`, and calls `clientRepository.saveAndFlush(client)`. `cascade = ALL` persists the Client and Documents in the same transaction; any failure (validation, duplicate, constraint, lock) rolls everything back.
2. **`saveAndFlush`:** used on write paths so database constraints (including `uk_clients_email`, `uk_clients_tax_identifier`, and `uk_documents_document_number`) are evaluated within the service transaction, allowing `DataIntegrityViolationException` to be translated to `409`.
3. **`email` normalization and uniqueness:** the service/mapper applies `trim()` + `lowercase(Locale.ROOT)` before the duplicate pre-check and before persistence. The DTO is still validated with `@Email`; uniqueness is enforced by `uk_clients_email`. `taxIdentifier` follows the same pre-check + constraint pattern, but its uniqueness rule is `[ASSUMPTION]` (P-04): the name `uk_clients_tax_identifier` is `[DECISION]`, while the rule is not a confirmed requirement.
4. **Global uniqueness of `Document.number`:** checked in `DocumentService` (and when creating a Client with Documents) against all Documents, not just those belonging to that Client; duplicates return `409`.
5. **Rollback:** domain exceptions extend `RuntimeException`; Spring rolls back by default.
6. **Self-invocation:** calls to `@Transactional` methods must go through the Spring proxy (do not call them from within the same bean).
7. **Optimistic locking:** both entities use `@Version`; conflicts → `OptimisticLockingFailureException` → `409`.
8. **Lazy/N+1:** `open-in-view=false`; Client `get` uses `@EntityGraph(attributePaths = "documents")`; `list` maps only scalar fields to `ClientSummaryResponse`; entity-to-DTO mapping happens within the transaction.
9. **Isolation:** default propagation (`REQUIRED`). Do not use `REQUIRES_NEW` or pessimistic locking without a proven need.
10. **Dates:** `expirationDate` is not validated against the present; past dates are not rejected (Q-03).
11. **Concurrency:** two simultaneous requests with the same email may both pass the pre-check; only one persists and the other violates `uk_clients_email`. `DataIntegrityViolationException` is caught and translated to `409 Conflict` (`ProblemDetail`), as with `taxIdentifier` and `document_number`. The pre-check is never treated as the uniqueness guarantee.

---

## 8. Test strategy

Requirement RNF-05: at least **one happy-path unit test per endpoint**.

| # | Endpoint | Class / test | Type | Mocks / setup | Main assertions |
|---|---|---|---|---|---|
| 1 | `POST /clients` | `ClientControllerTest.createClient_returns201` | `@WebMvcTest` + MockMvc | `ClientService` mocked | `201`, `Location`, `$.id`, `$.documents[0].number` |
| 2 | `GET /clients` | `ClientControllerTest.listClients_returns200` | `@WebMvcTest` + MockMvc | `ClientService` mockado | `200`, `$[0].firstName`, `$[0].taxIdentifier` |
| 3 | `GET /clients/{id}` | `ClientControllerTest.getClient_returns200` | `@WebMvcTest` + MockMvc | `ClientService` mockado | `200`, `$.id`, `$.documents` |
| 4 | `PUT /clients/{id}` | `ClientControllerTest.updateClient_returns200` | `@WebMvcTest` + MockMvc | `ClientService` mockado | `200`, `$.firstName`, `verify(service).update(...)` |
| 5 | `DELETE /clients/{id}` | `ClientControllerTest.deleteClient_returns204` | `@WebMvcTest` + MockMvc | `ClientService` mockado | `204`, `verify(service).delete(1L)` |
| 6 | `GET /clients/{id}/documents` | `DocumentControllerTest.listDocuments_returns200` | `@WebMvcTest` + MockMvc | `DocumentService` mockado | `200`, `$[0].number` |
| 7 | `POST /clients/{id}/documents` | `DocumentControllerTest.addDocument_returns201` | `@WebMvcTest` + MockMvc | `DocumentService` mockado | `201`, `Location`, `$.number` |
| 8 | `GET /clients/{id}/documents/{docId}` | `DocumentControllerTest.getDocument_returns200` | `@WebMvcTest` + MockMvc | `DocumentService` mockado | `200`, `$.id`, `$.expirationDate` |
| 9 | `PUT /clients/{id}/documents/{docId}` | `DocumentControllerTest.updateDocument_returns200` | `@WebMvcTest` + MockMvc | `DocumentService` mockado | `200`, `$.description` |
| 10 | `DELETE /clients/{id}/documents/{docId}` | `DocumentControllerTest.deleteDocument_returns204` | `@WebMvcTest` + MockMvc | `DocumentService` mockado | `204`, `verify(service).delete(...)` |
| 11 | `POST /auth/login` | `AuthControllerTest.login_returns200` | `@WebMvcTest` + MockMvc | `AuthService` mockado | `200`, `$.accessToken`, `$.tokenType == Bearer` |

Implementation notes:

- Use `@MockitoBean` (Spring Boot 3.4+) and `@AutoConfigureMockMvc(addFilters = false)` in controller slices; test security separately with `@SpringBootTest`.
- Use service tests with `@ExtendWith(MockitoExtension.class)` for business rules (duplicate email, `taxIdentifier`, and global `number`; not found; logical atomicity).
- Use `@DataJpaTest` + H2 for constraints (`uk_clients_email`, `uk_clients_tax_identifier`, `uk_documents_document_number`), cascade persist/delete, orphan removal, and entity graphs. The schema is created by the **same Flyway migrations** with `ddl-auto=validate`, as at runtime.
- Use `@SpringBootTest` for the authentication flow (`401` without a token, `200` with a token) and for **real atomicity**: force a database failure during `create` and verify `clientRepository.count() == 0` (non-transactional test).
- Test profile with dedicated H2, `spring.flyway.enabled=true`, `spring.flyway.locations=classpath:db/migration`, `ddl-auto=validate`, and `open-in-view=false`; each run starts from an empty, deterministic schema.
- **Flyway + `validate` is the chosen integration test strategy** (DA-23). `create-drop` (or any auto-DDL) is **not** used as a substitute for migrations. Integration tests start the context with the same test profile; Flyway applies the same migrations as at runtime and Hibernate validates the result. If the schema differs from the entities, the context fails. Tests therefore explicitly validate schema↔JPA alignment (DA-25), in addition to functional behavior.
- Tests that need data isolation should clean up or use transaction rollback within the test while retaining the Flyway-created schema — do not switch to `create-drop`.
- Coverage: happy-path tests are required; negative tests (`400`, `401`, `404`, `409`) are recommended as a complement.
- Run: `.\mvnw.cmd clean verify` (Windows) or `./mvnw clean verify` (Unix).

Additional tests arising from the settled decisions:

| # | Scenario | Class / test | Type | Main assertion |
|---|---|---|---|---|
| T-01 | Duplicate `email` after normalization on `POST /clients` (e.g. `"  ADA@Example.COM "` vs `ada@example.com`) | `ClientServiceTest.create_duplicateEmail_throwsConflict` / `ClientControllerTest.createClient_duplicateEmail_returns409` | Mockito / `@WebMvcTest` | `409 Conflict` |
| T-02 | Duplicate `email` after normalization on `PUT /clients/{id}` | `ClientServiceTest.update_duplicateEmail_throwsConflict` | Mockito | `409 Conflict` |
| T-03 | Duplicate `document_number` across different Clients | `DocumentServiceTest.add_duplicateNumberAcrossClients_throwsConflict` | Mockito | `409 Conflict` |
| T-04 | Global database constraint `uk_documents_document_number` | `DocumentRepositoryTest.numberMustBeGloballyUnique` | `@DataJpaTest` + H2 (Flyway + `validate`) | `DataIntegrityViolationException` |
| T-05 | JWT required on business endpoints | `SecurityIntegrationTest` | `@SpringBootTest` + MockMvc | `/auth/login` is public (`permitAll`); `/api/**` without token → `401`; with token → `200` |
| T-06 | Past `expirationDate` is accepted | `DocumentServiceTest.create_pastExpirationDate_isAccepted` | Mockito | creation succeeds |
| T-07 | Email normalization on `create`/`update` | `ClientServiceTest.create_normalizesEmail` / `ClientMapperTest` | Mockito / unit | `"  ADA@Example.COM "` is persisted and returned as `ada@example.com` |
| T-08 | Email uniqueness under concurrency | `ClientRepositoryTest.emailMustBeUnique` / `ClientServiceTest.create_concurrentDuplicateEmail_returns409` | `@DataJpaTest` + H2 (Flyway) / integration | one operation succeeds; the other violates `uk_clients_email` → `409` |
| T-09 | Profile-based security policy (dev vs prod) | `SecurityProfileTest` | `@SpringBootTest` with profiles | Swagger enabled in dev and disabled in prod; H2 Console only in dev; Actuator exposes only `health` in prod with `show-details: never`; `JWT_SECRET` required in prod |
| T-10 | Migrations↔JPA alignment | `SchemaMigrationIntegrationTest` | `@SpringBootTest` with test profile (Flyway + `validate`) | context starts with the same migrations as runtime; any type/column/constraint mismatch with entities fails startup |

---

## 9. Consolidated decisions and rationale

| Decision | Rejected alternative | Rationale | Impact/risk |
|---|---|---|---|
| Maven | Gradle | Conventional and predictable. | No relevant impact. |
| Boot 3.5.16 + Java 21 | Boot 4.x | Stable ecosystem, compatible with springdoc 2.x/jjwt 0.12.x. | The 3.5 line is nearing OSS end of life; migrate to 4.x before production. |
| In-memory H2 + Flyway + `ddl-auto=validate` | Auto-DDL (`update`/`create`) or `schema.sql` | Versioned, reproducible schema; schema↔JPA alignment validated at startup (revised Q-09, DA-07/DA-17). | Migrations must track entities; drift causes an explicit startup failure. Data is lost on restart (Q-10), so migrations run against an empty schema. |
| No Lombok/MapStruct | Code generation | Less magic, explicit code, and fewer code-generation failures. | More boilerplate. |
| DTOs + manual mappers | Expose entities | Avoids JSON recursion, lazy loading, and coupling. | Additional mapping code. |
| `open-in-view=false` | OSIV enabled | Requires explicit fetching and prevents silent N+1 queries. | Requires mapping within the transaction. |
| Nested Documents (decided) | Independent `/documents` endpoints | Reflects ownership and atomicity; Q-06 confirmed the scope. | No relevant impact; the path is settled. |
| Stateless JWT + in-memory user (required scope) | Form login/sessions or optional JWT | Suitable for REST; Q-04 confirmed login/JWT is in scope. `/auth/login` is public (`permitAll`) and `/api/**` is protected (DA-20). | An in-memory user is unsuitable for production; business endpoints require a token. Fallback: HTTP Basic/session or explicitly mark the requirement unmet. |
| Multi-stage Dockerfile (in scope) | Maven plugin | Simple, portable, no extra dependencies; Q-08 confirmed Docker. | Image build is more manual. |
| Swagger through springdoc (in scope) | springfox | Current standard for Boot 3; Q-08 confirmed Swagger. Enabled in dev and **disabled by default in prod** (DA-21). | Requires profile configuration; documentation is not exposed in production. |
| Profile-based security | Single configuration | Dev exposes Swagger/H2 Console; prod disables both and requires environment-provided authentication properties/secrets. Actuator is not implemented. | Requires secret management and environment-specific configuration. |
| `JWT_SECRET` required in prod with fail-fast | Optional/insecure default secret | Startup fails if the secret is missing, blank, or invalid for HS256 (DA-22). | Deployment requires the variable; errors are detected early, not at first login. |
| Actuator health-only in prod, without details | Expose `info`/health details | Historical decision, not implemented or a confirmed requirement; no health endpoint exists for probes. | No health endpoint is integrated into the container. |
| Integration tests with Flyway + `validate` | `create-drop`/auto-DDL in tests | Ensures runtime parity and validates schema↔JPA alignment (DA-23/DA-25). | Tests depend on migrations; any drift breaks the test context. |
| Migrations↔JPA checked until `validate` passes | Manual inspection / accept drift | Types, columns, nullability, and constraints must match; startup is the gate (DA-25). | Model changes require a matching migration. |
| `document_number` | `number` column | Avoids an H2 reserved word. | Column name differs from the Java field. |
| Explicit naming (`clients`, `documents`, `uk_*`, `fk_*`, `idx_*`) | Implicit Hibernate names | Stable and readable; migrations and annotations use the same names (DA-18). | More verbose migrations/annotations. |
| Unpaginated Client list (decided) | `Page<T>` | Q-07 confirmed a simple list. | May not scale; can be extended later. |
| Normalized, unique `email` (`uk_clients_email`) | Non-unique email / store as received | Q-01 confirmed uniqueness; normalize with `trim` + `lowercase(Locale.ROOT)` before pre-check and persistence (DA-19). | Clients with the same normalized email are rejected; returned value is normalized. |
| Globally unique `document_number` (`uk_documents_document_number`) | Uniqueness per Client | Q-02 confirmed a global identifier; duplicates across Clients return `409`. | A number used by another Client blocks the operation; may affect UX/data imports. |
| No future-date rule for `expirationDate` | `@Future`/reject expired documents | Q-03 confirmed the date is informational. | Expired documents remain valid to the API; business validation is left to the consumer. |

---

## 10. Schema and profile-based security configuration `[DECISION]` (Flyway + `validate`, revised Q-09)

The schema is created and versioned by **Flyway** from `src/main/resources/db/migration`; Hibernate only validates the result (`spring.jpa.hibernate.ddl-auto: validate`). `ddl-auto: update` is not used at runtime. Flyway runs before Hibernate validation, so any schema↔JPA mismatch explicitly fails startup.

The YAML examples in this section record a configuration planned in the exercise history and are not copies of the runtime files. For current behavior, `src/main/resources/application.yaml`, `application-dev.yaml`, and `application-prod.yaml` take precedence; there is no Actuator configuration.

`src/main/resources/application.yaml` (base, common to all profiles):

```yaml
spring:
  application:
    name: clients-api
  datasource:
    url: jdbc:h2:mem:clientsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration
  h2:
    console:
      enabled: false

# Swagger/OpenAPI disabled by default; only the dev profile enables it (DA-21).
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never

app:
  security:
    jwt:
      # Required in prod; startup fails if missing, blank, or invalid (DA-22).
      secret: ${JWT_SECRET}
```

`src/main/resources/application-dev.yaml`:

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console

springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info

logging:
  level:
    org.flywaydb: debug
    org.hibernate.SQL: debug

app:
  security:
    auth:
      username: ${AUTH_USERNAME}
      password-hash: ${AUTH_PASSWORD_HASH}
    jwt:
      secret: ${JWT_SECRET}
```

`src/main/resources/application-prod.yaml`:

```yaml
spring:
  h2:
    console:
      enabled: false

springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

management:
  endpoints:
    web:
      exposure:
        # Health only; info is not exposed in prod (DA-24).
        include: health
  endpoint:
    health:
      show-details: never

logging:
  level:
    root: info

app:
  security:
    jwt:
      # No default: a missing/invalid value causes startup to fail (DA-22).
      secret: ${JWT_SECRET}
```

- Activation: use `SPRING_PROFILES_ACTIVE=dev` for development and `prod` for production; without an active profile, the base applies the minimum configuration.
- **Swagger/OpenAPI:** `[DECISION]` (DA-21) — **enabled in development** (`springdoc.*.enabled: true`) and **disabled by default in production** (`springdoc.*.enabled: false`, also in the base configuration). There is no need to protect documentation paths in prod because they are not exposed.
- **H2 Console:** enabled only in dev (`/h2-console`); disabled and not exposed in prod.
- **Actuator:** `[DECISION]` (DA-24) — `health` and, at most, `info` in dev; **`health` only** in production, with `management.endpoint.health.show-details: never`. `info` is not exposed in prod. Any additional endpoints must be protected.
- **JWT and credentials:** `[DECISION]` (DA-22) — `JWT_SECRET`, `AUTH_USERNAME`, and `AUTH_PASSWORD_HASH` come from the environment/secret manager in all profiles; the repository has no default credentials or secrets. Startup fails if they are missing or invalid; the HS256 key must have at least 256 bits and the password must be supplied as a BCrypt hash. The user remains in-memory (Q-04).
- **Migrations:** `V1__create_clients.sql` creates `clients` (PK `id`, `uk_clients_email`, `uk_clients_tax_identifier`); `V2__create_documents.sql` creates `documents` (PK `id`, `uk_documents_document_number`, FK `fk_documents_client`, index `idx_documents_client_id`). The `clients` → `documents` order is required; applied migrations are immutable and future changes require a new version (`V3__...`). Migrations are the single source of the schema and must match the JPA entities for `validate` to pass (DA-25).
- The test profile uses the same migrations with `ddl-auto=validate` (section 8); **`create-drop` is not used** as a substitute (DA-23). Integration tests validate schema↔JPA alignment.
- In-memory H2 data does not persist across restarts (Q-10); migrations run against an empty database at each startup.

---

## 11. Resolved questions and open items

Questions Q-01 to Q-10 and the seven open items were answered/resolved by the author and are consolidated in this section. These decisions are binding and reflected in sections 1 to 10. None of the seven items remains undecided.

### 11.1 Assignment questions (Q-01..Q-10)

| ID | Question | Decision | Rationale |
|---|---|---|---|
| Q-01 | Must `email` be unique? | Yes; `clients.email` is normalized (`trim` + `lowercase(Locale.ROOT)`) and has the `uk_clients_email` constraint; duplicates return `409 Conflict`. | Prevents duplicate contacts/accounts; the service pre-check is backed by the constraint, which guarantees uniqueness even under concurrency (DA-13/DA-19). |
| Q-02 | Is `Document.number` unique per Client or globally? | **Globally** unique; the `uk_documents_document_number` constraint makes duplicates return `409 Conflict`. | A document number is a global identifier; the composite `(client_id, document_number)` constraint is replaced (DA-14). |
| Q-03 | Should expired documents be rejected on create/update? | No; `expirationDate` is informational, with no `@Future` rule and no rejection of past dates. | The date is informational; the API does not validate it against the present (DA-15). |
| Q-04 | Is login/JWT required or optional? | Required in scope; keep `POST /auth/login` public (`permitAll`), use JWT HS256, an in-memory user + BCrypt, and protect `/api/**`; configure Swagger/H2/Actuator by profile. | It is no longer a nice-to-have; authentication is part of confirmed scope (DA-10/DA-16/DA-20). |
| Q-05 | Is a specific Java/Spring Boot version required? | Java 21 + Spring Boot 3.5.16, with no migration to 4.x. | Confirmed, stable version compatible with springdoc 2.x and jjwt 0.12.x (DA-02). |
| Q-06 | Should Documents have their own CRUD endpoints? | Yes; nested endpoints at `/api/v1/clients/{clientId}/documents` (POST, GET, GET-by-id, PUT, DELETE). | Promoted from assumption to confirmed decision; reflects ownership and atomicity (P-02). |
| Q-07 | Should GET Clients be paginated? | No; `GET /api/v1/clients` returns `List<ClientSummaryResponse>`. | A simple list without `Page<T>` remains the confirmed choice. |
| Q-08 | Are Docker and Swagger part of the evaluation? | Yes; include both. | Swagger through springdoc (DA-12) and Docker through a multi-stage Dockerfile (DA-11). |
| Q-09 | Should the schema use scripts/migrations instead of auto-DDL? | **Flyway** for versioned migrations + `ddl-auto: validate`; the previous `update` choice is reverted. | Later revision: the schema is versioned, reproducible, and validated at startup. Mismatches are detected explicitly (startup failure) instead of silent drift (DA-07/DA-17, section 10). |
| Q-10 | Must data persist across restarts? | No; keep in-memory H2. | Data does not persist across restarts; suitable for the exercise (RNF-01). |

### 11.2 Resolved open items (author's decisions)

The seven open items were resolved by the decisions below. None remains undecided; the only item that remains an `[ASSUMPTION]` is **`taxIdentifier` uniqueness (P-04)**, which is outside these seven decisions (only the constraint name was decided).

| # | Open item | Decision | Record |
|---|---|---|---|
| 1 | Swagger in production | Disabled by default in production; enabled in development. | DA-21; sections 6.3, 9, and 10 |
| 2 | `JWT_SECRET` in production | Required; the application fails at startup if missing or invalid (fail-fast). | DA-22; sections 9 and 10 |
| 3 | Test strategy | Integration tests with Flyway + Hibernate `ddl-auto=validate`; `create-drop` is not a substitute for migrations. | DA-23; section 8 |
| 4 | Flyway + H2 | Only `org.flywaydb:flyway-core`, without `flyway-database-h2`; Spring Boot 3.5.16 manages Flyway 11.7.2. | Section 3; DA-07/DA-17 |
| 5 | `taxIdentifier` constraint | The name `uk_clients_tax_identifier` was accepted/decided. The **uniqueness rule** remains `[ASSUMPTION]` (P-04), to be confirmed against the assignment. | DA-18; sections 1.5, 5.2, 5.3, and 5.4 |
| 6 | Actuator in production | Not implemented; there is no Actuator dependency or endpoint. | Current status; see DA-24 |
| 7 | Migrations↔JPA | Ensure types, columns, constraints, and JPA entities match for `ddl-auto=validate` to pass. | DA-25; sections 2, 5.4, 8, and 10 |

### 11.3 Additional decisions (DA-18..DA-25)

> Note: decisions DA-18 to DA-25 were made after Q-01..Q-10 were resolved (DA-21..DA-25 also close the seven open items) and do not add new assignment questions; they are recorded here for traceability.

| ID | Topic | Decision | Rationale |
|---|---|---|---|
| DA-18 | Naming/constraints | Plural `snake_case` tables, `snake_case` columns, PK `id`, FK `client_id`, and explicit `uk_*`/`fk_*`/`idx_*` names. The `uk_clients_tax_identifier` name is decided; its uniqueness rule remains P-04. | Stable schema shared by migrations and annotations; `validate` passes without mismatches (section 5.4). |
| DA-19 | Normalized email | Apply `trim()` + `lowercase(Locale.ROOT)` before the pre-check and persistence; `uk_clients_email` enforces uniqueness. | Prevents duplicates caused by case/whitespace; concurrent violations → `409`. |
| DA-20 | Profile-based security | Public login, `/api/**` protected by JWT; Swagger/H2 only in dev; minimal Actuator; JWT secret required from the environment in prod. See DA-21/DA-22/DA-24. | Reduces the attack surface without affecting development (section 10). |
| DA-21 | Profile-based Swagger | Swagger/OpenAPI enabled in dev and disabled by default in prod (springdoc disabled in base/prod). | Resolves the “protected or disabled” choice (section 10). |
| DA-22 | `JWT_SECRET` fail-fast | Secret required in prod; startup fails if missing, blank, or invalid for HS256; no default in prod. | Detects configuration errors at startup (section 10). |
| DA-23 | Tests with Flyway + `validate` | Integration tests use the same migrations and `ddl-auto=validate`; `create-drop` is not a substitute. | Validates schema↔JPA alignment and prevents drift (section 8). |
| DA-24 | Minimal Actuator | Not implemented: there is no Actuator dependency or `health` endpoint. | Historical record; not a confirmed functional requirement or current behavior. |
| DA-25 | Migrations↔JPA alignment | Types, columns, nullability, constraints, and names must match for `validate` to pass at runtime and in tests. | Makes alignment verifiable and blocking (DA-07/DA-17). |

---

## 12. Summary

Layered architecture on Spring Boot 3.5.16 / Java 21, with `Client` as the aggregate root and `Document` as a child entity persisted atomically. The API exposes CRUD for Clients and nested Documents under `/api/v1`, RFC 9457 errors, service-layer transactions, stateless JWT, Swagger, and Docker. Flyway manages the schema and Hibernate validates it with `ddl-auto: validate`; email is normalized and unique, `document_number` is globally unique, and past `expirationDate` values are accepted. `/auth/login` is public, `/api/**` requires JWT, Swagger is enabled in dev and disabled in prod, and H2 Console is enabled only in dev. The database remains in-memory H2 in all profiles. There is no Actuator and no fail-fast validation of the BCrypt format. Integration tests use the migrations and `validate`. `taxIdentifier` uniqueness remains an additional decision beyond the original requirement summarized in section 1.5.
