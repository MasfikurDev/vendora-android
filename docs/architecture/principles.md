# Architecture Principles

## Start with a buildable baseline
Generate and verify a standard native Android project before choosing a large multi-module structure. Begin with clear package boundaries; split Gradle modules only when build isolation, ownership, or dependency boundaries justify the extra complexity.

## Suggested layers
- **UI:** Compose screens, reusable components, navigation, and screen state.
- **Domain:** business rules and use cases that are valuable to test independently.
- **Data:** repositories, Room entities/DAOs, mappings, and local data sources.
- **Core model:** stable types shared across layers, only where sharing is actually needed.

Use MVVM where it clarifies screen state. Coroutines and Flow are the standard asynchronous primitives. Choose dependency injection after checking the generated project and current compatible versions.

## Local persistence
Room/SQLite is the initial source of truth. Keep writes transactional. Model stock movement history explicitly, snapshot sale/purchase line values, and add tested schema migrations. Export and restore behavior must be designed around database consistency rather than copying an active database blindly.

## Multiple shops
Represent the shop/business identity explicitly. Any shop-owned record and relevant DAO query must carry or enforce that scope. Test isolation using two shops with overlapping product names/SKUs.

## Money and quantities
Use integer minor units for currency. Define rounding rules for discounts, tax, unit cost allocation, and margins. Quantity precision should be explicit per business type; do not assume all stock is whole-number units.

## AI integration
Define a provider-neutral boundary only when the first AI feature is implemented. Keep provider calls out of repositories that enforce core business invariants. Begin with read-only summaries and questions, use structured tool inputs, validate authorization, and compute numerical answers from trusted application data.

## Testing
Prioritize deterministic domain tests, Room persistence/transaction tests, migration tests, shop-isolation tests, and UI tests for high-frequency flows. CI must use the actual Gradle wrapper and verified project tasks.
