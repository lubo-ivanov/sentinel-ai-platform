# Step 11 — Sidecar Buffered Forwarder

Parent: [PLAN.md](../PLAN.md) | Design: [sidecar.md](../sidecar.md)

## Goal

Insert a sidecar in front of each producer. Producers POST events to localhost; sidecar handles Kafka publishing, batching, retry, and disk spillover when Kafka is unreachable.

## Sub-steps

### 11a — Module scaffold ✅
- New `sidecar/` Maven module added to root `pom.xml`
- `SidecarApplication` with `@SpringBootApplication` + `@ConfigurationPropertiesScan`
- `POST /events` endpoint in `EventController` — accepts `RawSignal`, returns `202 Accepted`
- `RawSignal` record in `signal/` package — **no `source` field** (sidecar stamps it from config)
- `application.yml` with env-var-backed defaults for all config

### 11b — In-memory queue + Kafka publisher (in progress)
- `SidecarProperties` — `@ConfigurationProperties(prefix="sidecar")` record: `source`, `signalsTopic`, `queueCapacity`
- `kafka/KafkaProperties` — `@ConfigurationProperties(prefix="kafka")` record: `bootstrapServers`, `producer{acks, enableIdempotence}`
- `kafka/KafkaConfig` — `@Configuration`, builds `producerProperties()` from `KafkaProperties`
- `kafka/SignalPublisher` — `@Component`, creates `KafkaProducer`, `publish(RawSignal)`, `@PreDestroy close()`
- `EventQueue` — wraps `ArrayBlockingQueue<RawSignal>`, `offer()` returns false + logs warn when full
- Background publisher thread drains queue to Kafka
- `EventController.receive()` wired to `EventQueue.offer()`

### 11c — State machine + /health
- States: `NORMAL`, `DEGRADED`, `DOWN`, `RECOVERY`
- `SidecarState` enum + `StateManager` component with explicit transition methods
- `/health` endpoint returns state + buffer depth + disk spill count

### 11d — Disk spillover
- On Kafka failure → spill to append-only files (one JSON per line)
- On recovery → drain disk first (in arrival order), then resume in-memory
- Configurable spill directory (`sidecar.spill-dir`) and max disk cap
- `events_dropped_total` counter when disk cap also exceeded

### 11e — Source ownership + producers switch to HTTP
- Producers drop `source` field from `RawSignal` and their entire Kafka infra (`kafka/` package deleted from each producer)
- Producers gain a simple HTTP client (`RestClient`) that POSTs to `${SIDECAR_URL}/events`
- `SignalEmitter` in each producer calls HTTP client instead of `SignalPublisher`

### 11f — Compose wiring
- Root `Dockerfile` gets `sidecar-build` + `sidecar-runtime` targets
- Three sidecar instances in `compose.yml`:
  - `payment-sidecar` (port 9082, `SIDECAR_SOURCE=payment-service`)
  - `order-sidecar` (port 9083, `SIDECAR_SOURCE=order-service`)
  - `inventory-sidecar` (port 9084, `SIDECAR_SOURCE=inventory-service`)
- Each producer depends on its sidecar (`condition: service_healthy`)
- E2E verified: all producers → sidecars → Kafka → Sentinel

### 11g — Metrics
- `events.received` counter (tagged by source)
- `events.published` counter
- `buffer.depth` gauge
- `events.spilled` counter
- `events.dropped` counter

## Architecture decisions

- **One module, three instances** — same Docker image, different config per instance via env vars in compose. No per-producer Dockerfiles needed.
- **`@ConfigurationPropertiesScan` on `SidecarApplication`** — registers all `@ConfigurationProperties` classes in the package tree automatically. No need for `@EnableConfigurationProperties` anywhere. This is cleaner than `@EnableConfigurationProperties` on `KafkaConfig` because `SidecarProperties` has no single owner class.
- **`RawSignal` has no `source` field** — sidecar stamps `source` from `sidecar.source` config, used as the Kafka partition key. Producers become truly source-agnostic.
- **`signal/` sub-package** — `RawSignal` lives in `com.sentinelai.sidecar.signal`, matching the producer pattern.
- **No Kubernetes** — explicitly out of scope per `PLAN.md`. Compose is the deployment target. Sidecar injection patterns (like Istio) require K8s — not applicable here.
- **Queue full → drop, not block** — `ArrayBlockingQueue.offer()` (non-blocking). Producer's `POST /events` always returns quickly. Drops logged + counted. Disk spillover handles sustained Kafka outages before queue fills.
- **At-least-once delivery** — sidecar may replay on retry/recovery. Step 10 idempotency (Redis SET NX + DB UNIQUE constraint) makes this safe.

## application.yml structure

```yaml
server:
  port: ${SIDECAR_PORT:9090}

sidecar:
  source: ${SIDECAR_SOURCE:unknown}
  signals-topic: ${SIDECAR_SIGNALS_TOPIC:signals.raw}
  queue-capacity: ${SIDECAR_QUEUE_CAPACITY:10000}

kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  producer:
    acks: all
    enable-idempotence: true
```

## What to learn

- The producer-sidecar contract: producers don't know Kafka exists. Their failure mode is "sidecar unreachable" only.
- Bounded queue + backpressure: `offer()` vs `put()` — non-blocking drop beats blocking producer.
- File I/O for append-only logs — `BufferedWriter` with `flush()` per line for durability.
- State-machine modeling — explicit states beat implicit booleans.
- `@ConfigurationPropertiesScan` vs `@EnableConfigurationProperties` — scan on main class for global registration; explicit annotation when a config class tightly owns its properties.

## Done when

- Three producers each have their own sidecar.
- Killing Kafka via `docker compose stop kafka`:
  - Producers continue posting to sidecars successfully.
  - Sidecar metrics show buffer depth climbing, then disk spillover.
  - No events lost (verified by counting before kill, after recovery).
- Restarting Kafka:
  - Sidecars drain disk back to Kafka.
  - Sentinel sees the burst arrive in order.
  - Idempotency from step 10 prevents any reprocessing artifacts.
- Demo recording-ready: this is the core wow-moment.

## Things to skip

- Multi-destination routing. Kafka only.
- Compression. Plain JSON over the wire is fine.
- Adaptive batching. Fixed batch size + flush interval.
- Kubernetes sidecar injection patterns. Out of scope.
