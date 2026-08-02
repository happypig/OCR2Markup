# Implementation Plan: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Branch**: `004-cbrd-parse-endpoint` | **Date**: 2026-07-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-cbrd-parse-endpoint/spec.md`

## Summary

The plugin's AI Markup action currently calls an OpenAI-compatible chat completions endpoint
directly from each editor's machine using client-stored credentials and fine-tuned model names.
This plan replaces that call with a single POST to the DILA-hosted CBRD Parse endpoint
(`https://cbss.dila.edu.tw/cbrd/parse`), removes the six obsolete OpenAI preference fields
and the client-side system prompt, and adds three new preference fields: CBRD Parse endpoint
URL, shared bearer token (secure
storage), and request timeout. The existing review-then-replace workflow, undo history,
async executor lifecycle, and diagnostic-export machinery are preserved. New behavior is
added for client-side input pre-validation (FR-019), ignore-second-invocation concurrency
with in-flight selected XML feedback (FR-015), and cancel-on-close (FR-020).

## Technical Context

**Language/Version**: Java 8 (source/target/release = 8) running inside Oxygen XML Editor / SDK 27.1.0.3+ on Java 17

**Primary Dependencies**: Oxygen XML SDK 27.1.0.3 (provided), `org.json:json:20240303` (runtime), JUnit 4.13.2, Mockito 4.11.0, AssertJ 3.24.2, Hamcrest 2.2 (test)

**Storage**: Oxygen `WSOptionsStorage` plugin preferences; secure credential storage via `getSecretOption`/`setSecretOption`. No database/filesystem.

**Testing**: `mvn test` (JUnit 4, Mockito, AssertJ). Surefire runs `*Test`/`*Tests`, excludes `*IntegrationTest`. Headless AWT, parallel methods.

**Target Platform**: Oxygen XML Editor / SDK 27.1.0.3+ desktop plugin (Java Swing)

**Project Type**: Oxygen XML Editor plugin (desktop-app)

**Performance Goals**: Processing feedback in the result area within 500 ms; success/failure no later than timeout + 500 ms; editor responsive throughout (NFR-002). Timeout default 30,000 ms — the pre-feature AI Markup timeout. The 10,000 ms Ref-to-Link lookup default is deliberately NOT reused: `/cbrd/parse` performs a server-side model transformation, so a 10 s ceiling would regress the success path (FR-016).

**Constraints**:
- Java 8 bytecode baseline; no Java 11+ APIs (constitution Technology Standards)
- `HttpURLConnection` only (no `HttpClient`)
- Do not block the EDT; all UI updates via `SwingUtilities.invokeLater`
- Shared executor (`Executors.newFixedThreadPool(2)`) reused; shutdown in `applicationClosing()`
- Token never displayed/logged except as non-reversible fingerprint
- New i18n keys must ship in `en_US`, `zh_CN`, `zh_TW`
- Existing UTF-8 validation, tag removal, Ref-to-Link actions unchanged
- 002 diagnostics features preserved (only AI Markup OpenAI call path + preferences removed)

