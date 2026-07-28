# Step 09 — Three Producers, Distinct Failure Shapes

Parent: [PLAN.md](../PLAN.md)

## Goal

Add Order and Inventory services alongside Payment. Each emits *different shapes* of failure so the demo has variety and anomaly detection has interesting work.

## What to build

- `order-service/` — simulates checkout transitions. Failure modes:
  - `CHECKOUT_FLOW_DEGRADATION` — slow checkout completion.
  - `ORDER_STATE_ANOMALY` — unexpected state transitions.
- `inventory-service/` — simulates stock reservation. Failure modes:
  - `STOCK_RESERVATION_TIMEOUT` — slow reservation.
  - `STOCK_CONSISTENCY_RISK` — phantom reservations / mismatched counts.
- Each service follows the pattern from [step 04](step-04-dummy-producer.md): scheduled job simulating activity, configurable failure injection, Kafka producer.
- New anomaly rules in Sentinel covering each failure type.
- Update the demo flow — a single trigger in one service shouldn't create N incidents across all services. Each service is independent.

## What to learn

- How to keep three services aligned without copy-paste — extract a shared `producer-common` module if it helps.
- Configuration-driven failure injection so each service's failure profile is tweakable for demos.
- The cost of having three Spring Boot apps booting locally (memory, startup time). Optimize JVM flags accordingly.

## Things to think about

- **Consistency of event schema.** All three services emit the same `OperationalEvent` shape with different `type` and `source` values. Don't let services diverge.
- **Kafka partition key switch.** Payment-service currently keys by `signal.id` (random distribution — no ordering guarantees). Once 3 producers exist, add `source` field to `RawSignal` and switch key to `source` so all signals from `payment-service` land on the same partition (per-producer ordering preserved). Also propagate `source` into the record on the consumer side so `SignalIngestService.ingest(source, signal)` gets the real producer identity rather than the hardcoded `"payment-service"` from step 06.
- **Demo control.** Each service should expose a small "trigger failure burst" admin endpoint so the demo can deterministically cause an anomaly without waiting for random injection.
- **Dependencies between services.** Skip them. Each service is independent. Multi-service incident correlation (e.g., "payment failure caused order failure") is interesting but out of scope; keep it for an interview talking point: *"In a real system I'd correlate across services via a shared correlation_id; here each service is independent for simplicity."*

## Done when

- Three services running in compose, each producing distinct event types.
- Triggering each service's "failure burst" endpoint creates the corresponding incident.
- Grafana dashboard panel shows event throughput per service distinctly.

## Things to skip

- Cross-service correlation logic.
- Real domain logic — these are simulators, not real services. Don't over-invest in modeling.

## Look ahead

These three services + their sidecars ([step 11](step-11-sidecar.md)) are the producers in the wow-moment demo. Their job is to be predictable, controllable, and varied.
