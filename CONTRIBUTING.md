# Contributing

## Workflow
1. Read `README.md` and `AGENTS.md`.
2. Choose or create a focused GitHub issue before substantial work.
3. Create a short-lived branch from the current `main`, for example `feat/shop-switcher` or `fix/stock-rounding`.
4. Make the smallest coherent change and add tests.
5. Run the relevant checks locally. Once the Android scaffold exists, the baseline commands are:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat assembleDebug
   ```
6. Open a pull request describing the behavior change, tests run, and any migration or user-visible impact.

## Commit messages
Use concise Conventional Commit-style messages:
- `feat: add shop switching`
- `fix: prevent negative stock on sale`
- `test: cover return stock movements`
- `docs: record local database decisions`
- `build: configure Android CI`

## Design principles
- Prefer mobile-first, accessible Compose UI and established Material 3 components.
- Preserve offline usability and data integrity.
- Avoid adding dependencies unless they solve a real need.
- Keep ordinary business workflows independent of AI.
- Document important architectural choices in `docs/decisions/`.

## Pull request checklist
- [ ] Scope is focused and linked to an issue.
- [ ] Tests cover the changed behavior and relevant regressions.
- [ ] Build and tests were run, or limitations are stated.
- [ ] Database changes include explicit migration and migration tests.
- [ ] No secrets, real customer data, or unrelated formatting changes are included.
- [ ] UI handles loading, empty, error, offline, and small-screen states as relevant.