**Scale/Scope**: Single-user desktop editor plugin; one parse request per AI Markup invocation; selections up to 4,000 characters.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Event Storming First | Pass | `spec.md` Event Storming section defines actors, commands, domain events, policies, external boundaries. |
| II. Problem Frames | Pass | `spec.md` Problem Frames section separates problem domain, solution domain, shared phenomena. |
| III. Clean Architecture | Pass | New `CbrdParseApiClient` lives in `infrastructure/api`; new domain configuration object and error classifier live in `domain/`; command orchestration stays in `application/`; UI stays in `workspace/`. Dependencies point inward. |
| IV. TDD (BDD + TDD) | Pass | All 22 functional requirements carry Given-When-Then acceptance criteria (FR-003 → US2/4, FR-012 → US4/8, FR-016 → US1/7 + US2/5, FR-017 → US2/6, FR-018 → US4/10, FR-021 → US4/11, FR-022 → US4/9). `tasks.md` carries explicit 🚨 TEST GATE / 🎯 COMPLETION GATE tasks and `[T-SPEC]`/`[T-UNIT]`/`[T-INTEGRATION]` classification, with GREEN order Domain → Application → Infrastructure. Test seams: `*ForTests`, `CapturingConnectionFactory`. |
| V. DDD | Pass | New domain terms: `CbrdParseConfiguration`, `ParseError`, `DocumentLanguageResolver`; reuses `ReferenceConversionSession`, `SanitizedTroubleshootingRecord`. |
| VI. CQRS | **Deviation (tracked)** | `RunAiMarkupDiagnosticsCommand.execute` returns markup XML — domain data — which Principle VI forbids for Commands. The class also mutates nothing: the only state change is `replaceSelectionWithText` in the Replace action. Inherited from `002-ai-api-diagnostics`; this feature makes it the primary path. See Complexity Tracking. |
| VII. Defensive Programming | Pass | Null-check Oxygen SDK returns; validate token, endpoint URL, selection, timeout; disconnect HTTP connection in `finally`; map all 9 service error codes + generic + connectivity. |
| VIII. Async-First | Pass | Reuses shared executor, `CompletableFuture.supplyAsync`, `SwingUtilities.invokeLater`; adds cancel-on-close (FR-020). |
| IX. Comprehensive i18n | Pass | New keys added to `translation.xml` in all three languages; `TranslationBundleCompletenessTest` enforces. |
| X. Continuous Verification | Pass | `mvn test` after every code change; tests written first. |

**Gate result**: PASS with one tracked deviation (Principle VI) — see Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/004-cbrd-parse-endpoint/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── contracts/
│   └── openapi.yaml     # CBRD Parse endpoint contract
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (Maven project root: `Models/Gemini2.5/dila-ai-markup-plugin`)

```text
src/main/java/com/dila/dama/plugin/
├── workspace/
│   └── DAMAWorkspaceAccessPluginExtension.java   # AI Markup action, concurrency, async, replace, close hook
├── preferences/
│   └── DAMAOptionPagePluginExtension.java        # Remove OpenAI rows; add CBRD Parse rows
├── application/
│   └── command/
│       └── RunAiMarkupDiagnosticsCommand.java    # Retarget to CBRD Parse client/config
├── domain/
│   ├── model/
│   │   ├── CbrdParseConfiguration.java           # NEW: endpoint URL, token fingerprint, timeout
│   │   ├── ParseError.java                       # NEW: enum of 9 service error codes
│   │   └── AiMarkupDiagnosticSession.java        # Reused state machine
│   └── service/
│       ├── CbrdParseErrorClassifier.java          # NEW: maps ParseError -> guidance key
│       ├── DocumentLanguageResolver.java          # NEW: xml:lang on document root -> zh|jp
│       ├── RequestValidationService.java          # Add CbrdParseConfiguration overload
│       └── DiagnosticClassifier.java              # Retarget: retire OpenAI branches
├── infrastructure/
│   └── api/
│       ├── CbrdParseApiClient.java               # NEW: POST JSON, Bearer auth, HttpURLConnection
│       ├── CbrdParseRequest.java                 # NEW: {text, lang} — no other fields
│       ├── CbrdParseResponse.java                # NEW: success/failure result type
│       └── HttpUrlConnectionFactory.java         # Reused
└── (removed)
    ├── domain/model/MarkupServiceConfiguration.java   # OpenAI-shaped; retire
    ├── infrastructure/api/OpenAiCompatibleChatClient.java  # OpenAI client; retire
    └── infrastructure/api/OpenAiErrorResponse.java        # OpenAI error parsing; retire

src/main/resources/
├── plugin.xml
└── i18n/translation.xml                          # Add new keys (en_US, zh_CN, zh_TW)

src/test/java/com/dila/dama/plugin/
└── (mirror structure for new client/config/classifier tests)
```

**Structure Decision**: The plan keeps the existing clean-architecture layout. New CBRD Parse
classes are siblings of the OpenAI-era classes they replace. No new top-level packages are
introduced. The OpenAI-shaped `MarkupServiceConfiguration` and `OpenAiCompatibleChatClient`
are retired rather than overloaded, keeping the domain model honest.

### Retirement Sequencing (build-safety constraint)

