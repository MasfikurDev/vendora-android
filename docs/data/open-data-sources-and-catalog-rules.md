# Open data sources and real-world catalog rules

Last reviewed: 2026-10-28

## What the app uses now

The shop-creation dialog reads `app/src/main/assets/catalog/shop_types.json`. The catalog is bundled with the APK, so shop creation works offline. It contains stable internal IDs, English and Bengali display labels, common aliases, and optional reference tags.

The current Room schema still stores the selected English label in `ShopEntity.businessType` for compatibility with existing saved shops. The catalog IDs are not yet persisted as canonical shop-type IDs; adding a dedicated ID column should be a separate, explicit Room migration. Do not silently change existing values.

## Offline home-appliance category suggestions

The app includes `app/src/main/assets/catalog/home_appliances.json`, a generic offline category list for electronics and home-appliance shops. It contains broad product categories and example product types only; it contains no real shop names, brand names, official company links, product imagery, prices, or inventory records. Brand choices remain user-entered and shop-specific.


## Sources and intended use

### 1. OpenStreetMap shop tagging

- Source: [Key:shop](https://wiki.openstreetmap.org/wiki/Key:shop) and [Shops](https://wiki.openstreetmap.org/wiki/Shops).
- Use: reference vocabulary for retail types and mapping hints, such as `shop=general`, `shop=convenience`, `shop=clothes`, `shop=hardware`, and `shop=stationery`. Pharmacy is commonly represented with `amenity=pharmacy`, not `shop=pharmacy`.
- License: OpenStreetMap data is under ODbL 1.0. Attribute OpenStreetMap where its database content is reused; evaluate share-alike requirements before distributing a derived database. This small app catalog is an adapted, selected taxonomy, so retain this source record and review obligations before release.
- Limits: these tags describe mapped places and primary retail activities. They are not a Bangladesh legal business register, not a tax code list, and not proof that a shop is licensed.

### 2. Bangladesh Bureau of Statistics (BBS)

- Source: [BBS industrial statistics and classifications](https://bbs.gov.bd/pages/static-pages/6922de09933eb65569e17bb3), which lists Bangladesh Standard Industrial Classification (BSIC) 2020 and a Wholesale and Retail Trade Survey.
- Use: authoritative reference for future reporting or a formal industry-classification field, after verifying the actual document, section, version, and permitted reuse terms.
- Limits: do not present Vendora's user-friendly shop types as BSIC codes. We have not copied a full BSIC table into the app, and the current app does not claim official BSIC compliance.

### 3. Bangladesh Customs Import Export Hub

- Source: [Bangladesh Customs & VAT Import Export Hub](https://hub.bangladeshcustoms.gov.bd/) and its [FAQ](https://hub.bangladeshcustoms.gov.bd/faq).
- Use: a reference for HS-code-based goods descriptions, tariff information, required certifications, and import/export procedures. This could support a later optional product HS-code field for traders who need it.
- Limits: HS codes classify goods for customs; they are not shop types or general retail product categories. Rates and requirements change, so do not bundle unverified tariff values or treat a local product category as a customs classification.

### 4. Open Food Facts

- Source: [Open Food Facts API documentation](https://openfoodfacts.github.io/openfoodfacts-server/api/) and [license guidance](https://openfoodfacts.github.io/openfoodfacts-server/api/tutorials/license-be-on-the-legal-side/).
- Use: optional future barcode lookup for packaged foods, product name, brand, package quantity, ingredients, and nutrition fields where available. Use API v3 for new integrations, identify the app with a custom User-Agent, respect documented rate limits, and cache results with their source and retrieval time.
- License: database ODbL 1.0; individual contents DbCL 1.0; images generally CC BY-SA. Before copying or redistributing records/images, follow the applicable license, attribution, and share-alike requirements.
- Limits: contributions are voluntary and may be missing, stale, or incorrect. Never overwrite a shop owner's price, cost, stock count, or locally entered details with remote data. Show imported fields as suggestions and let the user confirm them. The core inventory and checkout must work offline.

### 5. GS1 Global Product Classification (GPC)

- Source: [GS1 standards repository](https://ref.gs1.org/standards/) and [GPC Browser](https://gpc-browser-dev.gs1.org/).
- Use: a potential interoperability reference for product classification if Vendora later needs supplier/retailer data exchange.
- Limits: do not assume the GS1 taxonomy is an unrestricted open dataset merely because the browser is public. Verify current download terms and reuse rights before bundling its schema or values. For now, Vendora uses its own lightweight category field and user-defined categories.

## Vendora design rules

1. **Offline first:** bundled shop-type suggestions must not depend on a network request.
2. **Editable:** users can select a suggestion, but can still identify a shop using the closest available type; keep an “Other / mixed business” option.
3. **Local fit:** include common Bangladesh retail patterns such as grocery/general stores, wholesale, clothing, mobile/computer shops, hardware/building supplies, stationery, pharmacy, produce, bakery/sweets, fish/meat, agricultural inputs/feed, household goods, and vehicle parts.
4. **Separate dimensions:** shop type, product category, brand, unit, barcode, and regulatory classification are distinct fields. Do not overload one field with all of them.
5. **No false compliance claims:** a shop type or HS-code suggestion does not verify trade licenses, VAT/TIN status, pharmacy permissions, product authenticity, or any other legal requirement.
6. **Provenance:** any imported product field should record source, source identifier (for example barcode), fetched timestamp, and verification status. Images require their own license/attribution treatment.
7. **Validation:** validate numeric prices as nonnegative integer poisha; stock and threshold as nonnegative integers; preserve barcode as text (leading zeros matter); do not assume every product has a barcode or globally unique code.
8. **Sensitive verticals:** pharmacy/food and other regulated inventory need optional batch, expiry, recall, and traceability workflows where relevant. Do not imply those workflows are implemented until tested.
9. **Change control:** update this document and catalog metadata when adding sources. Do not scrape sites against terms or silently bundle large third-party datasets.

## Follow-up implementation ideas (not yet implemented)

- Add a dedicated `businessTypeId` column via an explicit Room migration, preserve the old label, and map old values through aliases.
- Add optional product lookup that requires user action, works with cache, respects source limits/licenses, and never blocks checkout.
- Add a CSV import preview with source/field mapping, duplicate handling, validation, and a rollback-safe import transaction.
- Add configurable expiry/batch tracking only for shops that enable it, with test coverage and clear audit history.
- Add a documented data-quality test set for catalog uniqueness, aliases, stable IDs, barcode formatting, unit consistency, and backward compatibility.


## Catalog governance and release checklist

Treat every external dataset or taxonomy as a versioned dependency, even when it is bundled as a small JSON file.

Before adding or changing catalog content:

1. **Identify the exact source material.** Record the canonical source URL, the page or dataset name, the version/date retrieved, and which fields were copied, transformed, or authored by Vendora.
2. **Confirm the license for that exact material.** A public webpage, a public API, and a downloadable database can have different terms. Record attribution, share-alike, notice, and redistribution requirements before merging.
3. **Separate source values from Vendora-authored content.** Label translations, aliases, display names, and normalization rules as authored/adapted where applicable; do not imply that a government body or data provider approved them.
4. **Preserve provenance with records.** If remote product lookup is introduced, keep the source name, source identifier, retrieval timestamp, and user verification status alongside imported fields where practical.
5. **Validate the artifact.** Check JSON/schema validity, unique stable IDs, localized labels, alias collisions, and expected fallback behavior in automated tests.
6. **Review distribution impact.** Update `THIRD_PARTY_NOTICES.md`, this guide, and any in-app attribution surface required by the source's terms.
7. **Avoid false assurance.** Catalog presence is not a legal classification, product-authenticity guarantee, or regulatory approval.

### Current catalog boundary

The bundled shop-type catalog is a lightweight onboarding taxonomy with English/Bengali labels and reference tags. It is not an extract of the full OpenStreetMap database, a full BBS BSIC table, a customs tariff database, or a product database. Because the catalog references OSM tagging vocabulary, keep the source provenance and re-evaluate the exact material and license obligations before materially expanding or redistributing it. This documentation is a compliance aid, not legal advice.
