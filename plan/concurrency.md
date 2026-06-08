# Concurrency

Parent: [PLAN.md](PLAN.md)

## Where concurrency actually appears

Three places in this project have concurrency worth thinking about:

1. **The incident store** — multiple Kafka consumer threads may try to create/update the same incident simultaneously. Solved with database-level guarantees (unique constraints, optimistic locking) plus careful service-layer logic. Introduced in [step 02](steps/step-02-thread-safe-store.md), revisited at [step 08](steps/step-08-correlation.md).

2. **Sliding-window counters in Redis** — Redis itself serializes commands per key, so the primitive is safe. The service layer uses Lua scripts or pipelines for multi-step atomic operations. Covered in [step 07](steps/step-07-anomaly-detection.md).

3. **LLM enrichment fan-out** — for each incident, multiple LLM calls (summary, remediation, optionally postmortem) can run in parallel. This is the only place async/parallel actually pays off in a meaningful way. Covered in [step 14](steps/step-14-virtual-threads.md).

Everywhere else (REST handlers, Kafka consumers, database writes) — let Spring and the platform handle concurrency. Don't add async.

## Why virtual threads, not CompletableFuture or Reactor

**Virtual threads (Java 21+, Project Loom)** are the right modern choice for this project:

- **Reads like blocking code.** No callback chains, no flatMap soup, no infectious reactive types. The mental model stays simple.
- **Massive interview signal in 2026.** Every Java interviewer asks about Loom. Having shipped real code that uses it puts the conversation on concrete ground.
- **Cheap.** Virtual threads are not OS threads — millions can exist. For a fan-out of 3 LLM calls per incident, the cost is essentially zero.

CompletableFuture works but is more ceremony for the same outcome. Worth knowing for interviews; don't pick it as the primary tool today.

Reactor/WebFlux is overkill — reactive types infect the entire codebase, and the only async hot spot is one method. Not worth it.

Spring `@Async` hides too much — implicit thread pools, awkward result coordination. Avoid.

## The pattern for parallel LLM enrichment

When an incident is created and needs enrichment:

1. Open a virtual-thread-per-task executor (in a try-with-resources).
2. Submit each LLM call (summary, remediation, etc.) as a task.
3. `get()` each future.
4. Combine results into the enriched incident.

The whole block is ~10-15 lines and reads top-to-bottom like sequential code. Wall-clock is the slowest single call rather than the sum.

For Java 21+ specifically, **Structured Concurrency** (preview) is even cleaner and more idiomatic — `StructuredTaskScope.ShutdownOnFailure` propagates cancellation if any task fails. Worth using if the JDK version supports it as a stable feature by then; otherwise a plain virtual-thread executor is fine.

## Ollama parallelism caveat

Ollama serializes requests across all clients by default. Firing 3 parallel calls from virtual threads will *not* speed things up unless `OLLAMA_NUM_PARALLEL` is set on the server side. The fix is one environment variable in docker-compose.

This is worth measuring and writing about — "I set up parallel enrichment, observed it wasn't actually parallel, traced it to Ollama's default config, and tuned it" is a much better story than "I used virtual threads."

## Where to NOT add concurrency

- **Kafka consumers.** Already concurrent via consumer groups. Adding async inside a listener invites offset-commit bugs.
- **REST handlers.** Sub-100ms. Not the bottleneck.
- **Database writes.** Postgres handles concurrency. Don't fight it with futures.
- **Sidecar event forwarding.** Sequential publishing with batching is simpler and fast enough.

The discipline is: **add async where it solves a measured latency problem, not because async sounds modern.**

## Interview talking points

- "I used Java 21 virtual threads to fan out LLM calls. Wall-clock dropped from ~30s sequential to ~12s parallel — measured."
- "I considered CompletableFuture and Reactor. Virtual threads kept the code linear and didn't infect the rest of the codebase with reactive types."
- "First measurement showed the fan-out wasn't actually parallel — Ollama serializes by default. Setting `OLLAMA_NUM_PARALLEL` was the real fix."
- "Everywhere else I let the platform handle concurrency — Kafka consumer groups, Postgres transactions, Redis atomic ops. Don't add async without a reason."
