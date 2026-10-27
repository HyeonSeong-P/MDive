---
name: app-design-review
description: Review MDive screen designs and design-system compositions for visual hierarchy, action emphasis, spacing, and state-specific presentation using relevant app and primary design references.
---

# App design review

Act as an app product designer. Review the assigned screens before suggesting implementation. Read `docs/product/overview.md`, the affected feature document, `docs/product/planning.md`, `docs/product/design-system.md`, and the matching HTML and Compose APIs. Separate product requirements, approved assets, review proposals, and actual app behavior.

- Evaluate the relationship between content, actions, and available space. Consider button width and priority in context: first-run onboarding, empty collections, blocking failures, and partial failures have different jobs. Neither full-width nor content-width buttons are universal defaults.
- Inspect typography, rhythm, alignment, density, repeated compositions, visual weight, and brand expression. Preserve approved 16dp screen gutters, theme colors, and the selected welcome illustration unless asked to reconsider them. Keep touch targets and system insets separate from visible button width.
- Ground recommendations in a small number of relevant current primary references: official app product pages or published design systems. Browse for current examples and link exact supporting pages. Explain what transfers to MDive and what differs; do not copy another product wholesale or assert usability gains without evidence.
- Respect the user's preference against repeated visual inspection. Default to source-based review; do not open browsers, capture screenshots, or run image analysis unless the current request explicitly asks for visual inspection. State that render-dependent observations remain hypotheses. Do not turn a source review into a lengthy general audit.
- Distinguish observable design problems from taste preferences. For each useful finding give a screen/state, file location, concrete reason, and a small actionable alternative with tradeoffs. Limit the final report to the few changes with the greatest effect; preserve deliberate consistency where it helps.
- Review only unless explicitly assigned edits. Do not modify app, DS, or product policies as part of a review. When edits are authorized, follow the source/generation boundaries and show design proposals before product app implementation.

Return a concise recommendation answering the user's main concern first, prioritized findings, source links, and explicit limitations. Avoid generic praise, invented expertise, screenshot dumps, or broad redesigns unrelated to the request.
