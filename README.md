<div align="center">

# Vendora

### One workspace for every business you run.

**An offline-first Android shop-management app built for practical retail operations, reliable local data, and a workflow that can grow with the business.**

[![Android CI](https://github.com/MasfikurDev/vendora-android/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/MasfikurDev/vendora-android/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Offline first](https://img.shields.io/badge/Architecture-Offline--first-0A7B83)](#architecture-at-a-glance)

**Project status:** Early implementation · Not production-ready

</div>

---

## At a glance

Vendora is designed as a **business-management workspace**, not just a product list. An owner can manage multiple shops from one app, keep each shop's records scoped to that business, and perform essential operations without depending on a network connection.

The current foundation is Kotlin, Jetpack Compose, Material 3, and Room over SQLite. Product and stock operations are being built around explicit repositories and transactional data changes, with particular attention to checkout integrity and recoverable legacy imports.

> **Important:** Vendora is under active development. Do not use it as the only storage location for real business records. Backup/restore, broader migration testing, security review, and production release readiness are not complete.

## Contents

- [Product direction](#product-direction)
- [Current implementation](#current-implementation)
- [Architecture at a glance](#architecture-at-a-glance)
- [Data integrity principles](#data-integrity-principles)
- [Technology stack](#technology-stack)
- [Get started](#get-started)
- [Build and test](#build-and-test)
- [CI and installable debug builds](#ci-and-installable-debug-builds)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Project documentation](#project-documentation)

## Product direction

Vendora's intended navigation is organized around five clear destinations:

| Destination | Purpose |
|---|---|
| **Home** | Business overview, recent activity, key metrics, and low-stock attention |
| **Products** | Product catalog, reusable product details, prices, barcodes, and stock levels |
| **Sell** | Fast checkout with clear totals and safe stock deduction |
| **Insights** | Sales trends, profit analysis, and business reporting |
| **Business** | Business identity, shop settings, and active-shop switching |

This table describes the **product direction**, not a claim that every screen or workflow is already implemented. The current app remains in an early implementation phase.

The product is intended to support different retail business types while keeping common workflows consistent. Reusable brands, categories, units, barcodes, and low-stock thresholds should reduce repeated data entry without forcing every shop into one rigid template.

## Current implementation

The following capabilities have foundations in the current source. They should not be interpreted as complete production features.

- **Multiple-shop foundations:** create/select shop records and scope product and transaction operations to a shop.
- **Local persistence:** Room/SQLite database with DAO and repository access.
- **Product and stock records:** product persistence and stock movement records.
- **Transactional checkout path:** validate cart stock before writes, record sale lines and price/cost snapshots, deduct stock, and append stock movements in a Room transaction.
- **Legacy data import:** import from older SharedPreferences/JSON records with validation and transactional writes while preserving the original preferences.
- **Automated checks:** debug APK build, JVM unit tests, instrumentation-test source compilation, APK signature verification, and persistent signing-certificate verification when configured.

### Known gaps

- Room schema migration and legacy-import recovery require broader tests.
- Instrumentation tests must be executed to validate runtime behavior; compiling their sources is not the same as passing them.
- Persistence-level foreign-key constraints and database-enforced case-insensitive SKU uniqueness are not yet complete.
- Product deletion is currently a hard delete; a retention/archive policy is needed before relying on historical reporting.
- Returns, expenses, backup/restore, and broader financial reporting need further implementation and testing.
- Physical-device update installation has not been verified across devices.
- Production signing, privacy/security review, and a production release process remain outstanding.

## Architecture at a glance

```text
Compose UI (Android)
        |
        v
Repository / business operations
        |
        v
Room DAOs + SQLite transactions
        |
        +--> Shops and shop-scoped records
        +--> Products and stock movements
        +--> Sales and sale-line snapshots
        |
        v
Local-first operation; network is not required for core flows
```

**Architecture rules**

1. Keep UI rendering separate from persistence and business rules.
2. Scope reads and writes to the active shop where the data belongs to a shop.
3. Perform sale creation and stock deduction atomically.
4. Store money as integer minor units (poisha), not floating-point values.
5. Snapshot relevant prices and costs on sale lines so later product edits do not rewrite transaction history.
6. Treat stock movements as an auditable record of inventory changes rather than silently overwriting history.
7. Use explicit Room migrations. Never introduce destructive migration fallbacks for real user data.
8. Keep AI optional. Financial calculations and permission checks must remain deterministic application responsibilities.

## Data integrity principles

Retail software must protect the records behind each sale. Vendora follows these principles as its implementation evolves:

- **Atomic checkout:** a sale must not be partially recorded if stock validation or a write fails.
- **Shop isolation:** one shop's operations must not accidentally read or mutate another shop's records.
- **Auditable inventory:** stock changes should have a reason and a movement record.
- **Stable money representation:** use integer minor units and explicit rounding rules.
- **Safe evolution:** validate schema and import changes with tests before changing persisted data.
- **Honest verification:** a successful build is not proof that runtime database behavior or device installation works.

These principles guide the implementation; they do not mean every persistence-level safeguard is already complete.

## Technology stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Persistence | Room / SQLite |
| Build | Gradle Wrapper, Android Gradle Plugin, Kotlin Gradle Plugin |
| Code generation | KSP |
| Automated verification | GitHub Actions, JVM unit tests, optional Android instrumentation |
| Java toolchain | JDK 17 |
| Android SDK | Compile/target SDK 36; minimum SDK 26 |

Dependency versions are maintained in `gradle/libs.versions.toml` and the Gradle build files.

## Get started

### Prerequisites

- JDK 17
- Android SDK platform 36
- Android SDK Build-Tools 36.0.0
- Git and the repository's committed Gradle Wrapper
- Android Studio or another compatible Kotlin/Android development environment

Clone the repository:

```shell
git clone https://github.com/MasfikurDev/vendora-android.git
cd vendora-android
```

Open the project in Android Studio, allow Gradle sync to finish, and ensure the required Android SDK packages are installed.

## Build and test

### Windows PowerShell

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

To run Android instrumentation tests on a connected device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Linux / macOS

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

To run Android instrumentation tests on a connected device or emulator:

```shell
./gradlew connectedDebugAndroidTest
```

The first two commands do not require an emulator. The connected instrumentation task requires a connected Android device or emulator.

## CI and installable debug builds

Vendora's workflow is designed to give quick feedback on routine changes without starting an emulator on every push or pull request.

| Workflow action | Routine behavior |
|---|---|
| Debug build | Runs |
| JVM unit tests | Runs |
| Instrumentation-test source compilation | Runs |
| APK signature verification | Runs |
| Persistent signing-certificate comparison | Runs when the persistent signing key is configured |
| Emulator-backed instrumentation | Opt-in; disabled by default |
| Artifact upload | Runs after successful verification |
| GitHub prerelease publishing | Only when explicitly requested during manual dispatch |

### Build a feature APK

1. Open **GitHub → Actions → Android CI and Release Builds**.
2. Select **Run workflow** and choose the branch you want to test.
3. Leave `publish_release` unchecked if you only need a downloadable workflow artifact.
4. Enable `run_instrumentation` only when you want the slower emulator-backed runtime tests.
5. Download the APK from the completed run's artifacts, or from the prerelease page if publishing was intentionally enabled.

Routine CI does not automatically build every feature-branch push. Manual dispatch is the intended path for a feature APK.

### Updating an existing installation

Android permits an in-place update only when the APK is compatible with the installed app. In practice, verify all of the following:

- The **application ID** remains `dev.vendora.android`.
- The APK is signed by the **same signing certificate** as the installed version.
- The new APK has a **higher version code**.
- The installed build was not signed with a temporary or different key.

Same-repository CI builds can use the configured persistent debug signing secrets. Fork pull requests must not receive those secrets and use temporary signing; those APKs may not update an installation signed with the persistent key.

If Android rejects an update, record the exact APK source and compare signing-certificate fingerprints before changing CI or uninstalling the existing app. Uninstalling can delete local application data.

**Debug previews and beta artifacts are for testing, not production releases.** Do not keep the only copy of real business records in a test build.

## Roadmap

The roadmap is intentionally incremental: data correctness first, then reliable everyday workflows, then richer business insights.

- [x] Room-backed persistence and repository foundations
- [x] Shop-scoped product access foundations
- [x] Transactional checkout and stock-movement path
- [x] CI verification and debug-signing consistency checks
- [ ] Broader Room migration and legacy-import regression coverage
- [ ] Database-enforced ownership and SKU integrity constraints
- [ ] Physical-device update verification
- [ ] Product archive and transaction-history retention policy
- [ ] Complete purchase, return, expense, and reporting workflows
- [ ] Local export and tested backup/restore
- [ ] Refined mobile-first business dashboard and catalog experience
- [ ] Optional AI assistance behind a validated, privacy-aware provider boundary

A checked item means the foundational implementation exists; it does not imply production-grade completeness.

## Contributing

Contributions should keep `main` buildable and focus on one coherent change at a time.

1. Create a short-lived branch from the latest `main`.
2. Keep UI, business logic, and persistence responsibilities separated.
3. Add or update tests for changes to money, stock, checkout, shop isolation, and database migrations.
4. Never commit signing keys, credentials, private API keys, or real customer data.
5. Open a pull request describing the behavior change, tests run, migration implications, and remaining limitations.

Before merging, inspect the exact commit and CI run. A skipped instrumentation step must be reported as skipped, not passed.

## Project documentation

- [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) — current implementation, known risks, and decisions.
- [AI agent worklog](docs/AI_AGENT_WORKLOG.md) — development history and agent-facing guidance.
- [Agent instructions](AGENTS.md) — repository rules and workflow constraints.
- [Contribution guide](CONTRIBUTING.md) — development and pull-request expectations.
- [Architecture principles](docs/architecture/principles.md) — design and data-integrity rules.

## License

No license has been selected yet. A public repository is not automatically open source; until a license is added, do not assume permission to reuse or redistribute the code.

---

<div align="center">

**Vendora** · Build for the realities of running a business.

<sub>Early-stage software. Verify before relying on it for business-critical records.</sub>

</div>
