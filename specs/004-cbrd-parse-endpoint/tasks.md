# Tasks: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Input**: Design documents from `/specs/004-cbrd-parse-endpoint/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Included (TDD mandated by constitution Principle IV). Write tests FIRST, verify RED, then implement to GREEN.

Each test task carries a Principle IV classification tag: `[T-SPEC]` (encodes a spec acceptance
scenario), `[T-UNIT]` (domain/infrastructure unit), `[T-INTEGRATION]` (workspace/EDT/lifecycle).
🚨 TEST GATE and 🎯 PHASE/COMPLETION GATE tasks are blocking: no task after a gate may start
until the gate's condition holds. Implementation within each phase follows Domain → Application
→ Infrastructure → UI → resources.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Implementation Progress

**2026-08-01: 82/84 tasks complete. All eight 🚨 TEST GATEs and all five 🎯 PHASE GATEs passed.**
**`mvn test`: 351 tests, 0 failures** (baseline was 148 — +203 tests, 57 test classes).
**`mvn clean package`: BUILD SUCCESS**, Java 8 bytecode confirmed (class major version 52).

**2026-08-02: Phase 8 code tasks (T080–T082, T081a), Phase 9 (T090–T093a), Phase 9 follow-up (T094–T096), T097/T097a (FR-022 transport-error capture, schema 1.1.0), T098/T098a (Oxygen HTTP protocol handler status recovery), and T099/T099a (token label renamed "CBRD bearer token*:", token row moved to second, all six preference labels kept identical to English in every language) complete. `mvn test`: 365 tests, 0 failures**
(baseline 354 + T080 + T082's two no-status companions). The Java-17
`getErrorStream()` quirk is fixed (`CbrdParseApiClient` recovers the HTTP
status from the `IOException` message text and classifies 401 → UNAUTHORIZED);
the two Ref-to-Link preference labels are renamed to "CBRD Link Endpoint" and
"CBRD Link timeout (ms)" (display-only, FR-023/US5); the Parse token row is
renamed "CBRD bearer token*:", moved to row 2, and all six preference labels
ship identical to their English text in zh_CN/zh_TW (display-only, FR-023/US5);
and the 0.5.0 release notes fold in all changes (version NOT bumped).
T083 (manual quickstart S10 revalidation in Oxygen — gates T084) and T055 still
owed before release.

All of Phases 1-7 are done except the two items below, which cannot be completed in a headless
environment or with the current build configuration:

- **T055 (unchecked)** — the 10 quickstart scenarios need a running Oxygen XML Editor with a real
  DILA parse token. No Oxygen installation or display is available here, so this is a genuine
  manual step still owed before release. Everything it would exercise has automated coverage
  except the visual layout of the preferences page and a live call to the real endpoint.
- **T057 (unchecked)** — four of its five conditions hold: `mvn test` is fully green; no
  `MarkupServiceConfiguration`/`OpenAi*` symbol remains under `src/` (only the DILA service's own
  `openai_*` wire codes and explanatory comments); every FR-001…FR-022 and NFR-001…NFR-005 is
  referenced by at least one passing test; plan.md Complexity Tracking is signed. The fifth,
  **coverage ≥ 80%, could not be measured — `pom.xml` configures no coverage plugin** (no JaCoCo).
  Adding one changes the build for the whole project, so it is left as an explicit decision rather
  than assumed. Do not treat T057 as passed until coverage is measured or the criterion is
  formally dropped.

Two files were added beyond the task list, both justified:

- `src/test/java/com/dila/dama/plugin/infrastructure/api/CapturingConnectionFactory.java` — a shared
  HTTP test seam; five CBRD Parse test classes need it. Its stub fields are named
  `stubResponseCode`/`stubResponseBody` deliberately: `HttpURLConnection` declares a protected
  `responseCode`, and an inherited member shadows the outer class's field inside the inner fake.
- `src/main/java/com/dila/dama/plugin/infrastructure/api/CbrdParseRequest.java` — the component
  T025/T043 referenced with no creating task (finding U1 in the analysis report).

Deviations from the task list as written, all recorded at the time:

- **T025 became additive**, then the OpenAI overload was removed in T056a. Replacing the signature
  in place would have broken two 002 test classes mid-phase.
- **`DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest` was retargeted during T027**, not
  T056b: it pinned the OpenAI configuration shape that T027 replaces, so it could not wait.
- **`RequestValidationServiceTest` and `OpenAiCompatibleChatClientTest` were deleted** in T056b
  rather than retargeted — every case in them was OpenAI-shaped, and
  `RequestValidationServiceCbrdParseTest` covers the replacement path with 18 cases.
- **quickstart.md's package command was corrected** to `mvn clean install`: the assembly that
  produces `dilaAIMarkupPlugin.zip` is bound to the `install` phase, not `package`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- Maven project root: `Models/Gemini2.5/dila-ai-markup-plugin/`
- Main source: `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/`
- Test source: `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/`
- Resources: `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/`
- Paths below are relative to the Maven project root.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline and add shared preference/i18n scaffolding before story work.

- [X] T001 Verify current build passes (`mvn test`) and note baseline on branch `004-cbrd-parse-endpoint` — _measured 2026-08-01: `Tests run: 148, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS in 10.9 s. Re-verify before starting._
- [X] T002 [P] Add preference key constants `KEY_CBRD_PARSE_API_URL` (`"cbrd.parse.api.url"`), `KEY_CBRD_PARSE_TOKEN` (`"cbrd.parse.token"`), `KEY_CBRD_PARSE_TIMEOUT_MS` (`"cbrd.parse.timeout"`) with defaults `https://cbss.dila.edu.tw/cbrd/parse`, empty, `30000` (FR-016 — NOT the `10000` Ref-to-Link value) in `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T003 [P] Add i18n keys `cbrd.parse.api.url.label`, `cbrd.parse.token.label`, `cbrd.parse.timeout.ms.label` in all three languages (`en_US`, `zh_CN`, `zh_TW`) in `src/main/resources/i18n/translation.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain/infrastructure that MUST be complete before ANY user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

**⚠️ ADDITIVE ONLY**: No OpenAI-era class, overload, test, or i18n key may be deleted in this
phase. Twelve files still reference them; deleting here makes the module uncompilable and
`mvn test` unrunnable (constitution Principle X). All deletions happen in T056a–T056c.
See plan.md → Retirement Sequencing.

### Tests for Foundational (RED first)

- [X] T004 [P] [T-UNIT] Write `CbrdParseConfigurationTest` for endpoint URL/token fingerprint/timeout validation in `src/test/java/com/dila/dama/plugin/domain/model/CbrdParseConfigurationTest.java`
- [X] T004a [P] [T-UNIT] Write `CbrdParseTimeoutDefaultTest` asserting the timeout defaults to `30000` when the preference is unset and falls back to `30000` when unparseable, in `src/test/java/com/dila/dama/plugin/domain/model/CbrdParseTimeoutDefaultTest.java`
- [X] T005 [P] [T-UNIT] Write `ParseErrorTest` for all 9 enum codes plus `UNEXPECTED_RESPONSE` and `CONNECTIVITY_FAILURE` in `src/test/java/com/dila/dama/plugin/domain/model/ParseErrorTest.java`
- [X] T006 [P] [T-UNIT] Write `DocumentLanguageResolverTest` covering `zh`, `zh-Hant`, `ja-JP`, `jp`, missing/default in `src/test/java/com/dila/dama/plugin/domain/service/DocumentLanguageResolverTest.java`
- [X] T007 [P] [T-UNIT] Write `CbrdParseApiClientTest` mirroring `CBRDAPIClientTest` with `CapturingConnectionFactory` for 200/400/401/422/502/503/connectivity in `src/test/java/com/dila/dama/plugin/infrastructure/api/CbrdParseApiClientTest.java`
- [X] T007a [P] [T-UNIT] Write `CbrdParseTimeoutBehaviourTest` asserting `setConnectTimeout`/`setReadTimeout` receive the configured value and that `SocketTimeoutException` maps to the timeout/connectivity guidance (FR-016, US1 scenario 7) in `src/test/java/com/dila/dama/plugin/infrastructure/api/CbrdParseTimeoutBehaviourTest.java`
- [X] T007b [P] [T-UNIT] Write `CbrdParseRequestBodyTest` asserting the serialized body contains exactly `text` and `lang` and no system prompt/model/platform field (FR-006, `additionalProperties: false`) in `src/test/java/com/dila/dama/plugin/infrastructure/api/CbrdParseRequestBodyTest.java`
- [X] T008 [P] [T-UNIT] Write `CbrdParseErrorClassifierTest` mapping each `ParseError` to its expected i18n guidance key, and asserting `UNAUTHORIZED` does not reuse the missing-token key (FR-010) in `src/test/java/com/dila/dama/plugin/domain/service/CbrdParseErrorClassifierTest.java`
- [X] T008b [P] [T-UNIT] Write `RequestValidationServiceCbrdParseTest` for endpoint URL scheme/host rejection producing `ai.markup.error.endpoint_url_invalid` (FR-021) in `src/test/java/com/dila/dama/plugin/domain/service/RequestValidationServiceCbrdParseTest.java`
- [X] **T008c 🚨 TEST GATE** — run `mvn test`; confirm T004–T008b are all written and FAILING (RED). No Phase 2 implementation task may start until this holds.

### Foundational Implementation (GREEN)

**Order: Domain → Application → Infrastructure → UI → resources (constitution IV.3).**

_Domain_

- [X] T009 Implement `CbrdParseConfiguration` immutable value object with `endpointUrl`, `timeoutMs`, `sharedToken`, `getTokenFingerprint()`, `toRequestUri()` in `src/main/java/com/dila/dama/plugin/domain/model/CbrdParseConfiguration.java`
- [X] T010 Implement `ParseError` enum (9 service codes + `UNEXPECTED_RESPONSE` + `CONNECTIVITY_FAILURE`) in `src/main/java/com/dila/dama/plugin/domain/model/ParseError.java`
- [X] T011 Implement `DocumentLanguageResolver` (FR-007) reading `xml:lang` from the document root element only via `XmlDomUtils`, mapping `zh*`→`zh`, `ja*`/`jp*`→`jp`, default `zh` in `src/main/java/com/dila/dama/plugin/domain/service/DocumentLanguageResolver.java`
- [X] T013 Implement `CbrdParseErrorClassifier` mapping `ParseError` to localized guidance keys, with `UNAUTHORIZED` distinct from the missing-token key, in `src/main/java/com/dila/dama/plugin/domain/service/CbrdParseErrorClassifier.java`
- [X] T017 **Add** a `validate(CbrdParseConfiguration)` overload to `RequestValidationService` (URL scheme/host → `ai.markup.error.endpoint_url_invalid`, timeout > 0, token presence). Do NOT remove the OpenAI overload — it is deleted in T056a — in `src/main/java/com/dila/dama/plugin/domain/service/RequestValidationService.java`
- [X] T017a Add a `CbrdParseConfiguration` path to `AiMarkupDiagnosticSession` alongside the existing `MarkupServiceConfiguration` one; state machine unchanged. Update `src/test/java/com/dila/dama/plugin/domain/model/AiMarkupDiagnosticSessionTest.java` (3 refs) in `src/main/java/com/dila/dama/plugin/domain/model/AiMarkupDiagnosticSession.java`
- [X] T017b Add `ParseError` classification to `DiagnosticClassifier` (delegating to `CbrdParseErrorClassifier`), leaving the OpenAI branches in place until T056a. Update `src/test/java/com/dila/dama/plugin/domain/service/DiagnosticClassifierTest.java` (4 refs) in `src/main/java/com/dila/dama/plugin/domain/service/DiagnosticClassifier.java`

_Infrastructure_

- [X] T012b Implement `CbrdParseRequest` immutable request type (`text`, `lang`) serializing to exactly `{"text","lang"}` — `additionalProperties: false`, no system prompt or model field — in `src/main/java/com/dila/dama/plugin/infrastructure/api/CbrdParseRequest.java`
- [X] T012a Implement `CbrdParseResponse` success/failure result type returned by `CbrdParseApiClient` in `src/main/java/com/dila/dama/plugin/infrastructure/api/CbrdParseResponse.java`
- [X] T012 Implement `CbrdParseApiClient` using `HttpURLConnection` via `HttpUrlConnectionFactory`: POST `CbrdParseRequest` JSON, `Authorization: Bearer`, configured connect/read timeouts, parse `ParseError` JSON, return `CbrdParseResponse`, disconnect in `finally`, in `src/main/java/com/dila/dama/plugin/infrastructure/api/CbrdParseApiClient.java`

_UI and resources_

- [X] T014 [P] Remove the six OpenAI preference UI rows (parse model, detect model, API key, base URL, chat path, AI timeout) from `init`, `apply`, `restoreDefaults`, and the field declarations in `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T015 [P] Remove the verified-dead OpenAI helpers `processAIMarkup`, `createJSONRequest`, `parseOpenAIResponse` (confirmed: no callers) from `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`. Do NOT delete `MarkupServiceConfiguration`, `OpenAiCompatibleChatClient`, or `OpenAiErrorResponse` here — 12 files still reference them and the module would not compile (see T056a)
- [X] T016 Add CBRD Parse preference UI rows (endpoint URL, token as `JPasswordField`, timeout) and wire `getSecretOption`/`setSecretOption` for the token in `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java` (depends on T014 — same file)
- [X] T018 Add i18n failure guidance keys for all 9 `ParseError` codes plus `ai.markup.error.unexpected`, `ai.markup.error.connectivity`, `ai.markup.error.token_not_configured`, and `ai.markup.error.endpoint_url_invalid` in all three languages in `src/main/resources/i18n/translation.xml`
- [X] **T018a 🎯 PHASE GATE** — `mvn test` green; T004–T008b now pass; no OpenAI class deleted yet.

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Mark up references through the DILA service without any OpenAI key (Priority: P1) 🎯 MVP

**Goal**: An editor selects CBETA reference text, invokes AI Markup, the plugin calls the DILA CBRD Parse endpoint with the shared token, shows the returned TEI P5 XML, and applies it only on Replace.

**Independent Test**: On a fresh install with only CBRD Parse endpoint URL and shared token configured, select a passage and complete one AI Markup operation through Replace without seeing any OpenAI-related field.

### Tests for User Story 1 (RED first)

- [X] T019 [P] [US1] [T-UNIT] Write a new `RunAiMarkupDiagnosticsCommandTest` (no such test exists today) against `CbrdParseApiClient` + `CbrdParseConfiguration`, asserting the success path returns `<ref>` markup and that `execute` takes no `systemPrompt` argument, in `src/test/java/com/dila/dama/plugin/application/command/RunAiMarkupDiagnosticsCommandTest.java`
- [X] T020 [P] [US1] [T-SPEC] Write `AIMarkupSuccessFlowTest` verifying the result area shows the markup and the Replace button appears (US1 scenarios 1-2) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupSuccessFlowTest.java`
- [X] T021 [P] [US1] [T-SPEC] Write `AIMarkupReplaceUndoTest` mirroring `RefToLinkReplaceFlowTest`, verifying insertion and undo (US1 scenario 3) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupReplaceUndoTest.java`
- [X] T022 [P] [US1] [T-SPEC] Write `AIMarkupConcurrencyTest` verifying the second invocation is ignored and "already in progress" plus the in-flight selected XML are shown (US1 scenario 5) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupConcurrencyTest.java`
- [X] T023 [P] [US1] [T-SPEC] Write `AIMarkupCancelOnCloseTest` verifying the in-flight request is cancelled and the result discarded when the editor closes (US1 scenario 6) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupCancelOnCloseTest.java`
- [X] T023a [P] [US1] [T-SPEC] Write `AIMarkupConfigGuardTest` verifying the missing-token guard (FR-010) and the missing/malformed endpoint URL guard (FR-021, US4 scenario 11) each show their own distinct message and send no request, in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupConfigGuardTest.java`
- [X] T008a [P] [US1] [T-INTEGRATION] Write `AIMarkupAsyncResponsivenessTest` asserting the request runs off the EDT, UI updates marshal via `SwingUtilities.invokeLater`, and processing feedback appears within 500 ms (FR-014, NFR-002) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupAsyncResponsivenessTest.java` _(moved here from Phase 2 — its production code is T024–T033a)_
- [X] **T023b 🚨 TEST GATE** — `mvn test`; confirm T008a and T019–T023a are RED before any US1 implementation task starts.

### Implementation for User Story 1

- [X] T024 [US1] Update `buildAiMarkupConfiguration` to read `KEY_CBRD_PARSE_API_URL`, `KEY_CBRD_PARSE_TOKEN` (secure), `KEY_CBRD_PARSE_TIMEOUT_MS` and return a `CbrdParseConfiguration` in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T025 [US1] **Add** `RunAiMarkupDiagnosticsCommand.execute(CbrdParseRequest, CbrdParseConfiguration, platform)` — no `systemPrompt` parameter (FR-004, FR-006) — calling `CbrdParseApiClient` and returning the markup XML on success (unwrapped: the service already emits the whole `<ref>` element), in `src/main/java/com/dila/dama/plugin/application/command/RunAiMarkupDiagnosticsCommand.java`. Keep the OpenAI `execute` overload until T056a: `DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest` (3 mocks) and `...AsyncDiagnosticsTest` mock the old signature, so replacing it here would break the build mid-phase — the same constraint as plan.md → Retirement Sequencing.
- [X] T026 [US1] Update `AIMarkupActionListener.actionPerformed` to pre-validate an empty selection (FR-019) before sending, showing the input-missing guidance and returning early, in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T027 [US1] Update `runAiMarkup` (`DAMAWorkspaceAccessPluginExtension.java:1389-1392`) to resolve `lang` via `DocumentLanguageResolver`, build a `CbrdParseRequest`, and **remove the `i18n("system.prompt.ai.markup")` argument** (line 1391), in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T028 [US1] Update `completeAiMarkupOperation` to show the returned markup and the Replace button on success, preserving the review-then-replace workflow (FR-008), in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T029 [US1] Ensure `ReplaceButtonActionListener` inserts the CBRD Parse markup result via `replaceSelectionWithText` so undo history is preserved (FR-009), in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T030 [US1] Update the `tryStartAiMarkupOperation` rejection branch to store and display the in-flight selected XML with the "already in progress" message (FR-015) in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T031 [US1] Add client-side too-long selection pre-validation (4,000 chars) showing the too-long guidance without sending (FR-019) in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T032 [US1] Implement cancel-on-close: store the in-flight `CompletableFuture`/cancel handle, cancel and discard in `applicationClosing()` and when the editor page is unavailable (FR-020) in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T033 [US1] Add the missing-token guard message (FR-010) when `cbrd.parse.token` is empty, before sending the request, in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T033a [US1] Add the missing/malformed endpoint URL guard (FR-021) using `RequestValidationService.validate(CbrdParseConfiguration)`, showing `ai.markup.error.endpoint_url_invalid` without sending, in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] **T033b 🎯 PHASE GATE** — `mvn test` green; quickstart Scenario 1 passes manually in Oxygen.

**Checkpoint**: User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Existing users upgrade without losing their work (Priority: P2)

**Goal**: After upgrade, OpenAI fields are gone, CBRD Parse endpoint URL is prefilled, and the editor only needs to enter the shared token; obsolete values are ignored.

**Independent Test**: Upgrade a machine with old OpenAI preferences populated; confirm fields gone, endpoint prefilled, enter token, complete one AI Markup operation.

### Tests for User Story 2 (RED first)

- [X] T034 [P] [US2] [T-SPEC] Write `UpgradePreferencesTest` verifying OpenAI keys are ignored, the CBRD Parse URL is prefilled, the token is empty, and the timeout default is `30000` (US2 scenarios 1, 5) in `src/test/java/com/dila/dama/plugin/preferences/UpgradePreferencesTest.java`
- [X] T035 [P] [US2] [T-SPEC] Write `UpgradeNoMigrationTest` verifying the old API key/base URL are not read or sent when the token is configured (US2 scenarios 2-3) in `src/test/java/com/dila/dama/plugin/workspace/UpgradeNoMigrationTest.java`
- [X] T035a [P] [US2] [T-INTEGRATION] Write `PreferencePageLayoutOrderTest` asserting the remaining preference rows (CBRD Link, CBRD Parse) preserve ordering after the OpenAI rows are removed (NFR-005) in `src/test/java/com/dila/dama/plugin/preferences/PreferencePageLayoutOrderTest.java`
- [X] T035b [P] [US2] [T-SPEC] Write `SharedTokenNonDisclosureTest` asserting the token field is a masked `JPasswordField`, the token is stored via `setSecretOption`, and it appears in logs/diagnostics only as a fingerprint (FR-003, NFR-004, US2 scenario 4) in `src/test/java/com/dila/dama/plugin/preferences/SharedTokenNonDisclosureTest.java`
- [X] **T035c 🚨 TEST GATE** — `mvn test`; confirm T034–T035b are RED.

### Implementation for User Story 2

- [X] T036 [US2] Ensure `restoreDefaults` sets the CBRD Parse defaults and does not revive OpenAI defaults in `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T037 [US2] Ensure `apply` persists only the CBRD Parse keys (secure token) and ignores obsolete OpenAI keys in `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T038 [US2] Verify `buildAiMarkupConfiguration` never reads the removed `dila.dama.*` keys in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] **T039a 🎯 PHASE GATE** — `mvn test` green; quickstart Scenario 7 passes.

> _(Former T039 "add i18n key for upgrade/migration guidance if needed" was removed on 2026-08-01: no requirement calls for an upgrade-guidance message — US2's contract is that obsolete values are silently ignored. If one is wanted later, add it with a named key and a matching FR.)_

**Checkpoint**: User Stories 1 and 2 should both work independently

---

## Phase 5: User Story 3 - Document language drives the request automatically (Priority: P3)

**Goal**: The plugin sends `zh` or `jp` based on the document root `xml:lang`, ignoring nested ancestors, with `zh` default when missing/unusable.

**Independent Test**: Open documents with `xml:lang="jp"` and `xml:lang="zh"`, invoke AI Markup, verify each request carries the matching language indicator without user configuration.

### Tests for User Story 3 (RED first)

- [X] T040 [P] [US3] [T-UNIT] Extend `DocumentLanguageResolverTest` for nested-ancestor divergence (document root wins, US3 scenario 4) in `src/test/java/com/dila/dama/plugin/domain/service/DocumentLanguageResolverTest.java`
- [X] T041 [P] [US3] [T-SPEC] Write `AIMarkupLanguageIntegrationTest` verifying the request `lang` matches the document root for `zh`, `jp`, and default (US3 scenarios 1-3, 5) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupLanguageIntegrationTest.java`
- [X] **T041a 🚨 TEST GATE** — `mvn test`; confirm T040–T041 are RED.

### Implementation for User Story 3

- [X] T042 [US3] Verify `DocumentLanguageResolver` uses `getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang")` on the document root only, not the selected element or ancestors, in `src/main/java/com/dila/dama/plugin/domain/service/DocumentLanguageResolver.java`
- [X] T043 [US3] Wire `DocumentLanguageResolver` into `runAiMarkup` by reading the current editor page document root element's `xml:lang` and passing the resolved `lang` to `CbrdParseRequest` in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T044 [US3] [T-UNIT] Add a unit test capturing the actual request JSON `lang` field via `CapturingConnectionFactory` for `zh` and `jp` documents in `src/test/java/com/dila/dama/plugin/infrastructure/api/CbrdParseApiClientLanguageTest.java`
- [X] **T044a 🎯 PHASE GATE** — `mvn test` green; quickstart Scenario 2 passes.

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: User Story 4 - Every parse failure tells the editor what to do (Priority: P4)

**Goal**: Each enumerated DILA service failure cause produces a distinct, actionable result-area message; unknown responses and connectivity failures have distinct fallbacks.

**Independent Test**: Trigger each enumerated DILA service failure mode and verify each produces a distinct, actionable AI Markup result-area message.

### Tests for User Story 4 (RED first)

- [X] T045 [P] [US4] [T-SPEC] Write `AIMarkupFailureMappingTest` verifying each `ParseError` code yields the correct localized guidance key in the result area (US4 scenarios 1-4) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupFailureMappingTest.java`
- [X] T046 [P] [US4] [T-SPEC] Write `AIMarkupConnectivityFailureTest` verifying `IOException`/`SocketTimeoutException` yields a distinct connectivity message (FR-013, US4 scenario 5) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupConnectivityFailureTest.java`
- [X] T047 [P] [US4] [T-SPEC] Write `AIMarkupUnexpectedResponseTest` verifying an unknown status/body yields the generic message with no crash and no document mutation (US4 scenario 8, FR-012) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupUnexpectedResponseTest.java`
- [X] T047a [P] [US4] [T-SPEC] Write `AIMarkupDiagnosticsExportTest` verifying a parse failure offers Export Diagnostics and that the exported package contains the token only as a fingerprint (FR-022, FR-003, US4 scenario 9) in `src/test/java/com/dila/dama/plugin/workspace/AIMarkupDiagnosticsExportTest.java`
- [X] **T047b 🚨 TEST GATE** — `mvn test`; confirm T045–T047a are RED.

### Implementation for User Story 4

- [X] T048 [US4] Update the `RunAiMarkupDiagnosticsCommand` failure path to use `CbrdParseErrorClassifier` and build a `SanitizedTroubleshootingRecord` for export (FR-022) in `src/main/java/com/dila/dama/plugin/application/command/RunAiMarkupDiagnosticsCommand.java`
- [X] T049 [US4] Update the `completeAiMarkupOperation` failure branch to show the classified guidance message and the Export Diagnostics button when a record exists (FR-011, FR-022) in `src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T050 [US4] Ensure `SecretRedactor` redacts the Bearer token in request traces for diagnostic export (FR-003, NFR-004) in `src/main/java/com/dila/dama/plugin/domain/service/SecretRedactor.java`
- [X] T051 [US4] Verify all 9 failure guidance keys plus `ai.markup.error.unexpected`, `ai.markup.error.connectivity`, `ai.markup.error.token_not_configured`, and `ai.markup.error.endpoint_url_invalid` exist in `en_US`, `zh_CN`, `zh_TW` in `src/main/resources/i18n/translation.xml`
- [X] **T051a 🎯 PHASE GATE** — `mvn test` green; quickstart Scenarios 6, 9, 10 pass.

**Checkpoint**: All user stories complete; failure surface fully mapped

---

## Phase 7: Polish, Cross-Cutting Concerns & Retirement

**Purpose**: Improvements that affect multiple user stories, then the deferred deletion of the OpenAI-era surface.

- [X] T052 [P] [T-UNIT] Update `TranslationBundleCompletenessTest` to assert all new keys across `en_US`/`zh_CN`/`zh_TW` — the 9 `ParseError` keys, `ai.markup.error.unexpected`, `ai.markup.error.connectivity`, `ai.markup.error.token_not_configured`, `ai.markup.error.endpoint_url_invalid`, and the three preference labels (NFR-001, FR-018, US4 scenario 10) — in `src/test/java/com/dila/dama/plugin/infrastructure/i18n/TranslationBundleCompletenessTest.java`
- [X] T053 [P] Run `mvn test` and confirm zero regressions in the named FR-017 suites: `UTF8ValidationServiceTest`, `ConvertReferenceCommandTest`, `ReferenceParserTest`, `ComponentTransformerTest`, `NumeralConverterTest`, `RefElementRewriterTest`, `ReferenceValidationTest`, `CBRDAPIClientTest`, `CBRDAPIClientErrorHandlingTest`, `CBRDResponseTest`, `RefToLinkReplaceFlowTest`, `DiagnosticClassifierPlatformParityTest` (US2 scenario 6)
- [X] T053a [P] [T-INTEGRATION] Write `ExecutorShutdownTest` asserting `executor.shutdown()` is called in `applicationClosing()` after the retarget, using `isExecutorShutdownForTests()` (NFR-003) in `src/test/java/com/dila/dama/plugin/workspace/ExecutorShutdownTest.java`
- [X] T054 Update `release-notes.xhtml` with the CBRD Parse endpoint change, the six removed preferences, and the new timeout default in `src/main/resources/release-notes.xhtml`
- [X] T054a Bump `pom.xml` `<version>` to `0.5.0` to match the release-notes heading T054 added. `pom.xml` is the single source: resource filtering substitutes `${project.version}` into `plugin.xml` and `extension.xml`, so no other file needs editing. Add `VersionConsistencyTest` in `src/test/java/com/dila/dama/plugin/infrastructure/release/VersionConsistencyTest.java` asserting the newest `<h4>` matches the declared version, so the two can never ship out of step again. _MINOR bump per project convention: feature-sized changes took MINOR (0.3.0 Java migration, 0.4.0 Ref-to-Link)._
- [X] T054b Bump the hardcoded `USER_AGENT` in `CbrdParseApiClient` to `0.5.0` — this feature's own new code, so shipping a knowingly-wrong header is not acceptable. `src/main/java` is NOT resource-filtered, so `${project.version}` cannot reach it; a `TODO(005)` marks it for the single-source fix. **`CBRDAPIClient`'s stale `DILA-AI-Markup/0.4.2` is deliberately left alone**: FR-017 requires Ref-to-Link behaviour to be unchanged by this feature, and its header is part of that behaviour. Both literals are fixed together in the follow-up feature.
- [ ] T055 Run all 10 quickstart.md validation scenarios end-to-end in Oxygen
- [X] T056 Remove remaining OpenAI references in imports and comments across `src/main/java/com/dila/dama/plugin/`

### Retirement (deletion is only safe here — see plan.md Retirement Sequencing)

**Run `mvn test` after each task in this block.**

- [X] T056a Delete the retired OpenAI classes now that every call site is retargeted: `src/main/java/com/dila/dama/plugin/domain/model/MarkupServiceConfiguration.java`, `src/main/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClient.java`, `src/main/java/com/dila/dama/plugin/infrastructure/api/OpenAiErrorResponse.java`; remove the OpenAI overloads from `RequestValidationService` and `DiagnosticClassifier` and the OpenAI configuration path from `AiMarkupDiagnosticSession`
- [X] T056b Update or delete the 8 test classes coupled to the retired types — delete `src/test/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClientTest.java`; retarget `RequestValidationServiceTest` (6 refs), `DiagnosticClassifierTest` (4), `AiMarkupDiagnosticSessionTest` (3), `DAMAWorkspaceAccessPluginExtensionTest` (3), `DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest` (9), `DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest` (8), `DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest` (4) to the CBRD Parse types
- [X] T056c Retire the 5 obsolete i18n keys (`system.prompt.ai.markup`, `error.no.APIKey`, `error.no.parse.model`, `http.error`, `llm.error`) from `src/main/resources/i18n/translation.xml`; remove them from the `isErrorMessage` key array (`DAMAWorkspaceAccessPluginExtension.java:1291-1316`) and **add the new `ai.markup.error.*` keys to that array** so failure messages are still recognized; update `src/test/java/com/dila/dama/plugin/i18n/LocalizationTest.java` (lines 49, 53) and `src/test/java/com/dila/dama/plugin/test/TranslationConsistencyTest.java`

### Post-Retirement Remediation (Phase 8 — S10 Java-17 Quirk)

Discovered during 2026-08-02 quickstart validation: S10 (wrong bearer token) surfaced the FR-013 connectivity message inside Oxygen instead of the FR-011 `unauthorized` guidance. Root-caused to the Java-17 `HttpURLConnection.getErrorStream()` quirk (see `spec.md` Clarifications 2026-08-02, US4 scenario 12, Edge Cases entry; `plan.md` "Java-17 `getErrorStream()` Quirk"). All tasks must keep `mvn test` green per Principle X. Baseline before remediation: **354 tests, 0 failures**, captured 2026-08-02.

- [X] **T080 🎯 TEST GATE (RED)** — Write `CbrdParseApiClientTest#unauthorizedOnJava17GetErrorStreamThrow` (and a sibling `unauthorizedOnJava17GetErrorStreamThrowRecoversStatusFromExceptionMessageWhenResponseCodeThrows` if `getResponseCode()` itself also throws on Java 17). Inject a `CapturingConnectionFactory` whose `HttpURLConnection` mock (a) returns 401 from `getResponseCode()` when called, and (b) throws `IOException("Server returned HTTP response code: 401 for URL: https://cbss.dila.edu.tw/cbrd/parse")` from `getErrorStream()`. Assert `CbrdParseResponse.getStatus() == 401`, `CbrdParseResponse.getError() == ParseError.UNAUTHORIZED`, `diagnosticFailureCategory() == CREDENTIALS`, and that the returned redacted `serviceErrorBody` is non-empty (so the export carries a cause string). Run `mvn test -Dtest=CbrdParseApiClientTest` and confirm the new test FAILS RED; no Phase 8 fix may land until this gate is RED. Expected-failure reason: today's catch block stamps `CONNECTIVITY_FAILURE`. Tags: [P] [US4] [T-UNIT].
- [X] T081 [US4] Implement status-recovery in `CbrdParseApiClient.execute` — extract a side helper `recoverStatusFromException(Throwable, int priorStatus)` that returns `priorStatus` when non-zero, else parses the canonical `Server returned HTTP response code: (\d{3}) for URL:` regex out of `e.getMessage()` (returns `<0` when no status is recoverable). Refactor the `catch (Exception e)` path so that when a recoverable status is present, the response is stamped via a status-known classification (401 → `UNAUTHORIZED`, 5xx → `UNEXPECTED_RESPONSE`, etc.) **with a non-empty `serviceErrorBody`** describing the JDK quirk ("Java-17 getErrorStream threw IOException; status recovered from exception message") rather than `""` — so the export record retains a meaningful cause; only when no status is recoverable (DNS/TLS/timeout) does it remain `CONNECTIVITY_FAILURE`. **Do not** change `ParseError` enum values, `HttpUrlConnectionFactory`, or any FR-011 wire-code mapping. Update the existing 200/400/401/422/502/503/connectivity tests to stay green; the RED test from T080 now turns GREEN. Tags: [US4].
- [X] **T081a 🎯 GATE** — `mvn test -Dtest=CbrdParseApiClientTest` green (T080 test now GREEN).
- [X] T082 [US4] Update `CbrdParseApiClientTest` to add at least two companion cases guarding the no-status path is still classified as connectivity: (a) `SocketTimeoutException` from `getResponseCode()` → `CONNECTIVITY_FAILURE` with empty `serviceErrorBody`; (b) `UnknownHostException` from `getResponseCode()` → `CONNECTIVITY_FAILURE`. These pin FR-013 against accidental widening under T081. Tags: [P] [US4] [T-UNIT].
- [ ] T083 [US4] Manual quickstart revalidation: re-run `quickstart.md` S10 (wrong token) and S6 (`unauthorized` row) in Oxygen on Java 17. Expected: result area shows `ai.markup.error.unauthorized` (zh_TW: "DILA 解析服務拒絕此存取權杖…"), Export Diagnostics control visible, diagnostics export JSON shows `failureCategory: CREDENTIALS`, `guidanceMessageKey: ai.markup.error.unauthorized`, `serviceErrorBody` non-empty, `token=****XXXX` fingerprint, `redactionApplied: true`. Record the signed-off result against S10 in the validation matrix referenced by `quickstart.md`. Tags: [US4] [T-MANUAL].
- [ ] **T084 🎯 COMPLETION GATE** — `mvn test` fully green with ≥ 356 tests (354 baseline + T080 + T082's additional cases); no `OpenAi*` symbol remains; `plan.md` Phase 8 section matches the shipped code; T083 signed off; Coverage ≥ 80% preserved.

### Phase 9 — Preference Page Label Renovation

Discovered during the 2026-08-02 review: the three Ref-to-Link rows were left labelled "CBRD API URL"/"CBRD timeout (ms)" after the CBRD Parse surface was added, so the page no longer distinguishes the Link lookup from the Parse endpoint. Scope decision (feature owner, 2026-08-02): rename **exactly two displayed labels** — `cbrd.api.url.label` → "CBRD Link Endpoint" and `cbrd.timeout.ms.label` → "CBRD Link timeout (ms)" — display-only, no storage-key/layout/default changes (see `spec.md` Clarifications 2026-08-02 Phase 9, US5, FR-023; `plan.md` "Phase 9: Preference Page Label Renovation"). All tasks keep `mvn test` green per Principle X; version stays `0.5.0`.

- [X] **T090 🚨 TEST GATE (RED)** — Update `src/test/java/com/dila/dama/plugin/preferences/PreferencePageLayoutOrderTest.java` so `remainingRowsAreCbrdLinkThenCbrdParse` expects `"CBRD Link Endpoint:"`, `"CBRD Referer header:"`, `"CBRD Link timeout (ms):"`, `"CBRD Parse endpoint URL:"`, `"CBRD Parse token*:"`, `"CBRD Parse timeout (ms):"` and `refToLinkRowsAreUntouchedByThisFeature` expects `"CBRD Link Endpoint:"`, `"CBRD Referer header:"`, `"CBRD Link timeout (ms):"`. Do NOT touch the storage-key assertions (`refToLinkPreferenceKeysAndDefaultsAreUnchanged`, `refToLinkTimeoutIsSeparateFromTheParseTimeout`, `pageKeyAndTitleAreUnchanged`) — those pin FR-023/NFR-005. Run `mvn test -Dtest=PreferencePageLayoutOrderTest` and confirm the new expectations FAIL RED (the fallback switch still returns the old strings); no Phase 9 fix may land until this gate is RED. Tags: [P] [US5] [T-SPEC].
- [X] T091 [US5] Rename the two displayed labels (display-only, FR-023):
  - `src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java` — `getMessage` fallback switch: `case "cbrd.api.url.label"` returns `"CBRD Link Endpoint:"`, `case "cbrd.timeout.ms.label"` returns `"CBRD Link timeout (ms):"`.
  - `src/main/resources/i18n/translation.xml` — `cbrd.api.url.label`: en_US `CBRD Link Endpoint:`, zh_CN/zh_TW `CBRD 連結端點：`; `cbrd.timeout.ms.label`: en_US `CBRD Link timeout (ms):`, zh_CN/zh_TW `CBRD 連結逾時 (毫秒)：`.
  - Do NOT rename the i18n keys, the storage keys (`cbrd.api.url`, `cbrd.referer.header`, `cbrd.timeout`), the row order, the defaults, or the Referer row. Do NOT touch the internal log/exception strings that mention "CBRD API URL"/"CBRD timeout". Tags: [US5].
- [X] **T091a 🎯 GATE** — `mvn test -Dtest=PreferencePageLayoutOrderTest` green (T090 now GREEN); `UpgradePreferencesTest` (same page, no label assertions beyond "noneMatch" OpenAI rows) stays green.
- [X] T092 [US5] Full no-regression: run `mvn test` — full suite green at 356 tests (no test count change: this phase edits existing assertions and i18n values only); FR-017 suites (`CBRDAPIClientTest`, `RefToLinkReplaceFlowTest`, etc.) green; `TranslationBundleCompletenessTest` green (keys unchanged). Tags: [US5].
- [X] T093 [US5] Update `src/main/resources/release-notes.xhtml` 0.5.0 entry (in place, version NOT bumped): note the two renamed preference labels ("CBRD Link Endpoint", "CBRD Link timeout (ms)") alongside the CBRD Parse fields already listed, and fold in the Phase 8 unauthorized-classification fix. Run `VersionConsistencyTest` — `pom.xml` stays `0.5.0`, newest `<h4>` stays `v0.5.0`. Tags: [US5].
- [X] **T093a 🎯 COMPLETION GATE** — `mvn test` fully green (356 tests); `PreferencePageLayoutOrderTest`, `VersionConsistencyTest`, `TranslationBundleCompletenessTest` green; `spec.md`/`plan.md`/`tasks.md` Phase 9 sections match the shipped code; no version bump.

- [X] T094 [US5] Reorder the six preference rows (display-only, Session 2026-08-02 Phase 9 — row order): in `DAMAOptionPagePluginExtension.init()`, render top to bottom "CBRD Referer header:", "CBRD Parse endpoint URL:", "CBRD Parse token*:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):". Do NOT touch storage keys, defaults, labels, or the Referer row's value. Tags: [US5].
- [X] **T094a 🎯 GATE** — `mvn test -Dtest=PreferencePageLayoutOrderTest,UpgradePreferencesTest` green with the reordered assertions (`rowsFollowTheDocumentedOrder`, updated `refToLinkPreferenceKeysAndDefaultsAreUnchanged` / `refToLinkTimeoutIsSeparateFromTheParseTimeout` / `endpointUrlIsPrefilledAndTheTokenIsEmpty` indices); `SharedTokenNonDisclosureTest` indices updated (token now row 2).
- [X] T095 [US4] Harden wrong-token classification (FR-011 follow-up): in `CbrdParseApiClient`, classify a non-2xx response with an empty/unreadable error body from the HTTP status (so 401 with no body → `UNAUTHORIZED`, never `CONNECTIVITY_FAILURE` or generic `unexpected`); add regression tests for (a) `getResponseCode()` itself throwing the canonical `IOException("Server returned HTTP response code: 401 for URL: ...")` and (b) 401 with no error body. Tags: [US4].
- [X] **T095a 🎯 GATE** — `mvn test -Dtest=CbrdParseApiClientTest` green at 22 tests; full `mvn test` green at 358 tests, 0 failures.
- [X] T096 [US5] Update `src/main/resources/release-notes.xhtml` 0.5.0 entry (in place, version NOT bumped): note the preference row reorder alongside the label renames and the Phase 8/9 401-classification fixes. Run `VersionConsistencyTest` — `pom.xml` stays `0.5.0`. Tags: [US5].
- [X] T097 [US4] Capture the transport cause in the diagnostics export (FR-022): add a sanitized, redacted `transportError` field (exception class + message chain, ≤1000 chars) to `SanitizedTroubleshootingRecord`, thread `CbrdParseResponse.getException()` through `RunAiMarkupDiagnosticsCommand` into the record, serialize it in `DiagnosticExportWriter`, and bump the export schema to `1.1.0` in `BuildDiagnosticExportQuery`. A connectivity failure (e.g. `SocketTimeoutException: connect timed out`) must appear in the export instead of only the opaque `CONNECTIVITY_OR_PROXY` category. Tags: [US4].
- [X] **T097a 🎯 GATE** — `mvn test -Dtest=RunAiMarkupDiagnosticsCommandTest,DiagnosticExportWriterTest,BuildDiagnosticExportQueryTest,AiMarkupDiagnosticSessionTest` green (add: connectivity error named in record, redaction of a token inside the error, no `transportError` for a known HTTP status, export serializes/omits the field); full `mvn test` green at 362 tests, 0 failures.
- [X] T098 [US4] **🚨 TEST GATE (RED)** — In `src/test/java/com/dila/dama/plugin/infrastructure/api/CbrdParseApiClientTest.java`, add `unauthorizedWhenOxygenHttpProtocolThrowsFromGetResponseCode` (an `IOException` with message `"401 Unauthorized for: " + ENDPOINT` thrown from `getResponseCode()`, the message format Oxygen's own stream handler `ro.sync.net.protocol.http.HttpExceptionWithDetails` produces) expecting `ParseError.UNAUTHORIZED`/status 401, and `multiWordReasonInOxygenHttpExceptionStillRecoversTheStatus` (`"503 Service Unavailable for: " + ENDPOINT` → status 503, NOT `CONNECTIVITY_FAILURE`). Run `mvn test -Dtest=CbrdParseApiClientTest` and confirm both FAIL RED (the current client only matches the JDK canonical message); no client change may land until this gate is RED. Tags: [US4] [T-UNIT].
- [X] T098a [US4] **🎯 GATE (GREEN)** — In `CbrdParseApiClient`, add `OXYGEN_HTTP_STATUS_FROM_EXCEPTION_MESSAGE = Pattern.compile("(\\d{3})\\s+[A-Za-z][A-Za-z ]*\\s+for:")` and refactor `recoverStatusFromException` to try both patterns per message (JDK then Oxygen) via a small `parseStatusFromMessage` helper; generalize the recovered cause string to `"No readable response body; status NNN recovered from exception message"`. `mvn test -Dtest=CbrdParseApiClientTest` green at 24 tests; full `mvn test` green at 364 tests, 0 failures; `VersionConsistencyTest` green (version stays `0.5.0`); release-notes 0.5.0 entry updated for the Oxygen handler. Tags: [US4].

- [X] T099 [US5] **🚨 TEST GATE (RED)** — In `src/test/java/com/dila/dama/plugin/preferences/PreferencePageLayoutOrderTest.java`, change `rowsFollowTheDocumentedOrder` to expect the new order ("CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):") and update the row-index comment in `refToLinkPreferenceKeysAndDefaultsAreUnchanged`; in `src/test/java/com/dila/dama/plugin/preferences/UpgradePreferencesTest.java`, update `endpointUrlIsPrefilledAndTheTokenIsEmpty` so the token (`JPasswordField`) is field index 1 and the endpoint URL is index 2; add a test asserting the zh_CN/zh_TW value of each of the six `cbrd.*.label` keys (`cbrd.referer.label`, `cbrd.parse.token.label`, `cbrd.parse.api.url.label`, `cbrd.parse.timeout.ms.label`, `cbrd.api.url.label`, `cbrd.timeout.ms.label`) is exactly identical to its en_US value; update the field-index assumptions in `SharedTokenNonDisclosureTest` (`fields.get(2)` → `fields.get(1)`) and the untranslated-label exemption in `TranslationXmlValidatorTest`. Run `mvn test -Dtest=PreferencePageLayoutOrderTest,UpgradePreferencesTest,TranslationBundleCompletenessTest` and confirm the label/order/translation assertions FAIL RED (the page still shows "CBRD Parse token*:" at row 3 with the old Chinese labels); no client change may land until this gate is RED. Tags: [US5] [T-UNIT].

- [X] T099a [US5] **🎯 GATE (GREEN)** — In `src/main/resources/i18n/translation.xml`, set the zh_CN/zh_TW values of all six `cbrd.*.label` keys to exactly their en_US text ("CBRD Referer header:", "CBRD bearer token*:", "CBRD Parse endpoint URL:", "CBRD Parse timeout (ms):", "CBRD Link Endpoint:", "CBRD Link timeout (ms):"); update the fallback for `cbrd.parse.token.label` in `DAMAOptionPagePluginExtension` to `"CBRD bearer token*:"`; swap the token row block (label + `JPasswordField`) above the endpoint URL row block in the option page builder so the order is Referer, token, Parse URL, Parse timeout, Link Endpoint, Link timeout. `mvn test -Dtest=PreferencePageLayoutOrderTest,UpgradePreferencesTest,TranslationBundleCompletenessTest,SharedTokenNonDisclosureTest,TranslationXmlValidatorTest` green; full `mvn test` green at 365 tests, 0 failures; `VersionConsistencyTest` green (version stays `0.5.0`); release-notes 0.5.0 entry updated for the token label rename + row swap + English-only labels. Tags: [US5].

- [ ] **T057 🎯 COMPLETION GATE** — `mvn test` fully green; no `MarkupServiceConfiguration` or `OpenAi*` symbol remains under `src/`; every FR-001…FR-023 and NFR-001…NFR-005 traced to a passing test or a signed-off manual quickstart scenario; `pom.xml` `<version>` matches the newest `release-notes.xhtml` heading (enforced by `VersionConsistencyTest`); coverage ≥ 80% (constitution Enforcement); plan.md Complexity Tracking approval recorded.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories. Additive only: no OpenAI class, overload, test, or i18n key may be deleted here.
- **User Stories (Phase 3–6)**: All depend on Phase 2 completion (gate T018a)
  - US1 can proceed first (MVP)
  - US2, US3, US4 may proceed after US1, or in parallel if team capacity allows
- **Polish (Phase 7)**: T052–T056 depend on all desired user stories being complete
- **Retirement (T056a–T056c)**: depend on T017, T017a, T017b, T024, T025, T027, and T048 all being complete — deleting earlier breaks the build (12 referencing files)
- **Phase 8 (T080–T084)**: post-retirement remediation discovered during 2026-08-02 quickstart validation — depends on T057 being achievable; T080 must be RED before T081 starts (BDD RED-GREEN), T081a gates T082, T082 gates T083, T083 (manual S10) gates T084
- **Phase 9 (T090–T093a)**: preference page label renovation decided 2026-08-02 — depends on T084 (the Parse surface it disambiguates); T090 must be RED before T091 starts (BDD RED-GREEN), T091a gates T092, T092 gates T093, T093a is the completion gate
- **Phase 9 follow-up (T094–T096)**: row reorder + wrong-token classification hardening decided 2026-08-02 — depends on T093a; T094 before T094a, T095 before T095a, T096 after both gates
- **Phase 9 follow-up 2 (T097–T097a)**: FR-022 transport-error capture decided 2026-08-02 (live Oxygen still showed `CONNECTIVITY_OR_PROXY`; export lacked the exception) — depends on T096; T097 before T097a
- **Phase 9 follow-up 3 (T098–T098a)**: Oxygen HTTP protocol handler status recovery decided 2026-08-02 (S11 export's `transportError` revealed `HttpExceptionWithDetails: 401 Unauthorized for: <url>`) — depends on T097a; T098 (RED) before T098a (GREEN)
- **Phase 9 follow-up 4 (T099–T099a)**: token label renamed "CBRD bearer token*:" with zh_CN/zh_TW kept identical to English + token row moved to second (display-only, FR-023/US5) decided 2026-08-02 — depends on T098a; T099 (RED) before T099a (GREEN)
- **T057** is the final gate; nothing follows it

### Gate Order

T008c (RED) → T018a → T023b (RED) → T033b → T035c (RED) → T039a → T041a (RED) → T044a → T047b (RED) → T051a → T057

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational — no dependencies on other stories
- **User Story 2 (P2)**: Depends on Foundational — independently testable; verifies upgrade behavior
- **User Story 3 (P3)**: Depends on Foundational (`DocumentLanguageResolver`); independently testable
- **User Story 4 (P4)**: Depends on Foundational (`CbrdParseErrorClassifier`); independently testable

### Within Each User Story

- Tests MUST be written first and FAIL before implementation (enforced by the 🚨 TEST GATE tasks)
- GREEN order: Domain → Application → Infrastructure → UI → resources
- Core implementation before edge cases
- Story complete before moving to next priority

### Parallel Opportunities

- T002/T003 (different files)
- T004–T008b foundational tests
- T009/T010/T011/T013 domain implementations (different files)
- T012b and T012a before T012 (T012 consumes both)
- T017/T017a/T017b (different files)
- T014/T015 (different files); T016 must follow T014 (same file)
- T008a and T019–T023a US1 tests
- T034–T035b US2 tests
- T040/T041 US3 tests
- T045–T047a US4 tests
- T052/T053/T053a polish tasks
- T056a → T056b → T056c are sequential, not parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "RunAiMarkupDiagnosticsCommandTest in src/test/java/.../application/command/RunAiMarkupDiagnosticsCommandTest.java"
Task: "AIMarkupSuccessFlowTest in src/test/java/.../workspace/AIMarkupSuccessFlowTest.java"
Task: "AIMarkupReplaceUndoTest in src/test/java/.../workspace/AIMarkupReplaceUndoTest.java"
Task: "AIMarkupConcurrencyTest in src/test/java/.../workspace/AIMarkupConcurrencyTest.java"
Task: "AIMarkupCancelOnCloseTest in src/test/java/.../workspace/AIMarkupCancelOnCloseTest.java"
Task: "AIMarkupConfigGuardTest in src/test/java/.../workspace/AIMarkupConfigGuardTest.java"
Task: "AIMarkupAsyncResponsivenessTest in src/test/java/.../workspace/AIMarkupAsyncResponsivenessTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories; additive only)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently via quickstart.md Scenario 1
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → MVP
3. Add User Story 2 → Test upgrade path independently
4. Add User Story 3 → Test language inference independently
5. Add User Story 4 → Test failure mapping independently
6. Polish, then Retirement (T056a–T056c) → Full feature complete

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (MVP)
   - Developer B: User Story 2 (upgrade)
   - Developer C: User Story 3 (language)
   - Developer D: User Story 4 (failures)
3. Stories complete and integrate independently
4. Retirement is done by one developer after all stories land — it touches shared test classes

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- `[T-SPEC]`/`[T-UNIT]`/`[T-INTEGRATION]` tags satisfy constitution Principle IV test classification
- Each user story is independently completable and testable
- Verify tests fail before implementing (TDD Red-Green-Refactor) — the 🚨 TEST GATE tasks make this explicit
- `mvn test` must be green at the end of every task, and RED at every 🚨 TEST GATE by design
- Deletion of OpenAI-era classes, tests, and i18n keys happens ONLY in T056a–T056c. Earlier deletion leaves the module uncompilable and `mvn test` unrunnable (constitution Principle X)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Preserve the existing diagnostics export pipeline (002) and Ref-to-Link/UTF-8/tag-removal behavior
