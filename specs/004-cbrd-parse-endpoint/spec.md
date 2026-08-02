# Feature Specification: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Feature Branch**: `004-cbrd-parse-endpoint`
**Created**: 2026-07-21
**Status**: Planned (plan.md, research.md, data-model.md, contracts/, quickstart.md, tasks.md complete)
**Input**: User description: "The part that turns selected CBETA reference XML into TEI P5 markup from AI Markup shall be replaced by the new endpoint `https://cbss.dila.edu.tw/cbrd/parse`, since DILA takes AWS Secret Manager and then provides the transformation from the server instead of client with pretrained OpenAI model. It will also cover the option which stores necessary infos."

The DILA AI Markup Assistant plugin today ships an "AI Markup" action that performs the transformation of a selected passage of CBETA reference text into TEI P5 marked-up XML by calling an OpenAI-compatible chat completions endpoint directly from each editor's machine, using a long-lived API key, base URL, chat-completions path, and two fine-tuned model names stored in plugin preferences. Going forward the organization will hold the model credentials in a central secret store and host the transformation as a DILA service: the CBRD Parse endpoint, documented at `https://cbss.dila.edu.tw/cbrd/openapi#tag/parse/POST/parse`. This feature replaces the client-side LLM call with a single call to that server-hosted endpoint, removes the now-obsolete client credentials from the plugin preferences, and stores the small set of configuration values the plugin still needs to reach the DILA service.

## Clarifications

### Session 2026-07-21

- Q: How should the existing client-side OpenAI path (API key, base URL, chat completions path, parse/detect model) be treated once `/cbrd/parse` replaces the AI Markup transformation? → A: Remove the manual key path entirely. The plugin only calls `https://cbss.dila.edu.tw/cbrd/parse`; the OpenAI API key, base URL, chat completions path, and fine-tuned model name fields are removed from preferences.
- Q: Where should the Bearer token for `/cbrd/parse` live in plugin preferences? → A: Add a shared token stored via Oxygen's secure option, plus the CBRD Parse endpoint URL (default `https://cbss.dila.edu.tw/cbrd/parse`) and a CBRD Parse request timeout.
- Q: Should the `lang` field (zh/jp) on the parse request be user-configurable or fixed? → A: Infer from the document. The plugin reads `xml:lang` from the document root element of the selection's document and sends `zh` or `jp` accordingly. No new user-facing language preference is added; when no usable `xml:lang` is present, `zh` is used as the default.
- Q: When AI Markup is invoked while another AI Markup parse operation is still in progress, what should the plugin do? → A: Ignore the second invocation, show a clear “AI Markup already in progress” message, and display the in-flight selected XML so the editor knows which selection is being processed.
- Q: Which element supplies `xml:lang` for the parse request language indicator? → A: Read `xml:lang` only from the document root element; do not use the selected element or any nested ancestor, and default to `zh` when the document root value is missing or unusable.
- Q: Should the plugin validate the selection locally before calling the DILA service, or always send and map service-returned input errors? → A: Pre-validate empty and too-long selections client-side and show the matching message without sending a request.
- Q: What should the plugin do when the editor closes the document or Oxygen while an AI Markup request is in flight? → A: Cancel/interrupt the in-flight request and discard the result silently; no markup is written into a closed document.

### Session 2026-08-02

- Q: When the DILA CBRD Parse endpoint returns HTTP 401 (Bearer token invalid or missing) but `java.net.HttpURLConnection.getErrorStream()` throws `IOException("Server returned HTTP response code: 401 for URL: ...")` on Oxygen's bundled Java 17 — so the 401 body never reaches `parseErrorCode` — should the plugin classify this transport-level exception as FR-013 (connectivity) or FR-011 (unauthorized)? → A: FR-011. The plugin MUST classify by HTTP status whenever one is available, even when the response body cannot be read because of a JDK quirk. A 401 — with the canonical `WWW-Authenticate: Bearer realm="parse"` header and `{"success":false,"error":"unauthorized"}` body — is a credential rejection, not a connectivity failure. When the plugin can recover the status code either from `getResponseCode()` returning cleanly or by parsing it out of the `IOException` message text, it MUST NOT route to FR-013. Only when no HTTP status is recoverable (true DNS failure, TLS handshake failure, socket timeout before any byte was returned) is FR-013 the correct category.
- Q: Where should this contract be encoded so the documentation alone is sufficient to prevent future regressions? → A: As an additional acceptance scenario (US4 scenario 12), a new Edge Cases entry, and a tightening of FR-011's classification rule. The new scenario MUST be unit-testable by injecting a connection factory whose `getErrorStream()` throws the canonical Java-17 `IOException`, so the regression guard runs in `mvn test` against the mocked transport, not against the live network.

### Session 2026-08-02 (Phase 9 — CBRD preference labels)

- Q: The Ref-to-Link preference rows are labelled "CBRD API Endpoint" and "CBRD timeout (ms)". Since this feature added a second CBRD surface (CBRD Parse), those two names no longer make clear which action each row feeds — "CBRD API" reads as if it covered the whole plugin. Should the labels be renamed? → A: Yes, rename exactly two labels, nothing else: "CBRD API Endpoint" → "CBRD Link Endpoint" and "CBRD timeout (ms)" → "CBRD Link timeout (ms)", signalling that these rows configure the CBRD Link lookup (Ref-to-Link action), not the CBRD Parse endpoint. No row reordering, no storage-key changes, no new rows, and no default changes. The preference-page layout order contract (NFR-005) is preserved as-is; only the displayed label text changes. Localized in every shipped language (en_US: "CBRD Link Endpoint:"/"CBRD Link timeout (ms):"; zh_CN/zh_TW: "CBRD 連結端點："/"CBRD 連結逾時 (毫秒)：").
- Q: Should the internal log/exception strings in `DAMAOptionPagePluginExtension` that say "CBRD API URL" or "CBRD timeout" be renamed in the same pass? → A: No. Those are diagnostic log messages, not user-facing preference labels; renaming them is out of scope for this change and would churn unchanged behavior (FR-017). Only the two displayed `JLabel` texts and their i18n values change.