Twelve files reference the three OpenAI-era classes being retired. Deleting them in Phase 2
would leave the module uncompilable until Phase 3, so `mvn test` could not run — violating
Principle X. The retirement is therefore two-step:

1. **Additive phase (Phase 2)**: introduce the CBRD Parse types; add a
   `validate(CbrdParseConfiguration)` overload to `RequestValidationService` and a CBRD Parse
   path to `AiMarkupDiagnosticSession`/`DiagnosticClassifier` *alongside* the OpenAI ones.
   Only genuinely unreferenced dead code (`processAIMarkup`, `createJSONRequest`,
   `parseOpenAIResponse` — verified: no callers) is removed here.
2. **Deletion phase (Phase 7)**: once every call site is retargeted, delete
   `MarkupServiceConfiguration`, `OpenAiCompatibleChatClient`, `OpenAiErrorResponse`, the
   OpenAI overloads, `OpenAiCompatibleChatClientTest`, and the five obsolete i18n keys —
   updating the seven remaining test classes and `LocalizationTest` in the same step.

`mvn test` must be green at the end of every task in both phases.

### Post-Phase-7 Remediation: Java-17 `getErrorStream()` Quirk (S10 regression)

**Trigger**: 2026-08-02 validation of `quickstart.md` Scenario 10 against the live
`https://cbss.dila.edu.tw/cbrd/parse` endpoint with a deliberately wrong bearer token
produced the FR-013 connectivity message ("Cannot reach the DILA parse service…")
instead of the FR-011 `unauthorized` guidance the contract requires. Root cause
captured in the diagnostics export
(`failureCategory: CONNECTIVITY_OR_PROXY`, `serviceErrorBody: ""`) and reproduced
with a standalone `HttpURLConnection` probe on JDK 25:

- `connection.getResponseCode()` returns 401 cleanly.
- `connection.getErrorStream()` is reachable on JDK 25, returning the body
  `{"success":false,"error":"unauthorized"}`.
- Under Oxygen's bundled Java 17, the same call path throws
  `IOException("Server returned HTTP response code: 401 for URL: https://cbss.dila.edu.tw/cbrd/parse")`
  from the equivalent of `getInputStream()` on a 4xx response, surfacing *as* a
  transport-level exception that today's `CbrdParseApiClient.execute` catch block
  stamps as `ParseError.CONNECTIVITY_FAILURE`.

**Spec encoding**: `spec.md` Clarifications Session 2026-08-02, FR-011 tightened,
US4 scenario 12 added, and a new Edge Cases entry pinning the JDK-quirk path to
FR-011 (not FR-013).

**Implementation approach** (chosen over alternatives, with rationale):

1. **Recover the HTTP status by parsing it out of the `IOException` message text**
   when `getResponseCode()` already returned a code but `getErrorStream()` threw.
   Status is parsed by a small regex `^Server returned HTTP response code: (\d{3})
   for URL:` on `e.getMessage()`. **Chosen** because: (a) minimal change to a 143-
   line class; (b) the regex anchor `^Server returned HTTP` is a stable,
   long-standing `sun.net.www.protocol.http.HttpURLConnection` message format
   present across JDK 8 through 25; (c) keeps the API client stream-shaped (no
   new dependencies); (d) wraps cleanly in a single test seam.
2. *Rejected — retry `getResponseCode()` once after the exception*: unlikely to
   differ from the first call and burns the connection's already-drained
   response. The exception message is the cheaper, more reliable signal.
3. *Rejected — switch HTTP client to `java.net.http.HttpClient` (Java 11+)*:
   Oxygen's plugin bytecode baseline is Java 8 (`AGENTS.md`, `quickstart.md:12`),
   so `HttpClient` is unavailable.
4. *Rejected — add an AWS-SDK / Secrets-Manager client to detect 401 ahead of
   transport*: the token is *not* an AWS-issue token here; the endpoint is merely
   AWS-fronted, and the published `https://cbss.dila.edu.tw/cbrd/openapi` contract
   treats 401 as a plain HTTP response. No AWS dependency belongs in the plugin.

