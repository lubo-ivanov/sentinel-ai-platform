# Step 02 — Thread-Safe Incident Store

Parent: [PLAN.md](../PLAN.md)

## Goal

Make the in-memory store from [step 01](step-01-hello-spring.md) safe under concurrent reads/writes. Add a `POST /api/v1/incidents` endpoint and demonstrate (with a test) that concurrent posts don't lose or corrupt data.

## What to build

- `POST /api/v1/incidents` accepting an incident DTO, returning the created entity with an assigned ID.
- Refactor `IncidentStore` from `List<Incident>` (unsafe) through a few iterations:
  1. `synchronized` methods.
  2. `ConcurrentHashMap<UUID, Incident>`.
  3. Atomic ID generation via `AtomicLong` or just `UUID.randomUUID()`.
- A test that fires N concurrent POSTs and asserts all N show up.

## What to learn

- Why a plain `ArrayList` breaks under concurrent writes (data races, lost updates, `ConcurrentModificationException`).
- Trade-offs between `synchronized`, `ReentrantLock`, and concurrent collections.
- Why `ConcurrentHashMap` is usually the right answer for keyed stores.
- How to write a deterministic concurrency test with `CountDownLatch` or `Phaser`.
- The link between this and the `exercises/week-01-concurrency/` work — same primitives, applied to real domain code.

## Why this matters

The exercises folder taught these primitives in isolation. Now they're applied to a real type with real semantics. The git diff between iterations 1, 2, and 3 is itself a teaching artifact — keep meaningful commits.

## Done when

- POST works.
- A test fires e.g. 100 parallel POSTs and verifies the store contains 100 distinct incidents with distinct IDs.
- The README (or a code comment, sparingly) references the related exercise.
- Committed with separate commits for each concurrency iteration so the progression is visible in `git log`.

## Things to skip

- Deduplication of POSTs. That's [step 10](step-10-idempotency.md).
- Persistence. That's [step 03](step-03-postgres.md).
- Optimistic locking. Not relevant until there's a real DB.

## Look ahead

When Postgres lands in [step 03](step-03-postgres.md), the in-memory store goes away — but the lessons here (atomicity, race conditions, dedup-by-key) carry over to thinking about transactions and unique constraints.
