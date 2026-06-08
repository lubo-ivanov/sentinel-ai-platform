# Step 10 — Idempotency and Deduplication

Parent: [PLAN.md](../PLAN.md)

## Goal

Make Sentinel safe under duplicate event delivery. Same event ID processed twice produces the same outcome — one event stored, no double-counted anomalies, no duplicate incidents.

## What to build

- Producer-side: every event has a UUID `event_id` set at creation (already done in [step 04](step-04-dummy-producer.md), verify).
- Consumer-side: dedup using **Redis SET NX with TTL** as the primary mechanism — `SET event:<id> 1 NX EX 86400`. If the key already existed, drop the event silently (with a metric increment).
- Database-side: `UNIQUE` constraint on `event_id` in `operational_events` table as a safety net. Treat constraint violations as expected, not errors.
- Anomaly counter: ensure double-incrementing the same event ID is impossible (the SET NX gate prevents it before the counter is touched).
- A metric: `events_deduped_total` counter.
- Test: replay the same event 5 times, verify exactly one is processed.

## What to learn

- Why exactly-once delivery is impossible in distributed systems and why "exactly-once processing" via idempotency is the practical answer.
- The SET NX pattern — atomic check-and-set in Redis.
- Why TTL matters on dedup keys — without it, the keyspace grows unbounded.
- The right TTL — at least longer than the maximum reasonable replay window. 24 hours is typical for event streams.
- Layered defense: Redis for fast-path, DB unique constraint as the truth.

## Things to think about

- **TTL choice.** Longer = safer against late replays, more memory. Short = faster eviction, risk of double-processing very-late duplicates. Pick a number and justify it in a comment.
- **Failure between SET NX and downstream processing.** If the consumer crashes after SET NX but before processing, the event is "lost" (deduped on retry). Mitigation: only set the dedup key *after* the event is durably stored. Order matters.
- **The DB unique constraint.** Don't catch it as a generic exception — translate it into the same "deduped, ignore" path. Never log it as ERROR; it's the system working correctly.

## Done when

- Replaying events produces no duplicates in the DB.
- Anomaly counters reflect unique events only.
- Metric `events_deduped_total` increments on duplicates.
- An integration test explicitly publishes the same event twice and asserts exactly one row.

## Things to skip

- Cross-service idempotency. Each service has its own concerns. Sentinel's idempotency is about Kafka redelivery from sidecars.
- Idempotency at the API layer (POST endpoints with `Idempotency-Key` header). Out of scope; mention in interviews.

## Look ahead

This step is what makes the sidecar's at-least-once delivery ([step 11](step-11-sidecar.md)) safe. The pairing — at-least-once transport + idempotent consumer — is *the* standard distributed-systems pattern. Be ready to explain why both halves are needed.
