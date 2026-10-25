# Vendora Product Direction

**Status:** Product direction / proposed UX. The screens below are conceptual mockups, not screenshots of implemented functionality. Use this document to guide future product and UI decisions; implementation status remains governed by the code and [project status ledger](PROJECT_STATUS_AND_HISTORY.md).

![Five-screen Vendora concept board: Home dashboard, image-first Products catalog, fast Sell checkout, real-data Insights, and social-account-style Business profile](images/vendora-product-direction.svg)

## Product vision

Vendora should feel like a **business-management workspace**, not a basic stock-counting app. A shop should have the identity and management experience of a social media business account: profile and branding, its own products and activity, business insights, and settings. The owner may switch between multiple businesses without mixing their data.

The home screen is the business overview. Selling must be immediately accessible. Product management should be visual and image-first.

## Primary navigation

The five destinations are the product's core information architecture, not merely a list of optional screens. **Sell should be visually prominent and reachable in one tap**; Home remains the landing destination for business performance. Use a mobile-first five-destination bottom navigation:

1. **Home** — overview and quick actions.
2. **Products** — image-first catalog and inventory.
3. **Sell** — prominent point-of-sale workflow; never bury checkout under management screens.
4. **Insights** — sales, profit, stock, and performance reports.
5. **Business** — business profile, switcher, and settings.

On narrow screens, prioritize content and touch targets; avoid squeezing secondary actions into the bottom bar. Keep navigation labels and accessibility semantics clear.

## Screen requirements

The concept board shows the intended relationship between all five destinations. Its sample business, products, numbers, charts, and stock counts are illustrative only; they must never be copied into production as real business data.

### 1. Home — business overview

- Show today's sales/revenue and estimated profit, plus order/transaction counts.
- Provide a sales/revenue chart with selectable date ranges.
- Show best-selling products, low-stock alerts, and recent transactions.
- Offer prominent quick actions such as **New sale**, **Add product**, and **Adjust stock**.
- Support loading, empty, and error states.
- Derive figures from persisted records. Never present hardcoded sample data as real analytics. Clearly label estimated profit and define which costs/discounts it includes.
- Only show period comparisons when the underlying data supports a meaningful comparison.

### 2. Sell — fast point of sale

- Give Sell a prominent, easy-to-hit bottom-navigation destination; do not make users open Products or Business first.
- Make the Sell destination one tap away from anywhere in the main app.
- Search products by name/SKU and support barcode scanning when implemented.
- Show product thumbnails, available stock, price, and clear quantity controls.
- Support multiple cart lines, per-item quantities, discount handling, subtotal, and final total.
- Validate the complete cart before mutating inventory. Prevent duplicate submissions and retain the cart when checkout fails.
- On success, record sale-line snapshots and stock movements; refresh inventory, dashboard aggregates, sales history, and movement history when revisited.
- Use BDT in integer poisha for calculations. Do not use floating-point currency.
- Provide clear payment/checkout confirmation and useful errors for insufficient stock or invalid quantities.

### 3. Products — image-first catalog

- Display product images in a responsive grid or list, with a clear switch if both layouts are offered.
- Support adding and editing product images using Android's supported photo picker/document selection approach. Handle cancellation, missing images, unsupported files, and load failures gracefully.
- Store a durable image reference rather than embedding large image blobs in the product JSON. Keep offline availability and backup/export implications in mind when choosing image storage.
- Show product name, SKU, category, selling price, cost where appropriate, stock quantity, and low-stock state.
- Support search and useful filters such as category and low stock.
- Include accessible image descriptions or meaningful fallback content; the image must not be the only way to identify a product.
- Keep product data scoped to the selected business.

### 4. Business — profile and management

Treat each business like a social-media-style business account, with identity and activity rather than only a settings form:

