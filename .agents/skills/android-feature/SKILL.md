---
name: android-feature
description: Implement an agreed MDive Android feature using the project's Compose, MVI, Hilt, and layer rules.
---

# Android feature implementation

Use this skill when building or changing an agreed app feature. Read the relevant product requirements and only the engineering documents that govern the affected code.

- For screen changes, follow links from the product requirements to the HTML screen specifications. Use `docs/product/planning.md` to distinguish their responsibilities; draft or undecided wireframes are not agreed requirements.
- Follow `docs/engineering/architecture.md` for layer boundaries, repository contracts, Hilt, and Compose components.
- Follow `docs/engineering/mvi.md` for each affected screen, and `docs/engineering/navigation.md` when the feature changes destinations.
- Implement the smallest complete user flow, including its loading and failure behavior. Add focused tests for business rules or important state transitions and run the checks relevant to changed code.
- Update product or engineering documentation when implementation changes an agreed behavior or boundary. Return what works, how it was verified, and any unresolved decision or limitation.
