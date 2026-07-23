# SentinelAI — Project Plan

## What this is

A backend showcase project for interview preparation. Three dummy services emit **raw signals** (loose, free-form events); a Sentinel service ingests them, **classifies** them into typed operational events, detects anomalies, correlates incidents, and uses a local LLM to generate summaries and remediation hints. The goal is a polished, demoable system that signals depth across modern Java backend skills — not a production-grade platform.

## Two-schema design

A deliberate architectural choice worth calling out up front:

- **`RawSignal`** — the external contract. Deliberately loose. Any producer (Java, Python, a legacy service dumping log lines) can emit these by POSTing JSON. No shared jar between producers and Sentinel.
- **`OperationalEvent`** — the internal contract. Strict, typed, produced by Sentinel's **classifier** from a `RawSignal`. Only Sentinel-internal stages see it.

The classifier is rules-first (fast, deterministic, cheap) with an LLM fallback for unmatched signals (see [step 12.5](steps/step-12.5-llm-classifier-fallback.md)). This shape mirrors real-world SIEM / AIOps platforms: heterogeneous inputs at the edge, structured pipeline inside.

## Audience and goals

- **Primary audience:** interviewers reviewing the GitHub repo and watching a short demo.
- **Primary goal:** demonstrate backend Java/Spring depth, distributed-systems fluency, and modern LLM integration.
- **Secondary goal:** provide a real codebase for the author to learn-by-doing — adding concepts incrementally, refactoring as understanding grows.

## The "wow moment"

Every design decision serves this 90-second demo:

> Three services emit raw signals. Sentinel classifies them into typed events — via rules for known shapes, via LLM fallback for the rest. Kafka is killed mid-demo. Sidecars buffer to disk. Kafka comes back. Events flush. Sentinel correlates a burst of classified payment failures into one incident. The local LLM (Ollama) generates a summary and remediation hint. The incident appears in the dashboard with a Slack-style notification.

See [architecture.md](architecture.md) for the system diagram and [demo-script.md](demo-script.md) for the full walkthrough.

## Scope decisions

**In scope:**

- 3 dummy producer services (Spring Boot)
- 1 sidecar binary (buffered event forwarder with disk spillover)
- 1 Sentinel service (ingest → detect → correlate → enrich → notify)
- Kafka, Postgres, Redis, Ollama — all via docker-compose
- Small dashboard (HTMX or minimal React) for incident view/ack/resolve
- Real LLM integration with provider abstraction
- Observability stack (Prometheus + Grafana, structured logs)

**Out of scope:**

- Authentication, authorization, multi-tenancy
- Live cloud deployment (local-only; demo via screen recording)
- Kubernetes
- Fancy ML for anomaly detection (sliding-window threshold rules are enough)
- Splitting Sentinel into 5 microservices (one service with internal modules; mention "would split in production" in interviews)

For the rationale behind each choice see [tech-stack.md](tech-stack.md).

## How this plan is structured

This file is the index. Detailed topics live in child documents — open them only when working on the relevant area, to keep context lean.

Live progress: [PROGRESS.md](PROGRESS.md) — current step, decisions made, concepts internalized.

**Cross-cutting topics:**

- [architecture.md](architecture.md) — components, data flow, system diagram
- [tech-stack.md](tech-stack.md) — what we use and why
- [llm-integration.md](llm-integration.md) — provider abstraction, Ollama, model override, benchmarking story
- [sidecar.md](sidecar.md) — buffered forwarder design, retry, disk spillover
- [concurrency.md](concurrency.md) — virtual threads, where parallelism pays off, Ollama tuning
- [observability.md](observability.md) — logging, metrics, health checks, Grafana dashboard
- [testing.md](testing.md) — Testcontainers strategy, what to test at each layer
- [demo-script.md](demo-script.md) — the 90-second wow-moment walkthrough

**Implementation steps** (one file each in [steps/](steps/)):

The project is built in ~18 small steps. Each step adds one concept, ends in a runnable commit, and may revisit and refactor earlier code. Rewriting is expected and encouraged — git history becomes the learning story.

