# Architecture

Parent: [PLAN.md](PLAN.md)

## High-level shape

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Payment    │      │   Order     │      │  Inventory  │
│  Service    │      │  Service    │      │  Service    │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │ HTTP (RawSignal)    │                    │
       ▼                     ▼                    ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Sidecar    │      │  Sidecar    │      │  Sidecar    │
│ (buffer +   │      │             │      │             │
│  retry +    │      │             │      │             │
│  spill)     │      │             │      │             │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                     │                    │
       └─────────────────────┼────────────────────┘
                             ▼
                       ┌──────────┐
                       │  Kafka   │
                       │(signals) │
                       └─────┬────┘
                             │
                             ▼
              ┌────────────────────────────────┐
              │       Sentinel Service         │
              │  ┌──────────────────────────┐  │
              │  │ Ingest (RawSignal)       │  │
              │  │   ↓                      │  │
              │  │ Classifier (rules→LLM)   │  │
              │  │   ↓                      │  │
              │  │ Detect (OperationalEvent)│  │
              │  │   ↓                      │  │
              │  │ Correlate → Enrich →     │  │
              │  │ Notify                   │  │
              │  └──────────────────────────┘  │
              └──┬─────────┬────────────┬──────┘
                 │         │            │
                 ▼         ▼            ▼
            ┌────────┐ ┌────────┐  ┌──────────┐
            │Postgres│ │ Redis  │  │  Ollama  │
            │signals │ │counters│  │  (LLM)   │
            │events  │ │        │  │          │
            │incidents│ │       │  │          │
            └────────┘ └────────┘  └──────────┘
                             │
                             ▼
                       ┌──────────┐
                       │Dashboard │
                       │  (HTMX)  │
                       └──────────┘
