# Step 04.5 — Rules-Based Classifier

Parent: [PLAN.md](../PLAN.md)

## Goal

Turn `RawSignal` into `OperationalEvent` via a **rules-first** classifier. Deterministic, fast, cheap. Signals no rule matches become `type = UNCLASSIFIED` and surface in a triage bucket rather than being dropped.

LLM fallback is out of scope for this step — it comes in [step 12.5](step-12.5-llm-classifier-fallback.md), once LLM infrastructure exists.

## What to build

- `OperationalEvent` internal type (POJO/record inside Sentinel). Fields: `id`, `sourceSignalId`, `source`, `timestamp`, `type`, `severity`, `classification` (nested: `method`, `ruleId`, `confidence`), `payload` (extracted structured data).
- `FailureType` enum with the initial known types (e.g., `PAYMENT_PROVIDER_TIMEOUT`, `PAYMENT_DECLINED`, `PAYMENT_RETRY_EXHAUSTED`, `UNCLASSIFIED`). Expand as producers get richer.
- A `ClassificationRule` interface — given a `RawSignal`, either return an `OperationalEvent` or return empty (rule didn't match).
- A `RuleEngine` that runs the catalog of rules against each signal, first-match-wins, with logged tie-breaks if multiple match.
- Concrete rule implementations for the payment-service signals (regex over `message`, checks against `hints`).
- `ClassifierService` that: reads a `RawSignal`, runs the engine, produces an `OperationalEvent`, persists it to a new `operational_events` table. Signals with no match produce an `UNCLASSIFIED` event still stored (nothing dropped).
- Flyway migration `V3__create_operational_events.sql` — table linked to `raw_signals` by `source_signal_id`.
- Metrics: `classifier.matched{ruleId}`, `classifier.unclassified`, latency histogram.
- Rule unit tests for each rule; a service-level test asserting end-to-end signal → event mapping.

## Rule DSL — decide during the step

Two options; pick one and go:

- **A. Annotated Java classes.** Each rule is a `@Component implements ClassificationRule`, expresses match logic in code. Pros: type-safe, refactorable, easy to test. Cons: adding a rule requires a redeploy.
- **B. YAML-driven engine.** Rules defined in a config file, loaded at startup. Pros: dynamic, no redeploy for new rules. Cons: DSL design work, less type-safe.

Recommendation: **start with A** — annotated Java rules. The demo doesn't need dynamic reloading. You can migrate to B later if needed (or leave it as a "would evolve to config-driven in production" interview talking point).

## What to learn

- **Rules-first classification pattern** — how SIEM/AIOps tools split "known bad signature" from "novel event."
- **Confidence-and-provenance metadata** on every classified event: which rule matched, at what confidence, when. This is what makes classification debuggable in production.
- **First-match-wins vs all-match** — how to design an ordered rule catalog and what to do when multiple rules match (log a warning; pick the first; possibly refine rules later).
- **Never drop input.** Unclassified isn't a failure — it's a state. Surface it, count it, act on it.

## Things to think about

- **Rule ordering matters.** Put specific rules before general ones. Design a way to enforce order (annotation with a priority, or explicit list in a config).
- **What goes into `payload`?** The rule that matched decides what structured fields to extract from the signal — e.g., for `PAYMENT_PROVIDER_TIMEOUT`, extract `provider` and `durationMs` from `hints`. Downstream stages (anomaly detection) rely on these fields.
- **Severity is a policy, not a fact.** A rule assigns severity based on the type + context, not on what the producer said. Producers can hint (`hints.level = ERROR`), but the platform makes the final call.
- **How to test rules.** Golden-signal fixtures — for each rule, keep 2–3 example `RawSignal` JSON files that must classify to a specific event. When you add a rule, add its fixtures. Prevents regression when rule ordering changes.

## Done when

- Every `RawSignal` written to `raw_signals` produces a corresponding row in `operational_events`.
- Signals matching known payment rules classify with `method = RULE`, `ruleId` populated.
- Signals not matching any rule store as `type = UNCLASSIFIED`.
- Classifier metrics exposed via Actuator.
- Rules are unit-tested; a service-level test walks signal → event.

## Things to skip

- LLM fallback for `UNCLASSIFIED` — [step 12.5](step-12.5-llm-classifier-fallback.md).
- Dashboard triage view — [step 15](step-15-dashboard.md).
- Config-driven / hot-reloadable rules — evolution, not step-04.5 work.
- Multi-rule voting / confidence fusion — over-engineering for now; first-match-wins is fine.

## Look ahead

Once the classifier exists, the pipeline `RawSignal → OperationalEvent → anomaly → incident` is complete in shape (even if downstream stages don't exist yet). Step 07 (anomaly detection) consumes `OperationalEvent`. Step 12.5 adds an LLM to propose rules for the tail. Step 15 adds a human-in-the-loop UI for accepting LLM proposals into the rule catalog.
