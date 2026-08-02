# Research: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Feature**: 004-cbrd-parse-endpoint
**Date**: 2026-07-31
**Status**: Complete

## Summary

This feature replaces the client-side OpenAI-compatible chat completions call in the
DILA AI Markup Assistant plugin with a single POST to the DILA-hosted CBRD Parse endpoint
(`https://cbss.dila.edu.tw/cbrd/parse`). The plugin stops storing OpenAI credentials and
model names; it only stores the CBRD Parse endpoint URL, a shared bearer token, and a
timeout. The existing review-then-replace workflow, undo history, async executor, and
diagnostic-export machinery are preserved.

Research was performed by codebase exploration and by fetching the live CBRD Parse
OpenAPI document from `https://cbss.dila.edu.tw/cbrd/openapi.json`.

## R1: DILA CBRD Parse Endpoint Contract

**Decision**: Call `POST /cbrd/parse` with `application/json` body `{ "text": <string>, "lang": "zh" | "jp" }`, authenticated with `Authorization: Bearer <shared token>`. Success returns `application/xml` (a `<ref>...</ref>` TEI P5 document). Errors return JSON `ParseError` with one of nine enumerated `error` codes.

**Rationale**: The live OpenAPI document (version 1.1.0) defines this contract exactly. The nine error codes map 1:1 to the causes enumerated in FR-011. The `text` field constraint (`minLength: 1`, `maxLength: 4000`) matches the client-side pre-validation limit in FR-019. The `lang` field (`enum: [zh, jp], default: zh`) matches the document-language inference in FR-007.

**Alternatives considered**:
- Reuse the existing `CBRDAPIClient` (Ref-to-Link): rejected. That client performs a GET with a `Referer` header and query-parameter input; the Parse endpoint requires a POST JSON body and Bearer auth. A new sibling client is cleaner than overloading the existing one.
- Infer the endpoint shape from the spec prose only: rejected. The live OpenAPI document is authoritative and was fetched directly.

## R2: HTTP Client Implementation

**Decision**: Implement a new `CbrdParseApiClient` in `infrastructure/api/` using `java.net.HttpURLConnection` via the existing `HttpUrlConnectionFactory`. Use `org.json` (already a dependency) to build and parse JSON. No new HTTP library is introduced.

**Rationale**: The plugin baseline is Java 8 and the constitution mandates `HttpURLConnection` for Java 8 compatibility. Both existing clients (`OpenAiCompatibleChatClient`, `CBRDAPIClient`) already use `HttpUrlConnectionFactory` + `org.json`. Reusing this pattern keeps the dependency surface unchanged and matches the existing test seam (`CapturingConnectionFactory`).

**Alternatives considered**:
- `java.net.http.HttpClient` (Java 11): rejected. The plugin compiles to Java 8 bytecode; raising the baseline requires a constitution complexity justification that is unnecessary for a single POST.
- Add OkHttp/Apache HttpClient: rejected. No existing dependency; adds packaging and compatibility risk for no functional gain.

## R3: Preference Storage

**Decision**: Add three new preference keys:
- `cbrd.parse.api.url` (plain option, default `https://cbss.dila.edu.tw/cbrd/parse`)
- `cbrd.parse.token` (secure option via `setSecretOption`/`getSecretOption`, default empty)
- `cbrd.parse.timeout` (plain option, default `30000`)

Remove the six OpenAI-era preference keys (`dila.dama.ft.parse.model`, `dila.dama.ft.detect.model`, `dila.dama.api.key`, `dila.dama.api.base.url`, `dila.dama.api.chat.path`, `dila.dama.api.timeout`) and their UI rows. Keep the existing CBRD Link keys (`cbrd.api.url`, `cbrd.referer.header`, `cbrd.timeout`) untouched.

**Rationale**: FR-002/003/016 require the new fields; FR-004 requires removing the old ones. The shared token uses Oxygen's secure credential storage exactly as the existing OpenAI API key does today (`optionStorage.getSecretOption`/`setSecretOption`). The timeout default `30000` matches the timeout the AI Markup action used before this feature (`dila.dama.api.timeout`), satisfying FR-016. The 10 s Ref-to-Link default was considered and rejected: `/cbrd/parse` performs a server-side model transformation, so a 10 s ceiling would time out legitimate requests and regress the success path (SC-007).

**Alternatives considered**:
- Migrate old OpenAI key/base URL to the new token/endpoint: explicitly rejected by FR-005 and the clarification session.
- Store the shared token in plain options: rejected by FR-003 (secure storage, non-display except fingerprint).

## R4: Document Language Resolution

