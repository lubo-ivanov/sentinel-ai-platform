# Step 06.5 — Multi-Broker Kafka (Prod-like Mode)

Parent: [PLAN.md](../PLAN.md)

## Why this step exists

Step 06 gets Kafka working end-to-end with a single broker. That's enough to learn client-side concepts (producer, consumer, offsets, DLQ) but hides everything that makes Kafka *interesting* as a distributed system — replication, ISR, leader election, failover.

This step bumps to a 3-broker cluster with `replication.factor=3` and `min.insync.replicas=2`, then demonstrates a broker-kill scenario where the topic stays available. That's a real demo moment for the interview story.

## What to build

### Compose
- Three Kafka services (`kafka-1`, `kafka-2`, `kafka-3`) in KRaft mode. Each with unique `KAFKA_NODE_ID`, matching `KAFKA_CONTROLLER_QUORUM_VOTERS` list, distinct advertised listeners.
- Shared `CLUSTER_ID` across all three (KRaft requirement).
- Update `bootstrap.servers` in payment-service and sentinel to `kafka-1:9092,kafka-2:9092,kafka-3:9092`. Any client hitting one broker gets the full metadata.

### Topic config
- Bump topic-creation config (in `AdminClient` code from step 06) so `signals.raw` and `signals.raw.dlq` use `replication.factor=3`, `min.insync.replicas=2`.
- Internal topics (`__consumer_offsets`, `__transaction_state`) also bumped to replication.factor=3 via broker env vars.

### Producer config
- `acks=all` (already the default with idempotence enabled).
- Verify: with `acks=all` + `min.insync.replicas=2`, a produce request only succeeds when 2 of 3 brokers acknowledge. Killing 1 broker keeps things working; killing 2 blocks writes.

### Demo scenario
- Start the stack, run payment-service emitting signals.
- `docker compose stop kafka-2` — one broker gone.
- Signals keep flowing, sentinel keeps consuming. Prove via `/actuator/metrics/classifier.duration` count still climbing.
- `docker compose start kafka-2` — broker rejoins, catches up via replication.

## What to learn

- **Replication and ISR.** What "in-sync replica" means, when a follower drops out of ISR, what triggers a shrink.
- **Leader election.** When leader broker dies, one of the ISR followers takes over. `unclean.leader.election.enable=false` (default in modern Kafka) — never elect out-of-sync replicas, prefer availability loss over data loss.
- **Controller quorum in KRaft.** With 3 controllers, tolerate 1 failure; with 5, tolerate 2. `(N/2)+1` alive needed.
- **acks + min.insync.replicas interaction.** `acks=1` gives you speed but you lose data on leader-crash-before-replicate. `acks=all` + `min.insync.replicas=2` gives you durability at the cost of write availability when the cluster degrades.
- **Rebalancing under failure.** Consumer group rebalancing when a partition leader moves.

## Things to think about

- **Quorum size.** 3 is minimum for meaningful HA (tolerates 1 loss). 5 tolerates 2 but doubles cost. For local demo, 3 is right.
- **Rack awareness.** In real prod you spread replicas across racks/AZs. Not applicable locally, but worth a comment in compose.
- **Startup time.** 3 brokers take longer to reach a healthy quorum. Adjust healthcheck `start_period`.
- **Log directory paths.** Each broker needs its own `KAFKA_LOG_DIRS`. Named volumes per broker, not a shared one.
- **Clean shutdown.** `docker compose stop` sends SIGTERM; Kafka handles it gracefully. `docker kill -9` simulates a hard crash — worth trying both in the demo.

## Done when

- 3 brokers running in compose, all healthy.
- Topics created with replication.factor=3, min.insync.replicas=2.
- Killing 1 broker: signals keep flowing, no data loss, sentinel keeps consuming.
- Killing 2 brokers: producer blocks (or fails after timeout) — proves min.insync.replicas is enforced.
- Restart of killed broker: it catches up via replication without operator intervention.

## Things to skip

- **Rack awareness / affinity.** Local compose has no racks.
- **Cross-broker security (mTLS, SASL).** Plaintext is fine; mention "would enable SASL in prod."
- **Tiered storage / KIP-405.** Way out of scope.
- **Cluster resizing / broker addition mid-flight.** Static 3-broker cluster.

## Look ahead

The failover demo pairs naturally with [step 11 sidecar](step-11-sidecar.md) — the sidecar's disk buffer covers *client-side* durability during a full Kafka outage; multi-broker replication covers *broker-side* durability during partial failure. Together they show two independent failure modes and their respective remediations.