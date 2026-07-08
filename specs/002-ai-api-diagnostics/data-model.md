# Data Model: Cross-Platform API Diagnostics

**Branch**: `002-ai-api-diagnostics` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)

---

## Overview

This document defines the domain and exchange models for AI Markup diagnostics, with emphasis on request validation, failure classification, sanitized troubleshooting capture, and manual export for support. The model preserves diagnostics-only behavior and Java 8-compatible implementation boundaries.

---

## Domain Entities

### 1. AiMarkupDiagnosticSession (Aggregate Root)

**Purpose**: Tracks a single AI Markup diagnostic workflow from invocation through classification and optional export.

**Package**: `com.dila.dama.plugin.domain.model`

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|------------------|
| `sessionId` | String | Yes | Unique identifier for the diagnostic workflow | UUID or equivalent unique value |
| `selectedTextLength` | int | Yes | Length of selected text submitted for AI Markup | `>= 0` |
| `configuration` | MarkupServiceConfiguration | Yes | Effective endpoint/model configuration for this workflow | Non-null |
| `status` | DiagnosticStatus | Yes | Current workflow status | Valid enum |
| `failureCategory` | DiagnosticFailureCategory | No | Classified failure category | Non-null only after classification |
| `guidanceMessageKey` | String | No | i18n key for the user-facing summary | Non-empty when classified |
| `troubleshootingRecord` | SanitizedTroubleshootingRecord | No | Captured sanitized metadata | Non-null after capture |
| `exportedPackage` | ExportedDiagnosticPackage | No | Manual export payload generated for support | Non-null only after export |
| `createdAtEpochMs` | long | Yes | Session creation timestamp | `> 0` |

**State Rules**:
- Starts in `INITIALIZED`
- Must not move backward once classification is completed
- Cannot reach `EXPORTED` unless `troubleshootingRecord` exists

### DiagnosticStatus

```java
public enum DiagnosticStatus {
    INITIALIZED,
    VALIDATING_CONFIGURATION,
    BUILDING_REQUEST,
    CALLING_ENDPOINT,
    PARSING_RESPONSE,
    CLASSIFIED_FAILURE,
    COMPLETED_SUCCESS,
    EXPORT_READY,
    EXPORTED
}
```

**Primary State Transitions**:

```text
INITIALIZED
  -> VALIDATING_CONFIGURATION
  -> BUILDING_REQUEST
  -> CALLING_ENDPOINT
  -> PARSING_RESPONSE
  -> CLASSIFIED_FAILURE
  -> EXPORT_READY
  -> EXPORTED

CALLING_ENDPOINT
  -> COMPLETED_SUCCESS
  -> PARSING_RESPONSE

COMPLETED_SUCCESS
  -> EXPORT_READY (only if user chooses export for troubleshooting context)
```

---

### 2. MarkupServiceConfiguration (Value Object)

**Purpose**: Represents the effective DAMA configuration needed to validate and call an OpenAI-compatible chat completions endpoint.

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|------------------|
| `baseUrl` | String | Yes | Configured API base URL | Absolute HTTPS URL, trimmed |
| `chatCompletionsPath` | String | Yes | Effective request path | Non-empty, normalized path |
| `modelName` | String | Yes | Selected AI Markup model identifier | Non-empty, trimmed |
| `apiKeyPresent` | boolean | Yes | Indicates whether a key is configured | True/false |
| `apiKeyFingerprint` | String | No | Non-secret fingerprint for troubleshooting | Redacted/masked format only |
| `timeoutMs` | int | Yes | Effective request timeout | Positive integer |
| `endpointKind` | String | Yes | `openai_hosted` or `openai_compatible` | Must match supported set |
| `proxyExpected` | boolean | Yes | Whether system proxy discovery is expected to apply | True/false |

**Validation Rules**:
- `baseUrl` must not include embedded credentials
- `modelName` must not be blank
- `timeoutMs` must be greater than zero

---

### 3. DiagnosticFailureCategory (Value Object / Enum)

**Purpose**: Canonical classification of AI Markup failures presented to users and support.

```java
public enum DiagnosticFailureCategory {
    CREDENTIALS,
    MODEL_ACCESS,
    MALFORMED_REQUEST,
    ENDPOINT_COMPATIBILITY,
    CONNECTIVITY_OR_PROXY,
    RATE_LIMIT_OR_CAPACITY,
    UNKNOWN_SERVICE_FAILURE
}
```

**Classification Sources**:
- HTTP status code
- Structured error body fields
- Local configuration validation
- Connectivity exceptions / proxy exceptions
- Classification confidence heuristics

