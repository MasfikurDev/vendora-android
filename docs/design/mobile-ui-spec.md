# Vendora mobile UI specification

**Design source:** [Vendora Android — Mobile UI System](https://www.figma.com/design/paHXNhjxyBfhnHjHhkYrKB)

**Status:** Initial visual direction and first native Compose refinements. This is not a claim that every planned screen is complete.

## Design principles

- Mobile-first, one-handed workflows with Selling kept directly accessible.
- Material 3 components and semantics rather than a custom widget framework.
- Clear financial hierarchy, readable stock quantities, restrained surfaces, and consistent rounded corners.
- Distinct text labels accompany status colors; color is never the only status cue.
- Keep the current shop visible in context and scope product/sale operations to that shop.
- Prefer real repository-backed values. Never show design mock values as live sales, profit, or activity.
- Offline operation remains the default; no screen should require network access for core product or checkout tasks.

## Visual foundations

| Token | Value | Use |
|---|---|---|
| Primary | `#145945` | Primary actions, active navigation, key figures |
| Action green | `#217A5C` | Secondary emphasis and chart accents |
| Canvas | `#F6F8F4` | Screen background |
| Surface | `#FFFFFF` | Cards, search fields, and controls |
| Primary text | `#172621` | Titles and important values |
| Secondary text | `#66756E` | Supporting copy and metadata |
| Outline | `#E0E5DE` | Subtle card/input borders |
| Mint surface | `#E0F0E3` | Selected/positive states |
| Attention surface | `#FFF2D4` | Stock attention |
| Error surface | `#FCE5E0` | Error/critical status |

Use an 8 dp spacing rhythm where practical, 12–20 dp corner radii, and touch targets around 48 dp or larger for primary actions.

## Screen responsibilities

### Home / Overview

- Show the selected shop and a concise inventory/business overview.
- Make the primary **Start selling** action immediately available from Overview; keep Add product and Inventory as secondary actions.
- Prefer verified values available from Room, such as product count, units, stock cost, and low-stock items.
- Sales totals, gross profit, recent sales, and time-series charts must remain absent or clearly unavailable until backed by persisted sales data and correct aggregation.
- Low-stock rows should navigate to inventory and show the actual quantity.

### Sell / Checkout

- Product search matches name or SKU.
- Quantities cannot exceed available stock in the UI; the repository remains the final authority.
- Cart total is derived from selected products and stored integer-poisha prices.
- Show loading, success, and failure states.
- Clear the cart and refresh inventory only after the repository confirms checkout success.
- A failed checkout must not imply stock was deducted.

### Products / Inventory

- Search by product name or SKU.
- Quick filters distinguish all items, in-stock items, and low-stock items.
- The current low-stock threshold is five or fewer units, matching existing app behavior; keep the UI consistent with that rule.
- Product edits and deletes must retain their existing repository pathways.

### Navigation and shop context

- Keep Overview, Products, Selling, and Insights available from the bottom navigation.
- The current shop name/type remains visible in the top app bar.
- Shop switching remains a separate flow; do not mix data between shops.

## Implementation boundary

Figma contains static sample product names and illustrative numbers for visual composition. They are not fixtures for the production UI and must not be copied into runtime screens. All financial values must come from repository-backed data and be formatted from integer poisha. The UI must not add dependencies solely to reproduce the mockup.
