# Contact Center Registry

A multi-tenant registry service for contact-center platform integrations. It keeps track of tenants (accounts/brands) and, for each tenant, the third-party contact-center providers they are wired into — Twilio, Five9, Genesys Cloud, Amazon Connect — along with a reference to the stored credentials and the integration's lifecycle status.

The service is a REST API backed by PostgreSQL, with schema managed by Flyway and a uniform JSON error contract for every failure mode.

## Features

**Tenant management**
- Create a tenant with a display `name` and a globally unique `slug`.
- Slugs must match `^[a-z0-9]+(?:-[a-z0-9]+)*$` — uppercase letters and surrounding spaces are rejected with `400 Bad Request` (enforced by Bean Validation before the service layer runs).
- Fetch a tenant by its UUID. Duplicate slugs are rejected with `409 Conflict`.

**Integration management (scoped per tenant)**
- Full CRUD for integrations under `/api/v1/tenants/{tenantId}/integrations`.
- Supported providers: `TWILIO`, `FIVE9`, `GENESYS_CLOUD`, `AMAZON_CONNECT`.
- Status lifecycle: `ACTIVE` (default on creation) / `DISABLED`.
- `credentialRef` stores only a pointer to the secret (e.g. `vault://demo/twilio`), never the credential itself.
- `displayName` must be unique **within a tenant** — enforced both in the service layer and by a DB unique constraint `(tenant_id, display_name)`.
- Paged listing with optional `status` filter (default: 20 per page, sorted by `createdAt` descending, max page size 100).

**Data integrity**
- Optimistic locking on integrations via a JPA `@Version` column; stale writes return `409 Conflict`.
- Integrations reference their tenant with `ON DELETE RESTRICT` (database-level guard; the API exposes no tenant deletion endpoint).
- All timestamps stored and served in UTC.

