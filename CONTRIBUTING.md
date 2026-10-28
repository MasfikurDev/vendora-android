# Contributing to Vendora

Thank you for considering a contribution. Vendora is an early-stage Android project; focused, testable changes and accurate status reporting are more useful than large speculative refactors.

## Before starting

1. Read the [README](README.md), [project status ledger](docs/PROJECT_STATUS_AND_HISTORY.md), [architecture principles](docs/architecture/principles.md), and [AI agent guidelines](AGENTS.md) if using a coding agent.
2. Search existing issues and pull requests before proposing duplicate work.
3. For substantial changes, open or identify an issue first so the scope and acceptance criteria are clear.
4. Check the target branch and dependency PRs. An open feature branch is not part of `main` until merged. If your change depends on another open PR, explicitly state that dependency and use a stacked PR only when necessary.

## Development environment

Use the versions pinned by the repository. The current baseline expects JDK 17, Android SDK platform 36/build-tools 36.0.0, and the committed Gradle wrapper.

**Windows PowerShell**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

**Linux or macOS**

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Run the relevant checks for your change. Database migrations and critical sales flows also need runtime verification using a test database and, where applicable, an emulator or physical test device. Do not report instrumentation tests as passed merely because their source compiled.

## Branches and commits

- Create a focused branch from the latest appropriate base, normally `main`.
- Use a descriptive prefix such as `feat/`, `fix/`, `docs/`, `test/`, or `build/`.
- Keep each pull request limited to one coherent outcome.
- Use concise Conventional Commit-style messages, for example:
  - `feat: add stock adjustment history`
  - `fix: reject checkout when stock is insufficient`
  - `test: cover room migration from v1 to v2`
  - `docs: explain product data provenance`

## Engineering expectations

- **Offline first:** routine shop, inventory, and checkout operations must not require network access.
- **Shop isolation:** include the owning shop in relevant reads, writes, uniqueness rules, and tests.
- **Financial correctness:** store BDT amounts as integer poisha; do not use floating-point arithmetic for money.
- **Auditable stock:** record stock changes and their reasons; do not silently rewrite history.
- **Transactional checkout:** sale records, line snapshots, stock changes, and movement records that must agree should succeed or fail together.
- **Safe persistence:** use explicit Room migrations, preserve existing user data, and avoid destructive fallbacks.
- **Accessible mobile UI:** handle small screens, loading, empty, validation, offline, and error states where relevant.
- **Dependencies:** add a library only when it solves a real problem; verify its official coordinates, maintenance, license, and compatibility.
- **AI boundaries:** treat generated output as untrusted suggestions. Validate inputs and permissions, and keep calculations in deterministic application code.
- **Open data:** record source, retrieval date/version where applicable, license, attribution requirements, and limitations before copying or bundling external data.

## Pull request checklist

Before requesting review, confirm what applies:

- [ ] The PR explains the problem, intended behavior, and scope.
- [ ] The PR is based on the correct branch and lists any dependency PRs.
- [ ] Tests cover new behavior and important regressions.
- [ ] Relevant Gradle build/tests were run, or the exact limitation is documented.
- [ ] Schema changes include an explicit migration and migration coverage.
- [ ] Critical checkout/stock changes consider transaction rollback and shop isolation.
- [ ] UI changes handle accessibility and relevant empty/error/offline states.
- [ ] User-facing behavior, README, roadmap, status, and agent handoff docs are updated where needed.
- [ ] New third-party code/data/assets have been reviewed for license and attribution obligations.
- [ ] No credentials, signing keys, private data, build output, or unrelated changes are included.
- [ ] The PR description states what was actually tested and what remains unverified.

## Review and merging

Maintainers may request changes, narrow scope, or defer a contribution. Do not bypass required checks or repository rules. For stacked PRs, merge the base dependency first and then rebase/retarget dependent PRs as appropriate. A successful check on an older commit does not validate a newer PR head.

## License of contributions

By submitting a contribution to this repository, you agree that your contribution is offered under the repository's [Apache License 2.0](LICENSE), unless you and the project owner agree otherwise in writing. You retain ownership of your contribution. Do not submit code or data you do not have the right to license under these terms.
