# Isolated UI preview workflow

## Purpose

`.github/workflows/ui-preview.yml` is a downstream visual-preview pipeline. It runs only after **Android CI and Release Builds** completes successfully for a build associated with `main`, including a manual build dispatch on `main`. It downloads the exact APK artifact from that run, launches it in an ephemeral Android emulator, creates clearly named sample data through the app's user interface, captures screenshots, and publishes the output to the separate `ui-previews` branch.

## Isolation and safety

- Does not run on pull requests, forks, feature branches, or failed APK builds.
- Does not edit application source, Gradle configuration, release tags, or the main branch.
- Does not publish an APK or GitHub release.
- Does not gate, cancel, or alter the result of the upstream Android CI.
- Uses a fresh emulator and clears only the app data on that disposable emulator.
- Uses sample shop/products only; never connect this workflow to real accounts or customer data.
- The workflow is permitted to write only its published preview branch through the Pages deploy action. Keep this branch dedicated to generated preview files.
- A failed screenshot run is a preview-only failure. It must be fixed without changing the APK workflow's success status.

## Output

- `screenshots/home.png`
- `screenshots/products.png`
- `screenshots/selling.png`
- `screenshots/insights.png`
- `index.html`, a simple gallery with the source build link and commit SHA

The README embeds the stable image paths from the `ui-previews` branch. The generated gallery is published as `index.html` on the `ui-previews` branch. Raw screenshot links work without enabling GitHub Pages; if Pages is later enabled for that branch, the HTML gallery can be served as a website.

## Trigger policy

The initial implementation intentionally previews successful builds associated with `main` only, including manually dispatched builds on `main`. It does not execute APKs produced by PRs or feature branches in a privileged `workflow_run` context. The upstream build artifact is named `vendora-build-<run_number>-<run_attempt>`; the preview job downloads that exact artifact by run ID.

## Demo automation

`scripts/ui-preview/capture_demo.py` uses Android UI hierarchy text and screen coordinates rather than fixed tap coordinates. It creates one disposable shop and three products, then captures the app's current Overview, Products, Selling, and Insights screens. If UI labels change, the preview job should fail clearly instead of publishing partial or stale images.

## Cost and performance

This pipeline consumes an additional hosted runner and emulator time after the main APK build. It has a 20-minute timeout and cancels older queued/running preview jobs when a newer main build arrives. The Android build and its checks do not wait for it.
