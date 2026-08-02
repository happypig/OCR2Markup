# Feature Specification: Restore Ref-to-Link against CBRD v1.1.0

**Feature Branch**: `005-cbrd-link-v11`

**Created**: 2026-08-03

**Status**: Draft

**Input**: User description: "Restore the Ref-to-Link action against CBRD API v1.1.0. The vendored contract is pinned to v1.0.0 (`GET /cbrd/link?q=…`); the live server is v1.1.0 and defines `POST /cbrd/link` only, taking a JSON body `{"q": "<ref>…</ref>"}`. The plugin still issues the GET, no route matches, and editors see `CBRD API error: HTTP 404`."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Editor converts a reference into a CBETA link (Priority: P1)

An editor working in Oxygen selects a `<ref>` element in a TEI document and runs the **`<ref> to link`** action. The plugin asks the DILA CBRD service to resolve the citation and shows the resulting CBETA Online URL, which the editor can insert with **Replace**.

Today this journey fails for every editor, on every reference: the plugin's request never reaches the service's handler, and the editor sees `CBRD API error: HTTP 404` — a message that reads like "your citation was not found" but actually means "the plugin asked in a way the service no longer accepts."

**Why this priority**: Ref-to-Link is entirely non-functional in production. This is the outage. Nothing else in this feature matters if this does not work.

**Independent Test**: Select a complete `<ref>` in Oxygen, run `<ref> to link`, and confirm a CBETA Online URL is returned and can be inserted. Delivers the whole user-facing value on its own.

**Acceptance Scenarios**:

1. **Given** an editor has selected `<ref><canon>T</canon><v>4</v><w>202</w><p>376</p><c>b</c><l>4</l></ref>`, **When** they run `<ref> to link`, **Then** the plugin displays `https://cbetaonline.dila.edu.tw/T04n0202_p0376b04` and the **Replace** button is enabled.
2. **Given** an editor has selected a reference containing Traditional Chinese canon names and full-width punctuation (`<ref><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>`), **When** they run `<ref> to link`, **Then** the citation is transmitted without corruption and the service returns a documented outcome — never a transport error.
3. **Given** the conversion succeeds, **When** the editor clicks **Replace**, **Then** the `<ref>` element is rewritten with the returned URL exactly as it was before this feature.

---

### User Story 2 - Editor gets a truthful message when a citation cannot be resolved (Priority: P2)

When the service *can* be reached but cannot resolve the citation, the editor needs to know that the citation is the problem — not the connection. The service distinguishes three outcomes and returns all of them as a successful exchange; the plugin must surface each one distinctly.

**Why this priority**: The outage taught the failure mode. A transport-level error message shown for a citation-level problem sends editors hunting for network faults, and it is what disguised this outage. Valuable independently of P1 because it governs every non-happy path.

**Independent Test**: Submit an incomplete citation and a well-formed but unmatchable citation, and confirm each produces its own message, distinct from the connectivity message.

**Acceptance Scenarios**:

1. **Given** a citation the service resolves to no entries, **When** the editor runs `<ref> to link`, **Then** the plugin shows the "no results" message (`error.no.results`) — not the transport-error message.
2. **Given** an incomplete citation such as `<ref><canon>T</canon><v>25</v></ref>`, **When** the editor runs `<ref> to link`, **Then** the plugin shows the service's own explanation via the service-failure message (`error.api.failed`), including the returned text.
3. **Given** the service or network is genuinely unreachable, **When** the editor runs `<ref> to link`, **Then** — and only then — the plugin shows the transport-error message (`error.api.http`) with the status.

---

### User Story 3 - The team learns about vendor drift before editors do (Priority: P3)

A maintainer changes or releases the plugin. If the CBRD service has changed its contract since the last release, the build tells them, instead of the change reaching editors as a production outage.

**Why this priority**: This outage was undetectable from inside the repository — the vendored contract said v1.0.0, the suite asserted the v1.0.0 shape, and everything was green while production was broken. Independently valuable: it protects every future release, including releases that touch nothing in this feature.

**Independent Test**: Point the guard at a contract version other than the one the plugin is built against and confirm the build fails with a message naming the drift.

