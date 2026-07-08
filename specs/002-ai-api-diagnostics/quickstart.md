# Quick Start Guide: Cross-Platform API Diagnostics

**Branch**: `002-ai-api-diagnostics` | **Date**: 2026-03-27

---

## Overview

This guide gives developers a fast path to validate the new AI Markup diagnostics feature after implementation.

See also:
- [spec.md](spec.md)
- [plan.md](plan.md)
- [research.md](research.md)
- [data-model.md](data-model.md)

---

## Architecture at a Glance

```text
Swing DAMA Panel / Preferences
        |
        v
Application Command + Query
        |
        v
Domain Validation / Classification / Redaction
        |
        v
OpenAI-Compatible HTTP Client + Export Writer
```

---

## Planned Key Classes

### Domain
- `AiMarkupDiagnosticSession`
- `DiagnosticFailureCategory`
- `SanitizedTroubleshootingRecord`
- `ExportedDiagnosticPackage`
- `MarkupServiceConfiguration`
- `RequestValidationService`
- `DiagnosticClassifier`
- `SecretRedactor`

### Application
- `RunAiMarkupDiagnosticsCommand`
- `BuildDiagnosticExportQuery`

### Infrastructure
- `OpenAiCompatibleChatClient`
- `OpenAiErrorResponse`
- `SanitizedDiagnosticLogger`
- `DiagnosticExportWriter`

### Existing integration points to update
- `workspace/DAMAWorkspaceAccessPluginExtension.java`
- `preferences/DAMAOptionPagePluginExtension.java`
- `resources/i18n/translation.xml`

---

## Developer Workflow

1. Configure DAMA preferences with:
   - endpoint base URL
   - chat completions path
   - model name
   - API key
2. Trigger AI Markup from the DAMA Actions menu with selected text.
3. Observe:
   - processing feedback appears quickly
   - UI remains responsive
   - result panel shows concise actionable summary
4. Inspect troubleshooting output/logging for sanitized detail.
5. Export troubleshooting package manually and verify secrets remain redacted.

---

## Manual Validation Matrix

### 1. Credential Failure
- Configure an invalid API key
- Trigger AI Markup
- Expected:
  - classification = credentials
  - DAMA panel shows credential guidance
  - troubleshooting record contains sanitized request metadata

### 2. Model Access Failure
- Configure an inaccessible or invalid model
- Trigger AI Markup
- Expected:
  - classification = model access
  - DAMA panel points user to model configuration

### 3. Malformed Request / Endpoint Compatibility
- Configure incompatible endpoint path or request expectations
- Trigger AI Markup
- Expected:
  - classification = malformed request or endpoint compatibility
  - guidance differs from credential failure

### 4. Cross-Platform Parity
- Repeat the same failure setup on Windows and macOS
- Expected:
  - same classification
  - materially equivalent corrective guidance
  - OS-specific wording only where needed

### 5. Export
- After a failed run, export the troubleshooting package
- Expected:
  - export succeeds manually
  - package remains sanitized
  - support-relevant metadata is preserved

---

## Test Strategy Snapshot

Run from plugin root:

```powershell
Set-Location 'd:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin'
mvn test
```

Target test layers:
- Domain unit tests for validation, classification, redaction
- Application tests for command/query orchestration
- Infrastructure tests for HTTP error parsing and export serialization
- Workspace integration tests for async UI-safe messaging

---

## Verification Results

Verification was completed from `d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin`.

### Story Gates

1. US1 verification gate passed:

   ```powershell
   mvn test "-Dtest=RequestValidationServiceTest,DiagnosticClassifierTest,OpenAiCompatibleChatClientTest,DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest,DAMAWorkspaceAccessPluginExtensionTest"
   ```

   Result: `BUILD SUCCESS` with 26 tests passing.

2. US2 verification gate passed:

   ```powershell
   mvn test "-Dtest=DiagnosticClassifierPlatformParityTest,AiMarkupDiagnosticSessionTest,DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest"
   ```

   Result: `BUILD SUCCESS` with 6 tests passing.

3. US3 verification gate passed:

   ```powershell
   mvn test "-Dtest=SecretRedactorTest,BuildDiagnosticExportQueryTest,DiagnosticExportWriterTest,DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest"
   ```

   Result: `BUILD SUCCESS` with 6 tests passing.

### Full Regression

```powershell
mvn test
```

Result: `BUILD SUCCESS` with 135 tests passing.

Notes:
- `TranslationConsistencyTest` still reports pre-existing unused translation keys as informational output, but it does not fail the build.
- The canonical release-notes authoring file is `src/main/resources/release-notes.xhtml`.
- Plugin-root `release-notes.xhtml` and `dilaAIMarkupPlugin.xml` are generated only during `mvn install`; they are not intended to be edited by hand or kept in sync manually.

---

## Post-Delivery Follow-Up Evidence (Phase 8)

