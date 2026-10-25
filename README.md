# Vendora

Offline-first Android business management for owners who operate one or more businesses.

> **Current status (2026-10-25):** Early implementation, not production-ready. The active feature stack includes shop-scoped catalog/inventory, stock adjustments and movement history, a basic sales screen, and multi-product cart/checkout on feature branches. Persistence still uses JSON in SharedPreferences, not Room/SQLite. See [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md) for the branch-aware status ledger.

## Product direction

Vendora is intended to be a **business-management workspace**, not just an inventory counter. Each shop should feel like a manageable business account with its own identity, image-backed product catalog, sales activity, insights, and settings. The home screen should be an overview/dashboard, and selling should remain one tap away.

![Vendora five-screen concept board: Home dashboard, Products catalog, Sell checkout, Insights analytics, and Business profile](docs/images/vendora-product-direction.svg)

Read the full [Product Direction and UX Requirements](docs/PRODUCT_DIRECTION.md). The image is an illustrative concept, not a screenshot of implemented UI.

## Product goals

- Manage multiple shops under one owner, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Build a visual product catalog with product images.
- Make point-of-sale checkout fast and easy to access.
- Provide an honest business overview with charts based on persisted records.
- Track products, stock movements, and sales; purchases, returns, expenses, and advanced reports remain planned.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Make AI optional; ordinary business operations must never depend on an AI service.

## Technology baseline

- Kotlin, Jetpack Compose, Material 3
- Android Gradle Plugin and Kotlin versions in `gradle/libs.versions.toml`
- JDK 17, Android SDK 36
- GitHub Actions: debug APK build and JVM unit tests
- **Current persistence:** SharedPreferences + JSON for shops and products
- **Target persistence:** Room/SQLite with explicit migrations, introduced as a focused migration
- Coroutines/Flow, repository boundaries, and lifecycle-aware state as implementation evolves

Do not describe planned technologies or UI as already implemented. Keep the app small and add architecture only when a feature needs it.

## Product experience direction

The proposed mobile-first UX is documented in [Vendora Product Direction](docs/PRODUCT_DIRECTION.md), with an illustrative five-screen concept board at [docs/images/vendora-product-direction.svg](docs/images/vendora-product-direction.svg). It defines the business-profile model, dashboard-first home, always-accessible selling, image-first products, and insights. These are product requirements and concepts, not claims that every screen is already implemented.

## Product scope

1. Shop creation, business profiles, and switching
2. Image-backed shop-scoped product catalog and inventory
3. Audited stock movement ledger
4. Fast point-of-sale and sales history
5. Business dashboard, insights, and reporting
6. Purchases, returns, expenses, and profitability
7. Local export and backup
8. Optional AI provider boundary for read-only business questions

Staff permissions, multi-device sync, cloud services, forecasting, and specialized pharmacy workflows are later phases.

## Engineering rules

- Store money as integer minor units (poisha), never floating-point currency.
- Record stock changes as auditable movements; do not silently overwrite inventory history.
- Validate a complete sale/cart before changing inventory; persist sale lines, stock decrements, and audit movements together. Current SharedPreferences commit is an interim consistency mechanism, not a substitute for a database transaction.
- Snapshot relevant prices and costs on transaction lines so historical reports remain meaningful.
- Include shop ownership/scope in relevant data access paths and tests.
- Verify the complete user workflow after writes: inventory quantities, dashboard summaries, sales history, and stock-movement history must reflect the persisted result when revisiting screens. Do not assume persistence means the UI state refreshed.
- Charts and metrics must derive from actual persisted records, include date/metric definitions, and handle empty or incomplete data honestly.
- Product images need a deliberate offline storage, cleanup, and backup strategy.
- Use explicit Room migrations and test them when Room is introduced.
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

CI builds a debug APK and runs JVM unit tests. Instrumentation tests are present in the scaffold but are not currently run by CI. A passing CI run proves the configured build/tests pass; it does not prove the app is production-ready.

## Git and release workflow

- Keep `main` buildable.
- Use focused short-lived feature/documentation branches and open pull requests.
- Pushes to branches run CI; pull requests targeting `main` run checks.
- Successful pushes to feature branches publish branch-specific GitHub prereleases for review; these are **debug preview APKs**, not stable beta or production releases.
- Only a successful push to `main` creates the main beta prerelease. These are **debug APK beta builds**, not signed production releases.
- Production releases require a separately planned, signed release process and explicit version/tag policy.
- Do not merge pull requests without explicit authorization from the repository owner.

See [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), [architecture principles](docs/architecture/principles.md), [Product Direction](docs/PRODUCT_DIRECTION.md), and [Project status and transition history](docs/PROJECT_STATUS_AND_HISTORY.md).

## License

A license has not yet been selected. Do not assume this project is open source merely because the repository is public.

## Feature preview releases

Each successful feature-branch push is intended to publish a branch-specific GitHub prerelease after `assembleDebug` and `testDebugUnitTest` pass. The workflow run must be checked before claiming a release exists. Feature previews are temporary review builds; only `main` publishes the project beta channel.

## AI coding skills

Repository-specific and upstream Android coding-agent skills are maintained in [`.agents/skills/`](.agents/skills/README.md). They cover end-to-end workflow verification, Compose quality, inventory transaction integrity, testing, Git/PR workflow, and selected upstream Google Android guidance.
