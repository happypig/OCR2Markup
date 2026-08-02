# Data Model: Restore Ref-to-Link against CBRD v1.1.0

**Feature**: `005-cbrd-link-v11` | **Date**: 2026-08-03

This is a **delta document**. The full model for the Ref-to-Link flow is
`specs/001-ref-to-link-action/data-model.md` and remains authoritative for
everything not listed here. Restating it would create a second copy that drifts —
the failure mode this feature exists to fix.

## What does not change

| Element | Where it is defined | Why it is unaffected |
|---|---|---|
| `TripitakaComponents` (Value Object) | 001 data-model §1 | Parsed from the selection; never touched by transport |
| `TransformedComponents` (Value Object) | 001 data-model §2 | Input to the client; its shape is unchanged |
| `CBRDResponse` (DTO) | 001 data-model §3 | Field names `success` / `found` / `msg` / `error` are identical in v1.1.0 |
| `ReferenceConversionSession` (Aggregate) | 001 data-model §4 | State machine unchanged |
| `ReferenceParser`, `ComponentTransformer` | 001 data-model, Domain Services | Pure domain, no transport awareness |
| `ConvertReferenceCommand` | 001 data-model, Application Layer | Orchestration unchanged (see plan.md Complexity Tracking for its standing Principle VI deviation) |
| `RefElementRewriter` | Domain service | Replace behaviour unchanged |

The v1.1.0 response schema is *stricter* than v1.0.0 (`oneOf` between success and
failure, `const` booleans, `additionalProperties: false`, `found` present only on
success), but every strictening is compatible with the existing DTO: the client
reads the same field names, and each field it reads is still present in the case
where it reads it.

## What changes

### 1. Link Request — representation only

The citation's **transport representation** changes; its **content** does not.

| Aspect | Before (v1.0.0) | After (v1.1.0) |
|---|---|---|
| Carrier | URL query string | Request body |
| Encoding | `URLEncoder.encode(xml, "UTF-8")` — percent-encoded | Raw XML inside a JSON string value |
| Key | `q` (query parameter) | `q` (JSON object member) |
| Declared type | none (no body) | `application/json; charset=UTF-8` |

**Validation rules**:

- The citation MUST reach the service byte-identical to the selection. Quotes,
  CJK characters, and full-width punctuation are content, not delimiters.
- The payload MUST be produced by a JSON serializer, never by string
  concatenation (research D1).
- An empty or blank citation is rejected before transport, as today
  (`error.invalid.xml`).

### 2. Contract Version — a new first-class value

Previously the contract version existed only as a comment inside a YAML file that
nothing read. It becomes a checked value.

| Field | Value | Held by |
|---|---|---|
| Version the client is built against | `1.1.0` | A constant in main code |
| Version the repository has vendored | read at test time | `specs/001-ref-to-link-action/contracts/cbrd-api.yaml` → `info.version` |
| Version the live service publishes | fetched only when opted in | `https://cbss.dila.edu.tw/cbrd/openapi.json` → `info.version` |

**Validation rules**:

- The first two MUST be equal, always, offline (FR-009).
- The third is compared only when the live probe is enabled; a mismatch is a
  **reviewable signal**, not necessarily a defect.

### 3. Link Response outcomes — unchanged data, sharpened meaning

No field changes. What changes is which outcomes are *reachable*, and this is the
substance of User Story 2.

| Outcome | Wire form | i18n key | Reachable before | Reachable after |
|---|---|---|---|---|
| Resolved | `200` `{"success":true,"found":["https://…"]}` | `success.link.generated` | ❌ never (all requests 404'd) | ✅ |
| No match | `200` `{"success":true,"found":[]}` | `error.no.results` | ❌ never | ✅ |
| Service failure | `200` `{"success":false,"msg":…}` or `{…,"error":…}` | `error.api.failed` | ❌ never | ✅ |
| Transport / protocol failure | non-2xx, or host-rewritten exception | `error.api.http` | ✅ **every request** | ✅ only for genuine transport faults |

## i18n keys raised by `CBRDAPIClient`

All seven exist in `src/main/resources/i18n/translation.xml` in all three
languages. **No key is added, removed, or retranslated by this feature.**

| Key | Raised at | Effect of this feature |
|---|---|---|
| `error.api.http` | `CBRDAPIClient:124` | Semantics **narrow**: still "non-2xx", but no longer reachable for outcomes the service returns normally. This is the user-visible point of the fix. |
| `error.api.failed` | `CBRDAPIClient:139` | Becomes reachable for the first time in production |
| `error.no.results` | `CBRDAPIClient:144` | Becomes reachable for the first time in production |
| `error.api.connection` | `CBRDAPIClient:61` | Unchanged |
| `error.api.response` | `CBRDAPIClient:34` | Unchanged |
| `error.api.timeout` | `CBRDAPIClient:96` | Unchanged — retry-on-timeout-only policy preserved |
| `error.invalid.xml` | `CBRDAPIClient:43` | Unchanged — pre-transport validation |

> Note for `tasks.md`: three of these keys have never been exercised against the
> real service, because every request failed at routing before reaching the code
> paths that raise them. Their unit tests pass today only because the fakes return
> canned bodies. Live confirmation (FR-014) should exercise at least the resolved
> and the service-failure outcomes.

## State transitions

Unchanged from 001. The conversion session moves
`Idle → Validating → Calling → (Resolved | NoResults | Failed | Error)`; this
feature alters only which terminal states are reachable in practice, not the
machine itself.
