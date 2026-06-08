# Tech Stack

Parent: [PLAN.md](PLAN.md)

Each choice is meant to be defensible in an interview — boring, recognizable, used well.

## Languages and runtimes

**Java 21.** Required for virtual threads (used in [concurrency.md](concurrency.md)). Modern Java features (records, pattern matching, sealed types) make the code feel current.

## Frameworks

**Spring Boot 3.x.** Default backend choice in the JVM ecosystem. Auto-config, Actuator, starters keep boilerplate down. Interviewers expect it; it's the path of least resistance.

**Spring for Apache Kafka.** Wraps the Kafka client with `@KafkaListener`, error handlers, retry, DLQ helpers. Use it but understand what it generates — don't be the candidate who can't explain consumer group rebalancing.

**Spring Data JPA + Hibernate.** For incident persistence. Basic — entities, repositories, transactions. Avoid Hibernate's exotic corners; keep queries explicit.

**Flyway.** Schema migrations. Versioned SQL, applied at startup. Mention "I treat schemas as code" in interviews.

## Infrastructure

**PostgreSQL 16.** Incident state, notification history, audit. One real database; no need for more.

**Redis 7.** Sliding-window counters, dedup keys with TTL, cooldown timers. Use the right data structures (sorted sets for windows, SET NX for dedup) — don't just use it as a key-value cache.

**Apache Kafka.** Event bus between sidecars and Sentinel. Use Confluent or Bitnami image in docker-compose. Single broker is fine for the demo; mention "would run 3+ in production" when discussing.

**Docker + docker-compose.** Single-command local stack. The demo lives or dies by `docker-compose up` working.

## Observability

**Micrometer + Prometheus.** Metrics from Spring Boot, scraped by Prometheus.

**Grafana.** One pre-built dashboard showing event throughput, anomaly counts, incident state, LLM latency.

**Logback with structured JSON logs.** Correlation IDs threaded through Kafka headers and HTTP requests.

See [observability.md](observability.md) for what to actually measure.

## LLM

**Ollama** running locally, default model **Qwen 2.5 7B** for structured output quality (revisit per [llm-integration.md](llm-integration.md)).

**Provider abstraction** so Claude or a mock can be swapped via config or per-request. The mock is critical for tests so CI doesn't need Ollama.

## Testing

**JUnit 5.** Default.

**Testcontainers.** Spin up real Postgres, Redis, Kafka in integration tests. Modern Java testing standard. See [testing.md](testing.md).

**Mockito** sparingly, mostly for the LLM client and external boundaries.

**Spring's `@SpringBootTest` slices** (`@DataJpaTest`, `@WebMvcTest`) where they fit.

## Code-level libraries

**Lombok — used pragmatically.** Add it where it pays: JPA entities (mutable, many fields → `@Getter @Setter @NoArgsConstructor`), `@Slf4j` for loggers, `@Builder` on complex DTOs. Skip it where records and constructor injection already do the job — don't sprinkle `@Data` on everything by reflex. The rule of thumb: add an annotation when you'd otherwise hand-write the same code; don't add one for its own sake.

## Build and CI

**Maven.** Familiar, well-supported, and the default for Spring Initializr. Build is straightforward; no need to learn a new DSL alongside everything else this project introduces.

**GitHub Actions.** Free for public repos. Runs tests + Testcontainers on push. A green badge on the README is worth the 30 minutes of setup.

## Dashboard

**HTMX + server-rendered HTML (Thymeleaf or a similar template engine).** Reasoning: backend role, don't want a full SPA build pipeline. HTMX gives ack/resolve buttons and live updates with ~0 JavaScript. Decide at [step 15](steps/step-15-dashboard.md).

Fallback: minimal React if HTMX feels limiting. Avoid full Next.js.

## Things deliberately NOT used

- **Kubernetes.** Time sink, no extra signal for this project. docker-compose is the right tool.
- **gRPC.** No interop need. HTTP + Kafka is enough.
- **Reactor / WebFlux.** Reactive types infect the codebase; virtual threads cover the same ground with simpler code.
- **GraphQL.** No client diversity to justify it.
- **A service mesh.** Three services don't need a mesh.
- **Config server / service discovery.** docker-compose DNS is sufficient.
- **OAuth / JWT / any auth.** Showcase project; auth is noise.

If asked "why didn't you use X" in an interview, the answer is some version of: *"X solves a problem I don't have at this scale. I'd add it when [specific trigger]."*
