# Step 18 — Demo Polish and README

Parent: [PLAN.md](../PLAN.md) | Demo plan: [demo-script.md](../demo-script.md)

## Goal

Convert a working system into a *showcase*. README that sells the project in 30 seconds, an architecture diagram, a recorded demo video, polished commit history.

## What to build

- **README.md** at the repo root. Sections:
  - One-paragraph hook — what it is, why it's interesting.
  - Embedded demo video / GIF.
  - Architecture diagram (PNG or Mermaid).
  - Quick start — `docker-compose up` and a link.
  - Tech stack with one-line justifications (link to [tech-stack.md](../tech-stack.md) for depth).
  - Key design decisions — one paragraph each: provider abstraction, sidecar pattern, idempotency, virtual threads.
  - Benchmark table — model latency / quality comparison.
  - "What I'd do differently in production" — humility + depth signal.
- **Architecture diagram.** Hand-drawn (Excalidraw, draw.io) or generated (Mermaid). Saved in repo.
- **Demo recording.** Screen capture per [demo-script.md](../demo-script.md). Under 2 minutes. Embed as MP4 and convert a short clip to GIF for the README.
- **Commit history cleanup** (light). Don't rewrite the whole history — that erases the learning story. But squash obvious "wip" / "fix typo" commits if they make the log noisy. Keep meaningful refactor commits visible.
- **A `CONTRIBUTING.md` or `dev-notes.md`** if useful — how to run, how to test, where things live.
- **Final pass on code quality.** Run a code review on the whole thing — `simplify` skill works here. Look for dead code, half-finished comments, unused dependencies, inconsistent naming.

## What to learn

- The README is the most-read file in any project. Treat it as marketing.
- The first 3 lines decide whether someone keeps scrolling.
- A demo GIF in the README converts dramatically better than text.
- Recruiters and hiring managers don't read code. Engineers do — but only after the README hooks them.

## Things to think about

- **Honesty in the README.** Don't claim "production-ready" — claim what's true: "showcase / learning project demonstrating X, Y, Z." Honesty reads as confidence.
- **Recording quality.** Hide your dock, clean desktop, terminal with readable font size. Small things matter.
- **Voiceover or captions.** Either works. Silent demos with captions are easier to record and re-record.
- **The "in production I'd…" section.** This is where you bank all the things you skipped — auth, K8s, distributed tracing, exactly-once semantics, schema registry. List them with brief justifications. This shows scope discipline AND awareness.

## Done when

- README hooks an interviewer in 30 seconds.
- Demo video plays in the README without leaving GitHub.
- Architecture diagram is current and readable.
- A friend can clone the repo and run `docker-compose up` with no other instructions.
- Final code review found no embarrassing leftovers.

## Things to skip

- A landing page / project website.
- Marketing copy ("revolutionary," "next-generation," etc.). Boring is professional.
- Detailed API docs (Swagger/OpenAPI is nice but optional). The dashboard is the user-facing surface.

## Look ahead

After this, the project is interview-ready. Maintenance is optional; the showcase is done.

When practicing for interviews, walk through the project using this structure:
1. The problem domain (incident detection in a microservices environment).
2. The wow moment (sidecar + idempotency + LLM enrichment).
3. Design decisions and the trade-offs behind them.
4. What you learned that was new.
5. What you'd do differently or add next.

Every section of the README maps to one of these talking points. That's not an accident.