**Acceptance Scenarios**:

1. **Given** the vendored contract records the version the plugin is built against, **When** that version no longer matches the version the plugin's requests target, **Then** the automated suite fails and names both versions.
2. **Given** a maintainer changes the request shape back to a form the current contract does not accept, **When** the suite runs, **Then** it fails on an assertion about the request's method and body — not on an assertion about a URL string.

---

### Edge Cases

- **Citation contains characters that require escaping.** Reference XML routinely contains double quotes (`xml:id="r26"`), CJK text, and full-width punctuation. The citation must arrive at the service byte-identical; a mangled payload must not be able to masquerade as "no results".
- **Service returns a failure explanation in Chinese.** The service's own message text (e.g. `經號或頁碼 至少要有一個`) is passed through to the editor. It is service-supplied content, not a plugin string, and so is not subject to the plugin's own translation requirement.
- **Non-success outcomes arrive as a successful exchange.** Both "no match" and "service could not process this" are returned by the service as a normal successful response carrying a `success` flag. Treating the exchange as failed because `success` is `false` would resurrect the current confusion in a new form.
- **The host application rewrites transport errors.** Oxygen installs its own URL stream handler, so a non-success response surfaces as a host-specific exception rather than a readable status. Any retained non-success path must recover the status the same way the AI Markup path already does.
- **Timeouts.** Existing behaviour — retry on timeout only — must be preserved. A routing or validation failure must not be retried.
- **A configured non-default service URL.** Editors may have overridden the service URL in preferences. The change must work with whatever URL is configured; no preference migration and no forced reset.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ref-to-Link MUST issue its conversion requests in the form the current CBRD `/link` contract accepts, so that requests reach the service's handler rather than failing at routing.
  - **Given** a valid reference selection, **When** the editor runs `<ref> to link`, **Then** the service accepts the request and returns one of its documented outcomes.
- **FR-002**: The reference XML MUST be transmitted verbatim — not URL-encoded — with UTF-8 preserved, including double quotes, CJK characters, and full-width punctuation.
  - **Given** a reference containing `大正`, `、`, `―` and a quoted attribute, **When** the request is built, **Then** the transmitted citation is identical to the selection.
- **FR-003**: Escaping of the citation into the request payload MUST use the project's existing JSON facility rather than hand-rolled string escaping.
  - **Given** a citation containing a double quote and a backslash, **When** the payload is built, **Then** it is well-formed and the service accepts it.
- **FR-004**: The plugin MUST map each of the service's three documented outcomes to its own distinct user message: a resolved link, "no results", and "service reported a failure" (carrying the service's explanation).
  - **Given** each documented outcome in turn, **When** it is received, **Then** the corresponding message key is used and no other.
- **FR-005**: The transport-error message MUST be reserved for genuine transport or protocol failures, and MUST NOT be reachable for any outcome the service returns normally.
  - **Given** a normal service response of any documented kind, **When** it is handled, **Then** the transport-error message is not shown.
- **FR-006**: The vendored contract in `specs/001-ref-to-link-action/contracts/` MUST record the contract version the plugin is actually built against, and MUST describe the request shape the plugin actually sends.
  - **Given** the vendored contract, **When** it is read alongside the plugin's request construction, **Then** the declared version and the described request shape both match what the plugin sends.
- **FR-007**: The automated suite MUST be able to observe the request's method, content type, and body. The test double used for Ref-to-Link MUST record all three.
  - **Given** a conversion is exercised in tests, **When** assertions run, **Then** the request method, content type, and body are each assertable.
- **FR-008**: Assertions that previously pinned the superseded request shape MUST be removed and replaced with assertions on the shape the service now requires.
  - **Given** the suite passes, **When** the request shape is reverted to the superseded form, **Then** at least one test fails.
- **FR-009**: The suite MUST fail when the contract version the plugin targets and the version recorded in the vendored contract diverge.
  - **Given** the two versions agree, **When** either one is changed, **Then** the suite fails with a message naming both versions.
- **FR-010**: Version-dependent assertions MUST assert the invariant rather than a frozen literal, so that a correct version bump does not turn the suite red.
  - **Given** the plugin version is bumped, **When** the suite runs, **Then** no test fails solely because a version string changed.
