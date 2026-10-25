# Vendora AI Coding Guidelines

These instructions apply to the whole repository unless a closer `AGENTS.md` overrides them.

## Before changing code
- Inspect the existing implementation and tests first.
- Keep changes focused on the current issue; do not scaffold unrelated features.
- Confirm the Android Gradle Plugin, Kotlin, Compose, Room, and SDK versions from the project files rather than guessing.
- Explain the intended change and relevant trade-offs before broad architectural changes.
- Check the active branch and pull requests: do not assume work on a PR branch is already present on `main`.

## Agent workflow and handoff
- Follow [the AI agent worklog and troubleshooting guide](docs/AI_AGENT_WORKLOG.md) for verified repository lessons, CI verification, and handoff expectations.
- Make focused changes on a short-lived branch and open/update a pull request for review.
- **Vendora auto-merge policy:** for new pull requests created as part of this work, enable GitHub auto-merge when available. Allow merge only after all required checks pass for the exact latest PR head SHA, the PR is not a draft, and GitHub reports it is mergeable with no unresolved blocking review/request or obvious data-loss/security risk. Prefer the repository's configured merge method. If auto-merge is unavailable, checks are missing/failing, a migration risks user data, or the change has material unresolved risk, stop and report the blocker rather than bypassing safeguards.
- This automatic-merge preference applies to the Vendora repository only. It does not authorize merging unrelated repositories or bypassing required checks/repository rules.
- Report precisely which commands/checks were run and their results. Never claim a build, test, deployment, or integration succeeded unless the result was observed.
- When source, project status, architecture, CI, or release behavior changes, update relevant documentation in the same change. Preserve a dated transition history where old statements could otherwise be confusing.
- When blocked, record the exact error, affected step, branch/SHA, and verified next action. Do not invent fixes or expose credentials.

## Architecture
- Keep the app offline-first. The local database is the source of truth.
- Use clear boundaries between UI, domain rules, and persistence. Prefer the simplest structure that supports current needs; do not create abstractions without a concrete use.
- UI state should be lifecycle-aware and driven by observable state such as Flow/StateFlow.
- Keep business rules out of composables and Android framework classes.
- Inject dependencies through the project's established mechanism; avoid service locators and global mutable state.
- Treat each shop's data as explicitly scoped. Test that queries and operations cannot leak data between shops.
- Use database transactions for multi-record operations that must succeed or fail together.

## Data integrity
- Represent currency as integer minor units (poisha for BDT), not Float or Double.
- Preserve price and cost snapshots on completed transaction lines.
- Track stock changes in an append-oriented movement ledger; do not silently rewrite historical movements.
- Use explicit, tested database migrations. Never use destructive migration fallbacks for production data.
- Validate inputs at boundaries and use domain-specific errors for expected failures.

## AI boundaries
- Core sales, inventory, and financial calculations must work without AI or network access.
- Treat AI responses as untrusted suggestions. Validate structured arguments and permissions before any action.
- Start with read-only AI features. Never expose secrets or private provider keys in the Android client.
- Calculate money, margins, stock, and reports in application code, not by trusting generated text.

## Quality
- Add or update unit tests for domain rules and regression cases.
- Review end-to-end state propagation after every write: when a sale or stock adjustment succeeds, verify inventory, dashboard aggregates, sale history, and stock-movement history refresh when revisited. A successful persistence test does not prove a composable has refreshed its remembered state.
- Test failed operations for zero partial effects across every related record (stock, sales, and movements); test shop isolation for each new transaction path.
- Add database/DAO tests for persistence, transaction behavior, and shop isolation.
- Add UI tests for critical user journeys where practical. If no emulator/UI test is available, add the strongest feasible state/store regression test and explicitly record the remaining UI verification gap.
- Run relevant Gradle tests and build tasks when the wrapper/project is available; report exactly what was and was not run.
- Do not claim a build or test passed unless its result was observed.
- Keep accessibility, small-screen layouts, loading/empty/error states, and offline behavior in mind.
- Never commit credentials, keystores, customer data, or generated local build files.

## Feature builds and release documentation

- Feature branches may publish branch-specific GitHub prereleases after a successful **push-triggered** build and unit-test run. Pull-request-triggered runs validate the change but should not publish releases.
- A successful push to `main` uses the separate beta-release path. Feature previews and beta builds are debug APKs, not signed production releases.
- When changing workflow behavior, update README/status/worklog documentation in the same branch. Record exact commit SHA and CI run links; never infer a published release from workflow configuration alone.


## Agent skill routing

Repository-local reusable skills live under [.agents/skills/](.agents/skills/README.md). Load the matching skill before relevant work:
- End-to-end workflow/state refresh: `.agents/skills/vendora-workflow-verification/SKILL.md`
- Compose UI, responsive layouts, accessibility, lifecycle: `.agents/skills/android-compose-quality/SKILL.md`
- Stock/sales/movement integrity and shop isolation: `.agents/skills/inventory-transaction-integrity/SKILL.md`
- Regression test planning and evidence: `.agents/skills/vendora-testing/SKILL.md`
- Branches, stacked PRs, reviews, CI, and handoff: `.agents/skills/vendora-git-workflow/SKILL.md`
- Upstream Google Android guidance is vendored under `.agents/skills/upstream-android/`; consult it for performance, adaptive Compose layouts, and test setup, but verify all recommendations against this repository's actual versions and architecture.