**Decision**: Read `xml:lang` only from the document root element. Use `XmlDomUtils.parseXml(selectedDocumentXml)` then `doc.getDocumentElement().getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang")`. Map values beginning with `zh` to `zh`, values beginning with `ja` or `jp` to `jp`, and default to `zh` when missing/unusable. Do not consult the selected element or any nested ancestor.

**Rationale**: FR-007 and the clarification session established document-root-only resolution. `XmlDomUtils` is already XXE-hardened and namespace-aware. The XML namespace for `xml:lang` is fixed (`http://www.w3.org/XML/1998/namespace`), so `getAttributeNS` is the correct API.

**Alternatives considered**:
- Nearest ancestor-or-self with `xml:lang` (standard XML inheritance): rejected by the clarification session; the team chose document-root-only for predictability.
- Add a user-facing language preference: rejected by the clarification session.

## R5: Concurrency and Single-Flight Behavior

**Decision**: Reuse the existing `aiMarkupInProgress` volatile flag and `tryStartAiMarkupOperation`/`finishAiMarkupOperation` synchronized guard. On a second invocation while an operation is in progress, ignore the second click, show `ai.markup.diagnostic.in.progress` (or a successor key), and display the in-flight selected XML so the editor knows which selection is being processed. Do not queue.

**Rationale**: FR-015 and the clarification session established ignore-and-show behavior. The infrastructure already implements single-flight; the only additive change is storing and displaying the in-flight selected text.

**Alternatives considered**:
- Queue one follow-up request: rejected by the clarification session.
- Allow parallel operations on different documents: rejected by FR-015 (second invocation ignored regardless).

## R6: Client-Side Input Validation

**Decision**: Before sending a parse request, validate the selection locally. If the selection is empty, show the input-missing guidance and do not send. If the selection exceeds 4,000 characters, show the too-long guidance and do not send. Use the same actionable messages the service would return.

**Rationale**: FR-019 and the clarification session established client-side pre-validation. The OpenAPI `text.maxLength` is 4,000. Pre-validation avoids unnecessary network calls and gives immediate feedback, mirroring the existing "do not send when token missing" guard.

**Alternatives considered**:
- Always send and map service-returned `text_is_required`/`text_is_too_long`: rejected by the clarification session.
- Pre-validate only empty: rejected; too-long is cheap to check locally and avoids a round trip.

## R7: Cancel-on-Close Behavior

**Decision**: When the document or Oxygen closes while an AI Markup request is in flight, cancel/interrupt the in-flight request and discard the result silently. No markup is written into a closed document. Store the in-flight `CompletableFuture` (or a cancel handle) and cancel it in `applicationClosing()` and when the editor page is no longer available.

**Rationale**: FR-020 and the clarification session established cancel-and-discard behavior. The existing `applicationClosing()` only shuts down the executor; it does not interrupt in-flight tasks. This is genuinely new behavior. A `Future.cancel(true)` plus ignoring the completion in the `thenAccept` (guarded by an "editor still open" check) is the safest approach.

**Alternatives considered**:
- Let the request finish and discard the result silently: rejected by the clarification session; wastes resources and delays executor shutdown.
- Cache the result for later viewing: rejected by the clarification session.

## R8: Diagnostics Preservation

**Decision**: Preserve the diagnostic-export pipeline (`SanitizedTroubleshootingRecord`, `SanitizedDiagnosticLogger`, `DiagnosticExportWriter`, `BuildDiagnosticExportQuery`, the Export Diagnostics button, `AiMarkupDiagnosticSession`). Retire the OpenAI-shaped `MarkupServiceConfiguration`, `OpenAiCompatibleChatClient`, `OpenAiErrorResponse`, and the OpenAI-oriented branches of `RequestValidationService`/`DiagnosticClassifier`. Introduce a new CBRD Parse configuration object and a new classifier that maps the nine `ParseError.error` codes to localized actionable messages, plus a generic fallback (FR-012) and a distinct connectivity message (FR-013).

**Retirement inventory** (verified by grep, 2026-08-01): 12 files reference the three retired
classes. Main: `RunAiMarkupDiagnosticsCommand`, `AiMarkupDiagnosticSession`,
`DiagnosticClassifier`, `RequestValidationService`, `DAMAWorkspaceAccessPluginExtension`.
Test: `AiMarkupDiagnosticSessionTest` (3 refs), `DiagnosticClassifierTest` (4),
`RequestValidationServiceTest` (6), `OpenAiCompatibleChatClientTest` (delete outright),
`DAMAWorkspaceAccessPluginExtensionTest` (3), `...AiMarkupDiagnosticsTest` (9),
`...AsyncDiagnosticsTest` (8), `...ExportDiagnosticsTest` (4).
`DiagnosticClassifierPlatformParityTest` is unaffected. Deletion is deferred to Phase 7 per
plan.md Retirement Sequencing so `mvn test` stays runnable throughout.

