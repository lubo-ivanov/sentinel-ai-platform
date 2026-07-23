# Step 09.5 — Rule Polish + Design Patterns

Parent: [PLAN.md](../PLAN.md)

## Why this step exists

By the end of step 09 there are 4–6 concrete rules across three producers. That's enough sample size to see genuinely reusable cross-cutting concerns — before that, extracting them is a guess. This step retrofits the rule engine with a small set of GoF patterns *justified by real duplication*, not speculation.

The initial `PaymentProviderTimeoutRule` is deliberately naive (free-text regex on `message`, no negation handling, structured hints under-used). This step tightens rule quality alongside introducing the patterns.

## What to build

### Rule quality
- Move from regex-on-free-text toward **structured hints as the primary signal**. Producers should emit `hints.errorCode`, `hints.httpStatus`, etc. Rules key off structured fields first; regex on `message` becomes a fallback.
- Negation handling — reject a match if a negation token ("no", "not", "without") appears near the keyword.
- Disambiguation — "connection timeout" vs "provider timeout" vs "read timeout" shouldn't collapse to the same `FailureType`.
- Per-rule confidence — return the confidence value in `OperationalEvent.Classification.confidence` so downstream stages can act on low-confidence classifications.

### Decorator pattern for cross-cutting concerns
Wrap `ClassificationRule` instances with reusable decorators. Each rule opts in to whichever wrapping stack makes sense for *it* — decorators are per-rule, not global.

Candidate decorators (add only what real duplication justifies):
- `NegationGuard(delegate)` — rejects a match if negation appears near matched tokens.
- `ConfidenceThreshold(delegate, min)` — only returns a match if the delegate's confidence exceeds `min`.
- `AuditRule(delegate)` — records every attempt (matched or not) for offline analysis. Feeds step 12.5's LLM fallback.
- `LoggingRule(delegate)` — structured log line per attempt.

Wiring lives in a `@Configuration` — `RuleEngine` doesn't know or care:
```java
@Bean
ClassificationRule paymentTimeoutRule() {
    return new NegationGuard(
             new AuditRule(
               new PaymentProviderTimeoutRule()));
}
```

### Strategy pattern for classifier types
Extract a `Classifier` interface with a `classify(RawSignalEntity) → OperationalEvent` method. Current `RuleEngine` becomes `RuleBasedClassifier` (implements `Classifier`). This unlocks:
- `LlmClassifier` (step 12.5) implements the same interface.
- `HybridClassifier` — rules first, LLM on miss — as a composition.

Wiring stays trivial; `ClassifierService` depends on `Classifier`, not on `RuleEngine`.

## What to learn

- **When patterns pay for themselves.** Introducing decorators with one rule is premature; with 3–4 they start earning their weight. Talking point: "I waited for the duplication before extracting the abstraction."
- **Composition over inheritance.** Every decorator is a wrapper, not a subclass.
- **Chain of Responsibility already exists** in the current `List<ClassificationRule>` iteration — this step doesn't add it, just names it.
- **Strategy vs Decorator.** Strategy chooses *which* classifier; Decorator adjusts *how* a specific rule behaves. Different axes.

## Things to think about

- **Don't over-decorate.** A decorator that wraps only one rule is dead weight; inline the logic. Only extract when 2+ rules need the same wrapper.
- **Intra-rule filter chains** (decomposing a single rule into staged predicates) — skip unless a rule genuinely needs 4+ conditions with confidence scoring. Premature otherwise.
- **Rule ordering.** With more rules, first-match-wins becomes non-obvious. Consider explicit `@Order` and log the applied order at startup (already logged; make sure it's stable across restarts).
- **Testing decorators.** Each decorator gets its own unit test with a mock delegate rule. Composition tests only for combinations you actually wire up.

## Done when

- 3–4 concrete rules exist across payment / order / inventory (from step 09).
- At least 2 decorators exist and are used by at least 2 different rules each.
- `Classifier` interface exists; `RuleBasedClassifier` implements it; `ClassifierService` depends on the interface.
- `PaymentProviderTimeoutRule` (or successor) uses structured hints as its primary signal, regex as fallback.
- Per-rule confidence values are non-zero and reflect actual match quality.

## Things to skip

- **Intra-rule filter chain.** Not needed at 3–4 conditions per rule.
- **Rule DSL / YAML config.** Java rules stay. Talking point: "YAML would be a natural next step for non-Java authors."
- **Retrofitting decorators to rules that don't need them.** Per-rule opt-in, not blanket application.

## Look ahead

Step 12.5's `LlmClassifier` slots into the `Classifier` interface introduced here. The `AuditRule` decorator's log is the exact input step 12.5 needs — unmatched signals with their attempted rules and rejection reasons.
