# Quickstart: CBRD Parse Endpoint Integration

**Feature**: 004-cbrd-parse-endpoint
**Date**: 2026-07-31

This is a validation guide for proving the feature works end-to-end. It references the
[contract](contracts/openapi.yaml) and [data model](data-model.md) rather than duplicating
implementation details.

## Prerequisites

- Oxygen XML Editor / SDK 27.1.0.3+ running on Java 17 (plugin still compiles to Java 8)
- Maven 3.x and JDK 8 available as `JAVA_HOME`
- Maven project root: `Models/Gemini2.5/dila-ai-markup-plugin`
- The DILA CBRD Parse endpoint (`https://cbss.dila.edu.tw/cbrd/parse`) is reachable
- A shared DILA parse token has been provisioned by the administrator
- A test TEI XML document open in Oxygen's Text mode

## Build and Run Tests

```bash
cd Models/Gemini2.5/dila-ai-markup-plugin
mvn test
```

Expected: all tests pass, including:
- New `CbrdParseApiClientTest` (HTTP client mapping for 200/400/401/422/502/503/connectivity)
- New preference/configuration tests asserting CBRD Parse keys and secure token storage
- Updated AI Markup command tests retargeted to CBRD Parse
- Existing Ref-to-Link, UTF-8 validation, and tag removal tests unchanged
- `TranslationBundleCompletenessTest` passes (all new keys present in `en_US`, `zh_CN`, `zh_TW`)

## Package the Plugin

```bash
mvn clean install
```

Expected: `target/dilaAIMarkupPlugin.zip` is produced and copied to the project root. The
assembly and the descriptor generation are bound to the `install` phase in `pom.xml`, so
`mvn clean package` alone produces only `target/dila-ai-markup-plugin-<version>.jar`.

## Install and Configure

1. Install the plugin in Oxygen via the bundled add-on or by unzipping into the Oxygen plugins folder.
2. Open **Options > Preferences > DILA AI Markup Assistant**.
3. Verify the six OpenAI-era fields are gone:
   - Parsing model, Detection model, AI API key, AI API base URL, AI chat path, AI timeout
4. Verify the CBRD Parse section exists with defaults:
   - **CBRD Parse endpoint URL**: `https://cbss.dila.edu.tw/cbrd/parse`
   - **CBRD bearer token**: (empty)
   - **CBRD Parse timeout (ms)**: `30000`
5. Enter the shared DILA parse token.
6. Click **Apply**.

## Validation Scenarios

### Scenario 1 — Core success path (US1)

1. Open a TEI XML document in Text mode with `xml:lang="zh"` on the root element.
2. Select a passage of unmarked CBETA reference text (< 4,000 characters).
3. Invoke **AI Markup** from the menu.
4. Expected: the result area shows processing feedback, then displays the returned TEI P5
   marked-up XML. The editor remains responsive while the request runs.
5. Review the markup; click **Replace**.
6. Expected: the selection is replaced and the change is undoable (Ctrl+Z reverses it).

### Scenario 2 — Document language drives the request (US3)

1. Open a document with `xml:lang="jp"` on the root element.
2. Select a passage and invoke AI Markup.
3. Expected: the request carries `lang=jp` and the service returns appropriate markup.
4. Repeat with a document whose root carries `xml:lang="zh"`.
5. Expected: the request carries `lang=zh`.
6. Repeat with a document whose root has no usable `xml:lang`.
7. Expected: the request carries `lang=zh` (default).

### Scenario 3 — Client-side input validation (FR-019)

1. Select nothing (empty selection) and invoke AI Markup.
2. Expected: the result area shows input-missing guidance and no request is sent.
3. Select > 4,000 characters and invoke AI Markup.
4. Expected: the result area shows too-long guidance and no request is sent.

### Scenario 4 — Concurrency (FR-015)

1. Select text and invoke AI Markup.
2. While the request is still in flight, invoke AI Markup again.
3. Expected: the second invocation is ignored, the result area shows "AI Markup already in
   progress" together with the in-flight selected XML, and no second request is sent.

### Scenario 5 — Cancel on close (FR-020)

1. Select text and invoke AI Markup.
2. While the request is in flight, close the document or Oxygen.
3. Expected: the in-flight request is cancelled/interrupted, the result is discarded silently,
   and no markup is written into a closed document.

### Scenario 6 — Failure mapping (US4)

For each enumerated `ParseError` code, verify the result area shows a distinct, actionable
message. See [contracts/openapi.yaml](contracts/openapi.yaml) for the full list.

- `unauthorized` (401) → credential guidance, distinct from connectivity
- `text_is_too_long` (400) → selection exceeds input limit
- `openai_unavailable` (502) → service temporarily unavailable, retry shortly
- `openai_rate_limited` (503) → service temporarily unavailable, retry shortly
- `invalid_model_output` (422) → specific model-output cause
- Unreachable endpoint → connectivity message distinct from service failures
- Unexpected status/body → generic failure, no crash, no document mutation

### Scenario 7 — Upgrade path (US2)

1. Start with the previous plugin version and OpenAI-style preferences populated.
2. Upgrade to the new version.
3. Expected: OpenAI fields are gone, CBRD Parse endpoint URL is prefilled, and obsolete
   values are ignored. Enter the shared token and complete one AI Markup operation.

### Scenario 8 — i18n completeness (NFR-001)

```bash
mvn test -Dtest=TranslationBundleCompletenessTest
```

Expected: passes. Every new key exists in `en_US`, `zh_CN`, and `zh_TW`.

### Scenario 9 — Malformed endpoint URL (FR-021)

1. Set the CBRD Parse endpoint URL preference to `not-a-url` and Apply.
2. Select text and invoke AI Markup.
3. Expected: the result area names the endpoint URL preference as the problem, no request is
   attempted, and the message differs from the connectivity message in Scenario 6.

### Scenario 10 — Diagnostics export on failure (FR-022)

1. Configure a wrong token and invoke AI Markup.
2. Expected: the `unauthorized` guidance appears with an **Export Diagnostics** control.
3. Export and open the package. Expected: the shared token appears only as a fingerprint
   (`****XXXX`), never in full.

## Next Step

The task list is generated in [tasks.md](tasks.md). Proceed to `/speckit.implement` once the
Complexity Tracking approval in [plan.md](plan.md) is recorded.
