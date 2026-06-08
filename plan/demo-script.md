# Demo Script — The 90-Second Wow Moment

Parent: [PLAN.md](PLAN.md) | Polished in [step 18](steps/step-18-demo-polish.md)

Every architectural decision in this project serves this demo. Build backwards from it.

## The narrative

> *"Three e-commerce services emit operational events. A sidecar fronts each one to handle transport reliability. The Sentinel service ingests via Kafka, detects anomalies, correlates them into incidents, and uses a local LLM to generate human-readable summaries and remediation hints. Watch what happens when infrastructure fails."*

## The 90-second walkthrough

**0:00–0:10 — The setup.** Show docker-compose up, the Grafana dashboard with steady event flow, three producer logs, and an empty incidents page.

**0:10–0:25 — Normal operation.** Trigger a simulated payment-provider issue in the Payment service. Watch:
- Producer logs the simulated failure.
- Sidecar metric ticks up (events received, events published).
- Sentinel logs ingestion.
- Anomaly counter in Redis crosses threshold (visible in Grafana).
- Incident appears in the dashboard with status `enriching`.
- A few seconds later, the LLM summary fills in.
- Notification appears in the console.

**0:25–0:50 — The failure.** Run `docker-compose stop kafka`. Watch:
- Sidecar buffer-depth metric climbs as events keep flowing in.
- After threshold, sidecar starts spilling to disk (visible in metric).
- Sentinel goes quiet — no events incoming, no new incidents.
- Producers keep emitting unaware. Their POST to localhost still returns 200.

**0:50–1:10 — The recovery.** Run `docker-compose start kafka`. Watch:
- Sidecar metric: disk-spilled events drain back through.
- Sentinel resumes consuming. Backlog flushes.
- A burst of payment failures during the outage now correlates into one incident, not many — idempotency at work.
- LLM enriches it with a summary that reflects the burst.

**1:10–1:30 — Close.** Click the incident in the dashboard. Show:
- The full event timeline.
- The AI-generated summary and remediation hints.
- Ack and resolve buttons.
- Notification history.

## What this demo proves to an interviewer

- **Distributed-systems fluency.** Buffering, retry, idempotency, recovery semantics — not theoretical, demonstrated.
- **Modern Java backend.** Spring Boot, Kafka, Postgres, Redis, Java 21 — all used well.
- **LLM integration done right.** Async, provider-abstracted, fails gracefully.
- **Operational thinking.** Metrics, health checks, structured logs — observable, not just running.
- **Polish.** It works on the first try. It looks good. It tells a story.

## Recording the demo

- **Tool:** any screen recorder. macOS built-in is fine.
- **Length:** under 2 minutes. Anything longer loses interest.
- **Voiceover:** optional but recommended. Even a quiet, factual narration is better than text overlays.
- **Speed:** real-time during the failure/recovery section so the buffering is visible. Fine to cut waits elsewhere.
- **Output:** MP4 or GIF embedded in the README. Hosted on the repo, not a third-party site.

## Common mistakes to avoid

- **Demo by text only.** "Trust me, it works" doesn't sell. Show the dashboard.
- **Too many panels on screen.** One Grafana dashboard, one terminal split into three, one browser tab. Three regions max.
- **Skipping the failure.** The wow moment IS the failure recovery. Don't cut it for time.
- **Letting the LLM be the focal point.** It's a feature, not the centerpiece. The system architecture is what's impressive.

## What the demo does NOT need

- A live cloud URL.
- A landing page or fancy dashboard styling.
- Real Slack/Discord integration — console + mock webhook is enough.
- Multiple model comparisons running live (those go in the README's benchmark table).
