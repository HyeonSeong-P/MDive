---
name: git-commit-push
description: Commit and push an authorized MDive change set using the repository's branch and Conventional Commit rules.
---

# Commit and push

Use this skill when the user asks to commit and push completed work. Follow `docs/engineering/git-workflow.md` for branch targets and commit messages.

- Inspect the current branch, upstream, working tree, and diff. Identify the files belonging to the requested change; leave unrelated user changes untouched. Check staged files too.
- Run the checks relevant to those changed files and `git diff --check`. Report failed checks and resolve failures caused by the change before committing.
- Stage intended paths explicitly. Run `git diff --cached --check` and review the staged diff for accidental files, secrets, and incomplete work. Make focused Conventional Commit messages describing the actual change.
- Push the current branch normally, setting `origin/<current-branch>` as upstream when it is new. Never force push or rewrite published history as part of this workflow. If the branch or upstream differs from the repository strategy, resolve that before pushing.
- Verify the remote branch and clean status for the committed scope. Return commit hash, branch, push result, checks, and any remaining local changes.

Do not create a PR in this workflow; use `git-open-pr` when the user asks for one.
