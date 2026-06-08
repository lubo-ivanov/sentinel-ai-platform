# Step 08 — Incident Correlation

Parent: [PLAN.md](../PLAN.md)

## Goal

Turn raw anomalies into incidents. An incident represents an ongoing problem; multiple related anomalies should update one incident, not create N. Manage the incident lifecycle in Postgres.

## What to build

- `Incident` entity expanded — fingerprint, status (open/acknowledged/resolved), first_seen, last_seen, anomaly_count, severity.
- A **fingerprint function** for anomalies — deterministic hash over (rule_name, key dimensions like provider/service). Two anomalies with the same fingerprint correlate to the same incident.
- Correlation logic:
  - On anomaly: look up open incident by fingerprint.
  - If found: update last_seen, increment anomaly_count, possibly bump severity.
  - If not: create new incident.
- An anomaly-to-incident audit table or column linking each anomaly to the incident it contributed to.
- Severity policy — e.g., 1 anomaly = LOW, 5+ in 5 min = MEDIUM, 10+ = HIGH. Configurable.
- An "auto-resolve" job — if no anomalies for an incident in N minutes, mark it RESOLVED automatically (configurable; consider just leaving manual resolve for the demo).

## What to learn

- Domain modeling — the difference between an event, an anomaly, and an incident, and why they're separate concepts.
- Optimistic locking with `@Version` for concurrent anomaly arrivals updating the same incident.
- Postgres unique constraints as a safety net (open incident per fingerprint at most once).
- State machines — incident has a lifecycle, transitions are explicit.

## Things to think about

- **Concurrency.** Two anomalies for the same fingerprint arrive simultaneously. Without care, you create two incidents. Solutions: unique partial index on `(fingerprint) WHERE status = 'open'`, or pessimistic lock, or upsert with `ON CONFLICT`. Pick one and explain the choice.
- **Internal Kafka topics?** This is the decision point for whether anomaly → correlation → enrichment is in-process or via Kafka. For now, keep it in-process (simpler). Mention "would split via topics for independent scaling" in interviews.
- **Fingerprint stability.** If you change the fingerprint function, in-flight incidents become unmatched. Treat it as a versioned artifact.

## Done when

- Burst of payment timeouts → exactly one open incident, with anomaly_count climbing.
- Different rule firing → different incident.
- Concurrent anomaly arrivals don't create duplicate incidents (verified via concurrent integration test).
- Manual ack/resolve via API works (REST endpoints exposed).

## Things to skip

- AI summary — that's [step 12](step-12-llm-v1.md).
- Notification routing — [step 16](step-16-notifications.md).
- Dashboard updates — [step 15](step-15-dashboard.md).

## Look ahead

Once incidents exist as proper entities, the LLM can enrich them ([step 12](step-12-llm-v1.md)) and notifications can fire ([step 16](step-16-notifications.md)). Keep the incident lifecycle clean — it's the central artifact of the system.