**Classification rule** (codifies FR-011's tightened text):

| Outcome of `execute(...)` | Stamp |
|---------------------------|-------|
| `getResponseCode()` returned a code `N`, body parseable, `error` field enumerated | `ParseError.fromWireCode(error)` |
| `getResponseCode()` returned `401`, body unavailable (Java-17 quirk) | `ParseError.UNAUTHORIZED` |
| `getResponseCode()` returned `N` (non-2xx), body unavailable, `N` enumerated by status-to-error map (e.g. 503→`OPENAI_RATE_LIMITED` when the body parse failed) | status-derived `ParseError` |
| `getResponseCode()` returned `N`, body unavailable, `N` not enumerated | `ParseError.UNEXPECTED_RESPONSE` (FR-012) |
| No HTTP status recoverable (`UnknownHostException`, `SocketTimeoutException` before any byte, `SSLHandshakeException`, `SocketException: Connection closed`) | `ParseError.CONNECTIVITY_FAILURE` (FR-013) |

**Test strategy**: BDD/TDD via an injection seam at
`CbrdParseApiClient(HttpUrlConnectionFactory)`. Add a unit test to
`CbrdParseApiClientTest` that injects a `CapturingConnectionFactory` whose
`HttpURLConnection` mock (a) returns 401 from `getResponseCode()`, and (b)
throws `IOException("Server returned HTTP response code: 401 for URL:
https://cbss.dila.edu.tw/cbrd/parse")` from `getErrorStream()`. Assert the
returned `CbrdParseResponse` carries `status=401`, `error=ParseError.UNAUTHORIZED`,
and the message string recovered from the exception so `redactor.redact(...)`
still has something to scrub. The test runs in `mvn test` against mocked
transport only — no live network dependency in CI.

**Out of scope for this remediation**: switching HTTP client; adding AWS SDK
imports; changing `HttpUrlConnectionFactory`'s proxy handling (already verified
DIRECT under both WinHTTP and Windows Internet Settings on the failing machine);
revising the 9 existing `ParseError` enum codes.

**Gate**: every task in this remediation runs under `mvn test` green at the end
of each step, matching Principle X. The baseline before remediation is
**354 tests, 0 failures**, captured 2026-08-02.

### Phase 9: Preference Page Label Renovation

**Trigger**: 2026-08-02 review after the Phase 8 remediation. The three
Ref-to-Link rows were left labelled "CBRD API URL"/"CBRD timeout (ms)" after
this feature added a second CBRD surface (CBRD Parse). "CBRD API" reads as if
it covered the whole plugin, so editors cannot tell which rows configure the
Ref-to-Link lookup vs. the AI Markup endpoint. Scope decision (feature owner,
2026-08-02): rename **exactly two displayed labels**, nothing else.

**Spec encoding**: `spec.md` Clarifications Session 2026-08-02 (Phase 9),
US5 (scenarios 1-4), FR-023, and a new Edge Cases entry.

**Renames** (display text only):

| i18n key | Old (en_US) | New (en_US) | New (zh_CN/zh_TW) |
|----------|-------------|-------------|-------------------|
| `cbrd.api.url.label` | `CBRD API Endpoint:` (fallback `CBRD API URL:`) | `CBRD Link Endpoint:` | `CBRD 連結端點：` |
| `cbrd.timeout.ms.label` | `CBRD timeout (ms):` | `CBRD Link timeout (ms):` | `CBRD 連結逾時 (毫秒)：` |

**Implementation approach** (chosen over alternatives, with rationale):

1. **Change the two i18n values in `translation.xml` (all three languages)
   and the two fallback strings in `DAMAOptionPagePluginExtension.getMessage`
   switch.** Chosen because: (a) the option page reads both — the live
   `PluginResourceBundle` in Oxygen and the test-path fallback when the
   resource bundle is absent — so both must agree; (b) no key renames, so
   `translation.xml` key→locale completeness (NFR-001) and
   `TranslationBundleCompletenessTest` are untouched; (c) no storage-key or
   default change, so `PreferencePageLayoutOrderTest` keeps asserting the same
   keys and defaults, with the six-row order and the two expected label strings
   updated to the documented layout (Session 2026-08-02 Phase 9 — row order).
