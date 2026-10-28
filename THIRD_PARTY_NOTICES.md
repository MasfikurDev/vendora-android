# Third-party notices and data licensing

Vendora's application source code is licensed under the Apache License 2.0; see the repository-root [LICENSE](LICENSE). That license does not replace the separate terms that may apply to third-party code, documentation, data, images, fonts, or other assets.

This file records known provenance and review requirements. It is **not yet a generated, exhaustive software bill of materials (SBOM) or dependency-license report**. Before production distribution, inventory the resolved runtime and build dependencies and include every notice required by their licenses.

## OpenStreetMap shop-tagging reference

The bundled shop-type catalog uses OpenStreetMap Wiki pages as a reference for tagging vocabulary and compatibility hints:

- [Key:shop](https://wiki.openstreetmap.org/wiki/Key:shop)
- [Shops](https://wiki.openstreetmap.org/wiki/Shops)

Distinguish two separate licensing contexts:

- **OpenStreetMap Wiki documentation:** the wiki content is offered under Creative Commons Attribution-ShareAlike 2.0 (CC BY-SA 2.0), subject to the terms and exceptions stated by the source.
- **OpenStreetMap database:** the OSM database is offered under the Open Database License 1.0 (ODbL). Database reuse and derivative database distribution can carry attribution and share-alike obligations.

The current catalog contains Vendora-authored English/Bengali labels and aliases plus strings referencing OSM tagging vocabulary. It does not bundle an extract of the OSM geographic database. This notice records provenance; it is not a legal determination about whether particular copied or adapted material triggers a specific license. Review the exact material and current source terms before materially expanding, copying, or redistributing the catalog.

Attribution and license information:

- https://www.openstreetmap.org/copyright
- https://wiki.openstreetmap.org/wiki/OpenStreetMap_License
- https://creativecommons.org/licenses/by-sa/2.0/
- https://opendatacommons.org/licenses/odbl/1-0/

## Other sources documented for possible future use

The following are research references, not a statement that their datasets are currently bundled or integrated:

- **Open Food Facts:** database ODbL 1.0; individual database contents DbCL 1.0; images may carry separate licenses. Read the [license guidance](https://openfoodfacts.github.io/openfoodfacts-server/api/tutorials/license-be-on-the-legal-side/) before using data or images.
- **GS1 Global Product Classification:** verify the current terms for the exact taxonomy files/values before copying or redistributing them. [GS1 standards](https://ref.gs1.org/standards/).
- **Bangladesh Bureau of Statistics:** verify reuse terms for the exact BSIC document or table before reproducing it. The app's user-friendly shop types are not official BSIC codes.
- **Bangladesh Customs:** verify the terms and current validity of any specific tariff or HS-code data before use. HS codes classify goods for customs purposes; they are not shop types.

## Android and build dependencies

Vendora uses Android, Kotlin, AndroidX/Jetpack Compose, Room, Gradle plugins, and test libraries. Dependency versions are declared in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts). Each dependency is governed by its own license and notice obligations.

The project does not currently publish a verified, exhaustive dependency-license inventory in this file. Before a production release:

1. Inspect the resolved dependency graph, including transitive runtime dependencies.
2. Use dependency metadata and the authoritative project/license files to identify licenses and required notices.
3. Check compatibility and attribution requirements for the intended distribution.
4. Include required third-party notices in the source repository and any applicable app distribution surface.
5. Re-run the inventory when dependencies or versions change.

Do not claim a dependency audit or SBOM exists until a generated artifact has been produced, reviewed, and committed or published.
