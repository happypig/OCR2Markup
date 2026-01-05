# Tasks: Ref to Link Action

**Input**: Design documents from `specs/001-ref-to-link-action/`
**Prerequisites**: `specs/001-ref-to-link-action/plan.md`, `specs/001-ref-to-link-action/spec.md`, `specs/001-ref-to-link-action/research.md`, `specs/001-ref-to-link-action/data-model.md`, `specs/001-ref-to-link-action/contracts/cbrd-api.yaml`, `specs/001-ref-to-link-action/quickstart.md`

**Source Root**: Implementation targets `Models/Gemini2.5/dila-ai-markup-plugin/` (Maven, Java 8)

**Tests**: INCLUDED. `specs/001-ref-to-link-action/spec.md` contains Given-When-Then BDD scenarios (US1–US3), and Constitution Principle IV mandates a test-first structure.

## Format: `- [ ] T###[-T-SPEC|-T-UNIT|-T-INTEGRATION] [P?] [US?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[US#]**: Which user story this task belongs to (US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline and locate integration points

- [X] T001 Run baseline build/tests in `Models/Gemini2.5/dila-ai-markup-plugin` (`mvn test`) and note failures (if any)
- [X] T002 Review action wiring + UI patterns in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T003 Review preferences storage patterns in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared building blocks used by all user stories

