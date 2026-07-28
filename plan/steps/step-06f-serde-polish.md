# Step 06f — Serde Polish (optional)

Parent: [PLAN.md](../PLAN.md)

## Goal

Push JSON (de)serialization from the caller into a proper Kafka `Serializer<T>` / `Deserializer<T>` implementation. Cleaner types on the producer/consumer, same wire format, no infrastructure change.

## What to build

- A `JsonSerializer<T>` and `JsonDeserializer<T>` under `sentinel/src/main/java/com/sentinelai/sentinel/kafka/serde/` (and mirror to `payment-service`)
- Wire them into `KafkaConfig.consumerProperties()` and payment-service's `SignalPublisher` construction
- Producer becomes `KafkaProducer<String, RawSignal>` — no manual `writeValueAsString`
- Consumer becomes `KafkaConsumer<String, RawSignal>` — `record.value()` is already typed
- `AbstractKafkaConsumer<V>` loses its manual `objectMapper.readValue(...)` — Kafka client hands over typed records
- Handle deserialization errors (Kafka's serde layer throws before the record reaches user code — needs error handler / DLQ routing)

## What to learn

- How Kafka's `Serializer<T>` / `Deserializer<T>` interface fits into the client's send/poll pipeline
- Where deserialization errors appear in the poll cycle and how to catch them without stalling the consumer
- Trade-off between "String + manual Jackson" (poison message caught in user code, easy to log) vs "typed serde" (cleaner code, deser errors bubble differently)

## Things to think about

- **Poison message handling.** With `StringDeserializer`, invalid JSON never happens at the Kafka layer — our `handleRecord` catches Jackson exceptions. With a typed `Deserializer`, an invalid JSON payload throws `SerializationException` inside `consumer.poll(...)` itself — the poll fails before we see records. Kafka client offers `errors.deserialization.exception.handlers` config for this; wire it up.
- **Class-per-topic serde vs generic `JsonDeserializer<T>` with class hint.** Confluent's `JsonDeserializer` reads the target class from a config property; simpler for a single-consumer app but couples the serde to the topic. A cleaner alternative is a factory: `JsonDeserializer<RawSignal>` constructed with the class at instantiation.

## Done when

- Producer sends `RawSignal` directly, no manual JSON conversion
- Consumer receives `RawSignal` directly, no manual JSON conversion
- Poison message → routed to DLQ, poll continues (regression test)
- Wire format on the topic is byte-identical to the pre-polish version (verify with `kafka-console-consumer`)

## Things to skip

- **Avro + Schema Registry.** Explicitly rejected by the plan ([architecture.md](../architecture.md) line 60) — the wire contract is JSON so producers stay language-agnostic and jar-free. Mention as trade-off in interviews.
- **Protobuf.** Same reasoning as Avro.

## Why "optional"

Mechanical refactor. Doesn't change what the system does or how it fails. Wire format unchanged. Worth doing for the interview walkthrough ("I wrote a small JsonSerde over Jackson") but nothing in the plan blocks on it.
