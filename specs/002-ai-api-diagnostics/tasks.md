---

description: "Task list for implementing cross-platform AI Markup diagnostics and About/help-menu release visibility"
---

# Tasks: Cross-Platform API Diagnostics and Support Visibility

**Input**: Design documents from `/specs/002-ai-api-diagnostics/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Explicit test-first coverage is required for this feature. Each user story includes specification, unit, and integration coverage tasks that must be written before implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g. `[US1]`, `[US2]`, `[US3]`)
- Include exact file paths in descriptions
- Test-first tasks use constitution-aligned IDs: `T###-T-SPEC`, `T###-T-UNIT`, and `T###-T-INTEGRATION`

## Path Conventions

- Plugin module root: `Models/Gemini2.5/dila-ai-markup-plugin/`
- Production code: `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/`
- Test code: `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/`
- Feature docs: `specs/002-ai-api-diagnostics/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare feature-specific documentation scaffolding and test placeholders needed for strict test-first execution.

- [X] T001 Create acceptance-matrix directory and placeholder spec test files in `specs/002-ai-api-diagnostics/acceptance/`
- [X] T002 Create feature package directories under `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/`, `domain/service/`, `application/command/`, `application/query/`, `infrastructure/api/`, `infrastructure/export/`, `infrastructure/logging/`, and `infrastructure/release/`
- [X] T003 [P] Create feature test package directories under `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/model/`, `domain/service/`, `application/command/`, `application/query/`, `infrastructure/api/`, `infrastructure/export/`, `infrastructure/i18n/`, `infrastructure/release/`, and `workspace/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core shared building blocks required before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create shared diagnostic value objects in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/DiagnosticFailureCategory.java`, `DiagnosticStatus.java`, `MarkupServiceConfiguration.java`, `SanitizedTroubleshootingRecord.java`, `ExportedDiagnosticPackage.java`, and `AiMarkupDiagnosticSession.java`
- [X] T005 Create shared domain services in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/RequestValidationService.java`, `DiagnosticClassifier.java`, and `SecretRedactor.java`
- [X] T006 Create shared infrastructure DTO/client skeletons in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClient.java`, `OpenAiErrorResponse.java`, and `RequestTraceSnapshot.java`
- [X] T007 Create shared export/logging skeletons in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/export/DiagnosticExportWriter.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/logging/SanitizedDiagnosticLogger.java`
- [X] T008 Create shared application orchestration skeletons in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/command/RunAiMarkupDiagnosticsCommand.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/query/BuildDiagnosticExportQuery.java`
- [X] T009 Refactor AI Markup integration seam in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java` so the existing `processAIMarkup()` path delegates to feature collaborators instead of containing all request logic inline

**Checkpoint**: Foundational architecture is ready. User stories can now proceed in priority order.

---

## Phase 3: User Story 1 - Diagnose AI Markup Failures Clearly (Priority: P1) 🎯 MVP

**Goal**: Classify AI Markup request failures and present concise, actionable guidance to the user without leaking secrets.

**Independent Test**: Trigger representative authentication, model-access, malformed-request, endpoint-compatibility, and unknown-service failures and verify the DAMA panel shows the correct failure category guidance with no secret leakage.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T010-T-SPEC [P] [US1] Write specification acceptance matrix for failure-classification scenarios, including malformed payload and proxy-interference cases, in `specs/002-ai-api-diagnostics/acceptance/us1-diagnostic-failure-matrix.md`
- [X] T011-T-UNIT [P] [US1] Add unit tests for credential shape, model identifier, endpoint base URL, and proxy-setting validation rules in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/RequestValidationServiceTest.java`
- [X] T012-T-UNIT [P] [US1] Add unit tests for HTTP failure classification across 400/401/403/404/429/5xx plus malformed-payload and endpoint-compatibility guidance in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/DiagnosticClassifierTest.java`
- [X] T013-T-INTEGRATION [P] [US1] Add integration-style tests for OpenAI-compatible error parsing and partial/empty error bodies in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClientTest.java`
- [X] T014-T-INTEGRATION [P] [US1] Add workspace integration tests for concise AI Markup failure summaries and non-blocking failure feedback in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest.java`

