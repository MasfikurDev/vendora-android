# Vendora: Professional Planning & Development Guide

This guide explains Vendora's plan in plain language for developers and non-developers. It describes what we intend to build, how work is organized, and how we decide that a feature is ready.

**Product principle:** essential shop operations must work offline. AI is optional and must never be required to record sales, calculate money, or update stock.

## 1. What we are building

Vendora is an Android workspace for an owner who manages one or more shops. Each shop has its own products, prices, stock, and business activity. The app should be easy to use on a phone and dependable for daily work.

### Main user journeys

1. Create or open a shop.
2. Add and edit products, including SKU/barcode, purchase cost, selling price, and quantity.
3. Sell one or more products in a checkout flow.
4. Reduce stock after a confirmed sale and record why stock changed.
5. Review business information while distinguishing estimates from actual recorded sales and profit.
6. Preserve existing data during app updates and provide an export/backup path.
7. Use optional AI for read-only questions about business data without making normal workflows depend on AI or internet access.

## 2. Product principles

- **Offline first:** local data is the source of truth.
- **Shop isolation:** products, sales, and stock movements always belong to a shop.
- **Accurate money:** store BDT amounts as integer poisha, not floating-point values.
- **Auditable stock:** retain movement records instead of only overwriting quantities.
- **Atomic checkout:** sale records and stock changes must succeed or fail together.
- **Safe migration:** app updates must not silently erase existing data.
- **Honest insights:** potential sales value is not actual revenue; inventory margin is not net profit.
- **Mobile-first usability:** important actions, especially selling, must be easy to reach.
- **AI is optional:** authoritative calculations and permissions stay in application code.

## 3. Development roadmap

Work should build from the latest `main` branch. The stages are ordered by dependency; they can overlap where safe.

| Stage | Deliverable | Completion means |
|---|---|---|
| 1. Foundation | Android app, shop/product models, baseline Room schema | Project and schema are understood and documented |
| 2. Local data | Room database and repository | Shops and products use one consistent Room-backed data path |
| 3. Safe migration | One-time import from existing SharedPreferences JSON | IDs, quantities, prices and shop ownership survive; repeat launch creates no duplicates |
| 4. Inventory integrity | Shop-scoped product editing and stock adjustment | Each stock change has a reason and cannot affect another shop |
| 5. Checkout | Multi-product cart and transactional sale | Sale lines, cost snapshots, stock deduction and movement entries commit together |
| 6. Mobile workflow | Connect screens to the repository and prioritize Selling | Loading, empty, error and success states work on phone-sized screens |
| 7. Business records | Purchases, returns, expenses and sales reports | Reports use recorded transactions and explain their totals |
| 8. Data safety | Export/backup and restore path | Users have a documented recovery option |
| 9. Optional AI | Read-only business assistant | Inputs and permissions are validated; AI does not perform unreviewed financial mutations |
| 10. Release readiness | Signing, privacy, accessibility, regression review and release notes | Production gaps are reviewed; a debug APK is not called a production release |

Migration and transaction integrity must not be skipped just to make the UI look finished.

## 4. Architecture in plain language

- **Screen (Compose UI):** displays information and collects user actions. It should not implement stock or financial rules.
- **ViewModel/state holder:** manages screen state, loading/errors, and user actions.
- **Repository:** provides one consistent way to read/write shops and products, so screens do not depend directly on storage details.
- **Room database/DAO:** stores local records and runs scoped queries and transactions.
- **Domain rules:** validate quantities, prices, discounts, and checkout rules independently of the screen.
- **Optional AI adapter:** can explain authorized data; it is not the database and does not calculate authoritative financial totals.

Keep the architecture simple. Add a layer when it prevents duplicated logic or protects data integrity.

## 5. Safe data migration

The old app stores shop/product data in SharedPreferences as JSON. Room is the intended durable store. Migration should be one-time and repeat-safe:

1. Check whether legacy data has already been imported.
2. Read and validate the legacy shop list and each shop's products.
3. Preserve IDs, shop ownership, quantities, SKU, and poisha prices.
4. Insert records in a database transaction with a conflict policy that does not overwrite newer Room data.
5. Mark migration complete only after the transaction succeeds.
6. Keep legacy values until the import is proven stable; do not clear them during the initial import.
7. If data is malformed or migration fails, preserve the original values and report a recoverable error.

Cover empty storage, valid existing data, repeated startup, malformed data, and app upgrades in migration verification.

## 6. Checkout and stock integrity

Checkout must be one atomic database transaction:

1. Reject an empty cart and non-positive quantities.
2. Load each product using both product ID and shop ID.
3. Confirm sufficient stock for every line before changing any product.
4. Snapshot the product name, SKU, selling price, and purchase cost.
5. Insert sale lines and stock movement records.
6. Decrease product quantities.
7. Commit all changes together. If a step fails, roll back the sale and stock changes.

A sale must not be reported successful while stock remains unchanged; stock must not be deducted without a matching sale record. Repeated taps must not create duplicate sales.

## 7. GitHub workflow without slowing development

- Start feature work from the latest `main`, not an unrelated or stale feature branch.
- Group related implementation into a useful feature-sized pull request.
- Avoid splitting one coherent change into many tiny PRs.
- Use existing CI for normal build/unit-test verification; avoid duplicate runs.
- When a check fails, inspect the failing job and fix the cause before rerunning.
- Add extra checks when risk warrants them, especially for migration, money, checkout, and data loss.
- Do not bypass required checks or merge with unresolved data-loss risk.
- Use a phone-installed APK for visual review when appropriate; emulator work is not required for every UI iteration.
- Keep `main` buildable and describe what is actually complete.

## 8. Definition of done

A feature is done when it solves a user problem, works offline where required, respects shop boundaries, validates inputs, handles failure safely, provides mobile-friendly loading/empty/error/success states, uses auditable money and stock rules, passes relevant checks, and has accurate documentation. Known limitations must be stated plainly.

## 9. Repository status snapshot (2026-10-28)

- The project uses Kotlin, Jetpack Compose, and Material 3.
- The Room schema foundation is present on `main`.
- Pull request [#23 — mobile-first dashboard and inventory views](https://github.com/MasfikurDev/vendora-android/pull/23) was still open when this guide was written. Its UI changes use SharedPreferences and do not activate Room.
- Room-backed repositories, safe legacy migration, and transactional checkout still need to be integrated before the persistence migration can be called complete.
- Check the current state of `main` and pull requests before implementing; an open PR is not automatically part of `main`.

## 10. Glossary

- **Offline first:** core features continue to work without a network.
- **Room:** Android's database library built on SQLite.
- **DAO:** the interface describing database operations.
- **Repository:** a consistent API between app logic and data storage.
- **Migration:** moving existing data to a new storage format without losing it.
- **Transaction:** database changes that all succeed together or all roll back.
- **SKU:** a shop's internal product code.
- **Stock movement:** a record explaining a quantity increase or decrease.
- **Snapshot:** a saved price/cost from the time of sale, so later price edits do not rewrite history.
- **CI:** automated build and test checks run by GitHub.
- **Debug APK:** an installable review build, not automatically a signed production release.
