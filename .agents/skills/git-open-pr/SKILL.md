---
name: git-open-pr
description: Open an authorized MDive GitHub pull request from a pushed branch with a consistent title and evidence-based body.
---

# Open a pull request

Use this skill when the user asks to open an MR or PR. GitHub calls it a pull request. Read `docs/engineering/git-workflow.md` for the target branch and title convention.

- Confirm the head branch, its remote push, target branch, diff, and working-tree status. If intended changes are still uncommitted or unpushed, return to the commit-and-push workflow. Check whether a PR already exists for the same head and base before creating another.
- Base the title and body on the actual commits and diff. Use a Conventional Commit title and the [PR body template](assets/pr-body.md). Replace every placeholder and omit sections that truly do not apply.
- List the checks actually run and their outcomes. Include relevant risks, open decisions, or testing limits; never claim a check that did not run.
- Write the finished body to a temporary UTF-8 file and call `gh pr create --body-file` with the explicit head and base. Preserve intended newlines. Use the repository's GitHub CLI login; if unavailable, report the specific obstacle.
- Verify the resulting PR URL, title, head, base, and state. Return the URL and a concise summary. Do not merge the PR as part of this workflow.
