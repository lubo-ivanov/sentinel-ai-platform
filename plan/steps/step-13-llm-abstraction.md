# Step 13 — LLM Provider Abstraction

Parent: [PLAN.md](../PLAN.md) | Design: [llm-integration.md](../llm-integration.md)

## Goal

Refactor the direct `OllamaClient` usage into a provider-agnostic abstraction. Add a `MockClient` for tests. Optionally add a `ClaudeClient` activated by API key. Support per-request model override.

## What to build

- `LlmClient` interface with methods: `summarize(Incident)`, `summarize(Incident, ModelOverride)`.
- `ModelOverride` record carrying optional provider name and model name.
- Implementations:
  - `OllamaClient` (existing, refactored to interface).
  - `MockClient` — returns canned summaries based on event type. No I/O. Used in tests and as fallback.
  - `ClaudeClient` — optional, only registered as a bean if `anthropic.api-key` config is set. Demonstrates "swap providers via config" without forcing the dependency in the default build.
- `LlmRouter` — itself implements `LlmClient`, wraps a `Map<String, LlmClient>` and dispatches by override or configured default.
- All call sites switched to use `LlmClient` (not `OllamaClient` directly).
- Test that integration tests use `MockClient` via Spring profile (no Ollama needed in CI).

## What to learn

- The strategy pattern in idiomatic Spring (multiple beans of same interface, qualifier or map-based dispatch).
- Conditional bean registration (`@ConditionalOnProperty`) for the optional Claude client.
- The value of having tests not depend on heavy local infrastructure (Ollama).
- Why "interfaces at the boundary" is good design — internal modules shouldn't care which LLM is in use.

## Things to think about

- **Interface granularity.** Single `summarize` for now. When [step 14](step-14-virtual-threads.md) adds remediation and postmortem, decide whether each is a separate method or one method with a "task type" enum. Either is defensible — pick the one that keeps prompts and parsing simple.
- **Configuration shape.** `sentinel.llm.provider=ollama|mock|claude`, `sentinel.llm.model=qwen2.5:7b`. Override via env var for demo control.
- **MockClient realism.** Make canned responses look real — varied, reflecting the input. Helps in dev when Ollama is slow or off.
- **Per-request override usage.** Identify one or two real call sites that benefit. Severity-based routing is the obvious one — critical incidents get a bigger model.

## Done when

- Same incident produces summary via Ollama in dev, MockClient in test.
- Switching `sentinel.llm.provider=mock` in dev makes the system run without Ollama.
- A unit test verifies `LlmRouter` picks the right implementation given an override.
- Severity-based override demonstrated for at least one rule (or noted as a follow-up).

## Things to skip

- Full Claude integration if no API key — register the class, leave the bean dormant.
- Streaming. Short summaries don't benefit.
- Token counting / cost tracking. Local Ollama is free; mention "would track in production for paid APIs."

## Look ahead

The abstraction here is what makes [step 14](step-14-virtual-threads.md) clean — the parallel fan-out doesn't care which provider answers, just that the interface returns results.