2. **Reorder the six rows in `init()`** so the Referer header comes first,
   then the three CBRD Parse rows (endpoint URL, token, timeout), then the CBRD
   Link endpoint and timeout rows. Chosen per feature owner 2026-08-02: the
   shared Referer header is action-independent and the CBRD Parse surface is
   the feature's own, so both lead before the two remaining Link rows. Pure
   layout change — storage keys, defaults, and both actions' behavior are
   untouched (FR-017, NFR-005).
3. *Rejected — rename the storage keys* (`cbrd.api.url` etc.): changes the
   persistence contract, orphans existing stored values, and violates the
   FR-017 "Ref-to-Link unchanged" guarantee for no user-visible benefit.
4. *Rejected — rename the internal log/exception strings* ("Failed to set CBRD
   API URL…"): diagnostic messages, not displayed labels; out of scope
   (spec.md Clarifications 2026-08-02 Phase 9).

**Test strategy**: BDD/TDD. Update `PreferencePageLayoutOrderTest` expectations
first (RED — the current fallback still returns the old strings), then apply
the renames (GREEN). Because the test path uses the fallback switch (no
resource bundle in a mocked `PluginWorkspace`), the RED→GREEN cycle is
exercised entirely by the fallback strings; the `translation.xml` values are
covered by the same label assertions under the real Oxygen resource bundle
(enforced by US5 scenario 3 + FR-023 wording) and are set in the same commit.

**Out of scope for this renovation**: renaming storage keys; adding/removing
rows; changing defaults or the Referer row; editing diagnostic log strings;
bumping the version (stays `0.5.0`). Row order IS in scope: the 2026-08-02
row-order clarification supersedes the earlier "no reordering" decision and
amends the US5/FR-023 order contract (spec.md Session 2026-08-02 Phase 9 — row
order).

**Gate**: `mvn test` green at the end, with `PreferencePageLayoutOrderTest`
RED→GREEN first. Version stays `0.5.0`; `VersionConsistencyTest` stays green
(release-notes 0.5.0 entry is edited in place, not bumped).

### Phase 9 follow-up 2: Transport-error capture in the diagnostics export

**Trigger**: 2026-08-02 — even after the Phase 8/9 401-classification fixes were
shipped and verified on disk, a live Oxygen instance still surfaced
"cannot reach the DILA parse service" (`CONNECTIVITY_OR_PROXY`). A direct probe
from the same machine (curl, plus a harness on Oxygen's bundled Temurin 21.0.9
and on JDK 25, with and without `-Djavax.net.ssl.trustStoreType=Windows-ROOT`)
returns a clean HTTP 401 with a readable `unauthorized` body, and no proxy is
configured anywhere. The exported diagnostic could not resolve the discrepancy
because it carried only the category — the underlying exception message was
dropped before `SanitizedTroubleshootingRecord` was built.

**Decision** (2026-08-02): capture the transport cause in the export. The
record gains a sanitized, redacted `transportError` field — exception simple
class name plus message, walking the cause chain, capped at 1000 chars —
populated from `CbrdParseResponse.getException()` for genuine transport-level
failures only (no recoverable HTTP status). A known HTTP status failure leaves
it empty. The export schema is bumped `1.0.0` → `1.1.0`. Secrets are passed
through the existing `SecretRedactor` like every other exported field.

**Implementation approach** (chosen over alternatives):

1. **Thread the exception through the command into the record** —
   `CbrdParseResponse` already carried it; `RunAiMarkupDiagnosticsCommand`
   dropped it. Chosen because it is the smallest change that reaches the
   existing export path and keeps the redaction in one place.
2. *Rejected — write a separate on-disk log file in the plugin*: adds
   user-visible file litter and a second redaction surface; the export is the
   designed support-sharing channel (FR-022).
3. *Rejected — surface the exception text in the result-area message*: the
   connectivity guidance is intentionally user-friendly (FR-013); raw exception
   text belongs in the support export, not the editor message.

**Test strategy**: unit tests for the record round-trip, the writer, and the
command's record building (transport error present on a thrown `IOException`,
absent for a known 401, token redacted when it leaks into an exception message).
Full suite green at 362 tests.

