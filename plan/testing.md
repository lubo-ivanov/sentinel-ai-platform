# Testing Strategy

Parent: [PLAN.md](PLAN.md)

## Goals

- High signal: tests catch real regressions, not implementation churn.
- Fast: full suite runs in <2 minutes locally.
- Realistic: integration tests use real Postgres, Redis, Kafka via Testcontainers — no mocks for infrastructure.
- CI-friendly: runs on GitHub Actions without manual setup.

## Test layers

**Unit tests** — pure logic, no Spring context, no I/O.

- Anomaly detection rules (given a sequence of counter values, does the rule fire when expected?).
- Incident correlation logic (does this anomaly match this open incident?).
- Sidecar buffer state machine (transitions between NORMAL / DEGRADED / DOWN / RECOVERY).
- LLM response parsing (handles malformed JSON, partial responses).

These should be the bulk of the test suite. Fast, focused, brutal.

**Slice tests** — one Spring layer at a time.

- `@DataJpaTest` for repositories and Flyway migrations.
- `@WebMvcTest` for REST controllers (with mocked services).

Use sparingly — only where the slice catches something unit tests can't.

**Integration tests** — Testcontainers, real infrastructure.

- End-to-end event flow: produce to Kafka → Sentinel ingests → incident created in Postgres → notification sent.
- Idempotency under duplicate event delivery.
- Anomaly threshold actually fires after N events in window.
- Sidecar buffers and recovers when Kafka is killed and restarted.

These are the highest-value tests. Slow but irreplaceable.

## What not to test

- **Spring framework itself.** No tests verifying `@Autowired` works.
- **Trivial getters/setters.** Records make most of these vanish; the rest aren't worth testing.
- **The LLM's actual output.** Test that the call happens, the response is parsed, errors are handled — not that the model produced "good" text.
- **Coverage percentages.** Aim for confidence, not a number. A 60%-covered codebase with smart tests beats 90% of trivia.

## Mocking strategy

- **Mock the LLM client** in tests via `MockClient` (which is a real implementation, not a test double — same interface used in production with `provider=mock`). This is cleaner than Mockito for the LLM layer.
- **Don't mock infrastructure** (Postgres, Kafka, Redis). Testcontainers runs the real thing in seconds.
- **Mock external HTTP** (mock webhook for notifications) with WireMock or a small embedded server.

## Testcontainers tips that earn interview points

- Use the **Singleton container pattern** — one Postgres/Kafka/Redis per JVM, shared across test classes. Cuts suite time dramatically.
- Use **`@DynamicPropertySource`** to wire container ports into Spring config. Don't hardcode.
- Pin container image tags. `postgres:16` is fine; `postgres:latest` is not — flaky CI guaranteed.
- For Kafka, use the **Confluent kafka-native** image when feasible — much faster startup than the standard image.

## CI

GitHub Actions workflow on push:

1. Set up Java 21.
2. `./mvnw verify` (compiles + unit tests + slice tests + integration tests via Failsafe).
3. Cache Maven and Docker layers.

Skip Ollama in CI — `MockClient` handles all LLM-dependent tests. The real Ollama runs only locally during development and demo.

## What "done" means for a step

A step is done when:

- The new code is tested at the appropriate layer (unit for logic, integration for cross-component flows).
- Existing tests still pass.
- The full suite runs in CI green.

Not "done" when:

- Tests pass but only because they were rewritten to match new behavior without thinking about what's actually being verified.
- New code is added without any tests at all "because it's just glue."

## Interview talking points

- "Testcontainers for real-infrastructure integration tests; LLM mocked via a same-interface implementation, not a test double."
- "Singleton container pattern keeps the suite under two minutes."
- "I test behavior at boundaries — anomaly rules, idempotency, buffer state machine — not coverage percentage."
