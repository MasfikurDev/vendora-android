---
name: vendora-workflow-verification
description: Verify complete Vendora user journeys and state propagation across Compose screens, navigation, storage, and related records. Use after changing inventory, stock, sales, shop switching, dashboard, or history workflows.
---

# Vendora workflow verification

Prove the complete user-visible workflow works, not merely that a method or persistence write succeeds.

## Required workflow
1. Read root AGENTS.md, relevant worklog/status docs, and current PR/base/head. Never assume stacked PR features are on main.
2. Trace the journey from user action through composable/event handler, domain/store operation, persistence, navigation, and every screen displaying changed data.
3. Identify state owners and refresh triggers. Inspect remember keys, screen re-entry, lifecycle-aware collection, and data loaded only once. Treat remembered snapshots of mutable persisted data as a stale-state risk.
4. After a successful sale or stock adjustment, verify after navigating away and back: product quantity and inventory list; dashboard counts and aggregates; sale history and price/name snapshots; stock movement history with correct before/delta/after values; active-shop scoping and unaffected other shops.
5. For failure paths, prove no partial changes occur in any related record.
6. Prefer a focused regression test for the actual defect. Add UI/navigation coverage when supported; otherwise state the UI gap explicitly.
7. Run targeted tests, then broader unit tests/build where available. Report exact commands, outcomes, commit SHA, and CI run. Never infer success from workflow configuration or an earlier SHA.

## Vendora-specific risks
- Product and shop persistence currently use JSON in SharedPreferences on the feature stack; synchronous commit is an interim mechanism, not a database transaction or concurrency guarantee.
- Do not claim Room, SQLite transactions, Flow-driven UI, or emulator verification exists unless confirmed in current code and run evidence.
- The known dashboard remembered-products refresh issue must be checked when touching sales, stock, navigation, or dashboard code; do not assume it is fixed.

## Completion checklist
- [ ] Trace user action to persisted data and visible UI.
- [ ] Check success and rejection/error paths.
- [ ] Check shop isolation.
- [ ] Add regression coverage.
- [ ] Verify current branch and exact tested SHA.
- [ ] Record unverified device/UI behavior.