- [X] T004 [P] Add runtime JSON parser dependency (e.g., `org.json:json`) in `Models/Gemini2.5/dila-ai-markup-plugin/pom.xml`
- [X] T005 [P] Add i18n keys for Ref-to-Link UI and errors in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T006 [P] Add i18n keys for new CBRD preferences labels/help text in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T007 Add CBRD option-key constants + defaults (endpoint URL, referer header value, timeout) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T008 Add CBRD preferences UI fields and save/load/apply logic in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java`
- [X] T009 [P] Add Ref-to-Link error keys to `isErrorMessage()` handling in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T010 [P] Add XML helper utilities for parsing/serialization (DOM parse from string, DOM to string) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/util/XmlDomUtils.java`
- [X] T011 [P] Add a small injectable HTTP abstraction for `HttpURLConnection` to enable integration tests (e.g., `HttpUrlConnectionFactory`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/HttpUrlConnectionFactory.java`

**Checkpoint**: Foundation ready for test-first story work.

---

## Phase 3: User Story 1 - Convert Reference to Link (Priority: P1) — MVP

**Goal**: Select a `<ref>`, invoke action, click Convert, and get a CBETA URL via CBRD API.

**Independent Test**: Select a `<ref>` in Oxygen, invoke the action, click Convert, and verify a valid link is shown in resultArea.

### Tests for User Story 1 (Write FIRST; ensure they FAIL before implementation)

- [X] T012-T-SPEC [P] [US1] Create acceptance test matrix from BDD scenarios (selection -> convert -> replace-visible) in `specs/001-ref-to-link-action/tests/us1-convert-reference.md`
- [X] T013-T-UNIT [P] [US1] Add failing unit tests for parsing + transformation rules in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/ReferenceParserTest.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/ComponentTransformerTest.java`
- [X] T014-T-UNIT [P] [US1] Add failing unit tests for CJK numeral conversion in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/NumeralConverterTest.java`
- [X] T015-T-INTEGRATION [P] [US1] Add failing integration-style tests for command orchestration and API request formatting (mocked HTTP) in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/application/command/ConvertReferenceCommandTest.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClientTest.java`

### Implementation for User Story 1

- [X] T016 [P] [US1] Create `TripitakaComponents` value object in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/TripitakaComponents.java`
- [X] T017 [P] [US1] Create `TransformedComponents` value object in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/TransformedComponents.java`
- [X] T018 [P] [US1] Create `ReferenceConversionSession` (aggregate + status enum) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/ReferenceConversionSession.java`
- [X] T019 [P] [US1] Add domain exceptions in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/InvalidReferenceException.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/model/TransformationException.java`
- [X] T020 [US1] Implement `<ref>` XML parsing/validation (DOM parse, root element check, extract `<canon>`, `<v>`, `<w>`, `<p>`, `<c>`, `<l>`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/ReferenceParser.java`
- [X] T021 [US1] Implement canon mapping + normalization, numeral conversion, and column mapping in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/ComponentTransformer.java` and `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/NumeralConverter.java`
- [X] T022 [P] [US1] Implement `CBRDResponse` DTO + JSON parse in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/CBRDResponse.java`
- [X] T023 [US1] Implement `CBRDAPIClient` (timeouts, Referer header, URL-encoded `q=<ref>...</ref>`, JSON handling) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClient.java`
- [X] T024 [US1] Implement `ConvertReferenceCommand` orchestration (parse -> transform -> call API) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/application/command/ConvertReferenceCommand.java`
- [X] T025 [US1] Add `REF_TO_LINK` to `OperationType` in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T026 [US1] Add Actions menu item (i18n key `menuItem.ref.to.link`) and wire `RefToLinkActionListener` in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T027 [US1] Add `convertButton` UI component and show/hide helpers in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T028 [US1] Implement `RefToLinkActionListener` to: set op context, validate selection is full `<ref>`, show selection in infoArea, and show Convert button in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T029 [US1] Implement Convert handler: run `ConvertReferenceCommand` on `executor`, update UI via `SwingUtilities.invokeLater()`, and on success display URL + show Replace button in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`

**Checkpoint**: US1 conversion works; Replace is visible only after a successful Convert.

---

## Phase 4: User Story 2 - Replace Old Links with New Links (Priority: P2)

**Goal**: Replace all existing `<ptr>` inside `<ref>` with a single new `<ptr href="..."/>` and set `checked="2"`.

**Independent Test**: After successful Convert, click Replace and verify `<ref>` updates correctly and Replace button is hidden.

### Tests for User Story 2 (Write FIRST; ensure they FAIL before implementation)

- [X] T030-T-SPEC [P] [US2] Create acceptance test matrix from BDD scenarios (ptr replacement + checked update + button hidden) in `specs/001-ref-to-link-action/tests/us2-replace-links.md`
- [X] T031-T-UNIT [P] [US2] Add failing unit tests for `<ref>` rewrite rules (remove all `<ptr>`, preserve other nodes, set `checked="2"`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/RefElementRewriterTest.java`
- [X] T032-T-INTEGRATION [P] [US2] Add failing integration-style test for Replace flow using mocked editor selection + document insertion in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/workspace/RefToLinkReplaceFlowTest.java`

### Implementation for User Story 2

- [X] T033 [US2] Implement `<ref>` rewrite service in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/RefElementRewriter.java`
- [X] T034 [US2] Store “current Ref-to-Link session” state needed for Replace (selected `<ref>` xml + generated URL) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T035 [US2] Branch `ReplaceButtonActionListener` so `REF_TO_LINK` uses `RefElementRewriter` (not generic replace-selection-with-resultArea) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T036 [US2] On successful replacement: hide Replace, clear/retain result as appropriate, and show success message in infoArea (i18n key `success.replacement.complete`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`

**Checkpoint**: US2 replacement works and does not risk destructive generic replacement.

---

## Phase 5: User Story 3 - Handle Invalid or Missing References (Priority: P3)

**Goal**: Provide clear errors for invalid selections, missing components, and API failures; allow manual retry (no auto-retry).

**Independent Test**: Select invalid/incomplete `<ref>` and verify correct error messages; Replace stays hidden.

### Tests for User Story 3 (Write FIRST; ensure they FAIL before implementation)

- [X] T037-T-SPEC [P] [US3] Create acceptance test matrix from BDD scenarios (invalid selection, missing parts, API error/no results) in `specs/001-ref-to-link-action/tests/us3-errors.md`
- [X] T038-T-UNIT [P] [US3] Add failing unit tests for validation errors (invalid xml, not `<ref>`, missing required tags) in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/domain/service/ReferenceValidationTest.java`
- [X] T039-T-INTEGRATION [P] [US3] Add failing integration-style tests for API error mapping (timeout/http/invalid json/empty found) in `Models/Gemini2.5/dila-ai-markup-plugin/src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClientErrorHandlingTest.java`

### Implementation for User Story 3

- [X] T040 [US3] Implement selection validation and errors in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java` (no selection, not text mode, invalid XML fragment)
- [X] T041 [US3] Implement missing-component error reporting (canon + volume required; others optional per spec) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/ReferenceParser.java`
- [X] T042 [US3] Implement transformation failure reporting (unknown canon, invalid numerals, unknown column) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/domain/service/ComponentTransformer.java`
- [X] T043 [US3] Implement API failure handling + user-facing mapping (timeout, connection, non-200 HTTP, invalid JSON, `found: []`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClient.java`
- [X] T044 [US3] Ensure retry behavior: Convert remains available after errors; Replace remains hidden; no EDT blocking in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`

**Checkpoint**: US3 errors are informative and safe (no accidental replacement).

---

## Phase N: Polish & Cross-Cutting Concerns

- [X] T045 [P] Verify new i18n keys exist for `en_US`, `zh_CN`, `zh_TW` and keep `TranslationConsistencyTest` passing in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml`
- [X] T046 [P] Update `specs/001-ref-to-link-action/quickstart.md` to match final class names/options keys/buttons
- [X] T047 Ensure async behavior never blocks the EDT (Convert uses `executor`, UI updates via `SwingUtilities.invokeLater()`) in `Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java`
- [X] T048 Run packaging build in `Models/Gemini2.5/dila-ai-markup-plugin` (`mvn -DskipTests=false install`) and verify `dilaAIMarkupPlugin.zip` includes any new runtime deps

---

## Dependencies & Execution Order

### User Story Dependencies

- **US1 (P1)**: depends on Phase 2 foundational tasks
- **US2 (P2)**: depends on US1 (needs generated URL and UI wiring)
- **US3 (P3)**: can start after Phase 2 and integrate as US1 lands

### Within Each User Story (Constitution Principle IV)

- `T###-T-SPEC` (BDD acceptance test matrix) -> `T###-T-UNIT` -> `T###-T-INTEGRATION` -> implementation tasks

---

## Parallel Opportunities

- Phase 2 tasks marked `[P]` can be done in parallel.
- For each story, unit tests and integration tests can proceed in parallel if they touch different files.

---

## Report

- Total tasks: 48
- Tasks per user story (including tests): US1 = 18, US2 = 7, US3 = 8
- Parallel opportunities: tasks marked `[P]`
- MVP scope: US1 only (Phase 3)
- Format validation: all tasks use `- [ ]` checkboxes, sequential IDs, and story labels only in story phases