### Session 2026-08-02 (Phase 9 — preference row order)

- Q: After the label renovation, should the six preference rows keep the CBRD-Link-first order (endpoint, Referer header, Link timeout, then Parse URL, token, Parse timeout), or is a different order preferred? → A: Reorder the rows so the shared, action-independent Referer header comes first, then the three CBRD Parse rows (endpoint URL, token, timeout), then the two remaining CBRD Link rows (endpoint, timeout). Final order: "CBRD Referer header:", "CBRD Parse endpoint URL:", "CBRD Parse token*:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):". This supersedes the Phase 9 "no row reordering" decision (Session 2026-08-02 Phase 9) and amends the US5/FR-023 order contract accordingly. The change is still display-only: storage keys, defaults, and behavior are untouched (FR-017, NFR-005), and version stays `0.5.0`.

### Session 2026-08-02 (Phase 9 — transport-error capture in the diagnostics export)

- Q: A connectivity failure ("cannot reach the DILA parse service") was reproduced in the live Oxygen instance even though a direct probe from the same machine returns a clean HTTP 401 — yet the troubleshooting export showed only `failureCategory: CONNECTIVITY_OR_PROXY`, an empty `serviceErrorBody`, and no `httpStatus`, with no way to see the underlying exception. How should the export surface the real cause? → A: Extend the sanitized troubleshooting record with a `transportError` field that carries a redacted, compact exception class + message chain (e.g. `SocketTimeoutException: connect timed out`), populated from `CbrdParseResponse.getException()` whenever the failure carried one (true transport-level failures only; a known HTTP status such as 401 must leave it empty). The export schema is bumped to `1.1.0`. The field is redacted with the same `SecretRedactor` as the rest of the record so a token that ever leaked into an exception message cannot reach the exported JSON.

### Session 2026-08-02 (Phase 9 — Oxygen HTTP protocol handler)

- Q: The `transportError` field from the live S11 export finally named the real cause: `HttpExceptionWithDetails: 401 Unauthorized for: https://cbss.dila.edu.tw/cbrd/parse`. That class is `ro.sync.net.protocol.http.HttpExceptionWithDetails`, thrown by Oxygen's own URL stream handler, which Oxygen installs as the default handler — so inside Oxygen a non-2xx response throws this exception with message `"NNN Reason for: <url>"` instead of the JDK's `IOException("Server returned HTTP response code: NNN for URL: ...")`. Since the plugin's `recoverStatusFromException` only matched the JDK message, the 401 was never recovered, `httpStatus` stayed null, and the failure was mis-routed to FR-013 connectivity. How should the client treat Oxygen's exception? → A: The client MUST treat any exception whose message carries an HTTP status as a status-bearing failure, not a transport failure. Concretely, `recoverStatusFromException` MUST also parse the status from the Oxygen message format (`^NNN <Reason> for: <url>`, reason may be multi-word). A 401 recovered from `HttpExceptionWithDetails` classifies as `unauthorized` under FR-011 exactly like the JDK-message case; only exceptions with no status anywhere in the chain (DNS/TLS/timeout) remain FR-013. The root cause is Oxygen's stream-handler installation, not the network: the direct probe works because it runs outside Oxygen's JVM.
- Q: Should the client additionally branch on the exception class (`instanceof HttpExceptionWithDetails`) instead of parsing the message? → A: No. Parsing the status out of the message keeps the recovery logic SDK-agnostic (mirroring how the JDK message is parsed) and works even if Oxygen changes the class name or the message variant, and it keeps `CbrdParseApiClient` unit-testable without the Oxygen SDK classes. The transport-error capture from the previous clarification is what made this root cause visible in the first place, so both stay.

### Session 2026-08-02 (Phase 9 follow-up 4 — token label renamed and row order swapped)

- Q: The second CBRD Parse row is labelled "CBRD Parse token*:" and sits third on the page (after "CBRD Parse endpoint URL:"). The token is sent as an HTTP `Authorization: Bearer` header, so the label does not say what it is, and the token row sits below the endpoint URL row even though the token is the primary thing an editor configures. Should the label be renamed and the rows swapped, and how should the labels be translated? → A: Yes to both, display-only. Rename the `cbrd.parse.token.label` value to "CBRD bearer token*:" in en_US and swap the two rows so the token row is second. Final order: "CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):". In addition, ALL six preference labels are kept exactly identical to their English text in zh_CN/zh_TW — "Bearer", "Token", "URL", "Endpoint", "Referer", and the "CBRD Parse"/"CBRD Link" prefixes are untranslated protocol/UI terms on this page, and the editor asked for the translations to stay identical to English. This amends the Session 2026-08-02 (Phase 9 — preference row order) order contract and FR-023. Storage keys (`cbrd.parse.api.url`, `cbrd.parse.token`, `cbrd.parse.timeout`, `cbrd.api.url`, `cbrd.referer.header`, `cbrd.timeout`), defaults, and behavior are untouched (FR-017, NFR-005), and version stays `0.5.0`.


## Event Storming

### Actors

- Markup Editor
- DILA CBRD Parse Service
- Plugin Runtime

### Commands

