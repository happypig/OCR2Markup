# Data Model: CBRD Parse Endpoint Integration

**Feature**: 004-cbrd-parse-endpoint
**Date**: 2026-07-31
**Source**: [spec.md](spec.md), [research.md](research.md), [contracts/openapi.yaml](contracts/openapi.yaml)

## Entities

### CbrdParseConfiguration

Immutable value object holding the three preference-derived values needed to call the DILA CBRD Parse endpoint.

| Field | Type | Validation | Notes |
|-------|------|------------|-------|
| `endpointUrl` | `String` | non-null, `https://` or `http://` scheme, non-empty host | default `https://cbss.dila.edu.tw/cbrd/parse` |
| `timeoutMs` | `int` | `> 0` | default `30000` — pre-feature AI Markup timeout; NOT the 10 s Ref-to-Link lookup default (FR-016) |
| `sharedToken` | `String` | non-null; may be empty (handled by FR-010 guard) | stored via secure option; never displayed except fingerprint |

**Derived methods**:
- `getTokenFingerprint()` → `****XXXX` style (last 4 chars), consistent with existing `MarkupServiceConfiguration.getApiKeyFingerprint()`.
- `toRequestUri()` → the `endpointUrl` as a `URI` (validated).

**Relationships**: Supplied to `CbrdParseApiClient` and `RunAiMarkupDiagnosticsCommand`.

**Replaces**: `MarkupServiceConfiguration` (OpenAI-shaped, retired under FR-004).

### CbrdParseRequest

Immutable request body built from the selection and resolved language.

| Field | Type | Validation | Notes |
|-------|------|------------|-------|
| `text` | `String` | `minLength: 1`, `maxLength: 4000` | selected CBETA reference text; pre-validated locally per FR-019 |
| `lang` | `String` enum | `"zh"` or `"jp"` | resolved from document root `xml:lang` per FR-007; default `"zh"` |

**Serialization**: `org.json.JSONObject` → `{"text": "...", "lang": "zh|jp"}`.

**Closed shape**: `contracts/openapi.yaml` declares `additionalProperties: false`. No system
prompt, model name, temperature, or platform field may be added to the body. The existing
`i18n("system.prompt.ai.markup")` argument threaded through
`RunAiMarkupDiagnosticsCommand.execute` is dropped, and the key is retired (FR-004, FR-006).

### CbrdParseResponse

Result of a `CbrdParseApiClient.execute(...)` call.

| Variant | Fields | Notes |
|---------|--------|-------|
| Success | `markupXml: String`, `httpStatus: int`, `trace: RequestTraceSnapshot` | HTTP 200, `application/xml` body |
| Failure | `httpStatus: int`, `error: ParseError`, `errorMessage: String`, `trace: RequestTraceSnapshot`, `exception: Throwable?` | mapped from `ParseError` JSON or connectivity exception |

### ParseError

Enum of the nine service-enumerated error codes, plus local/generic/connectivity categories.

| Enum value | HTTP status | Guidance key (i18n) | Maps to FR |
|------------|-------------|----------------------|------------|
| `TEXT_IS_REQUIRED` | 400 | `ai.markup.error.text_is_required` | FR-011 / FR-019 (also client-side) |
| `TEXT_IS_TOO_LONG` | 400 | `ai.markup.error.text_is_too_long` | FR-011 / FR-019 (also client-side) |
| `UNSUPPORTED_LANGUAGE` | 400 | `ai.markup.error.unsupported_language` | FR-011 |
| `UNAUTHORIZED` | 401 | `ai.markup.error.unauthorized` | FR-010 / FR-011 |
| `PARSE_API_NOT_CONFIGURED` | 503 | `ai.markup.error.parse_api_not_configured` | FR-011 |
| `OPENAI_CREDENTIALS_UNAVAILABLE` | 503 | `ai.markup.error.openai_credentials_unavailable` | FR-011 |
| `OPENAI_RATE_LIMITED` | 503 | `ai.markup.error.openai_rate_limited` | FR-011 |
| `OPENAI_UNAVAILABLE` | 502 | `ai.markup.error.openai_unavailable` | FR-011 |
| `INVALID_MODEL_OUTPUT` | 422 | `ai.markup.error.invalid_model_output` | FR-011 |
| `UNEXPECTED_RESPONSE` | any other | `ai.markup.error.unexpected` | FR-012 |
| `CONNECTIVITY_FAILURE` | n/a | `ai.markup.error.connectivity` | FR-013 |

