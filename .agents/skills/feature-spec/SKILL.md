---
name: feature-spec
description: Define or revise MDive user scenarios, scope, acceptance criteria, and repository HTML wireframe specifications before implementation.
---

# Feature specification

Use this skill when planning a new feature or changing its expected behavior. Read `docs/product/overview.md`, `docs/product/planning.md`, and the relevant existing product documents first.

- Turn the user's intent into concrete user scenarios, an initial scope, and observable acceptance criteria. Include loading, empty, and failure behavior when the feature needs them.
- Separate confirmed requirements from assumptions and open decisions. Return decisions that would materially change the feature to the main agent; leave unresolved choices clearly marked.
- Update the product document that owns the feature. Create a focused document under `docs/product/` only when the overview would become hard to use. For a new feature document, start from [the feature specification template](assets/feature-spec-template.md), adapt its sections, and remove placeholders.
- When planning screens, create or update offline HTML wireframes with adjacent screen descriptions under `docs/product/screens/`, following `docs/product/planning.md`. Keep scope, common policies, and acceptance criteria in Markdown; link the owning feature document and stable screen IDs instead of duplicating requirements. Start from [the screen specification template](assets/screen-spec-template.html), adapt it to the agreed scenarios, and remove example placeholders from actual specifications. Do not create app screens before their scenarios are agreed.
- Verify changed HTML in a browser, including relevant states and links. Report what was verified and any unavailable visual verification. Do not add an app dependency or publish a site to preview planning documents.
- Flag architecture or implementation implications for the main agent without choosing a parser, file source, permission model, or other undecided product behavior on the user's behalf.
- Return a short summary of changed documents, decisions made, open questions, and the criteria implementation should meet.
