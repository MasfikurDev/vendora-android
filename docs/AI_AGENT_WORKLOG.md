# Vendora AI Agent Worklog and Troubleshooting

Last updated: **2026-10-28**

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

## Open data and shop type catalog handoff

- Branch: `feat/shop-type-taxonomy-open-data`, based on `feat/reusable-product-details`; it is intentionally dependent on PR #30 and should not be merged to `main` before its base feature is integrated.
- The create-shop dialog loads the offline bundled asset `app/src/main/assets/catalog/shop_types.json` through `ShopTypeCatalog.kt`. The catalog currently has 21 types, stable IDs, English/Bengali labels, aliases, and OSM reference tags. The catalog is checked in as data rather than fetched from the network.
- For compatibility, the current Room `businessType` value remains the selected English label. Stable IDs are not persisted yet. A future migration should add an explicit `businessTypeId`, map legacy labels/aliases, and retain existing labels without destructive rewriting.
- Source/license guidance lives in `docs/data/open-data-sources-and-catalog-rules.md`. OpenStreetMap taxonomy is a reference with ODbL obligations to review; BBS BSIC is an official industry-classification reference; Bangladesh Customs HS codes classify goods rather than shops; Open Food Facts is a possible optional product lookup with ODbL/DbCL/image licensing and API-rate-limit obligations.
- Do not call this taxonomy legal or tax compliance. Do not import tariff rates, claim business licensing validation, or bulk-copy product images/data without source-specific legal and attribution review.
- Catalog instrumentation test checks IDs, labels, fallback option, and asset loading; CI only compiles instrumentation tests under the current workflow unless a device/emulator job is explicitly added.

## Current feature handoff: reusable product details

- Active branch: `feat/reusable-product-details`; this branch is based on the checkout-integrity feature branch, not on `main` directly.
- Product metadata now includes brand, category, unit, barcode, and per-product low-stock threshold. Keep currency as integer poisha.
- Room schema version is 2 on this branch. `VendoraDatabaseProvider` defines an explicit v1→v2 migration; do not remove it or introduce destructive fallback migration.
- Reusable brand/category/unit values are stored in `catalog_options`, scoped by shop and normalized case-insensitively. They intentionally survive product deletion.
- The legacy JSON importer accepts optional new fields and seeds reusable options during the one-time import. Missing fields keep compatible defaults.
- The product editor supports free-text entry and choosing saved values. Barcode camera scanning, product images, supplier/purchase workflows, expiry/batch tracking, CSV import/export, and shop logos are not implemented; roadmap status is in `docs/product/reusable-product-details.md`.
- Add or extend instrumentation tests for migration behavior and saved-option persistence. Current CI compiles Android instrumentation test sources but does not execute them on a device/emulator; never describe compilation as runtime test success.

## Verified repository lessons

### Stale status in the README

**Problem:** The README on `main` still described an old PR as the pending integration and claimed no shop-management features existed, while the documentation branch's status snapshot records newer shop-selection and local product-storage work.

**Handling:** Read the README and status document from the same ref; compare them with source and PR state. Correct stale statements and preserve a dated transition entry. Do not assume an open PR's contents are already on `main`.

### Room is current persistence; SharedPreferences/JSON is legacy import only

**Problem:** The README and status docs lagged behind the Room implementation on main.

**Handling:** The active feature stack includes Room entities/DAO/repository, sale snapshots, stock movements, and a one-time legacy SharedPreferences/JSON importer. Schema version is branch-dependent: the reusable-product-details branch adds v1→v2 migration. Always inspect the exact target ref before reporting the schema version; do not imply migration runtime tests passed unless observed. Keep original legacy data preserved when import validation fails.

### CI status and release publishing are different concerns

**Problem:** A successful main-branch build can be mistaken for a successful PR check, and a debug beta APK can be mistaken for a production release.

**Handling:** Check the workflow run for the exact PR head SHA. The workflow runs on pushes to branches and PRs targeting `main`; a feature prerelease is published only by a successful non-main push whose commit message contains `[phone-test]` (or a manual dispatch with the publish input), while the main beta requires manual dispatch on `main`. The beta artifact is a debug APK, not a signed production build. Re-verify workflow configuration and current run status before making a new claim.

