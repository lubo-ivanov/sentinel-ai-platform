# Step 04 — First Dummy Producer

Parent: [PLAN.md](../PLAN.md)

## Goal

Create the second Spring Boot application — `payment-service`. It simulates business activity and POSTs operational events to Sentinel's HTTP endpoint. End the step with both services running locally and events flowing.

## What to build

- New Maven module `payment-service/` (this implies converting the repo to a multi-module Maven build — do that here).
- Spring Boot app with one scheduled job that simulates payment authorizations every few seconds.
- A simple internal "failure injector" — randomly fails N% of payments, with configurable rates per failure type (provider timeout, declined, retry exhausted).
- An `EventEmitter` that POSTs `OperationalEvent` JSON to a configurable URL (Sentinel for now; sidecar later).
- A new `POST /api/v1/events` endpoint on Sentinel that accepts and stores events (separate table from incidents — these are raw events, not incidents yet).
- Flyway migration for the `operational_events` table.

## What to learn

- Multi-module Maven layout — parent `pom.xml` listing modules, shared config (Java version, Spring Boot version) in the parent.
- Spring's `@Scheduled` for periodic work; `RestClient` (or `WebClient`) for outbound HTTP.
- Externalized configuration — `application.yml` with environment-overridable URLs.
- The shape of an "operational event" — id, type, source, timestamp, payload, severity.

## Things to think about

- **Event schema.** Define it once, share via a small DTO module so both producer and Sentinel use the same types. Avoid copy-paste drift.
- **Event ID generation.** UUIDs at producer-side. Critical for idempotency in [step 10](step-10-idempotency.md).
- **Failure shapes.** Don't just emit "FAILURE" — emit *typed* failures: `PAYMENT_PROVIDER_TIMEOUT`, `PAYMENT_DECLINED`, `PAYMENT_RETRY_EXHAUSTED`. Diversity matters when anomaly detection comes online.

## Done when

- Both services start with `./mvnw -pl sentinel spring-boot:run` and `./mvnw -pl payment-service spring-boot:run`.
- Payment service logs simulated activity.
- Sentinel receives events and stores them.
- A simple integration test exercises the flow end-to-end.

## Things to skip

- Sidecar — [step 11](step-11-sidecar.md). For now the producer talks to Sentinel directly.
- Kafka — [step 06](step-06-kafka.md).
- Multiple producers — [step 09](step-09-three-producers.md).

## Look ahead

The direct HTTP wiring here will be replaced by sidecar → Kafka. But the event schema, failure types, and producer logic stay. Treat the schema as a contract and try not to thrash it later.
