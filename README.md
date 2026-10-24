# Vendora

Offline-first Android shop management for owners who operate one or more businesses.

> **Status:** Native Kotlin + Jetpack Compose scaffold is in place. The Gradle 8.13 wrapper is committed, and wrapper-based CI has passed on `main`. PR #8 still needs branch synchronization and a passing check on its updated head before it can be merged. No shop-management features have been implemented yet.

## Product goals

- Manage multiple shops under one owner account, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Track products, purchases, stock movements, sales, returns, expenses, and reports.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Make AI optional and read-only initially; ordinary business operations must never depend on an AI service.

## Technology baseline

- Kotlin, Jetpack Compose, Material 3
- Room / SQLite for local persistence
- Coroutines and Flow
- Repository-based data access and MVVM as features are added
- Unit, database, and UI tests
- GitHub Actions for build and test checks

Dependency versions are centralized in `gradle/libs.versions.toml`. Keep the initial app small; introduce architecture packages and additional libraries when the first feature needs them.

## Initial scope

1. Shop creation and shop switching
2. Product catalog
3. Audited stock movement ledger
4. Purchases, sales, and returns
5. Expenses, profitability, and reports
6. Local export and backup
7. Optional AI provider boundary for read-only business questions

Staff permissions, multi-device sync, cloud services, forecasting, and specialized pharmacy workflows are later phases.

## Engineering rules

- Store money as integer minor units (for example, poisha), never floating-point currency.
- Persist timestamps consistently and make time-zone display decisions explicit.
- Record stock changes as auditable movements; do not silently overwrite inventory history.
- Use transactions for operations that update a sale and stock together.
- Snapshot relevant prices and costs on transaction lines so historical reports remain meaningful.
- Include shop ownership/scope in relevant data access paths and tests.
- Use explicit Room migrations and test them.
- Never commit credentials, signing keys, private API keys, or real customer data.
- AI output is untrusted input. Validate tool arguments and permissions, and calculate financial results in application code.

## Requirements

- JDK 17
- Android SDK platform 36 and build-tools 36.0.0
- Gradle 8.13 (downloaded automatically by the committed Gradle wrapper)

## Build and test

From the repository root:

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

CI currently builds the debug APK and runs JVM unit tests. The instrumentation smoke test exists but is not yet run by CI because an Android emulator job has not been configured.

## Workflow

- Keep `main` buildable.
- Use short-lived branches such as `feat/shop-switcher`.
- Use conventional commit messages, for example `feat: add shop switching`.
- Run relevant tests before opening a pull request.
- See [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), and [architecture principles](docs/architecture/principles.md).

## License

A license has not yet been selected. Do not assume this project is open source merely because the repository is public.