| # | Step | Focus |
|---|------|-------|
| 01 | [Hello Spring](steps/step-01-hello-spring.md) | Spring Boot skeleton, REST, in-memory store |
| 02 | [Thread-safe incident store](steps/step-02-thread-safe-store.md) | Concurrency primitives applied to real code |
| 03 | [Postgres persistence](steps/step-03-postgres.md) | JPA, Flyway, transactions |
| 04 | [First dummy producer](steps/step-04-dummy-producer.md) | Multi-module Maven, second Spring Boot app, `RawSignal` ingest |
| 04.5 | [Rules-based classifier](steps/step-04.5-classifier.md) | `RawSignal` → `OperationalEvent` via rule engine |
| 05 | [Docker Compose foundation](steps/step-05-docker-compose.md) | Containerize, network, run end-to-end |
| 06 | [Kafka producer/consumer](steps/step-06-kafka.md) | Replace HTTP with Kafka, offsets, consumer groups |
| 07 | [Anomaly detection rules](steps/step-07-anomaly-detection.md) | Sliding-window counters, Redis |
| 08 | [Incident correlation](steps/step-08-correlation.md) | Group anomalies, dedup, state machine |
| 09 | [Three producers, distinct failures](steps/step-09-three-producers.md) | Payment, Order, Inventory with different failure shapes |
| 09.5 | [Rule polish + design patterns](steps/step-09.5-rule-polish-patterns.md) | Decorators for cross-cutting concerns, `Classifier` Strategy interface, structured-hints-first rules |
| 10 | [Idempotency and dedup](steps/step-10-idempotency.md) | Signal/event IDs, exactly-once-ish semantics |
| 11 | [Sidecar buffered forwarder](steps/step-11-sidecar.md) | Local HTTP, retry, disk spillover |
| 12 | [LLM integration v1](steps/step-12-llm-v1.md) | Ollama, single summary call on incidents |
| 12.5 | [LLM classifier fallback](steps/step-12.5-llm-classifier-fallback.md) | LLM proposes types for `UNCLASSIFIED` signals; human confirms → new rule |
| 13 | [LLM provider abstraction](steps/step-13-llm-abstraction.md) | Strategy pattern, mock for tests, per-request override |
| 14 | [Parallel LLM enrichment](steps/step-14-virtual-threads.md) | Virtual threads, fan-out summary + remediation + postmortem |
| 15 | [Dashboard UI](steps/step-15-dashboard.md) | HTMX list, ack/resolve, live updates, signal→event traceability |
| 16 | [Notification routing](steps/step-16-notifications.md) | Console + mock webhook, dedup, cooldown |
| 17 | [Observability polish](steps/step-17-observability.md) | Grafana dashboard, correlation IDs, classifier metrics, health checks |
| 18 | [Demo polish and README](steps/step-18-demo-polish.md) | Demo video, README, architecture diagram |

## Working rhythm

- **Work in small steps.** One concept per step. Commit when it runs.
- **Refactor freely.** A later step may rewrite an earlier step's code — that's the point.
- **Keep existing exercises.** `exercises/week-01-concurrency/` stays as foundational learning. The project will *use* those concepts in step 02.
- **No rigid timeline.** The 18 steps are sized for ~8 weeks at a relaxed pace, but move at whatever speed lets you actually internalize each concept.

## Open decisions (revisit later)

- **Dashboard tech:** HTMX vs minimal React vs server-rendered Thymeleaf. Decide at step 15 once the backend is stable.
- **Notification channels:** console always; mock webhook likely; real Slack/Discord webhook optional polish.
- **Sidecar language:** Java (consistent with the rest) vs Go (smaller binary, more "real" sidecar). Defaulting to Java for now; revisit at step 11.
- **Postmortem draft generation:** include or skip in LLM enrichment. Decide at step 14.
- **Rule DSL shape:** annotated Java class per rule vs a small config-driven engine (YAML). Decide at step 04.5.
- **Shared DTO jar between producers and Sentinel:** *decided against.* The wire contract is JSON; no jar. Producers stay language-agnostic; Sentinel deserializes into its own internal `RawSignal` class.
