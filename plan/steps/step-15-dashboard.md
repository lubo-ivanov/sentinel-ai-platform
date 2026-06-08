# Step 15 — Dashboard UI

Parent: [PLAN.md](../PLAN.md)

## Goal

A minimal web UI for viewing incidents, their AI summaries, and acknowledging/resolving them. Good enough to feature in the demo — not a product.

## What to build

- Decision: HTMX + server-rendered Thymeleaf vs minimal React. Default: **HTMX + Thymeleaf** for simplicity. Revisit if HTMX feels limiting.
- Routes:
  - `GET /` — list of incidents, sorted by last_seen desc, with status badges and severity colors.
  - `GET /incidents/{id}` — detail view: timeline, AI summary, remediation, ack/resolve buttons, related events.
  - `POST /incidents/{id}/ack` — sets status to acknowledged.
  - `POST /incidents/{id}/resolve` — sets status to resolved.
- Live updates — HTMX polling every 2-5 seconds on the list page, or SSE for richer push updates if confident.
- Styling — minimal CSS or a tiny utility framework (Pico CSS, water.css). No Tailwind build step.
- Status badges, severity colors, "enriching..." spinner while LLM is working.

## What to learn

- HTMX core attributes — `hx-get`, `hx-post`, `hx-trigger`, `hx-swap`. Five attributes cover 90% of the UI.
- Thymeleaf basics for server-rendered fragments.
- Why HTMX feels productive for backend-focused projects — no build step, no client state management, no JSON contract to maintain separately.
- SSE in Spring (`SseEmitter`) if going that route.

## Things to think about

- **Authentication.** Skip. Showcase project. Mention in interviews.
- **Pagination.** If incidents grow past ~100, simple cursor pagination. For demo, fixed limit of 50 most recent.
- **Time formatting.** Relative ("3 minutes ago") with absolute on hover. Good UX for incident lists.
- **Mobile.** Don't bother. Demo is on a laptop.

## Done when

- Open browser, see live list of incidents.
- Click one, see full detail with AI summary.
- Ack and resolve buttons work; status updates without full page reload.
- New incidents appear within a few seconds of creation.
- Looks reasonable in screenshots — not embarrassing.

## Things to skip

- A full SPA.
- Charts inside the dashboard — Grafana handles that.
- User management.
- Complex filtering/searching. Maybe a simple status filter.

## Look ahead

The dashboard is what an interviewer sees first when they `docker-compose up`. Spend a tiny extra polish pass on it — clean fonts, good spacing, sensible colors. Cheap polish, big visual return.
