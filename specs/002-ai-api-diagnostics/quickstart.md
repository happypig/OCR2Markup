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

## Implementation Notes

- Do not add automatic repair logic in this feature.
- Do not expose full service error bodies in the DAMA panel.
- Keep export user-initiated.
- Keep all user-facing text in `translation.xml`.
