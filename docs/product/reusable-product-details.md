# Reusable product details

Status: reusable product metadata is implemented in the dependent feature stack; sales activity is implemented on `feat/sales-history-activity` and pending CI/review.

## Implemented in this change

Every product can store:

- **Brand** — optional free text, with saved brand choices for the current shop.
- **Category** — optional free text, with saved choices.
- **Unit** — defaults to `pcs`; common units include pcs, kg, g, litre, ml, box, pack, dozen, and meter. Custom units are retained.
- **Barcode** — optional barcode value entry and indexed shop-scoped lookup support; camera scanning is not included in this change.
- **Low-stock threshold** — per-product restock alert quantity, defaulting to 5.

Brand, category, and unit options are stored in Room independently of products, so deleting a product does not erase the saved option. Matching is case-insensitive within a shop, and one shop's saved options are not offered in another shop. Existing Room v1 data is migrated to v2 with safe defaults; the legacy JSON importer also accepts these optional fields without requiring them in older data.

## Data and safety rules

- Keep all product and reusable-option records shop-scoped.
- Do not replace the explicit Room migration with destructive fallback behavior.
- Keep barcode values optional; do not assume all business types use unique barcodes.
- Use integer poisha for all prices.
- Keep saved options when a product is deleted; a future dedicated catalog-management screen may allow owners to rename or remove unused options deliberately.

## Sales activity added in the next feature stack

Checkout now writes a sale header and its product-line snapshots in the same Room transaction as stock deductions and stock-movement records. The Activity screen lists shop-scoped sales, totals, timestamps, and expandable item details. Existing version-2 sale-line records are preserved during the v2→v3 migration; because older records had no transaction identifier, each historical line is represented as its own legacy sale rather than guessing which lines belonged together.

The current CI workflow compiles instrumentation tests but does not execute them on an emulator/device. Physical-device verification of migration and checkout remains required before treating the new Activity flow as runtime-verified.

## Home-appliance category suggestions

The dependent `feat/home-appliance-catalog` branch adds generic offline categories for shops labeled Electronics or Home Appliances. Suggestions cover rice cookers, electric irons, refrigerators, cookware/casseroles, induction cookers, blenders, mixer grinders, water filters/purifiers, and electric kettles. No real-world business or brand names are bundled; brand values remain user-entered.


## Next useful additions (not implemented)

1. Product images, with local/offline storage and explicit backup behavior.
2. Supplier/contact records and purchase/receiving workflows.
3. Expiry dates and batch tracking for businesses that need them; avoid imposing pharmacy-specific fields on every shop.
4. Purchase history, returns, damaged/lost stock reasons, and printable/exportable stock reports.
5. CSV import/export with preview, duplicate detection, and recoverable validation errors.
6. Barcode camera scanning and label printing, after the manual barcode field and data model are stable.
7. Shop profile assets (logo, address, phone, currency/locale preferences) and shareable business identity.

These items are roadmap ideas, not claims about current functionality. Add them in small, testable changes with explicit Room migrations and update this document as status changes.
