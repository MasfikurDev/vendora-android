---
name: android-compose-quality
description: Review or implement Vendora Compose screens for mobile-first usability, correct state/lifecycle handling, accessibility, and responsive layouts.
---

# Android Compose quality for Vendora

## Before changing UI
- Read [`docs/PRODUCT_DIRECTION.md`](../../../docs/PRODUCT_DIRECTION.md) and its concept board before broad UI work; treat them as proposed product requirements, not proof of implemented screens.
- Preserve the intended experience: dashboard-first Home, one-tap Sell, image-first Products, business-profile/switching, and real-data-driven Insights. Keep the active business context consistent across all destinations.
- For product imagery, inspect the current model and storage before implementation; define durable local references, offline loading, fallback states, replacement/deletion cleanup, and backup implications. Do not embed large image blobs in JSON by default.
- Inspect actual Compose BOM, Kotlin, Android Gradle Plugin, Material 3 dependencies, navigation approach, and nearby screen patterns. Do not introduce APIs based on assumed versions.
- Keep UI mobile-first and Material 3. Reuse existing components/theme; avoid adding a UI library without a concrete need.
- Keep business rules and persistence out of composable bodies. Follow existing boundaries and add abstractions only when needed.

## State and lifecycle
- Identify the authoritative state owner. Prefer observable UI state from the existing architecture. If the screen currently reads storage directly, keep the change focused and document the limitation.
- Avoid one-time remember snapshots for data expected to change on another screen. Ensure screen re-entry refreshes changed persisted data.
- Keep side effects out of composition. Use effects or event callbacks only when their semantics fit; do not use LaunchedEffect to wrap synchronous code.
- Handle loading, empty, success, and error states where relevant. Prevent duplicate submissions and avoid clearing form/cart state before success is confirmed.

## Layout and accessibility
- Check narrow devices, long product names/SKUs, large font scale, keyboard/IME, insets, scrolling, and dialogs near screen edges.
- Provide meaningful semantics and labels for interactive controls; do not rely on color alone. Ensure touch targets and focus order are usable.
- Use stable item keys in lazy lists when records have stable identifiers. Avoid nested scrolling and excessive layout wrappers without reason.
- Preserve state across recompositions and configuration changes only when intended; do not overuse rememberSaveable for derived or sensitive state.

## Validation
- Add or update Compose behavior tests when practical, including empty/error states and important actions.
- Run existing JVM tests and build tasks if available. Distinguish source review from actual emulator/device testing.
- Review the diff for accidental behavior changes, duplicate state, inaccessible controls, and unsupported API usage.
