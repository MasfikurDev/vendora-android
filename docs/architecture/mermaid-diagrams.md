# Vendora Architecture Diagrams

These portable Mermaid diagrams document the offline-first architecture and transaction invariants. The entity model is explicitly a target model where noted; it is not a claim about the exact current Room schema.

## 1. System architecture and unidirectional data flow

```mermaid
flowchart LR
    subgraph PRESENTATION["Presentation"]
        UI["Jetpack Compose screens"]
        EVENTS["User intents"]
        VM["ViewModels"]
        STATE["StateFlow<UiState>"]
        UI --> EVENTS --> VM
        STATE --> UI
    end
    subgraph DOMAIN["Repository and business rules"]
        REPO["Repositories"]
        RULES["Validation and domain rules"]
        VM --> REPO
        REPO --> RULES
    end
    subgraph LOCAL["Offline-first local data"]
        DAO["Room DAOs"]
        TX["Room transaction"]
        DB[("SQLite database")]
        RULES --> DAO --> TX --> DB
        DB --> DAO --> REPO
        REPO --> STATE
    end
    MONEY["Money: integer minor units<br/>poisha / cents"]
    AUDIT["Stock movements: auditable log"]
    MONEY -. invariant .-> RULES
    AUDIT -. persisted locally .-> DB
```

**UDF:** user actions flow toward repositories; database results flow back through repository observations and `StateFlow` as immutable UI state. Core operations do not require network access.

## 2. Atomic checkout sequence

```mermaid
sequenceDiagram
    autonumber
    actor Cashier
    participant UI as Compose Sell screen
    participant Repo as Checkout repository
    participant Tx as Room transaction
    participant DAO as Room DAO
    participant DB as SQLite
    Cashier->>UI: Confirm cart
    UI->>Repo: checkout(activeShopId, cart)
    Repo->>Repo: Validate non-empty cart and positive quantities
    Repo->>Tx: Begin withTransaction
    loop Every cart line
        Tx->>DAO: Load product by activeShopId and productId
        DAO->>DB: SELECT scoped product
        DB-->>DAO: Product and current stock
        DAO-->>Tx: Product record
        Tx->>Tx: Check existence, shop scope, stock, and prices
    end
    alt Missing product or insufficient stock
        Tx-->>Repo: Raise validation error
        Repo-->>UI: Reject with product and available quantity
        Note over Tx,DB: Roll back; no partial sale, stock update, or movement persists
    else All cart lines valid
        Tx->>Tx: Freeze name, SKU, quantity, unit price, unit cost
        Tx->>Tx: Calculate totals with checked integer minor-unit arithmetic
        loop Each prepared line
            Tx->>DAO: Insert sale-line snapshot
            DAO->>DB: INSERT sale line
            Tx->>DAO: Decrement product stock
            DAO->>DB: UPDATE product quantity
            Tx->>DAO: Append movement with before/after and negative delta
            DAO->>DB: INSERT stock movement
        end
        Tx->>Tx: Commit transaction
        Tx-->>Repo: Checkout result
        Repo-->>UI: Success and total
        UI-->>Cashier: Show completed checkout
    end
```

**Atomicity:** sale-line writes, stock decrements, and stock-movement inserts belong inside the same transaction. Any validation failure or exception must roll back all writes. A low-stock warning is informational; it must never bypass stock validation.

## 3. Entity-relationship model

The diagram below is the **logical target model**. The current Room schema may not yet include a separate `Sale` header or declared foreign-key constraints. Implement those only through deliberate schema changes and migrations.

```mermaid
erDiagram
    SHOP ||--o{ PRODUCT : owns
    SHOP ||--o{ STOCK_MOVEMENT : scopes
    SHOP ||--o{ SALE : scopes
    SALE ||--|{ SALE_LINE : contains
    PRODUCT ||--o{ SALE_LINE : referenced_by
    PRODUCT ||--o{ STOCK_MOVEMENT : changes
    SHOP {
        string id PK
        string name
        string businessType
    }
    PRODUCT {
        string id PK
        string shopId FK
        string name
        string sku
        int quantity
        long purchasePriceMinor
        long sellingPriceMinor
    }
    STOCK_MOVEMENT {
        string id PK
        string shopId FK
        string productId FK
        int quantityDelta
        int quantityBefore
        int quantityAfter
        string reason
        long createdAt
    }
    SALE {
        string id PK
        string shopId FK
        long totalMinor
        long createdAt
    }
    SALE_LINE {
        string id PK
        string saleId FK
        string productId FK
        string productNameSnapshot
        string skuSnapshot
        int quantity
        long unitPriceMinor
        long unitCostMinor
        long lineTotalMinor
    }
```

Money fields use integer minor units (poisha/cents). Sale-line snapshots preserve historical values even if the product catalog changes later.

## 4. Active-shop isolation

```mermaid
flowchart TB
    OWNER["Owner"]
    SWITCH["Select active shop"]
    ACTIVE["Active shop context<br/>activeShopId"]
    VM["ViewModel receives shop-scoped state"]
    REPO["Repository operation receives shopId"]
    CHECK{"Shop exists and operation is scoped?"}
    DAO["DAO query / write with shopId predicate"]
    DB[("Room / SQLite")]
    REJECT["Reject operation"]
    OWNER --> SWITCH --> ACTIVE --> VM --> REPO --> CHECK
    CHECK -- Yes --> DAO --> DB
    CHECK -- No --> REJECT
    NOTE["Never trust UI selection alone.<br/>Scope reads and writes at repository / DAO boundaries."]
    NOTE -. security invariant .-> CHECK
```

Every shop-owned read and write must carry an explicit shop identifier. Product lookup, checkout validation, stock movement history, and reports must not silently fall back to unscoped queries. Tests should verify that a product or cart from one shop cannot be read or mutated through another shop's context.
