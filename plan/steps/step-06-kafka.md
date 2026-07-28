# Step 06 — Kafka Producer/Consumer

Parent: [PLAN.md](../PLAN.md)

## Goal

Replace direct HTTP from producer → Sentinel with Kafka. Producer publishes to a topic; Sentinel consumes from it and stores the same rows as before (raw signal + operational event) with the same processing semantics.

## Approach — hand-written Kafka client (no Spring Kafka)

Chose to use `org.apache.kafka:kafka-clients` directly, not `spring-kafka` / `@KafkaListener` / `KafkaTemplate`. Reason: educational value — for the interview walkthrough the whole poll loop, offset commit strategy, and shutdown protocol should be visible in the code, not hidden behind a framework annotation.

Trade-off: more code (~150 lines of `AbstractKafkaConsumer`). Payoff: nothing about the delivery guarantees is magic.

## What was built

**Payment-service (producer side):**
- `KafkaProperties` + `KafkaConfig` bind `kafka.*` YAML to type-safe records, expose `producerProperties()`
- `SignalPublisher` — owns a `KafkaProducer<String, String>`, serializes `RawSignal` → JSON via Jackson, sends async with callback logging failures. `@PreDestroy` closes cleanly.
- `SignalEmitter` — scheduled emit calls `SignalPublisher.publish(...)`. HTTP path (`RestClient`) removed.
- Config: `acks=all`, `enable.idempotence=true`. `signal.id` is the partition key today; switches to `source` in step 09.

