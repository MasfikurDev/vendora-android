# Vendora

Offline-first Android shop management for owners who operate one or more businesses.

> **Status:** Project setup and architecture planning. The Android application scaffold has not yet been generated or verified to build.

## Product goals

- Manage multiple shops under one owner account, with strict shop-scoped data access.
- Work reliably offline; local storage is the source of truth.
- Track products, purchases, stock movements, sales, returns, expenses, and reports.
- Keep financial calculations deterministic and auditable.
- Support configurable retail business types without forcing every business into the same workflow.
- Make AI optional and read-only initially; ordinary business operations must never depend on an AI service.

## Planned technology

- Kotlin, Jetpack Compose, Material 3
- Room / SQLite for local persistence
- Coroutines and Flow
- Repository-based data access and MVVM presentation
- Unit, database, and UI tests
- GitHub Actions for build and test checks

Exact dependency versions and module boundaries will be finalized against the generated Android project and documented in architecture decisions. Avoid creating a large multi-module structure before there is a demonstrated need.

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

## Getting started

The native Android project scaffold is the next setup step. Once Gradle files are present, validate locally on Windows with:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

These commands are not expected to work until the Android project and Gradle wrapper have been generated.

## Workflow

- Keep `main` buildable.
- Use short-lived branches such as `feat/shop-switcher`.
- Use conventional commit messages, for example `feat: add shop switching`.
- Run relevant tests before opening a pull request.
- See [CONTRIBUTING.md](CONTRIBUTING.md), [AGENTS.md](AGENTS.md), and [architecture decisions](docs/decisions/README.md).

## License

A license has not yet been selected. Do not assume this project is open source merely because the repository is public.
