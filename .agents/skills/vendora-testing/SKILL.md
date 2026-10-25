---
name: vendora-testing
description: Plan and add regression tests for Vendora Android behavior, especially inventory, sales, shop isolation, Compose UI state, and CI.
---

# Vendora testing and regression

## Select the right layer
- Pure calculations and validation: local JVM unit tests.
- Store/persistence logic: store tests with isolated contexts/preferences and explicit cleanup.
- Compose behavior: semantics-based Compose tests for user actions and state transitions when dependencies support them.
- Navigation and screen refresh: UI/navigation regression test where feasible; a store-only test does not prove screen state refresh.
- SQLite/Room behavior: database tests against in-memory SQLite once Room exists. Do not add Room just to satisfy this skill.

## Required transaction cases
For stock/sales changes, cover applicable cases:
- Successful sale updates quantities and creates matching sales/movements.
- Invalid or insufficient stock rejects the operation without partial changes.
- Duplicate cart lines are combined or rejected consistently.
- Wrong-shop product IDs cannot be sold or edited.
- Historical snapshots remain stable after product edits.
- Re-entering screens reflects persisted values.
- One shop's operations do not mutate another shop's records.

## Test discipline
1. Inspect existing dependencies, test source sets, test naming, and Gradle tasks before writing tests.
2. Add a regression test before or alongside a fix; prefer deterministic tests over timing-dependent sleeps.
3. Use fakes for controllable dependencies where practical; mock only when a fake is not feasible.
4. Assert all relevant records after failures, not only the exception.
5. Run a targeted test, then broader tests/build where feasible. Use the Gradle wrapper and supported commands.
6. If execution is unavailable, state tests were not run; source inspection is not test execution.
7. Report exact command, outcome, tested SHA, CI link, and remaining test/device gap.

## CI checks
- Inspect current PR status and latest head SHA. A green run for an earlier commit does not validate a later commit.
- Do not merge or mark ready when required checks are missing/failing or a material data-integrity/security issue remains.