---

### 4. SanitizedTroubleshootingRecord (Value Object)

**Purpose**: Support-oriented record of the failed AI Markup attempt with secrets removed.

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|------------------|
| `requestId` | String | Yes | Correlation ID for logs/export/UI | Non-empty |
| `platform` | String | Yes | OS family for parity troubleshooting | `windows` or `macos` |
| `endpointSummary` | String | Yes | Sanitized endpoint identity | No secrets, no full key material |
| `requestSnapshot` | String | Yes | Sanitized request metadata summary | No raw API keys/tokens |
| `httpStatus` | Integer | No | Response status if available | Valid HTTP code if present |
| `serviceErrorBody` | String | No | Fuller sanitized service error body | Sanitized before persistence/export |
| `failureCategory` | DiagnosticFailureCategory | Yes | Classified failure family | Non-null |
| `guidanceMessageKey` | String | Yes | i18n key for DAMA summary | Non-empty |
| `capturedAtEpochMs` | long | Yes | Capture time | `> 0` |
| `redactionApplied` | boolean | Yes | Whether sensitive values were redacted | Must be true |

**Validation Rules**:
- `requestSnapshot` must omit secrets and selected-text content
- `serviceErrorBody` must be sanitized before storage/export
- `redactionApplied` must always be true for persisted/exported forms

---

### 5. ExportedDiagnosticPackage (Value Object)

**Purpose**: User-initiated portable representation of troubleshooting data for support sharing.

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|------------------|
| `schemaVersion` | String | Yes | Export schema version | Non-empty semantic version |
| `generatedAt` | String | Yes | ISO-8601 timestamp | Non-empty |
| `sessionId` | String | Yes | Correlation to diagnostic session | Non-empty |
| `record` | SanitizedTroubleshootingRecord | Yes | Sanitized payload | Non-null |
| `exportReason` | String | No | Optional user/support note | Trimmed if present |

**Validation Rules**:
- Must never contain raw credentials
- Must be derivable only from a sanitized troubleshooting record

---

## Domain Services

### 1. RequestValidationService

**Purpose**: Validates local AI Markup configuration and request-shape prerequisites before the HTTP call is attempted.

**Inputs**:
- `MarkupServiceConfiguration`
- selected text metadata

**Outputs**:
- validation result with pass/fail reasons

**Primary Rules**:
1. Required settings must be present
2. Endpoint base URL/path must be coherent
3. Request-shape expectations for OpenAI-compatible chat completions must be satisfied
4. Validation failure maps to a user-meaningful failure category before network I/O

### 2. DiagnosticClassifier

**Purpose**: Maps local validation failures, HTTP responses, and exceptions into `DiagnosticFailureCategory` plus user guidance keys.

**Inputs**:
- validation failure details
- HTTP status
- sanitized error body
- thrown exception type/message

**Outputs**:
- failure category
- confidence
- guidance message key

### 3. SecretRedactor

**Purpose**: Removes or masks secrets from headers, payload metadata, and service error bodies before logging, storage, or export.

**Redaction Rules**:
1. API keys and bearer tokens are fully masked
2. Embedded credentials in URLs are removed
3. Selected text content is excluded from troubleshooting records unless explicitly allowed by a later feature
4. Masking must be stable enough to support support-side correlation without revealing the secret itself

---

## Relationships

```text
AiMarkupDiagnosticSession
  ├── uses MarkupServiceConfiguration
  ├── produces SanitizedTroubleshootingRecord
  ├── may produce ExportedDiagnosticPackage
  └── is classified by DiagnosticFailureCategory
```

---

## Validation Matrix

| Requirement | Model Element(s) |
|-------------|------------------|
| FR-001 to FR-005 | `MarkupServiceConfiguration`, `RequestValidationService`, `DiagnosticFailureCategory`, `DiagnosticClassifier` |
| FR-006 to FR-009 | `SanitizedTroubleshootingRecord`, `ExportedDiagnosticPackage`, `SecretRedactor` |
| FR-010 to FR-012 | `AiMarkupDiagnosticSession`, `DiagnosticStatus` |
| FR-013 to FR-016 | `DiagnosticClassifier`, `SanitizedTroubleshootingRecord`, `ExportedDiagnosticPackage` |

---

## Notes for Planning

- Existing `processAIMarkup()` in `DAMAWorkspaceAccessPluginExtension` will need decomposition to populate this model cleanly.
- Export is manual and sanitized; no background persistence across sessions is assumed.
