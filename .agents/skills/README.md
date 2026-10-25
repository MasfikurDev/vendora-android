# Vendora agent skills

These skills are repository guidance for AI coding agents. Custom Vendora skills encode project-specific invariants; upstream skills are retained separately with attribution and their original license.

## Vendora-specific skills
- [Workflow verification](vendora-workflow-verification/SKILL.md) — trace UI, navigation, persistence, and related screens.
- [Compose quality](android-compose-quality/SKILL.md) — accessible, lifecycle-aware, mobile-first Compose UI.
- [Inventory transaction integrity](inventory-transaction-integrity/SKILL.md) — stock, sale snapshots, movement history, and shop isolation.
- [Testing and regression](vendora-testing/SKILL.md) — layered tests and honest verification reporting.
- [Git workflow](vendora-git-workflow/SKILL.md) — branch/PR awareness, reviews, CI, and documentation.

## Upstream skills included
Copied from [Google's android/skills](https://github.com/android/skills) under Apache-2.0. These are upstream snapshots, not Vendora-authored guidance; update deliberately and retain the license.
- [Jetpack Compose performance](upstream-android/jetpack-compose-performance/SKILL.md)
- [Adaptive Compose layouts](upstream-android/compose-adaptive-layouts/SKILL.md)
- [Android testing setup](upstream-android/android-testing-setup/SKILL.md)

The upstream files can evolve independently. Confirm recommendations against this repository's actual Gradle/Compose versions and architecture.
