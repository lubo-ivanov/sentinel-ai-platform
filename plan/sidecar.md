# Sidecar — Buffered Event Forwarder

Parent: [PLAN.md](PLAN.md) | Built in [step 11](steps/step-11-sidecar.md)

## Why a sidecar exists in this project

Producers should not carry transport reliability logic. They emit events; they should not know about Kafka, retries, batching, or what happens when the bus is down. The sidecar isolates that concern so producers stay dumb and uniform.

This is the same pattern as **OTel Collector**, **Vector**, **Fluent Bit** — there's a real industry precedent for the design, and being able to name those references in an interview is part of the value.

## Responsibilities

- Accept events via local HTTP (`POST localhost:<port>/events`).
- Acknowledge to the producer immediately (fire-and-forget from the producer's point of view).
- Batch events for efficient downstream publishing.
- Publish batches to Kafka.
- On Kafka failure, retry with exponential backoff.
- After memory buffer threshold or sustained outage, spill to local disk.
- On Kafka recovery, drain disk-spilled events in order before accepting new ones into Kafka.
- Expose `/health` reflecting Kafka connectivity and buffer depth.

## Non-responsibilities

- No event transformation beyond minimal metadata enrichment (host, sidecar version, ingest timestamp).
- No business logic, no anomaly detection, no filtering.
- No multi-destination routing in v1 (Kafka only).

## Data flow states

```
NORMAL:    in-memory queue → Kafka
DEGRADED:  in-memory queue full + Kafka slow → spill new arrivals to disk
DOWN:      Kafka unreachable → drain in-memory to disk, accept new to disk
RECOVERY:  Kafka returns → drain disk → resume in-memory mode
```

## Key design decisions

**Memory buffer size.** Bounded queue (e.g., 10k events). When full, new events go to disk rather than blocking the producer. The producer's POST always succeeds quickly.

**Disk spillover format.** Append-only files, rotated by size or time. Each line a serialized event. Simple to reason about, simple to recover from. No need for an embedded DB.

**Ordering guarantees.** Best-effort within a sidecar. Drain disk before accepting in-memory-to-Kafka after recovery. Across sidecars, no ordering — that's Kafka's partitioning concern.

**At-least-once delivery.** Sidecar may republish on retry. Sentinel's idempotency layer ([step 10](steps/step-10-idempotency.md)) is what makes this safe.

**Backpressure.** If disk also fills (configurable cap), drop oldest events and increment a `events_dropped_total` metric. Loud failure beats silent unbounded growth.

## Implementation language

**Default: Java (Spring Boot)** for consistency with the rest of the project and to avoid context-switching cost.

**Alternative: Go.** Smaller binary, more "real" sidecar feel, no JVM overhead per producer. Tempting, but adds a language to the project. Defaulting to Java; revisit at step 11 if energy permits.

## What to measure

- Events received (rate, total).
- Events published to Kafka (rate, total).
- Buffer depth (gauge).
- Disk spilled events (count, size).
- Drops (counter — should stay zero in normal operation).
- Kafka publish latency (histogram).

These metrics make the demo's "kill Kafka, watch buffering, restart, watch drain" sequence visually compelling on the Grafana dashboard.

## Interview talking points

- "I built a buffered forwarder so producers don't carry transport-layer reliability — same pattern as OTel Collector or Vector."
- "It handles three states — normal, degraded, down — with explicit transitions."
- "Disk spillover is plain append-only files; I considered embedded RocksDB but the simplicity won."
- "At-least-once delivery is paired with idempotency at the consumer, which is the correct division of responsibility."
