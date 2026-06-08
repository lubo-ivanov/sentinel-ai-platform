# Architecture

Parent: [PLAN.md](PLAN.md)

## High-level shape

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Payment    │      │   Order     │      │  Inventory  │
│  Service    │      │  Service    │      │  Service    │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │ HTTP                │ HTTP                │ HTTP
       ▼                     ▼                     ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Sidecar    │      │  Sidecar    │      │  Sidecar    │
│ (buffer +   │      │ (buffer +   │      │ (buffer +   │
│  retry +    │      │  retry +    │      │  retry +    │
│  spill)     │      │  spill)     │      │  spill)     │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                     │                     │
       └─────────────────────┼─────────────────────┘
                             ▼
                       ┌──────────┐
                       │  Kafka   │
                       │ (events) │
                       └─────┬────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │       Sentinel Service       │
              │  ┌────────────────────────┐  │
              │  │ Ingest → Detect →      │  │
              │  │ Correlate → Enrich →   │  │
              │  │ Notify                 │  │
              │  └────────────────────────┘  │
              └──┬─────────┬─────────────┬───┘
                 │         │             │
                 ▼         ▼             ▼
            ┌────────┐ ┌────────┐  ┌──────────┐
            │Postgres│ │ Redis  │  │  Ollama  │
            │incidents│ │counters│  │  (LLM)  │
            └────────┘ └────────┘  └──────────┘
                             │
                             ▼
                       ┌──────────┐
                       │Dashboard │
                       │  (HTMX)  │
                       └──────────┘
```

## Components

**Dummy producers (Payment, Order, Inventory).** Each is a small Spring Boot app that simulates a domain — payment authorizations, order checkout transitions, stock reservations. They emit operational events when failures, retries, or anomalies happen in their simulated business logic. Each is a separate service so the demo has multiple sources of events with different shapes. See [step 09](steps/step-09-three-producers.md) for failure-mode design.

**Sidecar.** Runs alongside each producer. Producer POSTs events to `localhost:<sidecar-port>` and forgets. Sidecar batches, retries, and spills to disk if the downstream is unreachable. The sidecar's only job is making "fire-and-forget" actually reliable. See [sidecar.md](sidecar.md).

**Kafka.** Single topic for raw events (`operational.events.raw`) initially. Later steps may add `anomaly.detected`, `incident.created`, etc., as internal Sentinel topics — or keep them in-process as method calls. The boundary between "Kafka topic" and "in-process pipeline stage" is a design decision revisited at [step 08](steps/step-08-correlation.md).

**Sentinel service.** One Spring Boot app with internal modules:

- **Ingest** — Kafka consumer, schema validation, dedup by event ID.
- **Detect** — sliding-window threshold rules over Redis counters. Emits anomalies.
- **Correlate** — groups anomalies into incidents, manages incident state in Postgres.
- **Enrich** — calls the LLM (in parallel via virtual threads) for summary, remediation, optional postmortem.
- **Notify** — routes to console / mock webhook with dedup and cooldown.
- **API** — REST endpoints for the dashboard.

Keeping these as modules in one service (not 5 separate services) is deliberate — see [PLAN.md scope decisions](PLAN.md#scope-decisions).

**Storage.**

- **Postgres** — incidents, notification history, audit trail.
- **Redis** — sliding-window counters, dedup keys with TTL, notification cooldowns.
- **Disk (sidecar)** — local spillover when Kafka is unreachable.

**LLM (Ollama).** Local model server in docker-compose. Provider-abstracted so a Claude or mock implementation can be swapped in. See [llm-integration.md](llm-integration.md).

**Dashboard.** Minimal UI listing incidents with ack/resolve actions. Decided at [step 15](steps/step-15-dashboard.md).

## Data flow (happy path)

1. Payment service simulates a provider timeout and emits a `PAYMENT_PROVIDER_TIMEOUT` event.
2. Sidecar receives via local HTTP, batches with other events, publishes to Kafka.
3. Sentinel ingest module consumes the event, validates, dedupes, writes raw event for audit.
4. Detect module increments a Redis counter `payment_failures:provider=stripe:1m`. If counter crosses threshold, emits an anomaly.
5. Correlate module checks for an open incident matching the anomaly fingerprint. Either creates a new incident in Postgres or updates an existing one.
6. Enrich module fans out parallel LLM calls for summary + remediation. Updates incident when results arrive.
7. Notify module sends to console + mock webhook, respecting per-incident cooldown.
8. Dashboard polls the API (or receives via SSE) and displays the incident.

## Failure paths the demo highlights

- **Kafka down** → sidecars buffer to memory, then to disk after threshold. When Kafka returns, events flush in order.
- **LLM slow/unavailable** → incident appears immediately with status "summary pending"; enrichment fills in asynchronously. If LLM fails entirely, incident still exists with a "no AI summary available" placeholder.
- **Duplicate events** → idempotency layer (event ID + Redis SET NX with TTL) drops duplicates silently. Visible in metrics.

## Design boundaries to defend in interviews

- **Why one Sentinel service, not five?** Solo project, no team boundaries to enforce, no independent scaling needs. Modular monolith first; split when there's a real reason.
- **Why a sidecar at all?** Decouples producer reliability from transport reliability. Producers don't carry retry/buffer logic. Real pattern (OTel Collector, Vector, Fluent Bit). See [sidecar.md](sidecar.md).
- **Why Kafka and not just HTTP?** Replay, multiple consumers, decoupling, real distributed-systems talking surface (offsets, consumer groups, DLQ).
- **Why local LLM?** Zero ongoing cost, runs offline, demonstrates provider-agnostic design via the abstraction layer. See [llm-integration.md](llm-integration.md).