- Invoke AI Markup
- Read Document Language
- Build Parse Request
- Send Parse Request to CBRD
- Apply Returned TEI P5 Markup
- Report Parse Failure

### Domain Events

- AI Markup Invoked
- Document Language Resolved
- Parse Request Sent To DILA
- DILA Service Returned Marked-Up XML
- Markup Applied to Document
- Parse Request Rejected
- Parse Returned Invalid Output
- DILA Service Unavailable

### Policies

- When AI Markup is invoked and no shared parse token is configured, the plugin reports a clear "configure the DILA parse token" message instead of sending a request that will be rejected.
- When AI Markup is invoked and the document carries an `xml:lang` the DILA service does not support, the plugin still sends the request with the default language rather than silently failing.
- When the DILA service returns an error, the plugin surfaces the service's stated cause (unauthorized, malformed input, too-long input, unsupported language, server-side OpenAI unavailable, rate limited, or invalid model output) as distinct, actionable user guidance where the cause is known.
- When the DILA service returns marked-up XML, the plugin preserves the existing AI Markup review-then-replace workflow: the result is shown first, the user edits or cancels, and only an explicit "Replace" applies it to the document.
- When the user applies the result, the change is recorded in the editor's undo history, matching the existing AI Markup contract.
- When removing the obsolete client credential fields from preferences, the plugin does not migrate or import their values into the new CBRD Parse fields.

### External System Boundaries

- DILA CBRD Parse service (`https://cbss.dila.edu.tw/cbrd/parse`), documented at the DILA CBRD OpenAPI page.
- Oxygen XML Editor workspace, editor, and preferences environment.

## Problem Frames

### Problem Domain

- Markup editors currently manage long-lived OpenAI API keys by hand on each machine, which violates the organization's credential security rules.
- The transformation quality of the AI Markup action depends on a fine-tuned model the organization does not want exposed to client machines.
- Markup editors need the AI Markup action to keep working with the same review-then-replace workflow after the underlying transformation moves behind a DILA-hosted endpoint.

### Solution Domain

- The plugin calls the DILA-hosted CBRD Parse endpoint with the selected text and a shared bearer token; the service performs the transformation with the pretrained model on the server.
- The plugin preferences surface only the configuration the plugin genuinely needs to reach the DILA service: endpoint URL, shared token, and timeout.
- The plugin reads the document's `xml:lang` to choose the request language, so no new user-facing language setting is required.

### Shared Phenomena

- The selected CBETA reference text submitted for AI Markup.
- The `xml:lang` value on the document root element of the document containing the selection.
- The CBRD Parse endpoint URL, shared token, and request timeout stored in plugin preferences.
- The marked-up TEI P5 XML returned by the DILA service.
- The user's review-then-replace decision in the existing AI Markup result area.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mark up references through the DILA service without any OpenAI key (Priority: P1)

A markup editor selects a passage of unmarked CBETA reference text in a TEI XML document and invokes AI Markup. The plugin reads the document's language, sends the passage to the DILA CBRD Parse service using a shared token the team has configured once, receives TEI P5 marked-up XML, shows it in the existing AI Markup result area, and applies it to the document only when the editor clicks "Replace". At no point does the editor see, enter, or know an OpenAI API key, model name, or chat completions endpoint.

**Why this priority**: This is the core change — moving the transformation off the client machine onto the DILA service and removing the client-side OpenAI credential surface. Every other story refines migration, failure handling, or cleanup around this flow.

**Independent Test**: On a fresh install with only the DILA CBRD Parse endpoint URL and shared token configured, select a passage and complete one AI Markup operation through to "Replace" without ever seeing an OpenAI-related field.

**Acceptance Scenarios**:

1. **Given** the DILA CBRD Parse endpoint URL and shared token are configured in plugin preferences, **When** the editor selects a passage of CBETA reference text and invokes AI Markup, **Then** the plugin sends one request to the DILA CBRD Parse endpoint and displays the returned TEI P5 marked-up XML in the existing AI Markup result area.
2. **Given** the DILA service has returned marked-up XML, **When** the editor reviews it in the result area, **Then** the editor can edit or cancel before anything is written into the document, matching the existing AI Markup workflow.
3. **Given** the editor clicks "Replace", **When** the change is applied to the document, **Then** the change is recorded in the editor's undo history, matching the existing AI Markup contract.
4. **Given** an AI Markup operation is in flight, **When** the editor continues using the document, **Then** the editor remains responsive and the operation runs in the background, matching the existing AI Markup responsiveness contract.
5. **Given** an AI Markup parse request is already in progress, **When** the editor invokes AI Markup again, **Then** the plugin ignores the second invocation, does not send another request, and shows “AI Markup already in progress” together with the in-flight selected XML.
6. **Given** an AI Markup parse request is in progress, **When** the editor closes the document or Oxygen before the DILA service responds, **Then** the plugin cancels/interrupts the in-flight request and writes nothing into the closed document.
7. **Given** the CBRD Parse request timeout is configured and the DILA service does not respond within it, **When** the timeout elapses, **Then** the plugin stops waiting, shows the timeout/connectivity guidance, and writes nothing into the document.

---

### User Story 2 - Existing users upgrade without losing their work (Priority: P2)

An existing team member already has the current plugin installed with the old OpenAI-style preferences populated. After upgrading to the new plugin version, the AI Markup action no longer uses those fields; the new CBRD Parse endpoint URL prefills to `https://cbss.dila.edu.tw/cbrd/parse`, and the editor only needs to enter the shared DILA parse token once to resume work. Any obsolete OpenAI-related preference values are simply ignored, not silently migrated into the new fields.

