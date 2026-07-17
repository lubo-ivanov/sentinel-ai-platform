# Progress

Tracker for the 18-step plan. Each step links to its detail doc in `steps/`. Update status as work happens.

Status legend: ✅ done · 🟡 in progress · ⬜ not started

## Steps

| # | Step | Status | Notes |
|---|------|--------|-------|
| 01 | [Hello Spring](steps/step-01-hello-spring.md) | ✅ | Maven scaffold, `Incident` record, `IncidentStore`, `IncidentController`, `@WebMvcTest` smoke test. Spring Boot 3.4.1, Java 21. |
| 02 | [Thread-safe incident store](steps/step-02-thread-safe-store.md) | ✅ | 4-commit progression: unsafe ArrayList → race-proving test → `synchronized` → `ConcurrentHashMap`. POST endpoint added. |
| 03 | [Postgres persistence](steps/step-03-postgres.md) | ✅ | 03a deps, 03b compose+yml, 03c V1 migration, 03d-pre package refactor (api/domain/repository/service), 03d IncidentEntity + repository + IncidentService, DTO/entity split, IncidentStore deleted. |
| 04 | [First dummy producer](steps/step-04-dummy-producer.md) | 🟡 | Rescoped: producers emit `RawSignal` (loose), not `OperationalEvent`. Multi-module Maven conversion happens here (04a). No shared DTO jar. |
| 04.5 | [Rules-based classifier](steps/step-04.5-classifier.md) | ⬜ | New step. `RawSignal` → `OperationalEvent` via annotated Java rules; unmatched → `UNCLASSIFIED`. |
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

Step 04 — first dummy producer. Sub-step **04a** is the multi-module Maven conversion:

1. Move Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) from `sentinel/` to repo root. **✅ done.**
2. Create root parent `pom.xml` (`packaging: pom`, BOM import for `spring-boot-dependencies`, `pluginManagement` for `spring-boot-maven-plugin`). **✅ done.**
3. Slim `sentinel/pom.xml` — set parent to the root pom, drop inherited groupId/version/java.version, declare `spring-boot-maven-plugin` in child's `<build>`.
4. Verify: `./mvnw clean verify` from repo root builds sentinel through the reactor.
5. Verify: `./mvnw -pl sentinel spring-boot:run` still works (with Postgres up via compose).
6. Commit.
