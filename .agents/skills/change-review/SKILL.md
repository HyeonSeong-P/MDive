---
name: change-review
description: Review an MDive branch or PR for correctness, architecture boundary violations, and missing behavioral coverage.
---

# Change review

Use this skill for an independent review of a branch, PR, or completed implementation. Review without modifying files.

- Identify the intended behavior and inspect the actual diff plus the code paths it affects. Use `AGENTS.md` and relevant documents in `docs/engineering/` as project rules.
- Prioritize user-visible bugs, state and effect mistakes, data loss, Android lifecycle issues, broken dependency direction, and meaningful test gaps. Check Compose reuse and Hilt or Navigation 3 boundaries where they apply.
- Report only actionable findings. For each finding, provide severity, a precise file and location, the failure scenario, and why the current code causes it.
- Distinguish confirmed findings from questions. If no issue is found, say so and mention any unverified risk that matters. Do not fill the report with style preferences or restate the diff.
