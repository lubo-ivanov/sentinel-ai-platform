# Progress

Tracker for the 18-step plan. Each step links to its detail doc in `steps/`. Update status as work happens.

Status legend: ✅ done · 🟡 in progress · ⬜ not started

## Steps

| # | Step | Status | Notes |
|---|------|--------|-------|
| 01 | [Hello Spring](steps/step-01-hello-spring.md) | ✅ | Maven scaffold, `Incident` record, `IncidentStore`, `IncidentController`, `@WebMvcTest` smoke test. Spring Boot 3.4.1, Java 21. |
| 02 | [Thread-safe incident store](steps/step-02-thread-safe-store.md) | ✅ | 4-commit progression: unsafe ArrayList → race-proving test → `synchronized` → `ConcurrentHashMap`. POST endpoint added. |
| 03 | [Postgres persistence](steps/step-03-postgres.md) | 🟡 | 03a deps, 03b compose+yml, 03c V1 migration, 03d-pre package refactor (api/domain/repository/service). Next: IncidentEntity + repository. |
| 04 | [First dummy producer](steps/step-04-dummy-producer.md) | ⬜ | Repo becomes multi-module Maven here. |
| 05 | [Docker Compose foundation](steps/step-05-docker-compose.md) | ⬜ | |
| 06 | [Kafka producer/consumer](steps/step-06-kafka.md) | ⬜ | |
| 07 | [Anomaly detection rules](steps/step-07-anomaly-detection.md) | ⬜ | |
| 08 | [Incident correlation](steps/step-08-correlation.md) | ⬜ | |
| 09 | [Three producers, distinct failures](steps/step-09-three-producers.md) | ⬜ | |
| 10 | [Idempotency and dedup](steps/step-10-idempotency.md) | ⬜ | |
| 11 | [Sidecar buffered forwarder](steps/step-11-sidecar.md) | ⬜ | |
| 12 | [LLM integration v1](steps/step-12-llm-v1.md) | ⬜ | |
| 13 | [LLM provider abstraction](steps/step-13-llm-abstraction.md) | ⬜ | |
| 14 | [Parallel LLM enrichment](steps/step-14-virtual-threads.md) | ⬜ | |
| 15 | [Dashboard UI](steps/step-15-dashboard.md) | ⬜ | |
| 16 | [Notification routing](steps/step-16-notifications.md) | ⬜ | |
| 17 | [Observability polish](steps/step-17-observability.md) | ⬜ | |
| 18 | [Demo polish and README](steps/step-18-demo-polish.md) | ⬜ | |

## Cross-cutting decisions made along the way

- **Build tool:** Maven (was Gradle in the original plan; switched at step 01).
- **Spring Boot version:** 3.4.1 (briefly tried 4.0.6 but reverted — pre-release rough edges).
- **Lombok:** allowed pragmatically (original plan banned it; revised at step 01).
- **Git config:** root-level `.gitignore` and `.gitattributes`; per-module ones removed.

## Concepts internalized

- **Step 01:** Spring Boot auto-config, records as DTOs, constructor injection, `@WebMvcTest` slice tests, why field injection is the convention in test classes.
- **Step 02:** `ArrayList.add()` race (read-modify-write), `CountDownLatch` as starting gun + finish line, executor pool + queue model, `synchronized` (atomicity + memory visibility), `ConcurrentHashMap` lock striping, immutable snapshot vs live view trade-off.

## Next up

Step 03 — finish Postgres persistence. Remaining: IncidentEntity, IncidentRepository, IncidentService, controller wiring, delete IncidentStore, Testcontainers @DataJpaTest.
