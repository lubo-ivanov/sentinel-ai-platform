# Observability

Parent: [PLAN.md](PLAN.md) | Polished in [step 17](steps/step-17-observability.md)

A demo that shows live metrics during the wow-moment is far more compelling than one that doesn't. Observability is part of the showcase, not a chore.

## The three pillars, scoped

**Metrics:** Micrometer → Prometheus → Grafana. One dashboard with the panels that matter.

**Logs:** Structured JSON via Logback. Correlation IDs propagated across HTTP and Kafka. No fancy log aggregator (Loki/ELK is out of scope); reading container logs is fine.

**Traces:** Skipped. Distributed tracing across 3 services + a sidecar is buildable but takes a week and adds little signal beyond what the dashboard shows. Mention "I'd add OpenTelemetry traces in production" in interviews.

## Metrics worth exposing

**Per producer service:**

- `events_emitted_total{type, service}` — counter
- `business_failures_total{type, service}` — counter
- HTTP request metrics (Spring Boot defaults)

**Per sidecar:**

- `events_received_total` — from local HTTP
- `events_published_total` — to Kafka
- `buffer_depth` — gauge
- `disk_spilled_events_total` — counter
- `events_dropped_total` — counter (stays zero in healthy state)
- `kafka_publish_latency_seconds` — histogram

**Sentinel service:**

- `events_consumed_total{topic}`
- `anomalies_detected_total{rule}`
- `incidents_created_total{severity}`
- `incidents_open` — gauge
- `incident_enrichment_duration_seconds{stage}` — histogram (stage = summary, remediation, total)
- `llm_calls_total{provider, model, status}`
- `llm_call_duration_seconds{provider, model}` — histogram
- `notifications_sent_total{channel}`
- `dedup_hits_total`

## The Grafana dashboard

One pre-built dashboard saved as JSON in the repo, auto-loaded by the Grafana container at startup. Panels:

1. **Event throughput** — events/sec across producers, stacked.
2. **Sidecar buffer depth** — gauge or time-series; spikes during the kill-Kafka demo step.
3. **Open incidents by severity** — current state.
4. **LLM call latency p50/p95** — histogram heatmap is fine.
5. **Anomaly rule activations** — by rule name.
6. **Notification rate** — counter delta.

The dashboard's job is to make the demo's failure-and-recovery story visually obvious. When Kafka dies, panels 1 and 2 react in real time.

## Logging conventions

- **Format:** JSON, one line per event.
- **Required fields:** `timestamp`, `level`, `service`, `correlation_id`, `message`. Plus event-specific fields.
- **Correlation IDs:** generated at producer, passed via Kafka header (`x-correlation-id`), pulled into MDC in the consumer, included in every log line and downstream call.
- **Levels:** ERROR for failures that should page; WARN for degraded states; INFO for incident lifecycle transitions; DEBUG off in production-mode runs.

Don't log every event ingested at INFO — the volume buries the interesting lines. Sample or move to DEBUG.

## Health checks

Each service exposes `/actuator/health` with **real dependency checks**:

- Sentinel: Postgres reachable, Redis reachable, Kafka consumer assigned partitions, Ollama responding (with short timeout).
- Sidecar: Kafka reachable, disk writable, buffer not at hard cap.
- Producers: Sidecar reachable.

The default Spring health indicators handle Postgres/Redis/Kafka. Custom indicators only for Ollama and sidecar reachability.

## Interview talking points

- "I expose business metrics, not just JVM metrics — incidents created by severity, LLM call latency by model, sidecar buffer depth."
- "Correlation IDs propagate from producer through Kafka headers to Sentinel and into LLM call logs. I can trace a single incident end-to-end via grep."
- "Health checks include real dependency probes — Ollama, Kafka assignment, sidecar reachability — not just 'process is up'."
- "Skipped distributed tracing for scope; would add OpenTelemetry first thing in production."
