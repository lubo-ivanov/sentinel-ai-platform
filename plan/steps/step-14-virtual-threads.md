# Step 14 — Parallel LLM Enrichment with Virtual Threads

Parent: [PLAN.md](../PLAN.md) | Design: [concurrency.md](../concurrency.md)

## Goal

Make incident enrichment **asynchronous** and **parallel**. Incident creation returns immediately; enrichment runs in the background, calling the LLM for summary, remediation, and (optionally) a postmortem draft concurrently via Java 21 virtual threads.

## What to build

- An `IncidentEnricher` service that runs out-of-band from incident creation.
- Triggered when a new incident is created — enqueue an enrichment task. Options: ApplicationEventPublisher, an internal Kafka topic, or a polled Postgres table. Pick the simplest that's correct (probably Spring events for now).
- Enrichment uses `Executors.newVirtualThreadPerTaskExecutor()` (or `StructuredTaskScope` if the JDK version supports it as stable) to fan out:
  - `summarize(incident)`
  - `suggestRemediation(incident)`
  - `draftPostmortem(incident)` — optional, gated by config.
- Wait for all (with a timeout). Aggregate results. Update the incident.
- Status field on incident: `enriching`, `enriched`, `enrichment_failed`. Dashboard shows transitions.
- Tune `OLLAMA_NUM_PARALLEL=3` (or higher) on the Ollama container so calls actually run in parallel server-side.
- Metric: `incident_enrichment_duration_seconds{stage}` with stages `summary`, `remediation`, `postmortem`, `total`.
- Measure before/after the Ollama tuning. Capture the numbers for the README.

## What to learn

- Virtual threads — how they differ from platform threads, why they're cheap, when they win (I/O-bound fan-out).
- `StructuredTaskScope` if available — propagates cancellation cleanly when one task fails.
- The Ollama serialization gotcha — virtual threads alone don't parallelize if the server is single-threaded. Tuning is part of the story.
- Async event handling in Spring (`@EventListener` with `@Async`, or virtual-thread executor configured manually).
- Why partial results matter — if remediation fails but summary succeeds, store what we have.

## Things to think about

- **Trigger mechanism.** Spring events → simplest. Internal Kafka topic → more "production-y" but adds infra. Either is fine; document the choice.
- **Cancellation.** If the incident is resolved before enrichment finishes, do we cancel? Probably not worth the complexity; let it complete and write to a resolved incident.
- **Retry on transient LLM failure.** One retry with backoff is reasonable; more becomes a queue. Don't over-engineer.
- **Postmortem optionality.** Postmortem is large output, slow, and arguably premature for an active incident. Gate it behind config; mention "off by default during active incidents" in interviews.

## Done when

- Incident creation returns within tens of milliseconds.
- Background enrichment completes within ~12-15s on the target hardware (vs 30+s sequential).
- Dashboard shows incident immediately, summary fills in within seconds.
- Metrics show parallel call durations overlapping (visible in Grafana histograms).
- One LLM call failing doesn't block the others.
- README captures the before/after numbers.

## Re-enrichment on escalation

When an incident's `anomalyCount` reaches a multiple of `threshold * reenrichMultiplier`, re-enrich with accumulated context:

- **Trigger:** `anomalyCount % (threshold * reenrichMultiplier) == 0` in `updateIncident()`
- **Config:** `sentinel.detection.threshold` (default 5) and `sentinel.enrichment.reenrich-multiplier` (default 3) — re-enrichment fires at anomalyCount = 15, 30, 45...
- **Prompt context:** includes previous `ai_summary` as "prior analysis" + last N `recentMessages` from the anomaly burst
- **Goal:** model can say "incident is escalating — previously isolated to Stripe checkout, now affecting all payment methods"

### `recentMessages` in `Anomaly`

`AbstractBurstRule` collects the last N signal messages as it counts events and passes them in `Anomaly.recentMessages` (cap at 5). Used in:
1. First enrichment prompt — "Recent signal messages" section
2. Re-enrichment prompt — "New signals since last analysis" section

### Re-enrichment prompt shape

```
Prior analysis:
<previous ai_summary>

New signals since last analysis:
- "Connection to Stripe timed out after 30000ms"
- "Stripe API returned 504 Gateway Timeout"

Updated incident details:
- Anomaly count: 15
- ...
```



## Sub-steps

### 14a — `recentMessages` in `Anomaly` + `AbstractBurstRule` collects them ✅
- `DetectionProperties` record: `threshold`, `maxRecentMessages`, `reenrichMultiplier`, `window` — all bound from `sentinel.detection.*` in `application.yml`
- `@ConfigurationPropertiesScan` on `SentinelApplication`
- `Anomaly` gains `recentMessages: List<String>` (6th field)
- `OperationalEvent` gains `message: String` — populated from `signal.getMessage()` in all 5 classification rules + `RuleBasedClassifier.unclassified()`
- `AbstractBurstRule`: drops static `THRESHOLD`/`WINDOW` constants, injects `DetectionProperties`, collects signal messages into `recentMessages` list (null-safe, capped at `maxRecentMessages`, uses `new ArrayList<>()` copy on return to allow nulls)
- All 5 concrete burst rules: constructor updated to accept `DetectionProperties` and pass to `super()`
- 27 tests green

### 14b — `incidents.enrichment` Kafka topic + `IncidentEnrichmentMessage` published by `CorrelationService`
- Add `incidents.enrichment` to `kafka.topics` in `application.yml` (1 partition, RF=1)
- `IncidentEnrichmentMessage` record: `incidentId: UUID`, `anomaly: Anomaly`
- `CorrelationService` publishes to `incidents.enrichment` after `incidentRepository.save(entity)` — replaces synchronous `enrichmentService.enrich()` call
- **Why Kafka over Spring events:** durability — on JVM crash the message is replayed on recovery; incident won't stay `PENDING` forever

### 14c — `IncidentEnrichmentConsumer` calls `IncidentEnrichmentService`
- New consumer extending `AbstractKafkaConsumer<IncidentEnrichmentMessage>`
- Re-fetches `IncidentEntity` by ID, calls `IncidentEnrichmentService.enrich()`, saves result
- Runs on virtual thread executor
### 14d — Parallel fan-out (summary + remediation) via `StructuredTaskScope`
### 14e — `OLLAMA_NUM_PARALLEL=3` in compose + `incident_enrichment_duration_seconds` metric
### 14f — `recentMessages` used in enrichment prompt
### 14g — Re-enrichment trigger on escalation (`anomalyCount % (threshold * reenrichMultiplier) == 0`)
### 14h — `LlmRouter` with severity-based routing

## Things to skip
- Cancellation of in-flight enrichment.
- Incremental result streaming to the UI. Updates on completion are fine.

## Look ahead

After this step the system is functionally complete. Remaining steps are dashboard, notifications, observability polish, and demo. Don't add new core features past here.
