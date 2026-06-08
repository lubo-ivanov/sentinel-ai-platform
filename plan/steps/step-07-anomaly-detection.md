# Step 07 — Anomaly Detection Rules

Parent: [PLAN.md](../PLAN.md)

## Goal

Add the first piece of "intelligence" to Sentinel: detect when events cross a threshold within a time window. Use Redis for sliding-window counters. Emit anomaly objects (in-process for now) when rules fire.

## What to build

- Redis added to `docker-compose.yml` and Spring Data Redis dependency.
- A `Rule` abstraction — name, predicate over an event, window (e.g., 60s), threshold (e.g., 5).
- Initial rules:
  - `payment_provider_timeout_burst` — 5+ `PAYMENT_PROVIDER_TIMEOUT` from same provider in 60s.
  - `payment_decline_rate_spike` — 10+ declines in 60s overall.
- Counter implementation: Redis sorted sets (ZADD with timestamp score; ZCOUNT for window) or simpler INCR with TTL — pick based on accuracy needs.
- An `AnomalyDetected` domain object emitted (for now to a list/in-process queue, consumed by step 08).
- Metrics: counter activations, rule eval latency.

## What to learn

- Redis data structures: sorted sets for time-windowed counts, INCR + EXPIRE for simple counters, the trade-offs between them.
- Why `INCR + EXPIRE` is *not* atomic by default and how Lua scripts (`EVAL`) fix it.
- Sliding vs tumbling windows — which one your rule actually wants.
- How to keep rules declarative — config-driven if possible, so adding a rule doesn't mean adding code.

## Things to think about

- **Rule storage.** Hardcoded list of rules in code is fine for now. Mention "would store in DB and hot-reload in production."
- **Rule evaluation order.** Currently independent — each rule sees each event. Don't introduce dependencies between rules.
- **Cardinality explosion.** If a rule keys by `provider`, the counter space grows with the number of providers. Bound TTLs aggressively (counter TTL = window size + buffer).
- **False positives.** Threshold-based rules will false-positive. That's fine for the demo; correlation in [step 08](step-08-correlation.md) deduplicates.

## Done when

- Producer emits a burst of `PAYMENT_PROVIDER_TIMEOUT` events.
- Anomaly fires within ~1 second of threshold being crossed.
- Anomaly does NOT fire when events trickle below threshold.
- Unit tests cover the rule logic with synthetic event sequences.
- Integration test with Testcontainers Redis verifies the counter works end-to-end.

## Things to skip

- ML-based anomaly detection. Out of scope, not the point.
- Rule UI for editing. Out of scope.
- Cross-event correlation in the rule itself ("X happened then Y") — keep rules single-condition. Correlation across signals is [step 08](step-08-correlation.md).

## Look ahead

Anomalies emitted here are consumed by correlation in [step 08](step-08-correlation.md), which decides whether each anomaly creates a new incident or updates an existing one.
