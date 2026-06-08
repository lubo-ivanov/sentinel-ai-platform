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

## Things to skip

- A general async job framework. Don't build "Quartz Lite."
- Cancellation of in-flight enrichment.
- Incremental result streaming to the UI. Updates on completion are fine.

## Look ahead

After this step the system is functionally complete. Remaining steps are dashboard, notifications, observability polish, and demo. Don't add new core features past here.