### UI/instrumentation coverage is not implied by a successful JVM build

**Problem:** Passing unit tests and assembling the APK do not prove critical UI flows work on a device or emulator.

**Handling:** Report JVM unit tests, APK assembly, instrumentation source compilation, and instrumentation execution separately. Compiling Android tests catches test-source errors but does not prove runtime behavior; execution needs an emulator/device job.

### Shop isolation must be tested explicitly

**Problem:** A shop selector alone does not prove products and future transactions are isolated between shops.

**Handling:** Add tests for shop-scoped CRUD, duplicate SKU behavior within one shop, allowed duplicate SKUs across different shops if that is the chosen rule, malformed persisted JSON, and invalid/missing active-shop references. Keep these tests in place with Room and extend them for DAO/transaction behavior, migration paths, and runtime execution.

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

## Feature-branch release workflow update — 2026-10-24

- Current workflow change is on `feat/product-catalog-ui`, commit `12d052e976b68a5b24cc09071813b48303140b24`.
- Intended behavior: successful feature-branch pushes publish a branch-specific GitHub prerelease with APK, checksum, and build metadata; successful pushes to `main` retain the separate beta-release path.
- CI runs `40102203101` (push) and `40102207210` (pull request) were observed in progress for this exact SHA during the update. Do not report the new workflow as verified or claim a feature release exists until the run is complete and the Releases page confirms publication.
- This supersedes the older sentence in this worklog that says feature-branch pushes and PR checks should never publish releases. PR checks themselves do not publish releases; the separate push event to a feature branch is what invokes the feature prerelease step.
- Feature releases are debug previews, not signed production builds. Check the exact branch, SHA, run conclusion, and published tag before sharing a build.


## Checkout integrity tests — 2026-10-28

- Added instrumentation cases for successful multi-item checkout, insufficient-stock rollback with no partial sale/stock writes, and cross-shop product access rejection on `fix/checkout-integrity-prerelease`.
- CI should compile Android instrumentation test Kotlin sources without requiring an emulator. This is compilation coverage only; run the tests on a device/emulator before claiming runtime verification.
- Documentation was refreshed because main already contained Room and checkout behavior while the previous README/status text still described the pre-Room state.


## Open-source documentation refresh — 2026-10-28

- Expanded the README into a project entry point covering purpose, current preview status, build/test commands, verification boundaries, data principles, release caveats, and a documentation map.
- Expanded contributor guidance with stacked-PR dependency handling, migration/data-integrity expectations, test evidence requirements, and third-party license review.
- Added community entry points: Code of Conduct, Support, changelog, documentation index, and issue templates. These describe current capabilities only; they do not promise response-time SLAs or production support.
- Updated security reporting guidance and added a third-party data/source provenance requirement to agent guidance.
- Rewrote the current status snapshot to distinguish `main` from open stacked PRs #30 → #31 → #32. Historical contradictory entries remain under dated transition history for traceability.
- Source basis: GitHub's repository best-practice and healthy-contribution guidance recommends a clear README, license, contribution guide, code of conduct, and security/community entry points. See https://docs.github.com/en/repositories/creating-and-managing-repositories/best-practices-for-repositories and https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions.
- No build or tests were run for this documentation-only change. Validate Markdown links and YAML templates, then inspect CI for the exact PR head.


## Documentation maintenance rules — 2026-10-28

- The root README is the short project entry point; keep implementation status branch-aware and link to detailed docs instead of duplicating long architecture notes.
- `CONTRIBUTING.md`, `SECURITY.md`, `SUPPORT.md`, and `CODE_OF_CONDUCT.md` are separate user/community interfaces. Do not imply private reporting, response SLAs, support contracts, or enforcement capacity beyond what is actually configured.
- Update `CHANGELOG.md` for meaningful user-facing changes; unreleased entries must not be presented as published release notes.
- For third-party data/code/assets, capture exact source and applicable license/attribution requirements in `THIRD_PARTY_NOTICES.md`; the source guide explains data quality and catalog governance.
- The docs index is `docs/README.md`. Keep links current and distinguish shipped behavior from future integrations.
- The current documentation/license change is on PR #32, stacked after #31 and #30. Merge dependency order only after reviewing the exact current head checks.
