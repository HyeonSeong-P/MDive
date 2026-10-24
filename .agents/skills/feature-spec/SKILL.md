---
name: feature-spec
description: Define or revise an MDive feature's user scenarios, scope, and acceptance criteria before implementation.
---

# Feature specification

Use this skill when planning a new feature or changing its expected behavior. Read `docs/product/overview.md` and the relevant existing product documents first.

- Turn the user's intent into concrete user scenarios, an initial scope, and observable acceptance criteria. Include loading, empty, and failure behavior when the feature needs them.
- Separate confirmed requirements from assumptions and open decisions. Return decisions that would materially change the feature to the main agent; leave unresolved choices clearly marked.
- Update the product document that owns the feature. Create a focused document under `docs/product/` only when the overview would become hard to use. For a new feature document, start from [the feature specification template](assets/feature-spec-template.md), adapt its sections, and remove placeholders.
- Flag architecture or implementation implications for the main agent without choosing a parser, file source, permission model, or other undecided product behavior on the user's behalf.
- Return a short summary of changed documents, decisions made, open questions, and the criteria implementation should meet.