**API ergonomics**
- Bean Validation on every request body (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`).
- Global `@RestControllerAdvice` translating validation, not-found, duplicate, malformed-body, and locking failures into a single `ApiError` envelope.
- Actuator endpoints for `health` (incl. liveness/readiness probes) and `info`.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 (Spring WebMVC, Spring Data JPA, Hibernate) |
| Persistence | PostgreSQL |
| Schema migration | Flyway (`flyway-database-postgresql`), `ddl-auto: validate` |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Ops | Spring Boot Actuator, graceful shutdown |
| Build | Apache Maven 3.10 (via `mvnw` wrapper) |
| Tests | JUnit 5, Mockito, Spring Boot test starters, JaCoCo (100% line/branch gate) |
| Container | Docker, `eclipse-temurin` base image |
| CI | GitHub Actions (`./mvnw -B verify` on push/PR) |
| Dev tooling | Spring Boot DevTools |

## Requirements

- Java 17+
- PostgreSQL 14+ (any version supporting `TIMESTAMPTZ`)
- Docker (optional, for containerized runs)

## Getting Started

### 1. Start PostgreSQL

No `docker-compose.yml` is bundled — a one-liner is enough:

```bash
docker run --name cc-registry-db \
  -e POSTGRES_DB=contact_center \
  -e POSTGRES_USER=cc_registry \
  -e POSTGRES_PASSWORD=cc_registry \
  -p 5432:5432 -d postgres:16
```

Connection settings are read from environment variables with local defaults (see `src/main/resources/application.yaml`):

| Variable | Default |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `contact_center` |
| `DB_USER` | `cc_registry` |
| `DB_PASSWORD` | `cc_registry` |

Flyway runs the migrations in `src/main/resources/db/migration/` automatically at startup.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

### 3. Run the tests

```bash
./mvnw verify
```

`verify` also enforces **100% line and branch coverage** via the JaCoCo `check` goal (HTML report: `target/site/jacoco/index.html`). Every `com.exercise.registry` class except the Spring Boot main class must stay fully covered; a coverage regression fails the build.

> `ContactCenterRegistryApplicationTests` boots the full application context (including Flyway), so PostgreSQL must be reachable at `localhost:5432` for `./mvnw verify` to pass. Note that `.github/workflows/ci.yml` does not currently define a Postgres service, so the pipeline needs one added before `./mvnw -B verify` can go green on a clean runner.

### 4. Build a JAR and container image

```bash
./mvnw -B package -DskipTests   # produces target/contact-center-registry-0.0.1-SNAPSHOT.jar
docker build -t contact-center-registry .
docker run -p 8080:8080 \
  --add-host host.docker.internal:host-gateway \
  -e DB_HOST=host.docker.internal \
  contact-center-registry
```

`--add-host host.docker.internal:host-gateway` is required on Linux; Docker Desktop already resolves `host.docker.internal` to the host. If PostgreSQL itself runs in a container on the same Docker network, pass `-e DB_HOST=<postgres-container-name>` instead.

## API Reference

### Tenants

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/tenants` | Create a tenant | `201` |
| `GET` | `/api/v1/tenants/{tenantId}` | Fetch a tenant | `200` |

```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H 'Content-Type: application/json' \
  -d '{"name": "Acme Support", "slug": "acme-support"}'
```

### Integrations

| Method | Path | Description | Success |
| --- | --- | --- | --- |
| `POST` | `/api/v1/tenants/{tenantId}/integrations` | Register an integration | `201` |
| `GET` | `/api/v1/tenants/{tenantId}/integrations` | List (paged, `?status=ACTIVE`) | `200` |
| `GET` | `/api/v1/tenants/{tenantId}/integrations/{id}` | Fetch one integration | `200` |
| `PUT` | `/api/v1/tenants/{tenantId}/integrations/{id}` | Update an integration | `200` |
| `DELETE` | `/api/v1/tenants/{tenantId}/integrations/{id}` | Remove an integration | `204` |

```bash
curl -X POST http://localhost:8080/api/v1/tenants/$TENANT_ID/integrations \
  -H 'Content-Type: application/json' \
  -d '{"displayName": "Primary Voice", "provider": "TWILIO", "credentialRef": "vault://demo/twilio"}'

curl "http://localhost:8080/api/v1/tenants/$TENANT_ID/integrations?status=ACTIVE&page=0&size=20"
```

### Error responses

All errors share one shape:

```json
{
  "timestamp": "2026-10-21T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Tenant slug already exists: acme-support",
  "path": "/api/v1/tenants",
  "details": {}
}
```

| Situation | Status |
| --- | --- |
| Missing tenant / integration | `404 Not Found` |
| Duplicate tenant slug or integration display name | `409 Conflict` |
| Bean Validation failure (field-level `details`) | `400 Bad Request` |
| Malformed JSON / unknown enum value / bad path variable | `400 Bad Request` |
| Concurrent update (`@Version` mismatch) | `409 Conflict` |

### Health

```bash
curl http://localhost:8080/actuator/health
```

## Project Structure

```
contact-center-registry/
├── .github/workflows/ci.yml        # CI: setup Java 17, run ./mvnw -B verify
├── Dockerfile                      # JRE image, non-root user, port 8080
├── pom.xml                         # Dependencies, Java 17, Boot parent 4.1.1
├── mvnw / mvnw.cmd                 # Maven wrapper launchers
└── src/
    ├── main/
    │   ├── java/com/exercise/registry/
    │   │   ├── ContactCenterRegistryApplication.java   # Boot entry point
    │   │   ├── common/                                 # Cross-cutting API plumbing
    │   │   │   ├── ApiError.java                       # Standard error payload
    │   │   │   ├── GlobalExceptionHandler.java         # @RestControllerAdvice
    │   │   │   ├── ResourceNotFoundException.java      # -> 404
    │   │   │   └── DuplicateResourceException.java     # -> 409
    │   │   ├── tenant/                                 # Tenant bounded context
    │   │   │   ├── Tenant.java                         # JPA entity (tenants)
    │   │   │   ├── TenantRepository.java               # existsBySlug / findBySlug
    │   │   │   ├── TenantService.java                  # Slug normalization, dup check
    │   │   │   ├── TenantController.java               # /api/v1/tenants
    │   │   │   ├── CreateTenantRequest.java            # Validated request record
    │   │   │   └── TenantResponse.java                 # Response record
    │   │   └── integration/                            # Integration bounded context
    │   │       ├── Integration.java                    # JPA entity, @Version locking
    │   │       ├── IntegrationProvider.java            # TWILIO, FIVE9, GENESYS_CLOUD, AMAZON_CONNECT
    │   │       ├── IntegrationStatus.java              # ACTIVE, DISABLED
    │   │       ├── IntegrationRepository.java          # Tenant-scoped queries, paging
    │   │       ├── IntegrationService.java             # Business rules
    │   │       ├── IntegrationController.java          # /api/v1/tenants/{id}/integrations
    │   │       ├── CreateIntegrationRequest.java
    │   │       ├── UpdateIntegrationRequest.java
    │   │       └── IntegrationResponse.java
    │   └── resources/
    │       ├── application.yaml                        # Datasource, JPA, Flyway, Actuator
    │       └── db/migration/
    │           ├── V1__create_tenants.sql              # tenants table
    │           └── V2__create_integrations.sql         # integrations table + index/constraints
    └── test/java/com/exercise/registry/
        ├── ContactCenterRegistryApplicationTests.java  # Context load + main() smoke test
        ├── common/GlobalExceptionHandlerTest.java      # Every error handler + edge cases
        ├── tenant/
        │   ├── TenantTest.java                         # Entity + response mapping
        │   ├── TenantServiceTest.java                  # Create/get/duplicate/missing
        │   └── TenantControllerTest.java               # MockMvc: 201/200/400/404
        └── integration/
            ├── IntegrationTest.java                    # Entity, update(), response mapping
            ├── IntegrationServiceTest.java             # Full CRUD + paging + conflicts
            └── IntegrationControllerTest.java          # MockMvc: all endpoints + 400/404/409
```

Package layout follows a feature-first (bounded-context) structure: `tenant` and `integration` each hold their own entity, repository, service, controller, and request/response records, while `common` holds the shared error contract.

## License

No license file is currently included in this repository (`pom.xml` license metadata is empty).