**Out of scope**: changing the guidance message text, changing the client's
classification logic, bumping the plugin version (stays `0.5.0`).

**Gate**: `mvn test` fully green at 362 tests; `VersionConsistencyTest` green.

### Phase 9 follow-up 3: Oxygen HTTP protocol handler status recovery

**Trigger**: 2026-08-02 — the S11 diagnostics export (schema `1.1.0`, the
follow-up 2 `transportError` field) finally named the real cause of the
persistent "cannot reach the DILA parse service" message:
`HttpExceptionWithDetails: 401 Unauthorized for: https://cbss.dila.edu.tw/cbrd/parse`.
`ro.sync.net.protocol.http.HttpExceptionWithDetails` is thrown by Oxygen's own
URL stream handler, which Oxygen installs as the default handler, so inside
Oxygen a non-2xx response throws `"NNN Reason for: <url>"` instead of the JDK's
`IOException("Server returned HTTP response code: NNN for URL: ...")`. The
client's `recoverStatusFromException` only matched the JDK message, so the 401
was never recovered and the failure was mis-routed to FR-013 connectivity —
even though the request reached the server and returned a clean 401.

**Decision** (2026-08-02): recover the HTTP status from Oxygen's message format
too. Add a second status pattern (`(\d{3})\s+[A-Za-z][A-Za-z ]*\s+for:`) to
`recoverStatusFromException`, refactoring the per-message match into a small
helper. A 401 recovered from `HttpExceptionWithDetails` classifies exactly like
the JDK-message case (FR-011 → `UNAUTHORIZED`); only exceptions whose whole
cause chain carries no status remain FR-013.

**Implementation approach** (chosen over alternatives):

1. **Parse the status out of the message text** (second pattern), SDK-agnostic,
   mirroring how the JDK message is already parsed. Chosen because it needs no
   reference to Oxygen SDK classes in the client, survives Oxygen changing the
   class name or message variant, and keeps `CbrdParseApiClient` unit-testable
   through the existing `CapturingConnectionFactory` seam (an `IOException` with
   the same message text is a faithful stand-in, and a real
   `HttpExceptionWithDetails` is available at test compile time via the
   `provided` `oxygen-sdk` dependency).
2. *Rejected — branch on `instanceof HttpExceptionWithDetails` / `getStatusCode()`*:
   couples the recovery logic to a specific Oxygen SDK class, adds a compile-time
   dependency into domain/infrastructure code that is deliberately SDK-free, and
   breaks if Oxygen renames the class. Message parsing is more robust.

**BDD/TDD**: RED first — write US4 scenario 13 as failing unit tests
(`unauthorizedWhenOxygenHttpProtocolThrowsFromGetResponseCode`,
`multiWordReasonInOxygenHttpExceptionStillRecoversTheStatus`) against the
current client, confirm they FAIL RED, then add the pattern (GREEN). The cause
string on the recovered response is generalized to
`"No readable response body; status NNN recovered from exception message"` so it
is accurate for both the JDK and Oxygen messages.

**Out of scope**: changing guidance text, changing FR-013's classification rule,
bumping the plugin version (stays `0.5.0`).

**Gate**: `CbrdParseApiClientTest` green at 24 tests; full `mvn test` green at
364 tests; `VersionConsistencyTest` green.

### Phase 9 follow-up 4: Token label renamed to "CBRD bearer token*:" and row order swapped

**Trigger**: 2026-08-02 — the second CBRD Parse row was labelled "CBRD Parse
token*:" and sat third on the page. The token is actually sent as an HTTP
`Authorization: Bearer` header, so the label did not say what it was, and the
token row sat below the endpoint URL row even though the token is the primary
thing an editor configures. The editor asked to (a) rename the label to surface
the bearer scheme and (b) move the token row above the endpoint URL row.

