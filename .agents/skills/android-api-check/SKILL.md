---
name: android-api-check
description: Verify version-sensitive Android and Jetpack API choices for an MDive feature against official documentation.
---

# Android API check

Use this skill when an Android or Jetpack API, dependency, or recommended integration may have changed or is uncertain.

- Inspect the repo's current Gradle versions and the exact API being considered. Search and open primary Android or library-maintainer documentation; do not rely on search snippets alone.
- Verify the API's intended use, required artifacts or plugins, compatibility constraints, and lifecycle behavior relevant to MDive. Check Hilt and Navigation 3 integration when the task involves them.
- State what the source confirms, what is an inference, and what remains unverified. Link directly to supporting documentation.
- Return a concise implementation recommendation and any concrete change the main agent should make. Do not edit project files unless explicitly assigned an implementation task.
