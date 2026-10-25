---
name: inventory-transaction-integrity
description: Implement or review Vendora product, inventory, stock movement, sale, cart, and financial transaction logic.
---

# Inventory and transaction integrity

## Invariants
- Scope every read and write explicitly by shopId; reject missing or blank scope at boundaries.
- Store money in integer minor units (poisha for BDT), never Float/Double. Use checked arithmetic for quantity and monetary calculations where overflow is possible.
- Validate the entire operation before mutation: product existence in the selected shop, positive quantities, available stock, duplicate cart lines, and totals.
- A rejected operation must have zero partial effects on product quantities, sale records, and stock movements.
- A successful sale preserves line snapshots (product name/SKU, quantity, unit price, totals) and appends movement entries with accurate before/delta/after quantities.
- Keep movements append-oriented; do not silently rewrite history when a product changes or is deleted.
- Verify another shop's products and records remain unchanged.
- Do not assume SharedPreferences commit is equivalent to a database transaction or safe under concurrent writers. Current JSON storage is interim; state its limits until database-backed transactions exist.

## Workflow
1. Inspect current model/store implementation and tests before editing.
2. Map each mutation to all records changed and screens displaying those records.
3. Validate inputs and compute proposed changes without mutating persisted state.
4. Apply changes together using the current storage mechanism; handle persistence failure explicitly where possible.
5. Add tests for success, missing product, invalid quantity, insufficient stock, overflow, duplicate lines, blank/wrong shop scope, and no partial effects. Adapt cases to actual APIs.
6. Verify downstream inventory/dashboard/sales/movement views; see vendora-workflow-verification.
7. Run tests and report exact evidence.

## Avoid
- Floating-point currency.
- Trusting UI validation as the only validation.
- Updating stock without an audit movement.
- Recording sales without product/price snapshots.
- Treating an exception as proof that state remained unchanged.
- Claiming ACID guarantees from SharedPreferences.