**Decision** (2026-08-02): rename `cbrd.parse.token.label` to "CBRD bearer
token*:" and keep ALL six preference labels identical to their English text in
every shipped language — the zh_CN/zh_TW value of each label equals its en_US
value exactly ("Bearer", "Token", "URL", "Endpoint", "Referer", and the "CBRD
Parse"/"CBRD Link" prefixes are untranslated protocol/UI terms on this page).
Swap the token row to second, giving the order: "CBRD Referer header:",
"CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):",
"CBRD Link Endpoint:", "CBRD Link timeout (ms):". Purely display-only: storage
keys (`cbrd.parse.api.url`, `cbrd.parse.token`, `cbrd.parse.timeout`), defaults,
and behavior are untouched (FR-017, NFR-005); version stays `0.5.0`.

**Implementation approach** (chosen over alternatives):

1. **Change the label values + swap the two panel row blocks in place** — update
   the six `cbrd.*.label` keys in `i18n/translation.xml` (all three languages
   identical to English) and the `cbrd.parse.token.label` fallback string in
   `DAMAOptionPagePluginExtension`, and move the token row block (label +
   `JPasswordField`) above the endpoint URL row block in `buildPage`. Chosen
   because the row layout is driven by the order the components are added to the
   `GridBagLayout`; no key, default, or persistence change is involved.
2. *Rejected — translate the labels to zh_CN/zh_TW*: would break the editor's
   explicit requirement that the translations stay exactly identical to English;
   "bearer" has no natural everyday Chinese equivalent (RFC 6750 term).
3. *Rejected — reorder by adding a sort key / data-driven order*: over-engineering
   for six statically built rows; the block-swap is the smallest display-only
   change and keeps the `UpgradePreferencesTest`/`PreferencePageLayoutOrderTest`
   field-index contract obvious.

**BDD/TDD**: RED first — update the expected label/order in
`PreferencePageLayoutOrderTest.rowsFollowTheDocumentedOrder` ("CBRD bearer
token*:" second, "CBRD Parse endpoint URL:" third) and the field-index
assertions in `UpgradePreferencesTest.endpointUrlIsPrefilledAndTheTokenIsEmpty`
(token is now field index 1, endpoint URL index 2), plus a new test asserting
the zh_CN/zh_TW value of every one of the six `cbrd.*.label` keys equals its
en_US value. Confirm these FAIL RED against the current client, then apply the
translation + fallback + row-swap changes (GREEN). The pre-existing
`TranslationXmlValidatorTest` Chinese-character guard and
`SharedTokenNonDisclosureTest` field-index assumptions are updated to match the
deliberate untranslated labels and the new token row position.

**Out of scope**: changing the storage keys, defaults, guidance text, or any
request behavior; bumping the plugin version (stays `0.5.0`).

**Gate**: `PreferencePageLayoutOrderTest`, `UpgradePreferencesTest`, and the
token-translation test green; full `mvn test` green; `VersionConsistencyTest`
green.

## Complexity Tracking

| Violation | Why needed | Simpler alternative rejected because | Approval |
|-----------|------------|--------------------------------------|----------|
| Principle VI (CQRS): `RunAiMarkupDiagnosticsCommand.execute` returns domain data (the marked-up XML via `Result.getMarkupResult()`) | The AI Markup workflow is review-then-replace: the transformation result must reach the UI for review *before* any state change. The only state mutation happens later, in the Replace action (`replaceSelectionWithText`). Renaming to a Query in this feature would ripple through 29 references across 6 files, 4 of which are `002`'s already-green workspace test classes. | Splitting into `ParseSelectedReferenceQuery` (returns markup) + `ReplaceSelectionCommand` (mutates, returns status) is the constitutionally clean shape and is the right target. It is deferred because it re-opens `002`'s tested surface inside a feature whose stated contract (spec.md Out of Scope) is to preserve that surface unchanged. Note also that `/cbrd/parse` is a POST that consumes server-side model budget and is not cacheable, so it does not cleanly satisfy Principle VI's Query definition (*"read-only, no side effects, cacheable"*) either — the constitution lacks a category for a non-cacheable remote read. | **Approved 2026-08-01** — jeffwu@dila.edu.tw (feature owner), recorded during `/speckit.analyze` remediation. Deviation is time-boxed: see Follow-up. |

**Follow-up (required)**: file a refactor item to split the command into a query + command pair
once this feature ships, and raise the missing "non-cacheable remote read" category with
`/speckit.constitution` so Principle VI can classify this operation without a deviation.
