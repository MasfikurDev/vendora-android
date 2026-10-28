# Project Status and Transition History

Last verified: **2026-10-28**  
Repository: [MasfikurDev/vendora-android](https://github.com/MasfikurDev/vendora-android)

This document is the status ledger for the repository. Update it when implementation, architecture, CI/release behavior, or the active development path changes. Use dated entries so older assumptions are not silently overwritten.

## Current snapshot

> This snapshot describes the repository state as inspected on 2026-10-28 and distinguishes the default branch from open pull-request work. Re-check live branch and CI state before treating these notes as current.

### Default branch baseline

- The last specifically recorded `main` commit is `39fda25cf4225e88a7f7906d2db4e9b8000226c1`; verify the live default branch before using this SHA as current.
- The app is Kotlin + Jetpack Compose + Material 3.
- Room-backed shop, product, and checkout foundations are present in the codebase history; Room schema version and reusable metadata vary across the stacked feature branches listed below.
- CI builds a debug APK and runs configured JVM unit tests. The current workflow does not execute Android instrumentation tests on an emulator/device.
- Debug preview/beta APKs are not production-signed releases.

### Open, dependent pull requests

| PR | Branch | Purpose | Dependency / status |
| --- | --- | --- | --- |
| [#30](https://github.com/MasfikurDev/vendora-android/pull/30) | `feat/reusable-product-details` | Reusable brand/category/unit options, product metadata, Room v1→v2 migration, legacy import updates | Open; base feature dependency |
| [#31](https://github.com/MasfikurDev/vendora-android/pull/31) | `feat/shop-type-taxonomy-open-data` | Offline shop-type catalog, Bengali labels, source/provenance guidance | Open; based on #30 |
| [#32](https://github.com/MasfikurDev/vendora-android/pull/32) | `docs/licensing-and-third-party-notices` | Apache-2.0 source license, third-party notices, project documentation and community guidance | Open; stacked on #31 |
| [#33](https://github.com/MasfikurDev/vendora-android/pull/33) | `feat/sales-history-activity` | Group checkout lines into sale transactions, add v2→v3 migration, and show shop-scoped sales activity | Open; based on #32; verify CI against the latest head SHA before merging |

Do not assume these PR changes are available on `main` until the relevant PRs are merged. Preserve dependency order: #30 → #31 → #32. Recheck each PR's current base, head SHA, required checks, and mergeability before merging or retargeting.

### Implemented or present in the active development stack

- Shop selection/creation and shop-scoped business data.
- Room persistence with a legacy SharedPreferences/JSON import path.
- Product metadata fields including brand, category, unit, barcode text, and configurable low-stock threshold.
- Reusable brand/category/unit options scoped by shop.
- Multi-item cart and checkout foundations designed to commit sale headers, sale-line snapshots, stock deductions, and movement records transactionally.
- Sales Activity screen with per-shop sale totals, timestamps, item counts, and expandable line details on the next feature branch.
- Bundled shop-type catalog with stable internal IDs, English/Bengali labels, aliases, and OSM reference tags.
- Instrumentation test source for checkout integrity, shop isolation, reusable options, and catalog invariants.

These are implementation and test-source statements, not a claim that all behavior has passed runtime verification. Consult each PR's exact-head CI run and execute instrumentation tests on a device/emulator before treating the relevant flows as verified.

### Known gaps and non-goals for the current preview

- Android instrumentation tests are not executed by the current CI workflow.
- The selected shop-type ID is not yet persisted as a canonical Room field; the existing business-type label is retained for compatibility.
- Camera barcode scanning, product image management, complete purchase/return/expense workflows, backup/restore validation, and production signing are not established as complete by this snapshot.
- Historical sale-line records cannot recover their original multi-item transaction grouping; the v2→v3 migration preserves each existing row as an individual legacy sale.
- Optional Open Food Facts lookup, GS1 interoperability, formal BSIC mapping, and customs HS-code workflows are research/future possibilities, not shipped integrations.
- No claim is made that Vendora verifies trade licenses, VAT/TIN status, pharmacy permissions, product authenticity, tax compliance, or customs compliance.
- No production support window, response-time SLA, or bug-bounty program is promised.

### Verification rules

1. The exact target branch's source and workflow are authoritative.
2. Check CI against the latest PR head SHA; an older green run does not validate a later commit.
3. Distinguish build success, JVM unit tests, instrumentation source compilation, and instrumentation runtime execution.
4. A release exists only when the workflow result and GitHub Releases page confirm it.
5. Update this snapshot when the dependency graph, feature status, CI behavior, or release policy changes.
6. Keep older contradictory entries only as dated history; do not use them as current implementation guidance.

## Transition history

### 2026-10-24 — Vendora auto-merge preference documented

- Repository owner requested automatic merging for new Vendora PRs only.
- Updated `AGENTS.md` and `docs/AI_AGENT_WORKLOG.md` to prefer GitHub auto-merge only after exact-head required checks pass and mergeability, draft status, blocking reviews, and material data-loss/security risks are checked.
- This documents agent behavior; it does not by itself configure repository-wide GitHub auto-merge or background monitoring. Reading branch-protection settings was denied by the connected GitHub integration, so repository-level enforcement remains unverified.

### 2026-10-24 — AI agent guidance and troubleshooting handoff added

- Expanded `AGENTS.md` with explicit branch/ref awareness, PR review boundaries, exact-SHA CI verification, truthful test reporting, and documentation maintenance rules.
- Added [AI Agent Worklog and Troubleshooting](AI_AGENT_WORKLOG.md) to capture verified Vendora-specific lessons, including stale README status, the interim SharedPreferences/JSON implementation, debug beta versus production release distinctions, and the gap between JVM tests and instrumentation coverage.
- Included a repeatable failure-triage protocol and session-handoff template.
- Kept unrelated OpenCode/OpenRouter and Vercel incidents out of Vendora's troubleshooting history because they are not verified as incidents in this repository.

### 2026-10-24 — CI/release policy clarified

- CI is configured to run on pushes to all branches, pull requests targeting `main`, and manual dispatch.
- Only a successful push event to `main` is intended to publish a beta prerelease. Feature-branch pushes and PR checks should not publish releases.
- Verified run #26 for main commit `532689d...` completed successfully.
- Beta artifacts are debug APKs. A signed production release is a separate future task and must not be conflated with beta publishing.

### 2026-10-24 — Stale project status corrected

- The old README claimed PR #8 was the pending integration and that no shop-management features existed. That status was stale and has been replaced with the current snapshot.
- The README now distinguishes implemented local shop/product foundations from the planned Room database and not-yet-built workflows.
- This document was added to retain an explicit history rather than silently removing outdated statements.

### Earlier baseline — Initial architecture intent

- Project goals established: offline-first operation, multiple shops under one owner, strict shop-scoped data, integer minor-unit currency, auditable stock movements, optional/read-only AI, and mobile-first UI.
- Room/SQLite, repository boundaries, Coroutines/Flow, and MVVM are target architecture principles. They must not be described as current implementation until the migration is completed.

## Next recommended sequence

1. Compile the instrumentation test source in CI, then run it on an emulator or physical test device when available.
2. Verify successful and failed multi-item checkout, stock ledger consistency, two-shop isolation, legacy import, and persistence after restart.
3. Add explicit Room migration tests before changing the schema from version 1.
4. Correct remaining UI state/lifecycle gaps and improve sales history, product images/barcode support, and backup/export.
5. Keep debug preview/beta builds separate from a signed production release process.


### 2026-10-28 — Room checkout integrity work

- Rechecked main and found the status documents were stale: Room entities/repository, shop dashboard, selling flow, and transactional checkout are present in source.
- Added instrumentation test coverage for successful checkout, insufficient-stock rollback, and cross-shop product access on branch `fix/checkout-integrity-prerelease`.
- CI will compile instrumentation test sources, but execution still requires an Android emulator or device; no emulator execution is claimed here.
- Release status must be confirmed from the actual GitHub Actions run and Releases page, not inferred from workflow configuration.


### 2026-10-28 — Open-source documentation and licensing baseline

- Added the Apache-2.0 license for Vendora-authored application source and a third-party notices document that separates source-code licensing from external data, assets, and dependency terms.
- Expanded the README, contributor guidance, security policy, documentation index, and pull-request checklist; added community conduct/support guidance, a changelog, and structured bug/feature issue forms.
- Rewrote the current status snapshot to distinguish the default branch from open dependent PRs #30 → #31 → #32. Historical statements that no license was selected or Room was not implemented are retained only as dated history.
- Added catalog governance requirements for exact-source provenance, license review, validation, and distribution attribution.
- No application build or test suite was run for this documentation-focused change. Confirm the exact PR head's CI before merge.