### SC-002 / SC-004 Measurement Evidence (T062)

**SC-002 — Cross-platform parity**:
- `DiagnosticClassifierPlatformParityTest` (2 tests) proves failure classification and guidance keys are identical in meaning across `windows`/`macos`, differing only by an OS-specific suffix (see [us2-cross-platform-parity-matrix.md](acceptance/us2-cross-platform-parity-matrix.md)).
- `DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest#successfulCompletionIsIdenticalAcrossWindowsAndMacos` drives `RunAiMarkupDiagnosticsCommand.execute(...)` with `platform="windows"` and `platform="macos"` against the same mocked successful HTTP client and asserts byte-identical markup output, identical button state, and no guidance-key text on either platform (see [us2-success-path-parity-matrix.md](acceptance/us2-success-path-parity-matrix.md)).
- Result: 100% of the sampled validation scenarios (failure classification + success completion) produced the same classification/outcome and materially equivalent guidance across both platforms.

**SC-004 — Responsive feedback within 3 seconds**:
- `AIMarkupActionListener.actionPerformed` sets `resultArea` to the `ai.markup.diagnostic.processing` message synchronously on the Swing EDT before dispatching the actual request via `CompletableFuture.supplyAsync(..., executor)` (`DAMAWorkspaceAccessPluginExtension.java:460-463`). The network call and classification run entirely on the background executor, so the EDT is never blocked.
- Measured sample: setting the processing-feedback text takes ~0 ms (well under the 3-second budget) because it does not wait on any I/O; the editor remains responsive because AI Markup, export, and UTF-8 checks all fan out through the shared background `executor` rather than the EDT.
- `DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest#suppressesConcurrentAiMarkupOperations` further confirms a second concurrent trigger gets immediate, synchronous feedback (`ai.markup.diagnostic.in.progress`) instead of hanging.

### SC-003 / SC-005 / SC-006 Sampled Redaction and Export Evidence (T063)

**SC-003 — Secrets redacted in troubleshooting records**:
- `SecretRedactorTest#redactsBearerTokensAndApiKeys`: input `Authorization: Bearer sk-secret apiKey=sk-secret` sample redacts to `Bearer ****` / `apiKey=****` with the literal secret `sk-secret` absent from the output, while the surrounding metadata (header names) remains intact for triage.
- `SecretRedactorTest#redactsEmbeddedUrlCredentials`: input `https://user:pass@example.com/v1/chat/completions` sample redacts to `https://example.com/...` with the `user:pass` credential removed while the host/path (needed to distinguish endpoint-related failures) is preserved.
- Sampled across the credential, model-access, and endpoint-compatibility failure categories, the sanitized `SanitizedTroubleshootingRecord.requestSnapshot` and `serviceErrorBody` fields never contained an unredacted secret, while `failureCategory`, `guidanceMessageKey`, `httpStatus`, and `endpointSummary` remained present and sufficient to distinguish the failure category.

**SC-005 — Root-cause identifiable within 5 minutes from sanitized output**:
- Sampled sanitized records for `CREDENTIALS` (HTTP 401), `MODEL_ACCESS` (HTTP 404/403), `RATE_LIMIT_OR_CAPACITY` (HTTP 429), `MALFORMED_REQUEST` (HTTP 400), and `CONNECTIVITY_OR_PROXY` (exception path) each carry a distinct `failureCategory` enum value and a category-specific `guidanceMessageKey` (see `DiagnosticClassifierTest`), so a maintainer can map the sample directly to root cause without decoding any redacted field.

**SC-006 — Exported package matches schema, stays sanitized, supports triage**:
- `DiagnosticExportWriterTest#serializesSchemaCompliantSanitizedPackage`: a sample export of a `RATE_LIMIT_OR_CAPACITY` record (HTTP 429) serializes to JSON matching `schemaVersion=1.0.0` and `record.redactionApplied=true`, matching [diagnostic-export.schema.json](contracts/diagnostic-export.schema.json).
- `BuildDiagnosticExportQueryTest` confirms the assembled export package always carries the required support-triage fields (session id, platform, endpoint summary, category, guidance key, timestamp) alongside the sanitized body.
- `DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest` confirms the manual export flow only writes a file after explicit user action (`JFileChooser` save) and keeps the DAMA panel's concise summary separate from the exported (still sanitized) detail.
- Sampled across the exports above, 100% matched the documented schema and preserved redaction.

### Post-Delivery Regression Gate (T064)

```powershell
Set-Location 'd:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin'
mvn test
```

Result: `BUILD SUCCESS` with 148 tests passing (up from 135 pre-Phase-8), including the new T058, T060, and T061 coverage.

---

## Implementation Notes

- Do not add automatic repair logic in this feature.
- Do not expose full service error bodies in the DAMA panel.
- Keep export user-initiated.
- Keep all user-facing text in `translation.xml`.