**Why this priority**: A migration that breaks working users on upgrade day generates support load and stalls adoption; making the new path require only a token entry keeps the rollout safe.

**Independent Test**: Upgrade a machine that has the old OpenAI-related preferences populated. Open preferences, confirm the OpenAI fields are gone and the CBRD Parse endpoint URL is prefilled, enter the shared token, and complete one AI Markup operation successfully.

**Acceptance Scenarios**:

1. **Given** an editor upgrades with previously populated OpenAI-style preferences, **When** they open the plugin preferences after upgrade, **Then** the OpenAI API key, base URL, chat completions path, and parse/detect model fields are no longer present and the CBRD Parse endpoint URL is prefilled to `https://cbss.dila.edu.tw/cbrd/parse`.
2. **Given** the editor has entered the shared DILA parse token after upgrade, **When** they invoke AI Markup, **Then** the request uses the new CBRD Parse endpoint and shared token rather than any previously stored OpenAI-related value.
3. **Given** any obsolete OpenAI-related preference values remain in storage from the previous version, **When** AI Markup runs after upgrade, **Then** those values are ignored and never sent to the DILA service.
4. **Given** the editor has entered the shared DILA parse token, **When** they reopen the preferences page or export diagnostics, **Then** the token is never shown in plaintext — the field is masked and the diagnostics package contains only a non-reversible fingerprint.
5. **Given** a machine that has never set a CBRD Parse timeout, **When** the preferences page opens after upgrade, **Then** the CBRD Parse timeout field is prefilled with the default required by FR-016.
6. **Given** the upgrade is complete and the OpenAI fields are gone, **When** the editor uses UTF-8 validation, tag removal, or Ref-to-Link, **Then** each behaves exactly as it did before the upgrade.

---

### User Story 3 - Document language drives the request automatically (Priority: P3)

A markup editor is working on a document whose root element carries `xml:lang="jp"` and invokes AI Markup. The plugin sends the request to the DILA CBRD Parse service with the Japanese language indicator, the service returns appropriate markup, and the editor never has to pick a language. On a different document carrying `xml:lang="zh"`, the request is sent with the Chinese indicator without any extra action. The plugin uses the document root element only, even when the selection sits inside a nested element that carries a different `xml:lang`.

**Why this priority**: Removing the language decision from the user's plate keeps the AI Markup action one click; documenting the inference rule here keeps the behavior auditable.

**Independent Test**: Open two documents, one with `xml:lang="jp"` and one with `xml:lang="zh"`, invoke AI Markup on the same kind of selection in each, and verify each request carries the matching language indicator without any user configuration.

**Acceptance Scenarios**:

1. **Given** the document containing the selection has `xml:lang="zh"` on its document root element, **When** the editor invokes AI Markup, **Then** the plugin sends the request with the Chinese language indicator to the DILA service.
2. **Given** the document containing the selection has `xml:lang="jp"` on its document root element, **When** the editor invokes AI Markup, **Then** the plugin sends the request with the Japanese language indicator to the DILA service.
3. **Given** the document does not carry a usable `xml:lang` value, **When** the editor invokes AI Markup, **Then** the plugin sends the request with the Chinese language indicator as the default.
4. **Given** the selection sits inside a nested element whose `xml:lang` differs from the document root element, **When** the editor invokes AI Markup, **Then** the plugin sends the language indicator derived from the document root element only.
5. **Given** the document root element carries `xml:lang="ja-JP"`, **When** the editor invokes AI Markup, **Then** the plugin sends the request with the Japanese language indicator.

---

### User Story 4 - Every parse failure tells the editor what to do (Priority: P4)

A markup editor invokes AI Markup and the DILA CBRD Parse service rejects the request for a known reason — the shared token is missing or wrong, the input is empty or too long, the language is unsupported, the server-side OpenAI model is unavailable, the request was rate-limited, or the model output did not pass the service's own validation. Each of these failures produces a specific, actionable user-facing message rather than a generic "AI Markup failed".

**Why this priority**: The DILA service itself enumerates these failure causes; mapping each to plain guidance is what keeps support requests low, but it only matters once the main flow exists.

**Independent Test**: Trigger each enumerated DILA service failure mode the service documents, plus an unexpected response shape, an unreachable endpoint, and a malformed endpoint URL, and verify each produces a distinct, actionable AI Markup result-area message in every shipped language.

**Acceptance Scenarios**:

