# LLM Integration

Parent: [PLAN.md](PLAN.md)

## Goals

- Generate incident summary, remediation hint, and (optional) postmortem draft.
- Run entirely offline via Ollama — zero cost, no API keys, works on a plane.
- Stay provider-agnostic so Claude / OpenAI / a mock can swap in without touching call sites.
- Support per-request model override so the README can show a real benchmark.

Built across [step 12](steps/step-12-llm-v1.md), [step 13](steps/step-13-llm-abstraction.md), and [step 14](steps/step-14-virtual-threads.md).

## Provider abstraction

A single `LlmClient` interface with a `summarize` (and `suggestRemediation`, etc.) method. Implementations:

- **OllamaClient** — default. Talks to local Ollama via its HTTP API.
- **ClaudeClient** — optional. Activated only if `ANTHROPIC_API_KEY` is set. Useful as a "look how easily this swaps" talking point.
- **MockClient** — returns canned summaries. Used in tests so CI doesn't need Ollama. Also useful as a fallback when Ollama is unreachable.

A small `LlmRouter` (also implements `LlmClient`) dispatches to the right implementation based on:
1. Per-request override, if specified.
2. Configured default provider, otherwise.

This is the **strategy pattern** in textbook form. Worth being able to explain it as such.

## Per-request model override

The `summarize` method accepts an optional `ModelOverride` carrying provider and/or model name. Default-call uses the configured defaults; override lets specific code paths pick a different model.

Use cases:

- **Severity-based routing.** Critical incident → bigger model. Low-severity → fast small model. One line of code, real architectural decision.
- **Benchmarking.** A small CLI or test harness runs the same incident through Phi-3.5, Llama 3.1, and Qwen 2.5 to populate the README's comparison table. Without per-request override, this requires restarts.
- **Demo control.** The demo path can pin a fast model for predictable timing regardless of global config.

## Model selection

Defaulting to **Qwen 2.5 7B** for structured-output reliability in early testing — but verify on the actual hardware and revisit. Candidates worth comparing:

- **Phi-3.5 Mini (3.8B)** — fastest, smallest, sometimes weaker JSON adherence.
- **Llama 3.1 8B** — strong baseline, well-known.
- **Qwen 2.5 7B** — often best at structured output in this size class.

Skip 70B+ models — RAM and latency make them impractical for a laptop demo.

## Structured output strategy

Force the model to return JSON matching a schema (summary, remediation steps as a list, severity assessment, optional tags). Two approaches:

- **Prompt engineering** with a clear schema in the prompt and a parsing layer that retries on malformed output.
- **Ollama's structured outputs / format=json** when the chosen model supports it well.

Pick the simplest that's reliable on the chosen model. If the model produces invalid JSON >5% of the time, try a different model before adding parsing complexity.

## Async by design

Local LLM calls are slow (5–15s on CPU, 1–3s on GPU/Apple Silicon). Treat enrichment as **asynchronous**:

1. Incident is created in Postgres.
2. It's immediately visible in the dashboard with status `enriching`.
3. A background worker (virtual-thread executor) calls the LLM.
4. When the result arrives, the incident is updated and the dashboard reflects it.

This is also how production systems with paid APIs are built — async insulates user-facing latency from LLM latency. So it's the correct design, not a workaround. See [concurrency.md](concurrency.md) for the parallel fan-out within enrichment.

## Failure handling

- **LLM timeout** (>30s): mark enrichment failed, store a placeholder, expose as a metric.
- **LLM returns garbage** (parse fails after N retries): same as timeout.
- **Ollama down**: fall back to MockClient with a clear "AI unavailable" tag, or just store the placeholder. Don't block incident creation on enrichment success ever.

## Ollama tuning worth mentioning

Ollama serializes requests by default. To benefit from parallel enrichment, set `OLLAMA_NUM_PARALLEL=2` or `3`. Worth measuring the difference and noting it in the README — it's the kind of detail that signals depth.

## Interview talking points this enables

- "Strategy pattern with provider abstraction — Ollama default, Claude optional, Mock for tests."
- "Async enrichment so LLM latency doesn't block user-visible flows."
- "I benchmarked three models on 20 synthetic incidents — here's the table."
- "I routed critical incidents to a larger model; it's a one-line config change because of the per-request override."
- "I tuned `OLLAMA_NUM_PARALLEL` after measuring that virtual-thread fan-out wasn't actually parallelizing."
