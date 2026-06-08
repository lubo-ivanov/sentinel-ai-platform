# Step 06 — Kafka Producer/Consumer

Parent: [PLAN.md](../PLAN.md)

## Goal

Replace direct HTTP from producer → Sentinel with Kafka. Producer publishes to a topic; Sentinel consumes from it. The HTTP endpoint stays for now (will be removed later or repurposed) so we can compare.

## What to build

- Kafka + Zookeeper (or KRaft mode — preferred, simpler) added to `docker-compose.yml`.
- Spring for Apache Kafka dependency in both services.
- Producer config in `payment-service` — `KafkaTemplate`, JSON serializer, idempotent producer (`enable.idempotence=true`).
- Topic created on startup (or via init container): `operational.events.raw`, partitions=3, replication=1.
- `@KafkaListener` in Sentinel — manual offset commit, error handler, container factory configured.
- A DLQ topic `operational.events.dlq` for poison messages. Configure error handler to route to it after N retries.

## What to learn

- The Kafka mental model: topics, partitions, offsets, consumer groups.
- Producer acks (`acks=all`) and idempotence — what guarantees they actually provide.
- Manual vs auto offset commits — when to choose which, and the at-least-once / at-most-once trade-off.
- Consumer group rebalancing — what triggers it, what it costs.
- Why a DLQ matters and how Spring's `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` wire it up.

## Things to think about

- **Partition key.** What goes in the Kafka message key? Probably `service` or `correlation_id` so events from the same source land on the same partition (preserves ordering per service).
- **Schema evolution.** JSON is fine; Avro/Protobuf is interview-flex but adds infra (schema registry). Stay JSON for now; mention the trade-off.
- **Topic creation.** Auto-create is convenient but not production-correct. Use a small init job or AdminClient code. Worth doing the "right" way for the talking point.

## Done when

- Producer publishes to Kafka.
- Sentinel consumes and stores events to the same Postgres table as before.
- Killing Sentinel mid-flow and restarting it resumes from the last committed offset (manually verify).
- Sending a malformed event lands it in the DLQ topic, not the main flow.

## Things to skip

- Schema registry / Avro. Mentioned, not built.
- Multiple consumer groups. Single group for Sentinel.
- Exactly-once semantics across services. At-least-once + idempotency at the consumer ([step 10](step-10-idempotency.md)) is the right pairing.

## Look ahead

Kafka enables the sidecar pattern in [step 11](step-11-sidecar.md) (sidecar publishes to Kafka, not Sentinel). It also enables internal topics in later steps if Sentinel's modules want to communicate via the bus rather than method calls. Keep that flexibility in mind without over-engineering for it now.