### Implementation for User Story 1

- [X] T015 [P] [US1] Implement configuration validation and request-shape validation for credential shape, model identifier, endpoint base URL, and proxy-related settings in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/RequestValidationService.java`
- [X] T016 [P] [US1] Implement failure-category mapping and guidance-key selection for malformed payloads, endpoint mismatch, proxy/connectivity failures, and unknown service errors in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/DiagnosticClassifier.java`
- [X] T017 [P] [US1] Implement structured OpenAI-compatible error parsing in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/OpenAiErrorResponse.java`
- [X] T018 [US1] Implement request construction and HTTP response handling for OpenAI-compatible payloads, endpoint-path checks, and sanitized error-body capture in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClient.java`
- [X] T019 [US1] Implement diagnostic command orchestration in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/command/RunAiMarkupDiagnosticsCommand.java`
- [X] T020 [US1] Wire DAMA AI Markup failure handling to the diagnostic command and concise summary display, including explicit malformed-request and proxy-guidance branches, in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T021 [US1] Add user-facing AI Markup diagnostic message keys for all supported languages in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T022 [US1] Add regression assertions for existing successful AI Markup behavior in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionTest.java`
- [X] T023 [US1] Run the US1 verification gate from `Models/Gemini2.5/dila-ai-markup-plugin/` with `mvn test -Dtest=RequestValidationServiceTest,DiagnosticClassifierTest,OpenAiCompatibleChatClientTest,DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest,DAMAWorkspaceAccessPluginExtensionTest`

**Checkpoint**: User Story 1 should now classify and display AI Markup failures correctly and remain independently testable.

---

## Phase 4: User Story 2 - Keep Diagnostics Consistent Across Windows and macOS (Priority: P2)

**Goal**: Preserve equivalent diagnostic meaning and guidance across Windows and macOS while keeping the UI responsive and avoiding concurrent-operation ambiguity.

**Independent Test**: Simulate equivalent configuration and service failures on both platforms and verify identical classification with materially equivalent corrective guidance, responsive UI behavior, and guarded concurrent execution.

### Tests for User Story 2 ⚠️

