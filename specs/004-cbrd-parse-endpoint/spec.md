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
- **FR-011**: When the DILA CBRD Parse endpoint returns an enumerated failure cause (unauthorized, input missing, input too long, unsupported language, server-side provider unavailable, server-side provider rate-limited, server credentials unavailable, service not configured, or invalid model output), the plugin MUST display a specific, actionable result-area message that maps to that cause. The authoritative wire-level code for each cause is recorded in [contracts/openapi.yaml](contracts/openapi.yaml).
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
- **FR-022**: When an AI Markup parse attempt fails, the plugin MUST preserve the diagnostics affordance introduced in `002-ai-api-diagnostics`: a sanitized troubleshooting record is produced and the Export Diagnostics control is offered in the result area, with the shared token present only as a non-reversible fingerprint.

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
