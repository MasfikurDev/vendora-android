# Vendora as a Reusable Android App Template

## Purpose

Vendora should become both a useful shop-management app and a well-organized foundation for future Android projects. The reusable foundation must stay separate from Vendora-specific business rules so a future app can reuse the setup without inheriting inventory and sales features.

## Two layers

### 1. Reusable Android foundation

Keep these parts generic where the project actually needs them:

- Kotlin + Jetpack Compose + Material 3 app shell and theme.
- Navigation conventions and reusable screen/layout patterns.
- Lifecycle-aware state, coroutines, and clear UI/data boundaries.
- Room database setup patterns, repository conventions, and migration practices.
- Common validation and error-handling conventions.
- Unit-test structure and test naming.
- Gradle version catalog and build configuration.
- GitHub Actions build/test workflow and build artifacts.
- Contributor instructions, pull-request checklist, and release notes conventions.
- Optional reusable components such as empty states, loading/error states, and form-field patterns.

Do not create a generic framework for every possible future need. Extract a component only after it solves a real recurring problem.

### 2. Vendora product layer

Keep these features Vendora-specific:

- Shops and business profiles.
- Product catalog and SKU.
- Stock levels and stock movement ledger.
- Checkout, sale lines, purchases, returns, expenses, and reports.
- Retail-specific permissions and business rules.
- Optional AI business insights.

A future app should be able to omit this layer without rewriting the generic foundation.

## Recommended repository layout (target, not a claim about today's code)

```text
app/
  src/main/java/.../
    app/             # Application setup and navigation
    core/            # Truly shared UI and utilities
    data/            # Room, migrations, repositories
    feature/
      shops/         # Vendora-specific
      inventory/     # Vendora-specific
      sales/          # Vendora-specific
  src/test/           # Unit tests
docs/
  architecture/
  development/
.github/
  workflows/
```

Keep this as a direction, not a mandate to move every file immediately. Move code when doing so improves a feature or reduces repeated work.

## Template readiness stages

### Stage A — Reliable foundation
- [ ] Project builds from a clean checkout with the committed Gradle wrapper.
- [ ] CI runs dependency validation, debug build, and unit tests.
- [ ] Version catalog, SDK levels, JDK, and setup steps are documented.
- [ ] No credentials, personal data, or local-machine paths are committed.
- [ ] Errors in build/test jobs are diagnosable from logs.

### Stage B — Safe data layer
- [ ] Room schema and migration strategy are explicit.
- [ ] Legacy import is retry-safe and preserves the source until verification.
- [ ] Repository APIs are shop-/account-scoped where required.
- [ ] Multi-step data updates use database transactions.
- [ ] Tests cover migration, isolation, validation, and rollback behavior.

### Stage C — Reusable UI patterns
- [ ] App theme and typography are centralized.
- [ ] Common loading, empty, error, and confirmation states are consistent.
- [ ] Screens handle small phone widths and system insets.
- [ ] Forms have clear validation and accessibility labels.
- [ ] UI components are generic only where reuse is demonstrated.

### Stage D — Starter template
- [ ] Create a template-specific README with first-run instructions.
- [ ] Document the application ID/package rename and branding changes.
- [ ] Document how to remove Vendora-only features safely.
- [ ] Provide a sample feature and its unit test.
- [ ] Explain how to configure repository secrets and release behavior without exposing secrets.
- [ ] Choose and document a license before presenting the template as open source.
- [ ] Validate the template by creating a separate sample app from it.

## How future projects should use the template

1. Create a new repository from a stable template snapshot or use GitHub's template-repository feature once enabled.
2. Rename the app title, package/application ID, icon, and theme.
3. Keep only the generic foundation needed by the new app.
4. Replace Vendora-specific features with the new product's feature modules.
5. Run the documented clean build and tests before starting feature work.
6. Keep the new app in a separate repository so its history, releases, credentials, and product roadmap remain independent.

## Important cautions

- Do not copy a branch that has not passed CI or is not integrated into the chosen stable base.
- A debug APK and passing CI do not prove production readiness.
- Never include real shop/customer data, signing keys, API keys, or personal tokens in a template.
- Do not enable a public template repository until the license decision is explicit.
- Prefer a stable tag or a dedicated template branch over copying from an actively changing feature branch.
- Template extraction should not delay fixing Vendora's stock/sales correctness and completing its Room integration.

## Definition of ready

The project is ready to be advertised as a reusable Android starter only when the foundation is documented, CI is reproducible, data migration and core flows are tested, the template has been exercised in a fresh sample project, and its license and release limitations are clear.

Until then, describe it as a **template-in-progress**, not a production-ready starter.