**Sentinel (consumer side):**
- `KafkaProperties` + `KafkaConfig` — list-based topic config (`kafka.topics: [{name, partitions, replication-factor}]`), consumer timing knobs (`poll-timeout`, `pause-between-polls`, `shutdown-timeout`, `max-attempts`, `retry-backoff`), `topic(name)` lookup fails fast if not configured.
- `AbstractKafkaConsumer<V>` — reusable base class:
  - Single-threaded named executor (`kafka-consumer-<topic>`)
  - `@PostConstruct` subscribes, `@PreDestroy` calls `wakeup()` + graceful shutdown
  - Poll loop: `poll(POLL_TIMEOUT)` → deserialize JSON to `V` → subclass `process(...)` → `commitSync()` after batch
  - Retry on processing failure (config-driven `max-attempts`, `retry-backoff`); `JsonProcessingException` short-circuits (deterministic — retrying won't help)
  - On exhaustion: `onProcessingFailed(record, cause)` dispatched to subclass
  - If failure-write itself throws: `pollOnce` catches, skips commit, next poll retries whole batch — no signal loss even when Postgres is down
- `RawSignalConsumer extends AbstractKafkaConsumer<RawSignal>` — declares its topic, deserialization target, ingest handler, and failure handler (persists `INGESTION_FAILED` event)
- `KafkaTopicInitializer` — iterates `kafka.topics`, calls `AdminClient.createTopics` idempotently at startup; `TopicExistsException` treated as success

**Failure sink:**
- New `FailureType.INGESTION_FAILED` + `Classification.Method.FAILURE`
- Flyway V4 migration makes `operational_events.source_signal_id` nullable (failure events have no raw signal)
- `SignalIngestService.recordIngestionFailure(...)` persists a failure event carrying: raw payload string, error class + message, kafka topic/partition/offset
- Decision: DB failure sink instead of a Kafka DLQ topic. See PROGRESS.md cross-cutting for reasoning (unified triage view, LLM triage in step 12.5 covers both `UNCLASSIFIED` and `INGESTION_FAILED`).

**Infrastructure:**
- Kafka added to `compose.yml` in KRaft mode (single-broker, no ZooKeeper), ports remapped to `19092`/`39092` to avoid collision with other local Kafka clusters
- Named `kafka-data` volume mounted at `/var/lib/kafka/data` — data survives container restart
- Topic `signals.raw` created with 6 partitions, RF=1 (RF=3 in step 06.5)

## Delivery guarantees

- **At-least-once from producer**: `acks=all` + idempotent producer means a successfully-acked record is on the broker's disk
- **At-least-once from consumer**: `commitSync()` after successful `process(...)`; DB writes (raw_signals + operational_events) atomic via `@Transactional`; Kafka commit only if DB commit succeeded
- **No signal loss on failure**: poison messages become `INGESTION_FAILED` operational events (raw payload preserved); if Postgres is unavailable to write even that, the poll loop stalls and retries when DB recovers
- **Duplicates on replay**: expected. Idempotency (step 10) turns "at-least-once" into "effectively exactly-once" via signal-id dedup at ingest

## Configuration

`sentinel/src/main/resources/application.yml`:

```yaml
kafka:
  bootstrap-servers: kafka:9092
  consumer:
    group-id: sentinel-signals
    auto-offset-reset: earliest
    enable-auto-commit: false
    max-poll-records: 500
    poll-timeout: 3s
    pause-between-polls: 100ms
    shutdown-timeout: 10s
    max-attempts: 3
    retry-backoff: 500ms
  topics:
    - name: signals.raw
      partitions: 6
      replication-factor: 1
```

`payment-service/src/main/resources/application.yml` — producer side is simpler (bootstrap + acks + idempotence + topic name).

Everything a runtime operator would want to tune lives in YAML. Env-var override (`KAFKA_BOOTSTRAP_SERVERS`, etc.) works via Spring's relaxed binding.

## What was learned

- The Kafka client is fundamentally poll-based; long polling (`max.wait.ms` on the broker) makes it feel streaming. Kafka Streams runs the same loop internally.
- Consumer group protocol: partition assignment happens on first `poll()`, heartbeats run on a client-internal thread, rebalances are transparent to user code (unless you register a listener).
- `commitSync` after batch processing is the at-least-once boundary; commit-before-process would give at-most-once (data loss on crash between commit and process). Never do that.
- `consumer.wakeup()` is the only safe way to interrupt a thread blocked inside a Kafka client call. `Thread.interrupt()` won't do it.
- Serde: `String` + Jackson manually is fine; a typed `Deserializer<T>` is a mechanical polish (step 06f). Avro + Schema Registry rejected — plan wants JSON on the wire for language-agnostic producers.
- Topic creation via `AdminClient` beats auto-create; auto-create silently gives you 1 partition + `default.replication.factor=1`, which is not what you want.
- Partition count sizing: enough to cover consumer parallelism ceiling + producer throughput / per-partition throughput. Picked 6 for demo scale with headroom for eventual multiple producers.

## Things to think about (design decisions to defend in interviews)

- **Partition key.** `signal.id` today (random spread). Switches to `source` in step 09 when 3 producers exist — preserves per-producer ordering.
- **Schema evolution.** JSON stays. Avro/Protobuf mentioned as an alternative; rejected on grounds of language-agnostic producers + human-readable wire.
- **Topic creation.** Iterative `AdminClient.createTopics` at startup, YAML-driven list. `TopicExistsException` = success (idempotent restart).
- **Security.** Compose network is a private bridge — PLAINTEXT only. Production would be `SASL_SSL` + `SCRAM-SHA-512` (or mTLS), credentials from a secret manager. Deferred, documented in PROGRESS.md cross-cutting.

## Done when

- Producer publishes to Kafka. ✓
- Sentinel consumes and stores events to the same Postgres tables as before. ✓
- Killing Sentinel mid-flow and restarting it resumes from the last committed offset (manual test — verified). ✓
- Sending a malformed event lands it as an `INGESTION_FAILED` operational event, not silently dropped or halting the pipeline. ✓ (via 06e)

## Things skipped

- **Kafka DLQ topic.** Replaced by DB-based failure sink. See PROGRESS.md cross-cutting.
- **Schema registry / Avro.** Deferred to optional step 06f, or explicitly rejected — see step 06f doc.
- **Multiple consumer groups.** Single group (`sentinel-signals`) for now. When step 07 adds anomaly detection, decide whether classification publishes to `events.classified` (needs new consumer, new group) or classifier stays inline.
- **Exactly-once semantics across producer + consumer.** At-least-once + idempotent consumer (step 10) is the pragmatic choice.

## Look ahead

- **Step 06.5** — 3 brokers, RF=3, ISR=2. `replication-factor: 1` in YAML changes to `3`, plus compose changes. No consumer code changes.
- **Step 07** — anomaly detection over `OperationalEvent`. Decision: consume from DB or a new `events.classified` topic? Deferred.
- **Step 11** — sidecar. Payment-service goes back to HTTP-to-localhost; a separate sidecar process picks up the Kafka client code and adds disk buffering.
- **Step 12.5** — LLM triage over both `UNCLASSIFIED` and `INGESTION_FAILED` events.
