# Vendora AI Agent Worklog and Troubleshooting

Last updated: **2026-10-28**

This is a durable handoff for coding agents. It records repository-specific pitfalls and verified behavior; always inspect current source, PR state, and exact-SHA CI results before acting.

## Operating rules

1. Inspect the current branch/ref, relevant source and tests, README/status docs, and active PR before editing.
2. Make focused changes on a short-lived branch, use conventional commits, and open a PR instead of pushing unreviewed work to `main`.
3. Verify CI against the exact latest PR head SHA. A different branch or older successful run is not evidence.
4. Report JVM tests, emulator/instrumentation tests, APK assembly, signing checks, and physical-device tests separately.
5. Update the README and status/history docs when source behavior or CI policy changes.
6. Never claim a command, build, test, release, or device check passed unless the result was observed.
7. Do not commit credentials, keystores, API keys, customer data, or generated local build output.

## Current verified architecture notes

- Main source uses Kotlin, Compose, Material 3, Room/SQLite, a DAO, and `VendoraRepository`.
- The legacy SharedPreferences/JSON importer validates legacy records and imports them in a Room transaction. Original preferences are preserved. Migration-marker failure and retry behavior still warrant dedicated tests.
- Checkout in `VendoraCheckout.kt` validates the cart and stock before writing, then writes sale snapshots, stock deductions, and stock movements in one Room transaction.
- Repository product writes validate inputs and check case-insensitive SKU duplicates within a shop. The database does not yet enforce this invariant for every possible write path.
- Product deletion is currently a hard delete. Do not describe the inventory ledger as a complete immutable audit system until deletion/archival and transaction-history policies are designed.
- Room schema currently lacks explicit foreign-key constraints. Do not add constraints or change entity schemas without an explicit, tested migration plan.
- Do not describe SharedPreferences/JSON as the current primary persistence. It is the legacy import source.

## CI and debug APK updateability

Current workflow: `.github/workflows/android.yml`.

- Relevant pushes to `main` and pull requests targeting `main` run CI. Feature branches are built using manual workflow dispatch; feature pushes do not automatically run this workflow. Routine CI runs build/JVM tests and compiles instrumentation-test sources without an emulator. Emulator-backed execution is opt-in via `run_instrumentation` (default `false`) and must not run on routine pushes/PRs.
- Leave `publish_release` unchecked for a workflow artifact only. Check it only when a GitHub prerelease is explicitly wanted.
- Same-repository builds use the persistent debug keystore secrets. Fork PRs must not receive repository secrets and use temporary signing; those APKs cannot update persistent-key installs.
- A valid APK signature alone does not prove update compatibility. Compare the APK's SHA-256 certificate fingerprint against the configured persistent keystore, and confirm the application ID and version code.
- Android update compatibility requires the same application ID and signing certificate and a version code higher than the installed app. If the update still fails, capture the exact Package Installer error and compare metadata from both APKs instead of guessing.
- A successful CI run is not a physical-device update test. Keep the distinction explicit.

## Testing guidance

- Add JVM unit tests for pure business rules and validation.
- Add Room-backed instrumentation tests for transaction behavior, persistence, and shop isolation. Compile their sources in routine CI, but keep emulator execution opt-in unless the user explicitly requests a runtime test. Do not add emulator startup to every PR/push.
- Critical checkout regressions include successful sale, insufficient stock, rollback/no partial writes, price snapshots, totals, and cross-shop access rejection.
- Migration tests should cover empty storage, valid historical JSON, malformed records, duplicate/colliding IDs, partial import retry, and marker-write failure where practical.
- Do not claim instrumentation tests are CI-covered unless an emulator/device job is configured and the exact run passes.
- Never use destructive migration fallbacks to make tests pass or risk silently deleting user data.

## Troubleshooting protocol

When a build or integration fails:

1. Capture the exact error, failing step, branch, commit SHA, and run URL.
2. Separate the root cause from secondary failures.
3. Inspect existing dependency/toolchain versions; do not guess versions or silently upgrade them.
4. Make the smallest targeted fix and add a regression test when possible.
5. Re-run the narrow failing check and then the relevant full build/test set.
6. Verify the new run against the exact commit SHA and update the PR/status notes.
7. If blocked by missing credentials, permissions, runner limits, or unavailable tooling, state the precise blocker. Never expose credentials.

## Vendora PR merge policy

Prefer GitHub auto-merge only when available and when the exact latest PR head has all required checks passing, is not a draft, is mergeable, and has no unresolved blocking review or material data-loss/security risk. Never bypass branch protections. If auto-merge is unavailable or checks are missing/failing, report the blocker. This repository-specific preference does not authorize merging other repositories.

## Session handoff template

- **Branch / PR:**
- **Base SHA and head SHA:**
- **Files changed:**
- **Behavior completed:**
- **Checks actually run and result:**
- **CI run URL and tested SHA:**
- **Known limitations / follow-up:**
- **Docs updated:**
- **Merge status:** not merged unless authorized and safeguards pass.

## Transition entry — 2026-10-28

- Added persistent debug certificate fingerprint comparison to the Android workflow.
- Added emulator-backed instrumentation tests for checkout stock deduction, insufficient-stock rollback, and shop-scoped product/SKU validation.
- Updated README and project status to match the Room implementation and actual manual feature-branch workflow.
- Run #252 passed workflow commit `b19d9aec2c961a88f13e04425c1d0249d6e7feb8`: build, JVM tests, instrumentation-test source compilation, persistent signing-certificate comparison, and artifact upload succeeded; emulator instrumentation was skipped as configured. The physical-device update test remains unverified.

Older worklog entries claimed automatic feature-branch push prereleases and described Room as future work. Those are historical states, not the current workflow or architecture.

## CI cost and coverage policy — 2026-10-28

- Routine CI should provide fast deterministic feedback: build the debug APK, run JVM unit tests, compile Android instrumentation-test sources, verify APK signing/certificate consistency, and upload the artifact.
- Do not start an emulator on routine pushes or pull requests. Dispatch the workflow with `run_instrumentation=true` when actual Room/Android runtime validation is needed.
- Use shallow checkout and Gradle build caching; keep workflow artifacts for a short, useful period. Do not add expensive checks to the default path without a clear risk-reduction benefit.
- A skipped instrumentation step is an intentional time-saving choice, not a passed instrumentation test. Report it explicitly.
