# Vendora Product Scope

## Target
A mobile-first Android app for owners managing one or more small retail businesses, including grocery, fashion, electronics, hardware, stationery, and general retail. More specialized workflows can be added later without making the common app unnecessarily complex.

## Initial release priorities
1. Owner can create and switch between shops.
2. Each shop has its own catalog and inventory.
3. Purchases and stock adjustments create auditable stock movements.
4. Sales and returns update inventory consistently.
5. Expenses and transaction history support transparent profitability reports.
6. Data persists locally and can be exported/backed up.
7. App remains useful without internet access.

## UX expectations
- Mobile-first layouts, optimized for frequent one-handed tasks.
- Clear navigation and fast access to common tasks (search products, record sale, receive stock).
- Explicit confirmation for destructive or financially meaningful actions.
- Useful empty, loading, validation, and error states.
- Accessible labels, touch targets, and readable numeric values.
- Avoid dashboard clutter before real user workflows establish what is useful.

## Not in the first release
- Mandatory account creation or cloud dependency.
- AI-driven writes to inventory or financial records.
- Multi-device synchronization, staff roles, and complex enterprise permissions.
- Forecasting claims unsupported by sufficient historical data.
- Specialized regulated workflows such as pharmacy dispensing.

## Acceptance principle
Prioritize correctness and recoverability of business records over adding a large number of features. Every core workflow should be testable and understandable without AI.
