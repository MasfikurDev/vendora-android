# Vendora AI Agent Worklog and Troubleshooting

Last updated: **2026-10-24**

This is a durable handoff for coding agents working on Vendora. It records observed repository-specific pitfalls and the verified handling for them. It is not a substitute for inspecting the current source, workflow files, pull requests, and CI runs.

## Operating rules

1. **Inspect before editing.** Read `AGENTS.md`, the relevant source and tests, the current README/status documents, and the active PR/branch. Search for existing implementations before creating new ones.
2. **Use the repository's actual state.** The default branch may lag an open feature/documentation PR. Fetch the target branch explicitly when checking files, and label whether a statement describes `main`, a PR head, or intended architecture.
3. **Keep work reviewable.** Make focused changes on a short-lived branch, use conventional commits, describe the behavior and tests, and open a PR rather than pushing unreviewed work directly to `main`.
4. **Follow the repository's explicit merge policy.** For Vendora PRs created by the agent, enable GitHub auto-merge when available and only after the latest exact-head required checks pass, the PR is mergeable and not a draft, and no unresolved blocking review or material data-loss/security risk remains. Never bypass repository protections. If auto-merge cannot be safely enabled, explain the blocker.
5. **Verify checks from evidence.** Inspect the exact CI run and commit SHA. Never say a PR is green based on a different branch, an older commit, a workflow configuration, or a successful main-branch run.
6. **Report test scope precisely.** Distinguish commands actually run from CI checks observed. State when Android emulator/instrumentation coverage is absent.
7. **Treat docs as part of the implementation.** When source behavior, architecture, CI, release behavior, or project status changes, update the relevant docs in the same focused change. Keep transition history rather than leaving contradictory status statements.
8. **Never invent capabilities.** If an API/connector action, file, test, build result, or UI integration cannot be verified, say so and investigate rather than claiming success.

## Verified repository lessons

### Stale status in the README

**Problem:** The README on `main` still described an old PR as the pending integration and claimed no shop-management features existed, while the documentation branch's status snapshot records newer shop-selection and local product-storage work.

**Handling:** Read the README and status document from the same ref; compare them with source and PR state. Correct stale statements and preserve a dated transition entry. Do not assume an open PR's contents are already on `main`.

### SharedPreferences/JSON is interim storage, not Room

**Problem:** Documentation can easily make the planned architecture sound implemented.

**Handling:** State explicitly which ref contains the current shop/product persistence implementation. SharedPreferences + JSON is interim storage; Room/SQLite, DAOs, migrations, and database tests remain future work until code and tests confirm otherwise. Do not build later financial workflows on an assumed Room implementation.

### CI status and release publishing are different concerns

**Problem:** A successful main-branch build can be mistaken for a successful PR check, and a debug beta APK can be mistaken for a production release.

**Handling:** Check the workflow run for the exact PR head SHA. The documented workflow policy is that pushes to any branch and PR checks run CI, while only a successful push to `main` should publish a beta prerelease. The beta artifact is a debug APK, not a signed production build. Re-verify workflow configuration and current run status before making a new claim.

### UI/instrumentation coverage is not implied by a successful JVM build

**Problem:** Passing unit tests and assembling the APK do not prove critical UI flows work on a device or emulator.

**Handling:** Report JVM unit tests, APK assembly, and instrumentation/UI tests separately. Instrumentation tests are not considered CI-covered unless an emulator/device job is configured and its run passes.

### Shop isolation must be tested explicitly

**Problem:** A shop selector alone does not prove products and future transactions are isolated between shops.

**Handling:** Add tests for shop-scoped CRUD, duplicate SKU behavior within one shop, allowed duplicate SKUs across different shops if that is the chosen rule, malformed persisted JSON, and invalid/missing active-shop references. Revisit these tests when storage migrates to Room.

## Merge automation policy (Vendora only)\n\n- The repository owner requested automatic merging for new Vendora PRs only. Do not generalize this preference to other repositories.\n- Prefer GitHub auto-merge so required checks and repository rules remain authoritative; do not immediately merge around pending checks.\n- Before enabling auto-merge, verify the current PR head SHA, required checks, draft state, mergeability, and any blocking reviews.\n- If repository settings do not permit auto-merge or no required checks are enforced, disclose that limitation instead of implying a persistent repository-wide automation has been configured.\n- This agent instruction documents the desired workflow; it does not itself change GitHub repository settings or guarantee background monitoring.\n\n## Troubleshooting protocol

When a build, CI run, connector, or integration fails:

1. Capture the exact error text, failing step, branch, commit SHA, and run URL where available.
2. Separate the root cause from secondary failures. Do not make multiple unrelated changes at once.
3. Inspect the configuration and dependency versions already committed; do not guess versions or silently upgrade the toolchain.
4. Make the smallest targeted fix and add a regression test when the failure is testable.
5. Re-run the narrow failing check, then the broader relevant build/test set.
6. Verify the new run against the latest commit SHA and update the PR description or project status.
7. If blocked by missing credentials, permissions, an unavailable runner, or a service limit, document the exact blocker and a zero-cost/manual alternative only when one is verified. Never expose tokens or request that they be committed.

## Session handoff template

Agents should leave a short handoff in the PR description or work summary:

- **Branch / PR:**
- **Base SHA and head SHA:**
- **Files changed:**
- **Behavior completed:**
- **Checks actually run and result:**
- **CI run URL and tested SHA:**
- **Known limitations / follow-up:**
- **Docs updated:**
- **Merge status:** not merged unless the repository owner explicitly approved it.

## Evidence boundary

This log currently records repository-specific lessons confirmed from Vendora's documented state and workflow policy. It does not claim that unrelated OpenCode/OpenRouter, Vercel, or other-project incidents were caused or fixed in this repository. Add such incidents only when the relevant project, error, fix, and verification are known, and link the exact PR/commit/run when possible.
