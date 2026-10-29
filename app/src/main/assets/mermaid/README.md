# Offline runtime

`mermaid.min.js` is the unmodified self-contained IIFE from npm `mermaid@11.15.0`.
No remote CDN, module chunks or build-time npm installation is required by Gradle.
The exact vendor file is exempt from Git whitespace checks via `.gitattributes`;
its upstream whitespace and recorded checksum are preserved.

- npm tarball SHA-256: `155379a1ecc10ad87c20f739d61276b32fe78e08fbd81219134276fd1fc8393c`
- bundled JS SHA-256: `70137e77bb273bb2ef972b86e8b0400cca8be53cb25bfc45911a186dc98665de`
- provenance/license: `../third_party/mermaid/`

To update, download the exact npm tarball, extract `dist/mermaid.min.js` and
`LICENSE`, check checksums, preserve the embedded dependency license comments,
and rerun all five-family/offline/security/limit instrumentation fixtures.
`index.html` and `host.js` are the app-owned trusted host. They accept JSON input
from a narrow Kotlin adapter; they expose no native JavaScript bridge.

`minSdk 24` does not guarantee an old system WebView supports this JavaScript.
Unsupported engines time out or fail locally and retain the source action.
