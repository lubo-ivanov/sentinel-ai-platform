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
| 04.5 | [Rules-based classifier](steps/step-04.5-classifier.md) | ✅ | 04.5a types, 04.5b DB + entity + repository, 04.5c RuleEngine, 04.5d PaymentProviderTimeoutRule, 04.5e ClassifierService + wire into SignalIngestService, 04.5f actuator metrics (classifier.duration timer w/ percentiles, classifier.matched + classifier.unclassified counters), 04.5g ClassifierServiceTest. End-to-end verified via payment-service → Sentinel → operational_events with metrics visible on /actuator/metrics. |
| 05 | [Docker Compose foundation](steps/step-05-docker-compose.md) | ✅ | 05a Dockerfiles (multi-stage, JDK build → JRE runtime, Alpine base), 05b compose extended (sentinel + payment-service + postgres healthcheck + named volume + service DNS + env-var overrides for datasource/sentinel URL). 05c scripts and 05d README quick-start deferred until run env grows. Also required: `spring-boot-maven-plugin` repackage goal explicitly bound in both module poms (needed because BOM import doesn't provide the default binding that spring-boot-starter-parent does). |
| 06 | [Kafka producer/consumer](steps/step-06-kafka.md) | 🟡 | Config-driven topic list under `kafka.topics` (each entry has `name`, `partitions`, `replication-factor`). Currently one entry: `signals.raw` (6 partitions, RF=1). `events.classified` added when a consumer for it exists (likely step 07). Decision: hand-written Kafka (level 3), no Spring annotations (`@KafkaListener`/`KafkaTemplate`) — `kafka-clients` only, for educational value. Progress: 06a Kafka in compose (KRaft single-node, ports 19092/39092), 06b `kafka-clients` dep swap + `KafkaConfig` + `KafkaProperties` (`@ConfigurationProperties`), 06c3 payment-service `SignalPublisher` + `RawSignal` record extracted from `SignalEmitter`, 06c4 `SignalEmitter` rewritten to publish via Kafka (RestClient/HTTP removed), test rewritten with Mockito, 06c5a sentinel `KafkaProperties` + `KafkaConfig` (list-based), 06c5b `SignalConsumer` (single-threaded poll loop on named executor thread, `WakeupException` shutdown handling, manual `commitSync` per batch, poison messages logged-and-skipped for now → DLQ in 06e), 06c5c refactor: renamed `SignalConsumer` → `RawSignalConsumer`, extracted `AbstractKafkaConsumer<V>` base class (poll loop + executor + shutdown + deserialize framework; subclass declares topic name/valueType/process), timing knobs (poll timeout, pause, shutdown timeout) externalized to `kafka.consumer.*` YAML with `Duration` binding, 06d `KafkaTopicInitializer` iterates `kafka.topics` and calls `AdminClient.createTopics` idempotently (`TopicExistsException` = success), 06c7 kafka data volume mount in compose, 06c6 e2e verified via `docker compose up --build` — payment-service publishes → `signals.raw` topic (6 partitions confirmed) → sentinel's `RawSignalConsumer` deserializes → `SignalIngestService.ingest("payment-service", signal)` → `raw_signals` and `operational_events` tables populate. Still to do: 06e DLQ. Cleanup: `mockwebserver` dep is now unused in payment-service, `SENTINEL_URL` env var in compose.yml is stale (payment-service no longer uses HTTP). |
| 06f | [Serde polish (optional)](steps/step-06f-serde-polish.md) | ⬜ | Push manual `ObjectMapper` JSON conversion into a custom `Serializer<T>`/`Deserializer<T>` on the Kafka client. Same wire format, cleaner types. Optional — mechanical refactor, no behavior change. |
| 06.5 | [Multi-broker Kafka (prod-like)](steps/step-06.5-multi-broker-kafka.md) | ⬜ | 3 brokers KRaft, replication.factor=3, min.insync.replicas=2, broker-kill failover demo. Deferred until step 06 works on 1 broker. |
| 07 | [Anomaly detection rules](steps/step-07-anomaly-detection.md) | ⬜ | Operates on `OperationalEvent`. |
| 08 | [Incident correlation](steps/step-08-correlation.md) | ⬜ | |
| 09 | [Three producers, distinct failures](steps/step-09-three-producers.md) | ⬜ | |
| 09.5 | [Rule polish + design patterns](steps/step-09.5-rule-polish-patterns.md) | ⬜ | Decorators (NegationGuard, ConfidenceThreshold, Audit, Logging), `Classifier` Strategy interface, structured-hints-first rules. Justified by duplication once 3–4 rules exist. |
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

## Working conventions

Rules for how the author and the assistant collaborate on this project. Read these before starting a session.

- **Author writes the code, assistant guides.** The assistant explains what to change and why; the author implements. Assistant only writes code when explicitly asked.
- **Small chunks.** Break each step into micro-sub-steps (04.5a, 04.5b, ..., 06c1, 06c2, ...). One chunk = one concept = one commit.
- **Verify after each chunk.** Before moving to the next sub-step, the assistant re-reads the touched files, compiles the affected module (`JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw -pl <module> -am compile`), and confirms no typos or missed edits.
- **Short responses.** Assistant keeps replies tight — no long recaps, no exhaustive option surveys. Recommendation-first.
- **One-line commit messages.** No multi-paragraph commit bodies for these incremental steps.
- **Update the plan when the code diverges.** If an architectural or implementation decision differs from the plan, record it in "Cross-cutting decisions" (dated) and update the affected step doc at the end of that step. Don't leave the plan and code out of sync.
- **Update PROGRESS.md after each chunk.** Row status (⬜ → 🟡 → ✅), sub-step notes, "Next up" section — kept live, not batched.
- **Java version:** builds run on Java 21. The author's shell defaults to Java 25 (SAP Machine) which is incompatible with Lombok 1.18.34; prefix Maven commands with `JAVA_HOME=$(/usr/libexec/java_home -v 21)` or configure IntelliJ Project SDK to 21.
- **Extract constants and helpers in tests.** Prefer named constants over inline magic values; extract common setup into helpers rather than duplicating.
- **Lombok pragmatically.** `@Slf4j` for loggers, `@RequiredArgsConstructor` for final-field constructors, `@Getter`/`@Setter` on JPA entities. Skip on records — they already generate accessors.
- **Compose-only run mode (for now).** No dev-from-IDE dual config. Everything runs via `docker compose up --build`.

## Cross-cutting decisions made along the way

- **Build tool:** Maven (was Gradle in the original plan; switched at step 01).
- **Spring Boot version:** 3.4.1 (briefly tried 4.0.6 but reverted — pre-release rough edges).
- **Lombok:** allowed pragmatically (original plan banned it; revised at step 01).
- **Git config:** root-level `.gitignore` and `.gitattributes`; per-module ones removed.
- **Two-schema architecture (2026-07-17):** `RawSignal` (external, loose) + `OperationalEvent` (internal, strict) with a classifier bridging them. Added step 04.5 (rules-based classifier) and step 12.5 (LLM classifier fallback). Rules-first on the hot path, LLM only on the cold-batch tail with human-in-the-loop.
- **No shared DTO jar between producers and Sentinel (2026-07-17):** wire contract is JSON. Producers stay language-agnostic; Sentinel deserializes into its own internal classes. Dropped the previously-planned `events-api` module.
- **Multi-module Maven layout (in progress at 04a):** parent aggregator pom with BOM import (`spring-boot-dependencies` via `dependencyManagement`, not `<parent>` inheritance). Reason: composable, non-Boot children remain possible, more explicit.
- **Hand-written Kafka client (2026-07-23):** step 06 uses `org.apache.kafka:kafka-clients` directly — plain `KafkaProducer` + hand-written poll-loop consumer wrapped in an `ExecutorService`. No `spring-kafka`, no `KafkaTemplate`, no `@KafkaListener`. Reason: educational — the plan (`step-06-kafka.md`, `tech-stack.md`) originally said Spring Kafka; deviating so the interview walkthrough shows real Kafka client API knowledge (poll timeout, `commitSync`, `WakeupException` on shutdown) rather than framework magic. Step 06 doc will be rewritten at end of step to reflect actual approach.
- **`signals.raw` partition count = 6 (2026-07-24):** default topic auto-creation gives 1 partition, which is toy-scale. Six chosen because: divisible by 1/2/3/6 (clean rebalancing demo), fits 3+ eventual producers with headroom, small enough that broker overhead is negligible. `replicationFactor=1` for now (single broker) → bumps to 3 in step 06.5.
- **Partition key strategy (2026-07-24):** currently `signal.id` (random distribution — no ordering guarantees per producer). Will switch to `source` field in step 09 when 3 producers exist — preserves per-producer ordering, spreads load across partitions naturally. Requires adding `source` to `RawSignal` (deferred; see `architecture.md` line 60 — plan already treats `source` as a first-class field).
- **Kafka security deferred (2026-07-28):** step 06 uses `PLAINTEXT` on the compose bridge network — no TLS, no SASL, no ACLs. Production would use `security.protocol=SASL_SSL` + `sasl.mechanism=SCRAM-SHA-512` (or mTLS) with credentials from a secret manager, plus per-service ACLs. Not implemented because: (1) requires cert generation + JKS files, (2) triples compose complexity, (3) minimal incremental learning value. Documented as a talking point; may add to step 17 (observability polish) if time permits.
- **Serde: String + Jackson, polish deferred (2026-07-28):** producer manually serializes `RawSignal` → JSON string via `ObjectMapper` before Kafka's `StringSerializer` puts it on the wire. Consumer reverses. Custom `Serializer<T>`/`Deserializer<T>` implementations considered but deferred to a future polish step — mechanical refactor, same wire format. Avro + Schema Registry considered as an interview-flex; deferred permanently because plan (`architecture.md` line 60) explicitly wants JSON on the wire (language-agnostic producers, no shared jar). See new step 06f (serde polish, optional).

## Concepts internalized

- **Step 01:** Spring Boot auto-config, records as DTOs, constructor injection, `@WebMvcTest` slice tests, why field injection is the convention in test classes.
- **Step 02:** `ArrayList.add()` race (read-modify-write), `CountDownLatch` as starting gun + finish line, executor pool + queue model, `synchronized` (atomicity + memory visibility), `ConcurrentHashMap` lock striping, immutable snapshot vs live view trade-off.
- **Step 03:** DTO/entity separation, `@Transactional` at service layer, Hibernate `@Generated` for DB-managed timestamps, `@Version` for optimistic locking, `@MockitoBean` for Spring test slice mocking.
- **Between 03 and 04 (architecture pivot):** why LLMs belong on the cold path not the hot path; structure at the edge vs intelligence in the middle; the boundary between wire contracts (JSON) and code contracts (jars); why shared DTO jars can imply false coupling.

## Next up

**Step 06e — DLQ for poison messages.** Add `signals.dlq` topic (config-driven, 1 partition, RF=1 for now). When `RawSignalConsumer.handleRecord` catches a deserialization or processing exception, republish the raw record bytes to `signals.dlq` (with headers: original topic, offset, partition, error class, error message, timestamp) before advancing the offset. Currently poison messages are logged-and-skipped, silently vanishing — DLQ makes them inspectable and preserves the "no data loss" property.

Design questions for 06e:
- Where does the DLQ producer live? A shared `DlqPublisher` component available to any consumer, injected into `AbstractKafkaConsumer` as an optional collaborator.
- Should we retry N times before DLQ-ing? Kafka's `errors.deadletterqueue.retries` pattern says yes; for our scale probably not — deser failures don't get better on retry, and processing failures are usually deterministic (DB constraint violation, etc.).

Then step 06 is done — the plan doc `step-06-kafka.md` needs a full rewrite reflecting the hand-written approach actually taken.
