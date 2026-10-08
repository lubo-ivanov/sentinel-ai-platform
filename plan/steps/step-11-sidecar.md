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

### 11b — In-memory queue + Kafka publisher ✅
- `SidecarProperties` — `@ConfigurationProperties(prefix="sidecar")` record: `source`, `signalsTopic`, `queueCapacity`, `spillDir`, `maxSpillBytes`
- `kafka/KafkaProperties` — `@ConfigurationProperties(prefix="kafka")` record: `bootstrapServers`, `producer{acks, enableIdempotence}`
- `kafka/KafkaConfig` — `@Configuration`, builds `producerProperties()` from `KafkaProperties`
- `kafka/SignalPublisher` — `@Component`, creates `KafkaProducer`, `publish(RawSignal)`, `@PreDestroy close()`; uses `source` as Kafka partition key
- `buffer/EventQueue` — wraps `ArrayBlockingQueue<RawSignal>`, `@Getter`, `offer()` logs warn when full; gauge registered in constructor
- `buffer/ForwarderRunner` — virtual thread drain loop, `@PostConstruct`/`@PreDestroy`, spills on failure, drains on RECOVERY
- `EventController.receive()` wired to `NormalizationService` then `EventQueue`

### 11c — State machine + /health ✅
- States: `NORMAL`, `DEGRADED`, `DOWN`, `RECOVERY`
- `health/SidecarState` enum + `health/StateManager` — `AtomicReference` + `AtomicInteger` failure count; `getAndUpdate()` for atomic transitions; failure thresholds: 3→DEGRADED, 10→DOWN; success: DEGRADED/DOWN→RECOVERY, RECOVERY→NORMAL
- `controller/HealthController` — GET `/health` returns state + bufferDepth + spillCount

### 11d — Disk spillover ✅
- `spill/SpillManager` — append-only `spill.jsonl`, 100MB cap, `spill()` checks cap before write, `drain()` reads all lines + deletes file, `spillCount()` via `Files.lines()` try-with-resources
- `ForwarderRunner` spills on publish failure; checks `RECOVERY` state after `markSuccess()` and drains disk back to queue

### 11e — Source ownership + producers switch to HTTP ✅
- Producers drop `source` field from `RawSignal` and entire `kafka/` package
- `SidecarClient` in each producer — `RestClient` POST to `${sidecar.url:http://localhost:9090}/events`; `waitForSidecar()` `@PostConstruct` polls `/health` indefinitely until sidecar is ready
- `NormalizationService` in sidecar — stamps `source` from `SidecarProperties.source()` (`$HOSTNAME`), defaults `occurredAt` to `Instant.now().toString()` if null
- `RawSignalConsumer.process()` fixed: was hardcoded `"payment-service"`, now uses `raw.key()` (Kafka record key = source stamped by sidecar)

### 11f — Compose wiring ✅
- Root `Dockerfile` — `sidecar-build` + `sidecar-runtime` targets added; `sidecar/pom.xml` copied in base stage
- Three sidecar instances: `payment-sidecar`, `order-sidecar`, `inventory-sidecar` — all use `target: sidecar-runtime`, `network_mode: service:<producer>`, `depends_on: kafka: service_healthy`
- `network_mode: service:<producer>` shares producer's network namespace — sidecar listens on `localhost:9090` inside producer's network; no `SIDECAR_URL` env var needed
- Each producer gets `hostname: <service-name>` — `$HOSTNAME` inside sidecar = producer hostname = source name
- `application.yml` reads `${HOSTNAME:unknown}` directly as `sidecar.source`
- `IncidentEntity.lastSeen` fixed: `@Generated(event={INSERT,UPDATE})` — resolves HHH000502 immutability warning

### 11g — Metrics ✅
- `events.received` counter — `EventController`, tagged by source
- `events.published` counter — `ForwarderRunner`, tagged by source
- `buffer.depth` gauge — `EventQueue` constructor, tagged by source
- `events.spilled` counter — `SpillManager.spill()` on successful write, tagged by source
- `events.dropped` counter — `SpillManager.spill()` when disk cap exceeded, tagged by source

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