- **FR-011**: Existing Ref-to-Link behaviour outside request construction MUST be unchanged: selection validation, the Replace rewrite, retry-on-timeout-only, the result area's error styling, and the configured service URL.
  - **Given** the existing tests covering selection validation, the Replace rewrite, retry behaviour, and error styling, **When** the suite runs after this feature, **Then** all of them pass unmodified.
  - **Given** an editor who has overridden the service URL in preferences, **When** they run `<ref> to link`, **Then** the request goes to their configured URL and no preference is reset or migrated.
- **FR-012**: The service's current authentication expectations for this endpoint MUST be honoured; a header that the contract no longer requires MUST NOT cause the request to be rejected if it is still sent.
  - **Given** a request that still carries the previously-required identifying header, **When** it is sent, **Then** the service accepts it and returns a documented outcome.
- **FR-013**: No new user-facing strings are expected. If any is introduced, it MUST be present in all three supported languages (en_US, zh_CN, zh_TW) per Principle IX.
  - **Given** the set of user-facing message keys after this feature, **When** the i18n completeness test runs, **Then** every key resolves in en_US, zh_CN, and zh_TW.
- **FR-014**: The restored behaviour MUST be confirmed inside Oxygen against the live service — not solely against test doubles — before the feature is considered complete.
  - **Given** the built plugin installed in Oxygen, **When** an editor runs `<ref> to link` on a real document against the live service, **Then** a CBETA Online URL is returned, and the observation is recorded before sign-off.

### Key Entities

- **Reference (參考文獻)**: The citation the editor selected, as TEI `<ref>` XML. Carries canon, volume, work, page, column, and line components. Transmitted to the service unmodified; this feature changes only how it is carried, never its content.
- **Link Request**: One citation submitted for resolution. Under the current contract the citation travels in the request body under the key `q`.
- **Link Response**: The service's answer, in one of exactly three shapes — resolved (one or more CBETA Online URLs), resolved-but-empty (no matching entry), or failed (an explanatory message). All three are returned as a successful exchange; the distinction is carried in the payload, not in the transport status.
- **Vendored Contract**: The repository's committed copy of the service's interface description. Its role changes in this feature: from documentation that may silently rot, to an input the suite checks.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Ref-to-Link conversions that today fail with `CBRD API error: HTTP 404` succeed or return an accurate citation-level message — 0% of conversions produce a transport error for a citation the service can process.
- **SC-002**: An editor selecting a complete citation receives a CBETA Online URL without leaving the editor, in a single action, within the existing timeout budget.
- **SC-003**: Each of the three documented service outcomes produces a distinct, correct message; an editor can tell "the service is unreachable" from "this citation has no match" without reading logs.
- **SC-004**: A reference containing Traditional Chinese text and quoted attributes round-trips without corruption.
- **SC-005**: Reintroducing the superseded request shape causes at least one automated test to fail — verified by attempting it.
- **SC-006**: A divergence between the contract version the plugin targets and the version recorded in the repository causes the build to fail, naming both versions.
- **SC-007**: A correct plugin version bump causes no test to fail.
- **SC-008**: The full suite passes, and Ref-to-Link is exercised successfully in a real Oxygen session against the live service.

## Assumptions

- The service endpoint URL is unchanged; only the way a request is made to it has changed. Editors who have overridden the URL in preferences keep working with no migration.
- The service's response payload field names are unchanged, so the plugin's existing outcome handling remains valid. Only request construction is in scope.
- The AI Markup path (`/cbrd/parse`, delivered by feature 004) is unaffected and out of scope.
- Single-sourcing the plugin version so that hardcoded version strings cannot drift is feature 006, out of scope here. This feature only removes the *test assertion* that would turn red on a correct bump.
- Editors on older plugin builds continue to see the failure until they upgrade; no server-side or preference-level mitigation is available.
- The live contract facts underpinning this specification were verified against the production service on 2026-08-03 and are recorded, with the investigation that found them, in `exploration/ref2link_drift.md` — read §9 "Verified corrections" before §6.
