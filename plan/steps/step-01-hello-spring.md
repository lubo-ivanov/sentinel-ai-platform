# Step 01 — Hello Spring

Parent: [PLAN.md](../PLAN.md)

## Goal

Get a Spring Boot service running with one REST endpoint that lists hardcoded incidents from an in-memory list. End the step with `curl localhost:8080/api/v1/incidents` returning JSON.

## What to build

- New Maven project under `sentinel/`.
- Spring Boot starter, Java 21, Spring Web (MVC).
- One `Incident` record (id, title, severity, createdAt, status).
- One `IncidentController` with `GET /api/v1/incidents` and `GET /api/v1/incidents/{id}`.
- An `IncidentStore` bean wrapping a plain `List<Incident>` seeded with 3 fake incidents.
- One smoke test verifying the endpoint returns the seeded data.

## What to learn

- Spring Boot project setup with Maven.
- Auto-configuration basics — what `@SpringBootApplication` actually does.
- Records as DTOs.
- Constructor injection (no field injection — Spring's modern default).
- `@RestController`, `@GetMapping`, path variables.
- How `MockMvc` (or `WebTestClient`) tests an endpoint without booting Tomcat.

## Why this minimal start

We deliberately skip Postgres, validation, error handling, anomaly detection, and everything else. The only thing this step proves is that the skeleton runs. Every later step adds one concept on top.

## Done when

- `./mvnw spring-boot:run` starts the service cleanly.
- `curl localhost:8080/api/v1/incidents` returns 3 incidents.
- `./mvnw test` passes.
- Committed.

## Things you might be tempted to do but should not

- Set up Docker yet — that's [step 05](step-05-docker-compose.md).
- Add validation annotations. The shape will change soon; premature validation is wasted.
- Split into modules. One module is enough until at least step 04.

## Look ahead

The in-memory list will become thread-unsafe in [step 02](step-02-thread-safe-store.md), then move to Postgres in [step 03](step-03-postgres.md). Don't optimize for either now.