1. **Given** the shared DILA parse token is missing or wrong, **When** the editor invokes AI Markup, **Then** the result area names a credential problem distinct from a connectivity or service problem, and tells the editor to configure the shared token.
2. **Given** the DILA service rejects the request because the input is too long despite the client-side pre-check (for example, the service's limit is lower than 4,000 characters), **When** the failure is returned, **Then** the result area tells the editor the selection exceeds the service's input limit.
3. **Given** the DILA service cannot reach its upstream model provider or is rate-limited, **When** the failure is returned, **Then** the result area identifies the DILA service as temporarily unavailable and tells the editor to retry shortly.
4. **Given** the DILA service returns because its model output did not pass server-side validation, **When** the failure is returned, **Then** the result area names that specific cause rather than reporting a generic failure.
5. **Given** the editor cannot reach the DILA service at all, **When** the failure is returned, **Then** the result area identifies a connectivity problem distinct from the service-side failures.
6. **Given** the selection is empty when AI Markup is invoked, **When** the editor invokes AI Markup, **Then** the plugin shows the input-missing guidance and does not send a request to the DILA service.
7. **Given** the selection exceeds the DILA service’s 4,000-character input limit, **When** the editor invokes AI Markup, **Then** the plugin shows the too-long guidance and does not send a request to the DILA service.
8. **Given** the DILA service responds with a status code or body shape not enumerated in its published contract, **When** the response is received, **Then** the result area shows a generic failure message, the plugin does not crash, the selection is retained, and nothing is written into the document.
9. **Given** a parse failure has produced a sanitized troubleshooting record, **When** the editor exports diagnostics, **Then** the export control is offered in the result area and the shared token appears only as a non-reversible fingerprint.
10. **Given** the editor runs the plugin in each shipped interface language, **When** any new preference label, status message, or failure guidance introduced by this feature is displayed, **Then** it appears in that language with no missing-key fallback.
11. **Given** the CBRD Parse endpoint URL preference is empty or malformed, **When** the editor invokes AI Markup, **Then** the result area names the endpoint URL preference as the problem, no request is attempted, and the message is distinct from the connectivity failure message.
12. **Given** the DILA CBRD Parse endpoint returns HTTP 401 with `WWW-Authenticate: Bearer realm="parse"` and body `{"success":false,"error":"unauthorized"}`, **When** `java.net.HttpURLConnection.getErrorStream()` throws `IOException("Server returned HTTP response code: 401 for URL: https://cbss.dila.edu.tw/cbrd/parse")` (the observed Java-17 quirk that prevents the body from being read normally), **Then** the plugin MUST recover the HTTP status (either from a prior `getResponseCode()` call or by parsing the status code out of the exception message), classify the response as `unauthorized` under FR-011 and `ParseError.UNAUTHORIZED`, surface the `ai.markup.error.unauthorized` guidance (FR-011), expose the Export Diagnostics control (FR-022), and MUST NOT emit the FR-013 connectivity message.
13. **Given** Oxygen runs the plugin, **When** a request to the DILA CBRD Parse endpoint is rejected with HTTP 401 and Oxygen's own URL stream handler throws `ro.sync.net.protocol.http.HttpExceptionWithDetails` with message `"401 Unauthorized for: https://cbss.dila.edu.tw/cbrd/parse"` from `getResponseCode()` (so no status was ever captured), **Then** the plugin MUST recover the HTTP 401 by parsing the status code out of that message text, classify the response as `unauthorized` under FR-011 and `ParseError.UNAUTHORIZED`, surface the `ai.markup.error.unauthorized` guidance, and MUST NOT emit the FR-013 connectivity message — and the troubleshooting export MUST record the cause (FR-022). The same recovery MUST apply to any status in that message format (multi-word reasons such as `"503 Service Unavailable for: <url>"` included).

---

### User Story 5 - Preference labels distinguish the CBRD Link lookup from the CBRD Parse endpoint (Priority: P5)

A markup editor opens the plugin preferences page and sees six rows: one shared Referer header row, three for the CBRD Parse endpoint used by AI Markup (bearer token, endpoint URL, timeout), and two for the CBRD Link lookup used by Ref-to-Link (endpoint, timeout). The Link rows were originally labelled "CBRD API URL" and "CBRD timeout (ms)" — the "CBRD API"/"CBRD" names predate the CBRD Parse surface added by this feature, so they no longer distinguish the Ref-to-Link lookup from the AI Markup endpoint. After this change the endpoint row reads "CBRD Link Endpoint" and the timeout row reads "CBRD Link timeout (ms)", and the Parse token row reads "CBRD bearer token*:" (the token is sent as an `Authorization: Bearer` header), so the editor can tell which rows configure which action at a glance. Only the displayed label text and the row order change: the storage keys, the defaults, and both actions' behavior are untouched.

**Why this priority**: The Parse feature added a second CBRD surface on the same page; leaving "CBRD API" on the Link rows invites an editor to paste the CBRD Parse token into the wrong field or edit the wrong timeout. A label rename is cheap, display-only, and keeps the two surfaces unambiguous, but it is cosmetic polish that only matters once both surfaces exist.

**Independent Test**: Open the plugin preferences page after upgrade and confirm the rows read, top to bottom: "CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:" (not "CBRD API URL"/"CBRD API Endpoint"), "CBRD Link timeout (ms):" (not "CBRD timeout (ms)"), while the storage keys and defaults are unchanged.

**Acceptance Scenarios**:

1. **Given** the editor opens the plugin preferences page, **When** the rows are rendered top to bottom, **Then** they read "CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):".
2. **Given** the editor opens the plugin preferences page, **When** the fifth row is rendered, **Then** its label reads "CBRD Link Endpoint" in every shipped language; when the sixth row is rendered, its label reads "CBRD Link timeout (ms)" in every shipped language — identical to the English text because "Endpoint" and the "CBRD Link" prefix are untranslated on this page (Session 2026-08-02 Phase 9 follow-up 4).
3. **Given** the label renames and row reorder are applied, **When** the editor inspects the page, **Then** the six rows follow the documented order (Referer header, bearer token, Parse URL, Parse timeout, Link Endpoint, Link timeout) and the storage keys and defaults are unchanged (FR-023, NFR-005).
4. **Given** the label renames and row reorder are applied, **When** Ref-to-Link and AI Markup run, **Then** both actions behave exactly as before — the change is display-only (FR-017).
5. **Given** the editor views the plugin preferences page in Chinese, **When** any of the six rows is rendered, **Then** its label reads exactly the same text as in English — "CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):" — because "Bearer", "Token", "URL", "Endpoint", "Referer", and the "CBRD Parse"/"CBRD Link" prefixes are untranslated protocol/UI terms (Session 2026-08-02 Phase 9 follow-up 4).

---

### Edge Cases

- The selection is empty when AI Markup is invoked → US4 scenario 6, FR-019.
- The selection exceeds the DILA CBRD Parse service's 4,000-character input limit → US4 scenario 7, FR-019.
- The document carries an `xml:lang` value the DILA service does not accept (neither `zh` nor `jp`); FR-007 sends the default indicator rather than failing.
- The `xml:lang` attribute uses a regional subtag (for example `zh-Hant` or `ja-JP`) rather than a bare `zh` or `jp` → US3 scenario 5.
- The document root element is missing or cannot be determined for the current selection → US3 scenario 3 (default indicator).
- The shared DILA parse token is present but malformed, expired, or revoked → US4 scenario 1 (service `unauthorized`, distinct from FR-010's missing-token guidance).
- The DILA service responds with an unexpected status code or body shape not enumerated in its published contract → US4 scenario 8, FR-012.
- The plugin preferences contain a custom CBRD Parse endpoint URL that is malformed → US4 scenario 11, FR-021. An unreachable-but-valid URL falls through to FR-013.
- The JDK surfaces an HTTP 401 (or other non-2xx) response as `IOException("Server returned HTTP response code: NNN for URL: ...")` thrown from `HttpURLConnection.getErrorStream()` (observed on Oxygen's bundled Java 17 for 401 responses from the CBRD Parse endpoint) → US4 scenario 12, FR-011. The plugin MUST classify by the recoverable HTTP status, not by the exception class, so a 401 maps to `unauthorized` and not to FR-013 `connectivity`.
- Oxygen installs its own URL stream handler, so inside Oxygen a non-2xx response surfaces as `ro.sync.net.protocol.http.HttpExceptionWithDetails` with message `"NNN Reason for: <url>"` (observed live as `"401 Unauthorized for: https://cbss.dila.edu.tw/cbrd/parse"` in the 2026-08-02 S11 export) instead of the JDK canonical message → US4 scenario 13, FR-011. The plugin MUST recover the status from that message text exactly as it does for the JDK message (reason phrase may be multi-word), so a 401 maps to `unauthorized`; only exceptions whose whole cause chain carries no status (DNS/TLS/timeout) remain FR-013.
- The Ref-to-Link rows still read "CBRD API URL"/"CBRD timeout (ms)" after the CBRD Parse surface was added → US5 scenarios 1-2, FR-023. The label renames ("CBRD Link Endpoint", "CBRD Link timeout (ms)") and the row reorder (Referer header first, then the three CBRD Parse rows, then the CBRD Link endpoint and timeout rows) are display-only; storage keys and defaults are unchanged.
- The DILA service (or an intervening gateway/proxy) returns HTTP 401 for a rejected token with an empty or unreadable error body → US4 scenario 12, FR-011. The plugin MUST classify the failure as `unauthorized` from the status code alone — never as FR-013 `connectivity` and never as the generic `unexpected` cause — whenever any HTTP status is recoverable, whether the status comes from `getResponseCode()`, from the canonical `IOException("Server returned HTTP response code: NNN for URL: ...")` message, or from a prior successful `getResponseCode()` call before `getErrorStream()` threw.
- Two AI Markup operations are invoked in quick succession on the same document → US1 scenario 5, FR-015.
- A nested element of the selection carries an `xml:lang` that differs from the document root element → US3 scenario 4.
- The editor closes the document or the editor itself between invoking AI Markup and the DILA service responding → US1 scenario 6, FR-020.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The AI Markup action MUST transform selected CBETA reference text into TEI P5 marked-up XML by calling the DILA CBRD Parse endpoint, and MUST NOT call any OpenAI-compatible chat completions endpoint directly from the client.
- **FR-002**: The DILA CBRD Parse endpoint URL MUST be stored in plugin preferences and MUST default to `https://cbss.dila.edu.tw/cbrd/parse` when the editor has not configured a custom value.
- **FR-003**: The shared token used to authenticate against the DILA CBRD Parse endpoint MUST be stored in plugin preferences using the editor's secure credential storage, and MUST NOT be displayed, logged, or included in diagnostics except as a non-reversible fingerprint.
- **FR-004**: The plugin MUST remove the OpenAI API key, OpenAI base URL, chat completions path, fine-tuned parse model, fine-tuned detect model, and AI request timeout fields from plugin preferences, and MUST NOT read or send any of those values — nor any client-supplied system prompt — when AI Markup is invoked.
- **FR-005**: After an upgrade from a version that stored OpenAI-related preferences, the plugin MUST NOT migrate or import any of those values into the new CBRD Parse fields, and MUST NOT use them when AI Markup is invoked.
- **FR-006**: The plugin MUST send a request to the DILA CBRD Parse endpoint containing exactly the selected text and a language indicator — no system prompt, model name, or other client-supplied field — authenticated with the shared token, whenever AI Markup is invoked and the required configuration is present.
- **FR-007**: The plugin MUST determine the language indicator for the parse request by reading `xml:lang` only from the document root element of the document containing the selection, MUST NOT use the selected element or any nested ancestor for language resolution, and MUST send the Chinese indicator when the root value begins with `zh`, the Japanese indicator when the root value begins with `ja` or `jp`, and the Chinese indicator as the default when no usable root `xml:lang` is present.
- **FR-008**: The plugin MUST surface the marked-up XML returned by the DILA CBRD Parse endpoint in the existing AI Markup result area and MUST apply it to the document only when the editor explicitly chooses to replace the selection, matching the existing AI Markup review-then-replace workflow.
- **FR-009**: When the editor applies the returned markup, the plugin MUST record the change in the editor's undo history, matching the existing AI Markup contract.
- **FR-010**: When AI Markup is invoked and the shared DILA parse token is missing, the plugin MUST display a clear guidance message naming the missing token and MUST NOT send a request to the DILA service. This client-side guidance MUST be distinct from the message shown for the service's `unauthorized` response under FR-011: "token not configured" and "token rejected" are different situations for the editor and MUST NOT share one message.
- **FR-011**: When the DILA CBRD Parse endpoint returns an enumerated failure cause (unauthorized, input missing, input too long, unsupported language, server-side provider unavailable, server-side provider rate-limited, server credentials unavailable, service not configured, or invalid model output), the plugin MUST display a specific, actionable result-area message that maps to that cause. Classification MUST be driven by the HTTP status code and — when available — the `error` field of the parsed body. When the JDK surfaces a non-2xx response as `IOException("Server returned HTTP response code: NNN for URL: ...")` from `getErrorStream()` (observed on Oxygen's bundled Java 17 for HTTP 401), or when Oxygen's own URL stream handler throws `ro.sync.net.protocol.http.HttpExceptionWithDetails` with message `"NNN Reason for: <url>"` (observed live as `"401 Unauthorized for: https://cbss.dila.edu.tw/cbrd/parse"`), the plugin MUST recover the status code — whether from `getResponseCode()` or by parsing it out of the exception message text — and classify from the status against the contract above, rather than falling through to FR-013. Only failures with no recoverable HTTP status (DNS failure, TLS handshake failure, socket timeout before any byte was returned) are classified as connectivity under FR-013. The authoritative wire-level code for each cause is recorded in [contracts/openapi.yaml](contracts/openapi.yaml).
- **FR-012**: When the DILA CBRD Parse endpoint returns a status code or body shape not enumerated in its published contract, the plugin MUST display a generic failure message and MUST NOT crash, discard the selection, or write anything into the document.
- **FR-013**: When the plugin cannot reach the DILA CBRD Parse endpoint, the plugin MUST display a connectivity failure message distinct from any service-side failure message.
- **FR-014**: AI Markup network execution, language resolution, and failure reporting MUST run off the editor's UI event thread, and all UI updates MUST be marshaled back onto that thread, matching the existing AI Markup responsiveness contract.
- **FR-015**: When an AI Markup operation is already in progress, a second invocation MUST be ignored, MUST NOT start a parallel parse request, and MUST show a clear “AI Markup already in progress” message together with the in-flight selected XML so the editor can tell which selection is being processed.
- **FR-016**: The plugin MUST provide a configurable CBRD Parse request timeout stored in plugin preferences and applied to calls made to the DILA service. The default MUST be at least as generous as the timeout the AI Markup action used before this feature (30,000 ms), because the DILA service performs a model transformation server-side. The shorter Ref-to-Link lookup timeout MUST NOT be used as the default.
- **FR-017**: The plugin MUST preserve the behavior of the existing UTF-8 validation, tag removal, and Ref-to-Link actions unchanged by this feature.
- **FR-018**: All new user-facing text introduced by this feature (preference labels, result-area messages, status text, and failure guidance) MUST be localized in every language the plugin already ships.
- **FR-019**: Before sending a parse request, the plugin MUST validate the selection locally and MUST NOT send the request when the selection is empty or exceeds 4,000 characters, showing the same actionable messages the DILA service would return for input missing or input too long.
- **FR-020**: When the document or editor is closed while an AI Markup request is in flight, the plugin MUST cancel/interrupt the in-flight request and MUST discard the result silently; no markup MUST be written into a closed document.
- **FR-021**: When the configured CBRD Parse endpoint URL is missing, malformed, or uses an unsupported scheme, the plugin MUST display a configuration-error message naming the endpoint URL preference and MUST NOT attempt a request. This message MUST be distinct from the connectivity failure message required by FR-013.
- **FR-022**: When an AI Markup parse attempt fails, the plugin MUST preserve the diagnostics affordance introduced in `002-ai-api-diagnostics`: a sanitized troubleshooting record is produced and the Export Diagnostics control is offered in the result area, with the shared token present only as a non-reversible fingerprint. When the failure carried a transport-level exception (no HTTP status recovered), the exported record (schema `1.1.0`) MUST include a sanitized, redacted `transportError` field naming the exception class and message (Session 2026-08-02 Phase 9 — transport-error capture); a known HTTP status failure MUST leave that field empty.
- **FR-023**: The plugin MUST label the Ref-to-Link lookup endpoint preference "CBRD Link Endpoint" and its timeout preference "CBRD Link timeout (ms)" in every shipped language, replacing the pre-rename "CBRD API Endpoint"/"CBRD API URL" and "CBRD timeout (ms)" texts, and MUST render the six rows top to bottom in this order: "CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):" (Session 2026-08-02 Phase 9 — row order; amended by Session 2026-08-02 Phase 9 follow-up 4 — token label renamed and row order swapped). ALL six preference labels MUST be rendered identical to their English text in every shipped language — the zh_CN/zh_TW values are kept exactly identical to the en_US value because "Bearer", "Token", "URL", "Endpoint", "Referer", and the "CBRD Parse"/"CBRD Link" prefixes are untranslated protocol/UI terms on this page. The rename and reorder MUST be display-only: the storage keys (`cbrd.api.url`, `cbrd.referer.header`, `cbrd.timeout`, `cbrd.parse.api.url`, `cbrd.parse.token`, `cbrd.parse.timeout`), the defaults, and both actions' behavior MUST NOT change, and Ref-to-Link behavior MUST remain unchanged (FR-017, NFR-005).

### Non-Functional Requirements

- **NFR-001**: New user-facing text introduced by this feature MUST ship in every language the plugin already ships, and an automated translation-bundle completeness check MUST fail if any new key is missing in any supported language.
- **NFR-002**: The AI Markup action MUST show visible processing feedback in the result area within 500 ms of invocation, MUST keep the editor responsive for the entire in-flight period, and MUST surface a success or failure message no later than the configured CBRD Parse timeout plus 500 ms.
- **NFR-003**: Any background executor used for AI Markup network execution MUST be shut down during the existing plugin shutdown path, matching the existing executor lifecycle contract.
- **NFR-004**: The shared DILA parse token MUST be treated with the same redaction strength as the existing OpenAI API key, including non-display in logs, diagnostics, and user-facing messages.
- **NFR-005**: Removal of the existing OpenAI-related preference fields MUST NOT change behavior of the existing preferences page beyond removing those fields; the remaining preference page ordering and layout contract is preserved.

### Key Entities

- **DILA CBRD Parse endpoint**: The organization-hosted service that accepts selected CBETA reference text and a language indicator, authenticated by a shared bearer token, and returns TEI P5 marked-up XML. The default endpoint is `https://cbss.dila.edu.tw/cbrd/parse`.
- **Shared DILA parse token**: The single bearer credential the editor stores to authenticate against the DILA CBRD Parse endpoint; held in the editor's secure credential storage; never displayed except as a non-reversible fingerprint.
- **Document language resolution**: The process by which the plugin reads `xml:lang` on the document root element of the document containing the selection and determines the language indicator to send to the DILA service, defaulting to Chinese when no usable value is present.
- **AI Markup failure guidance**: The user-facing message produced for each DILA CBRD Parse failure cause the service enumerates, plus a generic fallback and a distinct connectivity message, replacing the previous OpenAI-style diagnostic mapping for the AI Markup path.
- **AI Markup workflow contract**: The existing review-then-replace workflow, result area, undo history, and non-blocking responsiveness guarantees that this feature preserves unchanged except for where the transformation is performed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After rollout, the number of OpenAI API keys, fine-tuned model names, or OpenAI-compatible base URLs stored on team members' editor machines for the AI Markup action is zero.
- **SC-002**: A new team member goes from plugin installation to a successful AI Markup operation by entering only the shared DILA parse token, with no other credential or model configuration required.
- **SC-003**: In 100% of sampled successful AI Markup operations after rollout, the transformation is performed by the DILA CBRD Parse endpoint rather than any client-side OpenAI-compatible call.
- **SC-004**: In 100% of sampled upgrades from the previous version, the AI Markup action works after the editor enters the shared DILA parse token, regardless of which OpenAI-related values were previously stored.
- **SC-005**: In 100% of sampled AI Markup failures from the causes the DILA service enumerates, the result area identifies the matching enumerated cause and shows actionable guidance instead of a generic failure.
- **SC-006**: In 100% of sampled documents carrying `xml:lang="zh"` or `xml:lang="jp"` (including regional subtags such as `zh-Hant` or `ja-JP`), the plugin sends the matching language indicator without any editor configuration.
- **SC-007**: The AI Markup action preserves the existing review-then-replace workflow, undo history, and editor responsiveness, with zero sampled regressions compared to the previous version on the success path.

## Assumptions

- The DILA CBRD Parse endpoint at `https://cbss.dila.edu.tw/cbrd/parse`, as documented at the DILA CBRD OpenAPI page, remains the production endpoint for this feature and accepts the request and response shape documented there.
- The administrator provisions the shared bearer token the editors will use to authenticate against the DILA CBRD Parse endpoint, following the organization's established secret-management practice, before rollout.
- The DILA service performs the pretrained-model transformation server-side, so the plugin no longer needs any OpenAI model name or OpenAI-compatible endpoint configuration.
- The existing AI Markup review-then-replace workflow, result area, undo-history behavior, and non-blocking responsiveness contract are accepted as the contract this feature preserves.
- The plugin already ships the same secure credential storage mechanism used today for the OpenAI API key; this feature reuses that mechanism for the shared DILA parse token.
- The plugin already ships the same CBRD preference pattern (endpoint URL, referer or token, timeout) used today by the Ref-to-Link action; this feature mirrors that pattern for the CBRD Parse endpoint.
- The plugin already supports the languages it ships; this feature adds new keys in those existing languages and does not introduce new languages.

## Dependencies

- **Administrator action (blocking)**: provision the shared bearer token the editors will use to authenticate against the DILA CBRD Parse endpoint and distribute it to the team out of band, following the organization's established secret-distribution practice.
- **DILA service availability (blocking)**: the DILA CBRD Parse endpoint at `https://cbss.dila.edu.tw/cbrd/parse` is operational and accepts the request and response shape documented at the DILA CBRD OpenAPI page before this feature is rolled out to editors.

## Out of Scope

- Corporate single sign-on, AWS SSO device flows, or any per-user corporate identity flow for obtaining the shared DILA parse token. The token is a single shared credential entered by the editor for now.
- Changing the existing AI Markup review-then-replace workflow, result area, undo-history behavior, or non-blocking responsiveness contract.
- Changing the behavior of the existing UTF-8 validation, tag removal, or Ref-to-Link actions.
- Removing the OpenAI-compatible diagnostics features shipped in `002-ai-api-diagnostics` from the rest of the plugin; only the AI Markup client-side OpenAI call path and its preference fields are removed.
- Adding new supported languages to the plugin.

## Notes

- The shared DILA parse token handled here is expected to be replaced by a corporate single-sign-on flow if and when the work tracked under `003-aws-sso-secrets` lands. The preference surface introduced by this feature (endpoint URL, shared token, timeout) is intentionally minimal so a future SSO feature can layer on top without another preferences-page rewrite.
