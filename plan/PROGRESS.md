# Progress

Tracker for the 18-step plan. Each step links to its detail doc in `steps/`. Update status as work happens.

Status legend: ✅ done · 🟡 in progress · ⬜ not started

## Steps

| # | Step | Status | Notes |
|---|------|--------|-------|
| 01 | [Hello Spring](steps/step-01-hello-spring.md) | ✅ | Maven scaffold, `Incident` record, `IncidentStore`, `IncidentController`, `@WebMvcTest` smoke test. Spring Boot 3.4.1, Java 21. |
| 02 | [Thread-safe incident store](steps/step-02-thread-safe-store.md) | ✅ | 4-commit progression: unsafe ArrayList → race-proving test → `synchronized` → `ConcurrentHashMap`. POST endpoint added. |
| 03 | [Postgres persistence](steps/step-03-postgres.md) | ✅ | 03a deps, 03b compose+yml, 03c V1 migration, 03d-pre package refactor (api/domain/repository/service), 03d IncidentEntity + repository + IncidentService, DTO/entity split, IncidentStore deleted. |
| 04 | [First dummy producer](steps/step-04-dummy-producer.md) | ✅ | 04a multi-module Maven, 04b SignalController + RawSignal DTO + ingest service, 04c payment-service producer (RestClient + @Scheduled + console control), 04d integration test intentionally skipped (manual verification + unit tests deemed sufficient). |
| 04.5 | [Rules-based classifier](steps/step-04.5-classifier.md) | 🟡 | 04.5a types done (FailureType, Severity, OperationalEvent record, ClassificationRule interface). 04.5b DB + entity + repository done (V3 migration, OperationalEventEntity, OperationalEventRepository). **Next: 04.5c — RuleEngine.** |
| 05 | [Docker Compose foundation](steps/step-05-docker-compose.md) | ⬜ | |
| 06 | [Kafka producer/consumer](steps/step-06-kafka.md) | ⬜ | Two topics now: `signals.raw`, `events.classified`. |
| 07 | [Anomaly detection rules](steps/step-07-anomaly-detection.md) | ⬜ | Operates on `OperationalEvent`. |
| 08 | [Incident correlation](steps/step-08-correlation.md) | ⬜ | |
| 09 | [Three producers, distinct failures](steps/step-09-three-producers.md) | ⬜ | |
| 10 | [Idempotency and dedup](steps/step-10-idempotency.md) | ⬜ | Applies to both signals and events. |
| 11 | [Sidecar buffered forwarder](steps/step-11-sidecar.md) | ⬜ | |
| 12 | [LLM integration v1](steps/step-12-llm-v1.md) | ⬜ | Enrichment of incidents. |
| 12.5 | [LLM classifier fallback](steps/step-12.5-llm-classifier-fallback.md) | ⬜ | New step. Batch LLM proposes classifications for `UNCLASSIFIED`; human accepts → new rule. |
| 13 | [LLM provider abstraction](steps/step-13-llm-abstraction.md) | ⬜ | |
| 14 | [Parallel LLM enrichment](steps/step-14-virtual-threads.md) | ⬜ | |
| 15 | [Dashboard UI](steps/step-15-dashboard.md) | ⬜ | Adds classifier triage view. |
| 16 | [Notification routing](steps/step-16-notifications.md) | ⬜ | |
| 17 | [Observability polish](steps/step-17-observability.md) | ⬜ | Classifier metrics too. |
| 18 | [Demo polish and README](steps/step-18-demo-polish.md) | ⬜ | |

## Cross-cutting decisions made along the way

- **Build tool:** Maven (was Gradle in the original plan; switched at step 01).
- **Spring Boot version:** 3.4.1 (briefly tried 4.0.6 but reverted — pre-release rough edges).
- **Lombok:** allowed pragmatically (original plan banned it; revised at step 01).
- **Git config:** root-level `.gitignore` and `.gitattributes`; per-module ones removed.
- **Two-schema architecture (2026-07-17):** `RawSignal` (external, loose) + `OperationalEvent` (internal, strict) with a classifier bridging them. Added step 04.5 (rules-based classifier) and step 12.5 (LLM classifier fallback). Rules-first on the hot path, LLM only on the cold-batch tail with human-in-the-loop.
- **No shared DTO jar between producers and Sentinel (2026-07-17):** wire contract is JSON. Producers stay language-agnostic; Sentinel deserializes into its own internal classes. Dropped the previously-planned `events-api` module.
- **Multi-module Maven layout (in progress at 04a):** parent aggregator pom with BOM import (`spring-boot-dependencies` via `dependencyManagement`, not `<parent>` inheritance). Reason: composable, non-Boot children remain possible, more explicit.

## Concepts internalized

- **Step 01:** Spring Boot auto-config, records as DTOs, constructor injection, `@WebMvcTest` slice tests, why field injection is the convention in test classes.
- **Step 02:** `ArrayList.add()` race (read-modify-write), `CountDownLatch` as starting gun + finish line, executor pool + queue model, `synchronized` (atomicity + memory visibility), `ConcurrentHashMap` lock striping, immutable snapshot vs live view trade-off.
- **Step 03:** DTO/entity separation, `@Transactional` at service layer, Hibernate `@Generated` for DB-managed timestamps, `@Version` for optimistic locking, `@MockitoBean` for Spring test slice mocking.
- **Between 03 and 04 (architecture pivot):** why LLMs belong on the cold path not the hot path; structure at the edge vs intelligence in the middle; the boundary between wire contracts (JSON) and code contracts (jars); why shared DTO jars can imply false coupling.

## Next up

**Step 04.5c — Rule engine.** Two files under `sentinel/src/main/java/com/sentinelai/sentinel/classifier/`:

1. **`RuleEngine.java`** — `@Component` that receives `List<ClassificationRule>` via constructor injection (Spring auto-collects all `@Component`-annotated rules). Public method `classify(RawSignalEntity)` iterates the list first-match-wins; returns an `UNCLASSIFIED` `OperationalEvent` if no rule matches. Startup log lists loaded rules.
2. **Skip the `NoopRule`** placeholder — go straight to 04.5d (`PaymentProviderTimeoutRule`, real logic, unit-tested).

After 04.5c/d: **04.5e** wires `ClassifierService` into `SignalIngestService` (sync, same transaction). Then **04.5f** actuator metrics, **04.5g** end-to-end signal→event service test.

Design decisions already agreed:
- Synchronous classification in the ingest transaction (Kafka comes in step 06).
- No LLM in the classifier at 04.5 — LLM is cold-path only, step 12.5.
- Annotated Java rules (Option A), not YAML.