- [X] T024-T-SPEC [P] [US2] Write specification acceptance matrix for Windows/macOS parity scenarios, including semantically equivalent proxy-troubleshooting guidance, in `specs/002-ai-api-diagnostics/acceptance/us2-cross-platform-parity-matrix.md`
- [X] T025-T-UNIT [P] [US2] Add unit tests for platform-aware guidance selection with equivalent meaning across Windows and macOS in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/DiagnosticClassifierPlatformParityTest.java`
- [X] T026-T-UNIT [P] [US2] Add unit tests for diagnostic session state transitions and concurrency guards in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/model/AiMarkupDiagnosticSessionTest.java`
- [X] T027-T-INTEGRATION [P] [US2] Add workspace integration tests for async UI-safe updates, concurrent request suppression, and responsive status feedback in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest.java`

### Implementation for User Story 2

- [X] T028 [P] [US2] Implement diagnostic session lifecycle and concurrent-operation rules in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/AiMarkupDiagnosticSession.java`
- [X] T029 [P] [US2] Extend failure classification to support platform-specific wording with equivalent meaning in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/DiagnosticClassifier.java`
- [X] T030 [US2] Integrate platform detection, async UI updates, and operation-state guarding in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T031 [US2] Add cross-platform wording and operation-state translation keys in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T032 [US2] Run the US2 verification gate from `Models/Gemini2.5/dila-ai-markup-plugin/` with `mvn test -Dtest=DiagnosticClassifierPlatformParityTest,AiMarkupDiagnosticSessionTest,DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest`

**Checkpoint**: User Stories 1 and 2 should both work independently, with parity and async safety verified.

---

## Phase 5: User Story 3 - Preserve Safe Troubleshooting Records (Priority: P3)

**Goal**: Capture sanitized troubleshooting records and support manual export for support sharing without exposing secrets.

**Independent Test**: Trigger a failure, verify sanitized metadata is captured, confirm the DAMA panel remains concise, and confirm exported diagnostic packages preserve redaction.

### Tests for User Story 3 ⚠️

- [X] T033-T-SPEC [P] [US3] Write specification acceptance matrix for troubleshooting-record and export scenarios, including support-usable sanitized exports, in `specs/002-ai-api-diagnostics/acceptance/us3-troubleshooting-export-matrix.md`
- [X] T034-T-UNIT [P] [US3] Add unit tests for secret redaction rules in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/SecretRedactorTest.java`
- [X] T035-T-UNIT [P] [US3] Add unit tests for export package assembly and required support-triage fields in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/application/query/BuildDiagnosticExportQueryTest.java`
- [X] T036-T-INTEGRATION [P] [US3] Add infrastructure tests for export serialization, schema compliance, and repeated redaction guarantees in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/export/DiagnosticExportWriterTest.java`
- [X] T037-T-INTEGRATION [P] [US3] Add workspace integration tests for manual export flow and redacted UI/logging separation in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest.java`

### Implementation for User Story 3

- [X] T038 [P] [US3] Implement secret masking and sanitization rules in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/SecretRedactor.java`
- [X] T039 [P] [US3] Implement sanitized troubleshooting record creation in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/SanitizedTroubleshootingRecord.java`
- [X] T040 [P] [US3] Implement export package assembly with schema-required support-triage fields in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/query/BuildDiagnosticExportQuery.java`
- [X] T041 [P] [US3] Implement sanitized logging adapter in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/logging/SanitizedDiagnosticLogger.java`
- [X] T042 [P] [US3] Implement manual export serialization in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/export/DiagnosticExportWriter.java`
- [X] T043 [US3] Wire troubleshooting capture, concise-vs-full detail separation, and manual export action into `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T044 [US3] Add export-specific user-facing translations in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T045 [US3] Run the US3 verification gate from `Models/Gemini2.5/dila-ai-markup-plugin/` with `mvn test -Dtest=SecretRedactorTest,BuildDiagnosticExportQueryTest,DiagnosticExportWriterTest,DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest`

**Checkpoint**: User Stories 1 through 3 should now be independently functional and safe to support-share.

---

## Phase 6: User Story 4 - Extend Existing Gear Menu with Preferences, User Manual, and About (Priority: P3)

**Goal**: Extend the existing gear icon menu so it keeps `Preferences...` first and adds `User Manual` then `About`, where `User Manual` opens the published documentation URL and `About` shows the installed plugin version plus the full current release notes from one packaged source of truth.

**Independent Test**: Open the existing gear icon menu in a built plugin package and verify it lists `Preferences...`, `User Manual`, and `About` in that order, opens `https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing`, and shows the installed plugin version plus the current release notes in `About` with a fallback message when the shared release-notes resource is unavailable.

### Tests for User Story 4 ⚠️

- [X] T046-T-SPEC [P] [US4] Write specification acceptance matrix for existing gear-menu order (`Preferences...`, `User Manual`, `About`), `User Manual` URL navigation, and `About` release-note scenarios in `specs/002-ai-api-diagnostics/acceptance/us4-gear-menu-support-matrix.md`
- [X] T047-T-UNIT [P] [US4] Add unit tests for shared release-notes loading and fallback behavior in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/application/query/LoadReleaseNotesQueryTest.java`
- [X] T047a-T-UNIT [P] [US4] Add unit tests for the configured `User Manual` target and existing gear-menu order contract metadata in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionAboutDialogTest.java`
- [X] T048-T-INTEGRATION [P] [US4] Add workspace integration tests for existing gear-menu order, `User Manual` navigation, `Preferences...` preservation, and `About` dialog content through `DAMAWorkspaceAccessPluginExtension#createMenuBar()` / `createOptionsMenu()` in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionAboutDialogTest.java`

### Implementation for User Story 4

