# Project Status and Transition History

Last verified against source: **2026-10-28**  
Repository: [MasfikurDev/vendora-android](https://github.com/MasfikurDev/vendora-android)

This file records the latest known implementation and keeps historical transitions visible. CI/device claims must be verified against the exact commit and run.

## Current snapshot

### Implemented in source

- Kotlin, Jetpack Compose, Material 3 Android application.
- Shop create/select foundations and support for multiple shop records.
- Room/SQLite database with shop, product, sale-line, and stock-movement entities; DAO queries scope products and transactions by shop.
- Legacy SharedPreferences/JSON importer that validates records, imports in a database transaction, and preserves original preferences.
- Repository operations for shop/product persistence, product stock movements, and shop-scoped reads.
- Checkout runs in a Room transaction, validates cart stock before writing, snapshots selling price and cost, records sale lines, deducts stock, and appends stock movements.
- GitHub Actions builds a debug APK, runs JVM unit tests, compiles instrumentation-test sources without starting an emulator, verifies the APK signature and persistent signing certificate when secrets are available, and uploads a workflow artifact. Full emulator instrumentation is opt-in on manual dispatch and disabled by default.
- Run #252 passed workflow commit `b19d9aec2c961a88f13e04425c1d0249d6e7feb8`: build, JVM tests, instrumentation-test source compilation, signing checks, and artifact upload succeeded. Emulator execution was intentionally skipped; runtime instrumentation and a physical-device update test remain unverified.

### Known limitations / risks

- Early implementation; not production-ready for sole storage of real business records.
- Database migration/import scenarios need broader automated tests, including interrupted and legacy-format cases.
- Checkout and shop-isolation instrumentation tests exist, but a passing build that skips emulator execution does not prove their runtime behavior. Run the opt-in instrumentation job before relying on these tests as runtime evidence.
- Product deletion is currently a hard delete. Define archival behavior and audit retention before formal financial auditing.
- Database schema currently lacks foreign-key constraints and database-enforced case-insensitive SKU uniqueness. Repository validation helps but does not guarantee every write path enforces these rules.
- No verified physical-device update test is recorded here. APK update compatibility requires the same application ID and signing certificate and a version code greater than the installed app's.
- Backup/restore, returns, expenses, complete reports, barcode scanning, production signing, privacy review, and full UI/device coverage remain unfinished.
- No license has been selected.

## CI and feature-build behavior

The current workflow is `.github/workflows/android.yml`.

- Relevant pushes to `main` run CI.
- Pull requests targeting `main` run CI.
- Feature branches are built by manual workflow dispatch; feature pushes do not automatically run this workflow. The routine job uses a shallow checkout, Gradle build cache, and compiles instrumentation test sources without launching an emulator.
- Leave `publish_release` unchecked to upload a workflow artifact only. Enable it only when a GitHub prerelease is intentionally requested.
- Same-repository builds must use the persistent debug keystore secrets. Fork PR builds do not receive repository secrets and therefore use temporary signing.
- CI must verify APK signing and compare the APK certificate fingerprint to the configured keystore when that keystore is available.
- CI build success and artifact upload do not establish that a specific phone can update an installed app. Record device results separately.

## Source-of-truth rules

1. Source code and the current workflow define implemented behavior; planning documents do not prove implementation.
2. Verify the exact branch, commit SHA, and workflow run before claiming a check passed.
3. Separate JVM tests, emulator/instrumentation tests, APK assembly, and physical-device installation tests.
4. Preserve legacy data; do not use destructive migration fallbacks to make tests pass.
5. Update this file and the README when source behavior, CI, or release behavior changes.

## Transition history

### 2026-10-28 — Debug updateability and critical-flow validation

- Previous signing workflow changes allowed same-repository PRs to use the persistent debug keystore while keeping fork PRs secret-safe.
- Added explicit APK signing-certificate fingerprint verification against the configured persistent keystore.
- Added Android instrumentation regression coverage for atomic checkout, insufficient-stock rollback, and shop-scoped product/SKU validation. After recognizing the runtime cost, emulator execution was changed to an opt-in manual-dispatch input (`run_instrumentation`, default `false`) rather than part of routine CI.
- Corrected README and status text to describe the current Room implementation and manual feature-branch build trigger instead of the old SharedPreferences-only architecture and automatic feature-push release claims.
- Verification: run #252 passed commit `b19d9aec2c961a88f13e04425c1d0249d6e7feb8`, including instrumentation-test source compilation. Emulator instrumentation was skipped by design. Physical-device update compatibility remains unverified.

### 2026-10-24 — Earlier implementation and workflow notes

Earlier snapshots described SharedPreferences/JSON as the current storage and Room as future work, and described automatic feature-push preview releases. Those statements were true of older development stages or intended behavior but no longer describe the current `main` workflow. They are retained here as history rather than as current instructions.

## Recommended follow-up after this change

1. Inspect the exact CI run for the latest workflow commit and resolve compile, JVM-test, or signing failures. Routine CI compiles test sources; dispatch with `run_instrumentation=true` only when runtime database validation is needed.
2. Install its persistent-key APK over the current app if the signing fingerprint matches; if Android still rejects it, capture the exact Package Installer error and compare installed/new APK metadata.
3. Add explicit schema migrations and migration tests before changing Room entities or database version.
4. Design product archival, sale receipt grouping, and persistence-level SKU/ownership constraints with a tested migration plan.
5. Continue product UI and business workflows only alongside regression tests.


### 2026-10-28 — Historical CI run audit

The older run history was reviewed before consolidating the current workflow:

- [Run #40311983219](https://github.com/MasfikurDev/vendora-android/actions/runs/40311983219) and [run #40314397197](https://github.com/MasfikurDev/vendora-android/actions/runs/40314397197) completed their build and unit-test jobs successfully under the older workflow. Their feature-preview release steps also ran; that is historical behavior, not the current manual-dispatch policy.
- [Run #40250634201](https://github.com/MasfikurDev/vendora-android/actions/runs/40250634201) built the APK but failed compiling JVM tests: `ProductStoreTest.kt:194` had an unresolved `single` reference. Treat that as an old test-source compilation defect, not an APK-build failure.
- [Run #40316840056](https://github.com/MasfikurDev/vendora-android/actions/runs/40316840056) and [run #40316824097](https://github.com/MasfikurDev/vendora-android/actions/runs/40316824097) failed during dependency metadata validation because their respective branch dependency graphs could not resolve `androidx.lifecycle:runtime-ktx:2.9.4` and `androidx.lifecycle:runtime-ktx:2.8.7`. Those were old branch configurations; do not assume they describe current main.
- [Run #40317257417](https://github.com/MasfikurDev/vendora-android/actions/runs/40317257417) failed at the same dependency-validation stage for `androidx.lifecycle:runtime-ktx:2.8.7`; subsequent build/test/artifact steps were skipped. It was not evidence of a checkout-test failure.
- The current main dependency catalog uses `androidx.lifecycle:lifecycle-runtime:2.9.4`, and [run #40674001494](https://github.com/MasfikurDev/vendora-android/actions/runs/40674001494) passed build, JVM tests, instrumentation-test source compilation, persistent signing-certificate verification, and artifact upload on the merged workflow commit.

The actionable lesson is to identify the exact failing step and branch before changing code. Build success, test compilation, instrumentation execution, signing verification, artifact upload, and release publication are distinct outcomes. Do not add an emulator to routine CI to address dependency-resolution or JVM test-compilation failures.
