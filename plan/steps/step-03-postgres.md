# Step 03 — Postgres Persistence

Parent: [PLAN.md](../PLAN.md)

## Goal

Replace the in-memory store with PostgreSQL. Schema managed by Flyway. Repositories via Spring Data JPA. End-to-end CRUD against a real database.

## What to build

- Add Postgres driver, Spring Data JPA, Flyway dependencies.
- `V1__create_incidents.sql` — incidents table with id (UUID), title, severity, status, created_at, updated_at, version (for optimistic locking).
- `IncidentEntity` mapped to the table. Keep the `Incident` record as a DTO at the API boundary; don't expose JPA entities directly.
- `IncidentRepository extends JpaRepository`.
- Refactor service layer: `IncidentStore` becomes `IncidentService` calling the repository.
- Local Postgres via Docker for development (foreshadowing [step 05](step-05-docker-compose.md)).
- A `@DataJpaTest` for the repository using Testcontainers Postgres.

## What to learn

- Flyway migration discipline — never modify a committed migration; add a new one instead.
- The DTO/entity separation and why exposing JPA entities through the API is a footgun (lazy-loading, accidental N+1s, leaking persistence concerns).
- `@Transactional` boundaries — service layer, not controller, not repository.
- Optimistic locking with `@Version`.
- Testcontainers basics — `@Container`, `@DynamicPropertySource`.

## Things to think about

- **UUIDs vs auto-increment IDs.** Pick UUID for distributed-system friendliness; mention the trade-off in interviews (index size, sortability — UUIDv7 fixes both).
- **Timestamps in UTC.** Use `Instant`, not `LocalDateTime`. Always.
- **created_at / updated_at via DB defaults vs JPA listeners.** Pick one; don't mix.

## Done when

- Service starts, applies migrations on first run.
- POST and GET hit Postgres.
- Repository test passes against Testcontainers Postgres.
- The in-memory store class is deleted (not commented out — *deleted*; git remembers).

## Things to skip

- Dashboard — [step 15](step-15-dashboard.md).
- Audit tables / event sourcing. Out of scope.
- Connection pool tuning. HikariCP defaults are fine.

## Look ahead

The schema will grow as the project does — anomalies, notifications, audit. Each addition is its own migration. Resist the urge to "design the whole schema up front." Add columns as steps need them.
