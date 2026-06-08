# Step 16 — Notification Routing

Parent: [PLAN.md](../PLAN.md)

## Goal

When an incident is created or its severity escalates, send a notification. Console always; mock webhook for "external" channels. Apply dedup and cooldown to avoid notification storms.

## What to build

- `Notifier` interface — `notify(Incident, NotificationKind)`.
- Implementations:
  - `ConsoleNotifier` — formatted log line, severity-colored.
  - `WebhookNotifier` — POSTs JSON to a configurable URL. The compose file includes a tiny "echo server" container that just logs incoming webhooks (httpbin or a 20-line Python receiver).
- Routing rules — config-driven mapping: severity → channels.
- Dedup: don't re-notify for the same incident within a cooldown window unless severity escalates. Track in Redis with TTL.
- Audit: persist every notification attempt to a `notifications` table (incident_id, channel, sent_at, status, response_code).
- Triggered by incident lifecycle events:
  - INCIDENT_CREATED.
  - INCIDENT_SEVERITY_ESCALATED.
  - INCIDENT_RESOLVED (optional, often noisy).
- Metrics: notifications sent, suppressed by cooldown, failed.

## What to learn

- The dedup vs cooldown distinction — dedup says "same notification, drop." Cooldown says "even if changed, wait before re-notifying."
- Why a webhook receiver in compose makes the demo tangible — interviewers see the JSON arrive, not just "notification was sent" in a log.
- Why notifications get persisted — auditability is a real interview talking point.

## Things to think about

- **Synchronous vs async sending.** Slow webhooks shouldn't block incident lifecycle. Send via the same virtual-thread executor used for enrichment, or a small dedicated one.
- **Retry on webhook failure.** One retry with backoff. Beyond that, log and move on. Don't build a notification queue.
- **Severity escalation logic.** "Same incident, severity went LOW → HIGH." Should re-notify. This is where dedup-on-fingerprint isn't enough.
- **Mock Slack/Discord.** Can format the payload to match real Slack webhook JSON shape — costs nothing and makes the demo feel realistic. "I formatted to match Slack's webhook contract; pointing at a real Slack URL would just work."

## Done when

- Console shows formatted notifications during demo flow.
- Webhook receiver container logs match.
- Triggering the same anomaly twice in a minute produces one notification, not two.
- Severity escalation produces a follow-up notification.
- Notification history visible via API and in the dashboard's incident detail view.

## Things to skip

- Real Slack/Discord/email integration. Mock webhook is enough.
- User preferences / notification routing per user. No users in this system.
- Templating engines for notification bodies. String formatting is fine.

## Look ahead

After this, only observability polish and the demo recording remain. The system is feature-complete.