- Logo/avatar, optional cover image, business name, description, business type, phone/contact details, address, and opening hours.
- Business switcher that clearly indicates the active business.
- Business-specific products, inventory, transactions, and analytics.
- Settings for business details and preferences.
- A future extension point for team members and role-based access; do not imply staff permissions exist before implemented.
- Switching businesses must switch every relevant screen and data query. Test that no products, sales, or dashboard totals leak across businesses.

### 5. Insights — useful, honest analytics

- Sales and revenue over time.
- Estimated gross profit/margin only when reliable cost snapshots are available.
- Best-selling products and stock that needs attention.
- Date filters and clear definitions for each metric.
- Empty states for new businesses and explicit handling of incomplete historical cost data.
- Charts should be accessible and accompanied by labels or summaries; do not rely on color alone.

## Visual and interaction principles

- Native Kotlin, Jetpack Compose, and Material 3 remain the baseline. Check the repository's actual dependency versions before adopting new APIs or libraries.
- Mobile-first layouts, comfortable touch targets, readable typography, and support for large font scales and narrow devices.
- Consistent product image aspect ratios, sensible placeholders, and resilient image loading.
- Clear loading, empty, success, and error states.
- Avoid one-time remembered snapshots for data that can change on another screen. Refresh or observe authoritative state on navigation return.
- Keep business rules and persistence out of composable bodies.
- Preserve the existing offline-first approach. Do not make ordinary selling or inventory actions dependent on a network or AI service.

## Data and integrity constraints

- Every business-owned record must be scoped to a stable business/shop identifier.
- Money is stored and calculated in integer minor units (poisha for BDT).
- Completed sales preserve product name/SKU and price/cost snapshots as needed for historical reporting.
- Stock adjustments and sales create append-oriented movement history.
- Validate an entire checkout before changing stock; a rejected operation must not partially update stock, sales, or movements.
- Current SharedPreferences + JSON storage is interim and does not provide full database transaction or concurrency guarantees. Plan the Room/SQLite migration with explicit, tested migrations before relying on production-level financial integrity.
- Product images need a defined persistence, cleanup, offline, and backup strategy; avoid orphaned files when products are deleted or images replaced.

## Suggested implementation sequence

The board is a product-design reference, not a mandate to implement all five screens in one pull request. Deliver the experience in small, reviewable slices and keep the navigation/data contracts consistent across them.

1. **Navigation and dashboard foundation:** establish the five destinations, active-business context, and dashboard state/empty states; verify cross-screen refresh.
2. **Image-backed product catalog:** image selection, durable image references, grid/list UI, search, filters, and accessibility.
3. **Fast selling workflow:** integrate the multi-product cart with product search/thumbnails, checkout validation, and post-sale refresh.
4. **Insights:** derive charts and metrics from real sale/stock records; label limitations and incomplete data.
5. **Business profile:** profile fields, switcher UX, and settings; preserve strict data isolation.
6. **Persistence hardening:** migrate to Room with tested migrations and database transactions before expanding financially significant workflows.
7. **Later extensions:** barcode scanning, team permissions, backup/export, purchases/returns/expenses, and optional read-only AI insights.

Split work into focused pull requests. For every write, trace the full journey through UI, validation, storage, navigation, and all affected screens. Add regression tests for failed operations, shop isolation, historical snapshots, and state refresh. Report only the tests and CI results actually observed.

## Acceptance checklist

- [ ] Home is an actual overview, with chart data sourced from persisted records.
- [ ] Sell is visible and reachable in one tap from the main navigation.
- [ ] Products display images or a clear fallback and support adding/changing an image.
- [ ] Switching businesses updates profile, catalog, sell context, and insights together.
- [ ] No cross-business data leakage.
- [ ] Failed checkout leaves stock, sales, and movement history unchanged.
- [ ] Successful checkout refreshes inventory and dashboard values on return.
- [ ] Charts explain their date range and metric definitions and have accessible text summaries.
- [ ] Narrow screens, large font scales, empty states, and image-loading failures are handled.
- [ ] Documentation and status ledger distinguish planned UX from implemented behavior.
