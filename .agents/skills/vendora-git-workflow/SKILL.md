---
name: vendora-git-workflow
description: Manage Vendora Git branches, stacked pull requests, reviews, CI verification, and engineering documentation.
---

# Vendora Git workflow

1. Inspect repository, branch, open PRs, and exact head/base SHAs before edits. Never assume a branch is integrated into main.
2. Keep changes focused. Preserve the user's branch strategy and stack order.
3. Before updating an existing file through GitHub, fetch its current content SHA and use the fresh SHA. Do not write the same path concurrently.
4. Prefer a dedicated feature branch and PR for new work. For an existing PR, update only the intended branch when the change belongs in that stack.
5. Keep AGENTS.md, README, status history, and worklog consistent when workflow or project status changes. Add dated history entries rather than erasing important history.
6. Inspect diffs after changes and verify files exist on the intended branch. If only GitHub Contents API commits are available, report direct branch commits; do not imply local build/test execution.
7. Verify CI for the exact latest PR head SHA. A green run for a prior SHA is not current evidence.
8. Vendora auto-merge preference applies only to this repository: enable it for new PRs when available, only after required exact-head checks pass, GitHub reports mergeable, no blocking review remains, and no material data-loss/security risk is unresolved. Never bypass safeguards.
9. Final handoff states branch/PR, commits, files changed, tests/CI actually observed, and remaining risks. Do not claim merge or validation without evidence.
