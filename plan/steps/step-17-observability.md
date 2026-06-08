# Step 17 — Observability Polish

Parent: [PLAN.md](../PLAN.md) | Design: [observability.md](../observability.md)

## Goal

Make the system *visibly* observable. Prometheus + Grafana running in compose, one curated dashboard that tells the demo's story, structured logs with correlation IDs threaded through, real health checks.

## What to build

- Prometheus added to `docker-compose.yml`. Scrape configs targeting each service's `/actuator/prometheus`.
- Grafana added to compose. Pre-provisioned with Prometheus as data source and one dashboard JSON in the repo (so it appears automatically — no manual setup).
- The dashboard panels (per [observability.md](../observability.md)):
  - Event throughput stacked by service.
  - Sidecar buffer depth and disk-spilled count.
  - Open incidents by severity.
  - LLM call latency p50/p95.
  - Anomaly rule activations.
  - Notification rate.
- Correlation IDs:
  - Generated at producer (UUID per simulated business action).
  - Propagated via Kafka header `x-correlation-id`.
  - Pulled into MDC at consumer.
  - Included in every log line via Logback JSON encoder.
  - Forwarded into LLM call logs.
- Structured JSON logs in all services — same schema everywhere.
- Real health checks — Postgres, Redis, Kafka assignment, Ollama probe, sidecar reachability.
- README section showing screenshots of the Grafana dashboard.

## What to learn

- Prometheus scrape configuration and label cardinality discipline.
- Grafana dashboard provisioning via JSON files mounted into the container.
- MDC (Mapped Diagnostic Context) for thread-local log fields.
- Logback JSON encoder configuration.
- Custom Spring Boot health indicators when defaults aren't enough.

## Things to think about

- **Label cardinality.** `events_emitted_total{type, service}` is fine. `events_emitted_total{event_id}` is a Prometheus killer — never label with high-cardinality fields.
- **Dashboard simplicity.** Six panels, not sixteen. Each panel must answer a question someone watching the demo would ask.
- **Log volume.** Don't INFO-log every event. Sample, or move to DEBUG. INFO should be incident lifecycle and significant transitions.
- **Correlation ID at sidecar.** Sidecar should preserve the producer's correlation ID, not generate its own.

## Done when

- Open Grafana, see live data populating the dashboard during a demo run.
- Grep one correlation ID across all service logs — see the full flow from producer through sidecar through Sentinel through LLM.
- Health endpoints accurately reflect dependency state (kill Postgres, see Sentinel go unhealthy).
- README has dashboard screenshots.

## Things to skip

- Distributed tracing (OpenTelemetry). Mention in interviews; don't build.
- Log aggregation (Loki, ELK). Container logs are fine.
- Alerting rules in Prometheus. The demo doesn't need them.

## Look ahead

This step elevates the project from "works" to "demonstrably operational." The dashboard is what the demo video focuses on during the failure-recovery sequence.
