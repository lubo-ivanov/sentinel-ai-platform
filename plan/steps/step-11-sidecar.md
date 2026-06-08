# Step 11 — Sidecar Buffered Forwarder

Parent: [PLAN.md](../PLAN.md) | Design: [sidecar.md](../sidecar.md)

## Goal

Insert a sidecar in front of each producer. Producers POST events to localhost; sidecar handles Kafka publishing, batching, retry, and disk spillover when Kafka is unreachable.

## What to build

- New module `sidecar/` — Spring Boot app (or plain Java; Spring is fine for consistency).
- Local HTTP endpoint `POST /events` accepting one event or a batch.
- In-memory bounded queue (e.g., `ArrayBlockingQueue` of 10k events).
- Background publisher thread (or virtual thread) that drains the queue to Kafka in batches.
- State machine — `NORMAL`, `DEGRADED`, `DOWN`, `RECOVERY` — with explicit transitions.
- Disk spillover — append-only files in a configured directory, rotated by size. New files when a roll threshold hits.
- On Kafka recovery: drain disk spillover before resuming in-memory publishing.
- `/health` endpoint reflecting Kafka reachability, buffer depth, disk-spilled count.
- Producer services updated to POST to `http://sidecar:<port>/events` instead of directly to Kafka.
- Each producer paired with its own sidecar instance in `docker-compose.yml`.

## What to learn

- The producer-sidecar contract: producers don't know Kafka exists. Their failure mode is "sidecar unreachable" only.
- Bounded queue + producer-side backpressure decisions: when the queue is full, what happens? Block? Drop? Spill immediately? Pick deliberately.
- File I/O for append-only logs — `FileChannel` with `force()` for durability, or simpler `BufferedWriter` if performance allows.
- State-machine modeling — explicit states beat implicit booleans every time.

## Things to think about

- **Producer-to-sidecar channel.** HTTP localhost is fine. Could be Unix sockets for marginal efficiency; not worth it.
- **Backpressure on the producer.** If the sidecar's POST endpoint is slow (queue full), the producer's call slows. That's fine for a simulator; in production a real producer would need timeout handling.
- **Disk file format.** One JSON event per line. Simple to debug, replay, and recover.
- **Recovery ordering.** Disk-spilled events drain in arrival order before any in-memory events. Don't mix them — that's a footgun.
- **Java vs Go for the sidecar.** See [sidecar.md](../sidecar.md). Default Java; revisit if energy permits.

## Done when

- Three producers each have their own sidecar.
- Killing Kafka via `docker-compose stop kafka`:
  - Producers continue posting to sidecars successfully.
  - Sidecar metrics show buffer depth climbing, then disk spillover.
  - No events lost (verified by counting before kill, after recovery).
- Restarting Kafka:
  - Sidecars drain disk back to Kafka.
  - Sentinel sees the burst arrive in order.
  - Idempotency from [step 10](step-10-idempotency.md) prevents any reprocessing artifacts from retries.
- Demo recording-ready: this is the core wow-moment.

## Things to skip

- Multi-destination routing. Kafka only.
- Compression. Plain JSON over the wire is fine.
- Adaptive batching. Fixed batch size + flush interval.

## Look ahead

This step is the structural centerpiece of the system. Once sidecars work, the rest is enrichment, UI, polish. Spend time getting the failure-recovery transitions right because the demo lives there.