**Rationale**: The spec's Out of Scope section says only the AI Markup client-side OpenAI call path and its preference fields are removed; the 002 diagnostics features remain for the rest of the plugin. The diagnostic-session state machine and export writer are agnostic to the underlying transport and can be retargeted to the Parse endpoint.

**Alternatives considered**:
- Keep `MarkupServiceConfiguration` and overload it for CBRD Parse: rejected. The class is OpenAI-shaped (`chatCompletionsPath`, `modelName`, `apiKey`, `endpointKind`); overloading it would muddy the domain model and violate DDD terminology.
- Remove diagnostics entirely: explicitly rejected by the spec.

## R9: Internationalization

**Decision**: Add new i18n keys for:
- Preference labels: `cbrd.parse.api.url.label`, `cbrd.parse.token.label`, `cbrd.parse.timeout.ms.label`
- Failure guidance: one key per `ParseError.error` code (9 keys), plus a generic fallback key and a connectivity key
- Concurrency message showing in-flight selected XML (update or replace `ai.markup.diagnostic.in.progress`)
- Empty/too-long pre-validation messages (may reuse the `text_is_required`/`text_is_too_long` guidance keys)

- Malformed endpoint URL configuration error (`ai.markup.error.endpoint_url_invalid`, FR-021)
- Missing-token guidance (`ai.markup.error.token_not_configured`, FR-010), distinct from `unauthorized`

Each key must ship in `en_US`, `zh_CN`, and `zh_TW` in `src/main/resources/i18n/translation.xml`. The `TranslationBundleCompletenessTest` will fail the build if any key is missing.

**Key retirement**: five OpenAI-era keys become obsolete — `system.prompt.ai.markup`,
`error.no.APIKey`, `error.no.parse.model`, `http.error`, `llm.error`. They are additionally
hard-listed in the live `isErrorMessage` key array
(`DAMAWorkspaceAccessPluginExtension.java:1291-1316`) and asserted present by
`LocalizationTest` (lines 49, 53). Removing them requires updating both call sites in the same
task, otherwise `LocalizationTest` fails. The new `ai.markup.error.*` keys must be added to the
`isErrorMessage` array so failure messages remain recognized.

**Rationale**: NFR-001/FR-018 and the constitution (Principle IX) require comprehensive i18n in all shipped languages. The existing bundle has three languages and a completeness test.

**Alternatives considered**:
- Add languages: out of scope by spec.

## R10: Test Strategy

**Decision**: Follow the existing test patterns:
- `CbrdParseApiClientTest`: mirror `CBRDAPIClientTest` with a `CapturingConnectionFactory` and canned `FakeHttpURLConnection` responses for 200/400/401/422/502/503 and connectivity failure.
- Configuration and preference tests: mirror `DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest` but assert the new CBRD Parse keys and secure token storage.
- Replace-flow test: mirror `RefToLinkReplaceFlowTest` to assert the result string is inserted at the caret and undo history is preserved.
- Cancel-on-close test: new test using `isExecutorShutdownForTests()` and a cancel hook.
- i18n completeness tests already exist and will enforce new keys.

**Rationale**: The existing test infrastructure (JUnit 4, Mockito, AssertJ, `*ForTests()` seam) is designed for this. No new test framework is needed.

**Alternatives considered**:
- Integration tests against the live DILA endpoint: out of scope for unit tests; the contract is captured in `contracts/openapi.yaml` and the quickstart guide.

## R11: Client-Side System Prompt Retirement

**Decision**: Drop the `systemPrompt` argument from `RunAiMarkupDiagnosticsCommand.execute` and
stop reading `i18n("system.prompt.ai.markup")` in `runAiMarkup`
(`DAMAWorkspaceAccessPluginExtension.java:1391`). Retire the key.

**Rationale**: The transformation instruction now lives on the DILA server with the pretrained
model. `contracts/openapi.yaml` declares `ParseRequest` with `additionalProperties: false` and
only `text`/`lang`, so a client-supplied prompt cannot be sent even if retained. Keeping the
plumbing would leave a dead i18n string that editors could mistake for a tunable.

**Alternatives considered**:

- Keep the parameter and ignore it: rejected — a dead parameter on the primary path invites
  reintroduction and confuses the command's contract.
- Send it as an extra JSON field: rejected — the contract forbids additional properties.

## Open Questions

None. All `NEEDS CLARIFICATION` items are resolved by codebase exploration, the live OpenAPI document, and the spec clarification session.
