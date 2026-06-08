# Step 12 — LLM Integration v1

Parent: [PLAN.md](../PLAN.md) | Design: [llm-integration.md](../llm-integration.md)

## Goal

When an incident is created, generate a one-paragraph human-readable summary using a local Ollama model. Display it in the incident record. Synchronous, single call, no abstraction yet — that's [step 13](step-13-llm-abstraction.md).

## What to build

- Ollama added to `docker-compose.yml` with the chosen default model pre-pulled (Qwen 2.5 7B or whatever proved best on the target hardware).
- An `OllamaClient` class — plain HTTP call to `/api/generate` or `/api/chat`.
- A prompt template — system message describing what an incident summary should contain, user message with the incident's structured details (rule that fired, recent events, severity).
- Structured output — instruct the model to return JSON with `summary`, `likely_cause`, `severity_assessment`. Parse and validate.
- New columns on `incidents`: `ai_summary`, `ai_likely_cause`, `ai_generated_at`. Migration via Flyway.
- Trigger: when correlation creates a new incident, call the LLM synchronously, store the result. (Async comes in [step 14](step-14-virtual-threads.md).)
- Fallback: on LLM error/timeout, mark `ai_summary_status = FAILED`, store nothing. Don't block incident creation.

## What to learn

- The Ollama HTTP API shape — request/response, streaming vs non-streaming.
- Prompt engineering basics — system prompts, structured output instructions, few-shot examples if needed.
- JSON parsing with retries on malformed output.
- Timeouts — local Ollama can take 5-30s. Set HTTP client timeouts accordingly.
- Why synchronous LLM calls are wrong for production but useful as a starting point — sets up the async refactor in [step 14](step-14-virtual-threads.md).

## Things to think about

- **Model choice.** Try 2-3 models on hand-picked test incidents. Note which produces valid JSON most reliably. The README's benchmark table starts here.
- **Prompt iteration.** First prompts will be bad. Plan to iterate. Save good prompt versions in a `prompts/` resource directory, versioned.
- **Determinism.** Set `temperature=0` for predictable demo output. Mention you'd raise it for diversity in production where appropriate.
- **Cost of leaving this synchronous.** Every incident creation now waits 5-15s. Ack the problem, fix it in [step 14](step-14-virtual-threads.md).

## Done when

- A new incident gets an AI-generated summary attached within ~30s.
- Malformed output / timeout / Ollama down → incident still exists, summary marked failed, system stays up.
- Manual API call returns the incident with summary populated.
- One unit test using a captured Ollama response (saved as a test resource) verifies parsing.

## Things to skip

- Provider abstraction — [step 13](step-13-llm-abstraction.md).
- Parallel enrichment (summary + remediation + postmortem) — [step 14](step-14-virtual-threads.md).
- Streaming — overkill for short summaries.
- Prompt caching / system-prompt prefix caching. Local Ollama doesn't expose this anyway.

## Look ahead

The naive sync call here gets refactored twice: into an abstraction in [step 13](step-13-llm-abstraction.md), then into parallel virtual-thread fan-out in [step 14](step-14-virtual-threads.md). That refactoring sequence is itself a great interview-story arc.