**Distinct from local guards**: `UNAUTHORIZED` (token rejected by the service) MUST NOT reuse
the FR-010 missing-token key (`ai.markup.error.token_not_configured`) — see FR-010. Likewise
`TEXT_IS_REQUIRED`/`TEXT_IS_TOO_LONG` are reachable from the service only when its limit is
tighter than the 4,000-character client pre-check (US4 scenario 2); the client-side guards in
FR-019 reuse the same two guidance keys deliberately. A malformed endpoint URL uses its own
key `ai.markup.error.endpoint_url_invalid` (FR-021), distinct from the connectivity key.

### DocumentLanguageResolver

Domain service that resolves the request language indicator from the document root element.

| Input | Output | Rule |
|-------|--------|------|
| Document root `xml:lang` (e.g. `zh`, `zh-Hant`, `ja-JP`, `jp`, missing) | `"zh"` or `"jp"` | begins with `zh` → `zh`; begins with `ja` or `jp` → `jp`; else default `zh` |

**Constraint**: Reads only the document root element; MUST NOT consult the selected element or nested ancestors (FR-007, clarification session).

### AiMarkupDiagnosticSession

Reused from feature 002. **State machine unchanged, but the configuration type is not**: the
session currently holds a `MarkupServiceConfiguration`. It is retargeted to
`CbrdParseConfiguration` (additive first, per plan.md Retirement Sequencing). The same applies
to `DiagnosticClassifier`, whose OpenAI branches are retired once the CBRD path is live.

```text
INITIALIZED → VALIDATING_CONFIGURATION → BUILDING_REQUEST → CALLING_ENDPOINT → PARSING_RESPONSE →
  { COMPLETED_SUCCESS | CLASSIFIED_FAILURE → EXPORT_READY → EXPORTED }
```

The session is retargeted to CBRD Parse but its lifecycle and export pipeline are preserved.

### SanitizedTroubleshootingRecord

Reused from feature 002. Holds redacted request/response details for the Export Diagnostics button. The existing `SecretRedactor.redactBearerTokens` pattern already covers Bearer tokens, so no new redaction logic is required.

## Preference Keys

| Key | Storage | Default | Notes |
|-----|---------|---------|-------|
| `cbrd.parse.api.url` | plain | `https://cbss.dila.edu.tw/cbrd/parse` | FR-002 |
| `cbrd.parse.token` | secure | (empty) | FR-003; `getSecretOption`/`setSecretOption` |
| `cbrd.parse.timeout` | plain | `30000` | FR-016; matches the retired `dila.dama.api.timeout`, not `cbrd.timeout` |
| `dila.dama.api.key` | secure | — | REMOVED (FR-004) |
| `dila.dama.api.base.url` | plain | — | REMOVED (FR-004) |
| `dila.dama.api.chat.path` | plain | — | REMOVED (FR-004) |
| `dila.dama.ft.parse.model` | plain | — | REMOVED (FR-004) |
| `dila.dama.ft.detect.model` | plain | — | REMOVED (FR-004) |
| `dila.dama.api.timeout` | plain | — | REMOVED (FR-004) |

**Migration**: No migration. Obsolete values are ignored, never imported (FR-005).

## State Transitions

### AI Markup Operation

```text
IDLE
  └─ invoke AI Markup
     ├─ endpoint URL missing/malformed? → SHOW_CONFIG_ERROR guidance (no request) → IDLE
     ├─ token missing? → SHOW_MISSING_TOKEN guidance (no request) → IDLE
     ├─ selection empty? → SHOW_INPUT_MISSING guidance (no request) → IDLE
     ├─ selection > 4000? → SHOW_TOO_LONG guidance (no request) → IDLE
     └─ valid? → IN_PROGRESS (single-flight guard)
          ├─ second invoke? → IGNORE + show "already in progress" + in-flight selected XML
          ├─ document/editor closes? → CANCEL_REQUEST + discard → IDLE
          ├─ success → SHOW_RESULT (review-then-replace) → {Replace → WRITE + undo → IDLE | Cancel → IDLE}
          └─ failure → SHOW_GUIDANCE (+ Export Diagnostics if record) → IDLE
```

## Validation Rules Summary

- Endpoint URL: non-empty, valid URI, scheme http/https. On failure the plugin shows
  `ai.markup.error.endpoint_url_invalid` and sends nothing (FR-021) — distinct from the
  connectivity key.
- Shared token: non-empty when AI Markup is invoked (FR-010 guard); otherwise missing-token guidance.
- Timeout: parse as integer; fallback to 30000 when unparseable.
- Selection: non-empty and `length <= 4000` before sending (FR-019).
- Language: derived from document root `xml:lang` only; default `zh`.
- Response: HTTP 200 → markup XML; enumerated error codes → mapped guidance; unknown status/body → generic; `IOException`/`SocketTimeoutException` → connectivity.
