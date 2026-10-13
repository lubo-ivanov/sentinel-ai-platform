# Step 09.6 — Producer Signal Variety

Parent: [PLAN.md](../PLAN.md)

## Goal

Make the three producer services emit realistic, varied signals so the demo has depth and the LLM enrichment has richer context to work with. Currently each emitter sends the same hardcoded values every tick — same provider, same SKU, same message. This makes incidents look identical and LLM summaries repetitive.

## What to build

### payment-service
- Multiple providers: `stripe`, `adyen`, `paypal`, `braintree`
- Varying timeout durations (e.g. 3000ms–9000ms)
- Varying amounts and currencies (USD, EUR, GBP)
- Randomized messages: `"stripe timeout after 5000ms"` → `"payment gateway unresponsive after {N}ms"`
- Occasional different failure types: connection refused, SSL error, rate limit

### order-service
- Realistic order IDs (UUID or alphanumeric, not just `order-1234`)
- Varied state transitions: `PENDING→FAILED`, `CONFIRMED→CANCELLED`, `PROCESSING→STUCK`
- Multiple services in the checkout flow: `checkout`, `payment-gateway`, `fraud-check`, `fulfilment`
- Varied `duration_ms` for checkout degradation (4000ms–15000ms)
- Occasional different failure shapes: duplicate order detection, fraud score threshold exceeded

### inventory-service
- Multiple SKUs and warehouses (not just `SKU-123` / `EU-WEST`)
- Realistic mismatch values (expected vs actual vary per emit)
- Multiple warehouses: `EU-WEST`, `EU-CENTRAL`, `US-EAST`, `APAC`
- Occasional different failure: reservation lock timeout, warehouse capacity exceeded

## What to learn

- How to make realistic test data generators without over-engineering — `ThreadLocalRandom` pools, small helper methods
- Why LLM prompt quality depends on input richness — richer hints → better summaries → better demo
- The difference between variety (different values each time) and unpredictability (different signal types — keep that controlled)

## Things to think about

- **Anomaly rules still need to fire.** Rules key on `ruleId` + a subject field (e.g. `provider`). If `provider` varies per emit, each provider accumulates its own counter — bursts still fire, just per-provider. That's good: demo can show `stripe` incident vs `adyen` incident.
- **Keep signal type distribution weighted.** The primary failure type should still dominate (e.g. 70% `PAYMENT_PROVIDER_TIMEOUT`, 30% other). Otherwise anomaly rules won't burst reliably.
- **Message field is the LLM's richest input.** Vary it meaningfully — include the concrete value (timeout ms, mismatch count, order ID) so summaries read like real incidents.
- **Don't add new signal types here.** This step is about enriching existing signal shapes, not introducing new classification rules. New signal types belong in a separate step.

## Done when

- Each emit produces different hint values (provider, SKU, amount, duration, etc.) each time
- Anomaly rules still fire reliably (burst threshold still reached within demo timeframe)
- LLM summaries reference the specific values from hints (provider name, timeout duration, etc.)
- No new classification rules needed — existing rules handle all emitted signal types

## Things to skip

- New signal types or new anomaly rules
- Weighted probability framework — simple `ThreadLocalRandom` modulo is enough
- Realistic business logic in the producers — they are still pure simulators
