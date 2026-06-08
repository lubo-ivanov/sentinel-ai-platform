# Step 05 — Docker Compose Foundation

Parent: [PLAN.md](../PLAN.md)

## Goal

`docker-compose up` starts both services + Postgres and the system runs end-to-end without anything installed locally except Docker.

## What to build

- `Dockerfile` for each service. Multi-stage build (Maven build stage → slim JRE runtime stage).
- Root `docker-compose.yml` with services: `postgres`, `sentinel`, `payment-service`.
- Postgres init via Flyway on Sentinel startup.
- Service discovery via Compose DNS — `payment-service` POSTs to `http://sentinel:8080/...`.
- Health checks in compose so dependent services wait.
- A `Makefile` or task runner script with `make up`, `make down`, `make logs`, `make rebuild`.

## What to learn

- Multi-stage Dockerfiles for Java — build-time JDK, runtime JRE, minimal layers.
- Compose `depends_on` with `condition: service_healthy`.
- How Compose networking works — service name as hostname.
- Volume management for Postgres data (named volume so data survives `down`/`up`).
- The `restart: unless-stopped` policy and when not to use it.

## Things to think about

- **Image size.** Use a slim base (`eclipse-temurin:21-jre-alpine` or `gcr.io/distroless/java21`). Compare image sizes before/after.
- **JVM flags.** Set `-XX:MaxRAMPercentage` so the JVM respects container memory limits. Don't hardcode `-Xmx`.
- **Local development workflow.** Some prefer running services from the IDE while infra runs in compose. Support both — the compose file should have a "minimal" mode for just infra.

## Done when

- `docker-compose up` brings the stack up cleanly from a clean clone.
- Logs show payment events being received by Sentinel.
- `docker-compose down -v` cleans up, next `up` starts fresh.
- README has a "Quick start" section with one command.

## Things to skip

- Kubernetes manifests. Out of scope.
- Production-grade secrets. Use plain env vars; mention "would use a secret manager in production" in interviews.
- Health checks beyond Spring Boot Actuator's defaults — refine in [step 17](step-17-observability.md).

## Look ahead

This compose file will grow — Kafka, Redis, Ollama, Grafana, Prometheus — but the structure stays. Treat each addition as one self-contained service block with its own health check.