- [X] T049 [P] [US4] Introduce a single packaged release-notes source in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/release-notes.xhtml` and update `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/extension.xml` plus `Models/Gemini2.5/dila-ai-markup-plugin/pom.xml` so the extension descriptor consumes that shared source during generation
- [X] T050 [P] [US4] Implement release-notes loading/query logic in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/query/LoadReleaseNotesQuery.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/release/ReleaseNotesResourceLoader.java`
- [X] T050a [P] [US4] Define the `User Manual` target constant and the existing gear-menu order metadata in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T051 [US4] Extend `DAMAWorkspaceAccessPluginExtension#createMenuBar()` / `createOptionsMenu()` so the existing gear menu preserves `Preferences...` and adds `User Manual` navigation to `https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing` plus `About` dialog behavior in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T052 [US4] Add `User Manual`, `About`, and release-note fallback translations in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`, explicitly reusing the existing `menuItem.preferences` key for `Preferences...`
- [X] T053 [US4] Run the US4 verification gate from `Models/Gemini2.5/dila-ai-markup-plugin/` with `mvn test -Dtest=LoadReleaseNotesQueryTest,DAMAWorkspaceAccessPluginExtensionAboutDialogTest,TranslationBundleCompletenessTest`

**Checkpoint**: User Story 4 should expose the existing gear-menu support actions in the documented order, the published user-manual link, and installed-version/release-note visibility without creating duplicate release-note maintenance paths.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Finalize contracts, documentation, and verification across the full feature.

- [ ] T054 Add automated i18n completeness verification for all supported languages, including the new About/help-menu keys, in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/i18n/TranslationBundleCompletenessTest.java`
- [ ] T055 [P] Update feature contracts plus the shared release-note generation and developer verification workflow documentation in `specs/002-ai-api-diagnostics/contracts/openai-compatible-chat-completions.yaml`, `specs/002-ai-api-diagnostics/contracts/diagnostic-export.schema.json`, and `specs/002-ai-api-diagnostics/quickstart.md`
- [ ] T056 Run full plugin regression suite from `Models/Gemini2.5/dila-ai-markup-plugin/` with `mvn test` and record verification results in `specs/002-ai-api-diagnostics/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup**: No dependencies, start immediately.
- **Phase 2: Foundational**: Depends on Setup and blocks all user story work.
- **Phase 3: US1**: Depends on Foundational and is the MVP.
- **Phase 4: US2**: Depends on Foundational and on the US1 verification gate plus diagnostic classification/orchestration seam.
- **Phase 5: US3**: Depends on Foundational and on the US1 verification gate plus troubleshooting-record creation path.
- **Phase 6: US4**: Depends on Foundational and on the US3 export path plus shared build metadata decisions being stable.
- **Phase 7: Polish**: Depends on completion of all desired user stories.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories once Foundational is complete.
- **US2 (P2)**: Builds on US1 diagnostic command and UI summary behavior, and should start only after `T023` passes.
- **US3 (P3)**: Builds on US1 classification/troubleshooting capture, and should start only after `T023` passes.
- **US4 (P3)**: Builds on the packaged release metadata conventions established in the feature and should start after US3 is stable enough that the combined release notes can be defined once and the existing gear-menu order contract is agreed.

### Within Each User Story

- Specification tests before unit tests
- Unit tests before integration tests
- All tests must fail before implementation starts
- Domain model/services before application orchestration
- Application orchestration before workspace integration
- Story verification gate before the next dependent story begins
- Translation updates before story sign-off

### Parallel Opportunities

- `T003` can run in parallel with `T001` and `T002`.
- In US1, `T010-T-SPEC` to `T014-T-INTEGRATION` can run in parallel, and `T015` to `T017` can run in parallel after tests exist.
- In US2, `T024-T-SPEC` to `T027-T-INTEGRATION` can run in parallel, and `T028` plus `T029` can run in parallel before workspace wiring.
- In US3, `T033-T-SPEC` to `T037-T-INTEGRATION` can run in parallel, and `T038` to `T042` can run in parallel before workspace wiring.
- In US4, `T046-T-SPEC` to `T048-T-INTEGRATION` can run in parallel, and `T049` plus `T050` can run in parallel before workspace wiring.
- `T054` and `T055` can run in parallel after implementation stabilizes.

---

## Parallel Example: User Story 1

```text
Task: "T010-T-SPEC [P] [US1] Write specification acceptance matrix for failure-classification scenarios, including malformed payload and proxy-interference cases, in specs/002-ai-api-diagnostics/acceptance/us1-diagnostic-failure-matrix.md"
Task: "T011-T-UNIT [P] [US1] Add unit tests for credential shape, model identifier, endpoint base URL, and proxy-setting validation rules in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/RequestValidationServiceTest.java"
Task: "T012-T-UNIT [P] [US1] Add unit tests for HTTP failure classification across 400/401/403/404/429/5xx plus malformed-payload and endpoint-compatibility guidance in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/DiagnosticClassifierTest.java"
Task: "T013-T-INTEGRATION [P] [US1] Add integration-style tests for OpenAI-compatible error parsing and partial/empty error bodies in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/api/OpenAiCompatibleChatClientTest.java"
```

## Parallel Example: User Story 2

```text
Task: "T024-T-SPEC [P] [US2] Write specification acceptance matrix for Windows/macOS parity scenarios, including semantically equivalent proxy-troubleshooting guidance, in specs/002-ai-api-diagnostics/acceptance/us2-cross-platform-parity-matrix.md"
Task: "T025-T-UNIT [P] [US2] Add unit tests for platform-aware guidance selection with equivalent meaning across Windows and macOS in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/DiagnosticClassifierPlatformParityTest.java"
Task: "T026-T-UNIT [P] [US2] Add unit tests for diagnostic session state transitions and concurrency guards in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/model/AiMarkupDiagnosticSessionTest.java"
Task: "T027-T-INTEGRATION [P] [US2] Add workspace integration tests for async UI-safe updates, concurrent request suppression, and responsive status feedback in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest.java"
```

## Parallel Example: User Story 3

```text
Task: "T033-T-SPEC [P] [US3] Write specification acceptance matrix for troubleshooting-record and export scenarios, including support-usable sanitized exports, in specs/002-ai-api-diagnostics/acceptance/us3-troubleshooting-export-matrix.md"
Task: "T034-T-UNIT [P] [US3] Add unit tests for secret redaction rules in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/SecretRedactorTest.java"
Task: "T035-T-UNIT [P] [US3] Add unit tests for export package assembly and required support-triage fields in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/application/query/BuildDiagnosticExportQueryTest.java"
Task: "T036-T-INTEGRATION [P] [US3] Add infrastructure tests for export serialization, schema compliance, and repeated redaction guarantees in Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/export/DiagnosticExportWriterTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Stop and validate AI Markup failure classification, guidance, and regression behavior
5. Demo/ship the MVP if only clear user diagnostics are needed initially

### Incremental Delivery

1. Add US1 for diagnostic correctness and regression protection
2. Add US2 for cross-platform parity and async/concurrency hardening
3. Add US3 for sanitized troubleshooting capture and manual export
4. Add US4 for the existing gear-menu support actions in the order `Preferences...`, `User Manual`, `About`, plus published user manual and `About` installed-version/release-note visibility from one packaged source
5. Finish with contracts, quickstart, and full-suite verification

### Team Strategy

1. One developer completes Setup + Foundational
2. After Foundational:
   - Developer A can drive US1
   - Developer B can prepare US2 tests in parallel after US1 seams are visible
   - Developer C can prepare US3 tests and export scaffolding in parallel after US1 troubleshooting models exist
   - Developer D can prepare US4 release-note resource/query work plus the existing gear-menu order contract once the combined release metadata is stable

---

## Notes

- All tasks use the required checkbox / ID / marker / file-path format.
- Story phases include explicit specification, unit, and integration coverage before implementation.
- Suggested MVP scope is **User Story 1** only.
- `T023`, `T032`, `T045`, and `T053` are story verification gates and should not be skipped.
- `T056` is the final full-regression gate and should not be skipped.



