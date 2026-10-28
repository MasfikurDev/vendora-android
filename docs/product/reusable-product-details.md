# Reusable product details

Status: implemented on `feat/reusable-product-details`; pending pull-request review and CI.

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

## Next useful additions (not implemented)

1. Product images, with local/offline storage and explicit backup behavior.
2. Supplier/contact records and purchase/receiving workflows.
3. Expiry dates and batch tracking for businesses that need them; avoid imposing pharmacy-specific fields on every shop.
4. Purchase history, returns, damaged/lost stock reasons, and printable/exportable stock reports.
5. CSV import/export with preview, duplicate detection, and recoverable validation errors.
6. Barcode camera scanning and label printing, after the manual barcode field and data model are stable.
7. Shop profile assets (logo, address, phone, currency/locale preferences) and shareable business identity.

These items are roadmap ideas, not claims about current functionality. Add them in small, testable changes with explicit Room migrations and update this document as status changes.
