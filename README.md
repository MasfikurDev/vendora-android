# Vendora

Offline-first Android shop management for owners who operate one or more businesses.

> **Current status (2026-10-28):** Early preview, not production-ready. The app uses Room for shops, products, reusable brand/category/unit options, sale-line snapshots, and stock movements; includes a mobile-first shop dashboard, inventory editing, multi-item selling, and transactional checkout. The legacy SharedPreferences/JSON data is imported once into Room. Checkout integrity and shop-isolation instrumentation tests are being added; CI compiles instrumentation tests but does not run them on an emulator. Check [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) for the detailed status.

## Product goals

- Manage multiple shops under one owner, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Track products, brands, categories, units, barcodes, configurable low-stock thresholds, stock movements, sales, returns, expenses, and reports.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Make AI optional; ordinary business operations must never depend on an AI service.

## Technology baseline

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin and Kotlin versions in `gradle/libs.versions.toml`
- JDK 17, Android SDK 36
- GitHub Actions: debug APK build and JVM unit tests
- **Current persistence:** Room/SQLite via Room entities, DAO, and repository; SharedPreferences/JSON is retained only as a legacy import source
- **Schema status:** Room schema is version 2 on the reusable-product-details feature branch, with an explicit v1→v2 migration; CI/device migration verification is still pending
- Coroutines/Flow, repository boundaries, and lifecycle-aware state as implementation evolves

Do not describe planned technologies as already implemented. Keep the app small and add architecture only when a feature needs it.

## Product scope

1. Shop creation and switching
2. Shop-scoped product catalog and inventory
3. Audited stock movement ledger
4. Purchases, sales, and returns
5. Expenses, profitability, and reports
6. Local export and backup
7. Optional AI provider boundary for read-only business questions

Staff permissions, multi-device sync, cloud services, forecasting, and specialized pharmacy workflows are later phases.

## Engineering rules

- Store money as integer minor units (poisha), never floating-point currency.
- Record stock changes as auditable movements; do not silently overwrite inventory history.
- Use transactions for operations that update a sale and stock together.
- Snapshot relevant prices and costs on transaction lines so historical reports remain meaningful.
- Include shop ownership/scope in relevant data access paths and tests.
- Use explicit Room migrations and test each schema change; the feature branch adds a v1→v2 migration.
- Never commit credentials, signing keys, private API keys, or real customer data.
- AI output is untrusted input. Validate tool arguments and permissions, and calculate financial results in application code.

## Build and test

Requirements: JDK 17, Android SDK platform 36/build-tools 36.0.0, and the committed Gradle wrapper (Gradle 8.13).

**Windows PowerShell**
```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

**Linux / macOS**
```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

CI builds a debug APK, runs JVM unit tests, and compiles Android instrumentation tests. Instrumentation tests are not executed by CI because no emulator job is configured. A passing CI run proves the configured build/tests pass; it does not prove the app is production-ready.

## Git and release workflow

- Keep `main` buildable.
- Use focused short-lived feature/documentation branches and open pull requests.
- Pushes to branches run CI; pull requests targeting `main` run checks.
- Successful pushes to feature branches publish branch-specific GitHub prereleases for review; these are **debug preview APKs**, not stable beta or production releases.
- Only a successful push to `main` creates the main beta prerelease. These are **debug APK beta builds**, not signed production releases.
- Production releases require a separately planned, signed release process and explicit version/tag policy.
- Use GitHub auto-merge for agent-created PRs only when the repository policy permits it and the exact PR head checks pass.

See [Reusable product details and roadmap](docs/product/reusable-product-details.md), [Open data sources and catalog rules](docs/data/open-data-sources-and-catalog-rules.md), [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), [architecture principles](docs/architecture/principles.md), and [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md).

## License

Vendora application source code is licensed under the [Apache License 2.0](LICENSE). Third-party data, assets, and dependencies may have separate terms; see [Third-party notices and data licensing](THIRD_PARTY_NOTICES.md). Do not assume the Apache license overrides those separate terms.

## Feature preview releases

Each successful feature-branch push is intended to publish a branch-specific GitHub prerelease after `assembleDebug` and `testDebugUnitTest` pass. The workflow run must be checked before claiming a release exists. Feature previews are temporary review builds; only `main` publishes the project beta channel.
