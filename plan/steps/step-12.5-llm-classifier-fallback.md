# Step 12.5 — LLM Classifier Fallback

Parent: [PLAN.md](../PLAN.md)

## Goal

Add an LLM-powered fallback classifier for `UNCLASSIFIED` signals. Runs on the cold path — batched, not per-signal — so LLM latency and cost don't sit on the hot ingest loop. Its output is a **proposal**, not a decision: it suggests a type + a candidate rule; a human accepts or rejects; accepted proposals become new rules in the catalog.

## Why this step exists here

Two reasons this comes after step 12 (LLM v1) rather than adjacent to step 04.5:

1. **Infra reuse.** The LLM provider abstraction from steps 12–13 is already in place, so this step is about wiring, not re-solving LLM plumbing.
2. **Rule catalog matures first.** By this point step 07 (anomaly detection) has run against classified events, step 09 has added more producers, and the rule catalog has grown. That gives the LLM meaningful few-shot examples ("here's how similar signals have been classified before") which improves proposal quality.

## What to build

- `LlmClassifierFallback` service that:
  - Periodically batches recent `UNCLASSIFIED` operational events (e.g. every 5 minutes, up to N events per batch).
  - Groups by `source` + similar `message` signature — clusters near-duplicate unclassified signals so the LLM sees one representative per cluster, not thousands of copies.
  - Calls the LLM with the signal(s) + the current rule catalog as context, prompting for: a proposed `type`, `severity`, `confidence`, and (bonus) a candidate rule expressed as JSON matcher shape (`message` regex, `hints` predicates).
  - Stores the proposal in a new `classification_proposals` table (status = `PENDING`).
- Flyway migration `V?__create_classification_proposals.sql` (number depends on how many migrations exist by then).
- Human-in-the-loop endpoints:
  - `GET /api/v1/proposals` — list pending proposals with the representative signal(s).
  - `POST /api/v1/proposals/{id}/accept` — accept: the proposal becomes a new rule; historical unclassified signals matching the new rule get re-classified.
  - `POST /api/v1/proposals/{id}/reject` — reject: proposal marked `REJECTED`; the signal cluster stays unclassified but suppressed from future proposals (with a TTL).
- Dashboard triage view (extends step 15) — surfaces the proposal queue.
- Metrics: `llm_classifier.proposals_generated`, `.accepted`, `.rejected`, `.latency`.

## What to learn

- **Cold-path vs hot-path LLM use.** Ingest classifier = hot path = rules only. Fallback = cold path = LLM OK. This distinction is critical: getting it backwards kills real systems.
- **Human-in-the-loop ML systems.** LLM proposes, human disposes. The feedback loop turns novel signals into deterministic rules over time.
- **Retroactive classification.** When a new rule is added, unclassified signals matching it should be re-classified so historical data catches up. Simple in principle; requires care around idempotency and ordering.
- **Prompt engineering for structured output.** LLM must return valid JSON matching a schema. Use Spring AI's structured output support or a small validator; retry on malformed output; log failures for prompt tuning.

## Things to think about

- **Clustering unclassified signals.** Without clustering the LLM will see near-duplicates thousands of times and burn tokens. Simple approach: hash normalized `message` (lowercase, strip UUIDs/numbers, take first N tokens); group by hash. Show LLM one representative per cluster with a count.
- **Rule generation is optional.** If the LLM struggles to produce a robust rule, it's still useful for it to just propose a `type` + `severity` for the batch. A human can hand-write the rule from the proposal.
- **Proposal decay.** Rejected proposals should suppress the signal cluster for a while but not forever — the rejection may have been wrong. Add a rejection cooldown (e.g. 7 days) after which a new proposal can be generated.
- **Cost bound.** The batch scheduler is a natural rate limit, but consider a daily token budget guardrail.
- **Confidence threshold.** Don't surface every proposal — hide low-confidence ones (or route them to a "low confidence" tab). Prevents dashboard noise.

## Done when

- Unclassified signals accumulate; the batch scheduler generates proposals.
- Proposals appear in the triage view; accepting one creates a new rule and re-classifies matching historical signals.
- Metrics show proposals generated and accept/reject rate.
- End-to-end test: emit a novel signal → LLM proposes → test accepts → same signal shape now classifies deterministically.

## Things to skip

- Fully autonomous rule addition (no human confirmation). Would be a disaster in production; even the demo shouldn't do it.
- Fine-tuning a small model for classification. Out of scope; the LLM proposal step is the interview story.
- Learning from rejections (RLHF-style). Overkill; a rejection just suppresses the cluster.

## Look ahead

This closes the loop the two-schema architecture opens: heterogeneous input at the edge, structured output in the pipeline, and a supervised way to grow the classifier over time. In interviews, this is the step to talk about when someone asks "how does the system handle unknown events?" — the answer isn't "we drop them" or "we call an LLM every time," it's "we surface them, propose deterministic rules, and grow the catalog under human review."
