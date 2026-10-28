# Project Status and Transition History

Last verified: **2026-10-28**  
Repository: [MasfikurDev/vendora-android](https://github.com/MasfikurDev/vendora-android)

This document is the status ledger for the repository. Update it when implementation, architecture, CI/release behavior, or the active development path changes. Use dated entries so older assumptions are not silently overwritten.

## Current snapshot

> **Current implementation note (2026-10-28):** This section supersedes older snapshots below where they say Room, the inventory UI, or selling workflows are not implemented. Those statements are retained as historical context, not current status.

### Implemented on main at the last inspected revision

- Kotlin + Jetpack Compose + Material 3 mobile app with shop selection and a shop dashboard.
- Room version 1 database for shops, products, sale-line snapshots, and stock movements.
- One-time legacy SharedPreferences/JSON import with malformed-record validation and preservation of original data on import errors.
- Shop-scoped product CRUD and SKU duplicate checks, stock adjustments, multi-item cart, and checkout transaction intended to commit sale lines, stock deductions, and movement records together.
- JVM unit tests and debug APK build in CI. CI does not run Android instrumentation tests on an emulator.
- Latest inspected main SHA before this branch: `39fda25cf4225e88a7f7906d2db4e9b8000226c1`.

### Active checkout-integrity change

- Branch: `fix/checkout-integrity-prerelease`.
- Adds Android instrumentation tests for successful checkout stock/sale/movement consistency, insufficient-stock rollback, and shop isolation.
- These instrumentation tests require compilation and device/emulator execution before their behavior can be considered verified. Do not treat test source alone as a passing test result.
- Current prerelease artifacts are debug/preview APKs, not production-signed builds.

### Active reusable-product-details change

- Branch: `feat/reusable-product-details`, based on the checkout-integrity branch head `702c9ce30872901a3bdbb11510ac0929001ad867`.
- Adds brand, category, unit, barcode, and configurable low-stock threshold fields to products.
- Adds shop-scoped reusable brand/category/unit options in Room, retained when the product that introduced them is deleted.
- Adds explicit Room v1→v2 migration and updates the legacy JSON importer to accept optional metadata.
- Adds instrumentation coverage for saved-option reuse. CI compilation and the migration behavior must still be checked against the actual PR run; instrumentation tests are not executed on an emulator in current CI.
- Product barcode entry is implemented; camera scanning is not included. Further roadmap items are recorded in [Reusable product details](product/reusable-product-details.md).

### Implemented in the repository

- Native Android app using Kotlin, Jetpack Compose, and Material 3.
- Shop selection/create flow with supported business categories: General store, Grocery, Fashion, Electronics, Hardware, Stationery, Pharmacy.
- Shop metadata stored locally using SharedPreferences + JSON.
- A `Product` model and `ProductStore` on the feature branch, including shop-scoped reads/writes, SKU uniqueness per shop, basic validation, and product deletion.
- GitHub Actions workflow builds a debug APK and runs JVM unit tests.
- Main branch CI for commit `532689d03b667ec365d59a2f74bec3d22c9fd0fa` passed: [run #26](https://github.com/MasfikurDev/vendora-android/actions/runs/40093372479).

### In progress / not yet production-ready

- Pull request #13, [Shop selection UX](https://github.com/MasfikurDev/vendora-android/pull/13), is the current feature branch for shop-selection resilience and the initial shop-scoped product-storage foundation. Do not merge without explicit repository-owner approval.
- The main-branch screen is still primarily the shop-selection experience; a complete product catalog workflow is not yet integrated into the app UI.
- Shop and product persistence currently use SharedPreferences + JSON, not Room. This is an interim implementation and is not the planned long-term database.
- Automated CI runs JVM unit tests and builds the debug APK. Instrumentation tests are not configured as a CI emulator job.
- Automated main-branch releases are beta prereleases containing a debug APK, not signed production builds.
- No license has been selected; do not imply a license or open-source permissions.

### Not implemented yet

- Room database, DAO/repository persistence, tested migrations.
- Full inventory UI: product list/search, create/edit flows, stock adjustments, barcode support.
- Purchase, sale, return, expense, and reporting workflows.
- Backup/export and restore validation.
- AI provider integration.
- Multi-device synchronization, staff permissions, and production signing/release pipeline.

## Source-of-truth rules

1. The code and current GitHub workflow are authoritative for what is implemented.
2. The README summarizes the current product and engineering status; this file records more detail and transitions.
3. `AGENTS.md` and `docs/AI_AGENT_WORKLOG.md` define agent workflow and record verified troubleshooting lessons.
4. Architecture principles and product scope describe intended constraints, not proof that the corresponding feature exists.
5. Mark a planned item as implemented only after code and relevant tests exist.
6. Record observed CI results with a run link; never infer success from a commit alone.
7. If a document conflicts with this snapshot, correct it in the same change that discovers the conflict and add a history entry below.

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