```

## Two schemas

- **`RawSignal`** — external contract. Loose. Producers of any language/shape can emit these by POSTing JSON. Fields: `id`, `source`, `timestamp`, `message` (free-form string), optional `hints` (any structured extras the producer wants to share). No shared jar with producers — the wire schema is the contract.
- **`OperationalEvent`** — internal contract. Strict, typed. Produced by the classifier from a `RawSignal`. Carries `sourceSignalId` back to the raw for traceability, plus `type`, `severity`, `classification.method` (`RULE` | `LLM` | `NOISE`), `classification.ruleId`, `classification.confidence`.

Every downstream stage — anomaly detection, correlation, enrichment — operates on `OperationalEvent`, never on `RawSignal`.

## Components

**Dummy producers (Payment, Order, Inventory).** Each is a small Spring Boot app that simulates a domain — payment authorizations, order checkout transitions, stock reservations. They emit `RawSignal` JSON when failures, retries, or anomalies happen in their simulated business logic. Producers are deliberately dumb: they know what happened but don't classify it or pick a "type" — that's the platform's job. See [step 09](steps/step-09-three-producers.md) for failure-mode design.

**Sidecar.** Runs alongside each producer. Producer POSTs signals to `localhost:<sidecar-port>` and forgets. Sidecar batches, retries, and spills to disk if the downstream is unreachable. The sidecar's only job is making "fire-and-forget" actually reliable. See [sidecar.md](sidecar.md).

**Kafka.** Two topics: `signals.raw` for producer-emitted signals, `events.classified` for post-classifier operational events. The classifier consumes from the first and produces to the second. Anomaly detection consumes from the second.

**Sentinel service.** One Spring Boot app with internal modules:

- **Ingest** — receives raw signals over HTTP (early steps) or Kafka (from step 06). Persists them raw for audit; forwards to classifier.
- **Classifier** — rules-first (regex/pattern per source), producing `OperationalEvent` with `classification.method = RULE`. Unmatched signals get `type = UNCLASSIFIED` and route to a triage bucket. LLM fallback (from step 12.5) proposes types for unclassified signals; humans confirm proposals into new rules.
- **Detect** — sliding-window threshold rules over Redis counters, operating on `OperationalEvent`. Emits anomalies.
- **Correlate** — groups anomalies into incidents, manages incident state in Postgres.
- **Enrich** — calls the LLM (in parallel via virtual threads) for summary, remediation, optional postmortem — on already-correlated incidents.
- **Notify** — routes to console / mock webhook with dedup and cooldown.
- **API** — REST endpoints for the dashboard.

Keeping these as modules in one service (not many separate services) is deliberate — see [PLAN.md scope decisions](PLAN.md#scope-decisions).

**Storage.**

- **Postgres** — raw signals (audit), classified operational events, incidents, notification history.
- **Redis** — sliding-window counters, dedup keys with TTL, notification cooldowns.
- **Disk (sidecar)** — local spillover when Kafka is unreachable.

**LLM (Ollama).** Local model server in docker-compose. Used for two distinct jobs: (1) enriching correlated incidents with human-readable summaries and remediation, and (2) proposing classifications for unmatched signals (fallback, human-in-the-loop). Provider-abstracted so a Claude or mock implementation can be swapped in. See [llm-integration.md](llm-integration.md).

**Dashboard.** Minimal UI listing incidents with ack/resolve actions; also surfaces the classifier triage view (recent unclassified signals + accept/reject LLM proposals). Decided at [step 15](steps/step-15-dashboard.md).

## Data flow (happy path)

1. Payment service simulates a provider timeout and emits a `RawSignal` with `message: "stripe authorize timed out after 8s"` and `hints: {level: ERROR, provider: stripe}`.
2. Sidecar receives via local HTTP, batches with other signals, publishes to Kafka topic `signals.raw`.
3. Sentinel ingest module consumes the signal, persists it raw, forwards to classifier.
4. Classifier runs its rule catalog. A rule matches on `hints.provider = stripe AND message ~ "timed out"` → produces `OperationalEvent` with `type = PAYMENT_PROVIDER_TIMEOUT`, `classification.method = RULE`. Publishes to `events.classified`.
5. Detect module increments a Redis counter `payment_failures:provider=stripe:1m`. If counter crosses threshold, emits an anomaly.
6. Correlate module checks for an open incident matching the anomaly fingerprint. Either creates a new incident in Postgres or updates an existing one.
7. Enrich module fans out parallel LLM calls for summary + remediation. Updates incident when results arrive.
8. Notify module sends to console + mock webhook, respecting per-incident cooldown.
9. Dashboard polls the API (or receives via SSE) and displays the incident, with drill-down back to the originating signals.

## Unclassified path

When a rule doesn't match a raw signal:

1. Classifier produces an `OperationalEvent` with `type = UNCLASSIFIED`, `classification.method = NONE`.
2. It bypasses anomaly detection but is stored and surfaced in the dashboard's triage view.
3. LLM classifier fallback (step 12.5) periodically batches unclassified signals and proposes a type + a candidate rule.
4. A human accepts or rejects the proposal. Accepted proposals become new rules; from then on similar signals are classified deterministically.

## Failure paths the demo highlights

- **Kafka down** → sidecars buffer to memory, then to disk after threshold. When Kafka returns, signals flush in order.
- **LLM slow/unavailable** → incident appears immediately with status "summary pending"; enrichment fills in asynchronously. If LLM fails entirely, incident still exists with a "no AI summary available" placeholder. Classifier does not depend on the LLM on the hot path — rules are always tried first, LLM fallback runs on a cold batch.
- **Duplicate signals** → idempotency layer (signal ID + Redis SET NX with TTL) drops duplicates silently. Visible in metrics.
- **Unclassifiable signal** → routed to triage, not dropped. Loud in the dashboard, quiet in metrics.

## Design boundaries to defend in interviews

- **Why one Sentinel service, not five?** Solo project, no team boundaries to enforce, no independent scaling needs. Modular monolith first; split when there's a real reason.
- **Why two schemas (`RawSignal` and `OperationalEvent`)?** Producers should be dumb and heterogeneous — accept anything at the edge. Downstream stages need typed input to be tractable. Splitting the schema at the classifier is the natural seam.
- **Why rules-first classification, not LLM-first?** LLMs at ingest volume are slow, expensive, and probabilistic. The producer already knows what happened at emit time and encodes it in the message + hints; a deterministic rule can recover that structure in microseconds. LLMs are reserved for the tail (unclassified) and for synthesis on incidents — cold paths, high-value output.
- **Why no shared DTO jar between producers and Sentinel?** The wire contract (JSON) is the real contract. A jar would create false coupling and imply producers must be JVM apps. They shouldn't.
- **Why a sidecar at all?** Decouples producer reliability from transport reliability. Producers don't carry retry/buffer logic. Real pattern (OTel Collector, Vector, Fluent Bit). See [sidecar.md](sidecar.md).
- **Why Kafka and not just HTTP?** Replay, multiple consumers, decoupling, real distributed-systems talking surface (offsets, consumer groups, DLQ).
- **Why local LLM?** Zero ongoing cost, runs offline, demonstrates provider-agnostic design via the abstraction layer. See [llm-integration.md](llm-integration.md).
