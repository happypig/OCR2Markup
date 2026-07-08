# Implementation Plan: Cross-Platform API Diagnostics and Support Visibility

**Branch**: `002-ai-api-diagnostics` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-ai-api-diagnostics/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.
**Post-Delivery Note**: The `002-ai-api-diagnostics` branch has already shipped. The 2026-04-07 edits in this plan tighten constitution traceability and add explicit follow-up work without changing the delivered feature behavior.

## Summary

Add cross-platform AI Markup diagnostics and request hardening to the DAMA plugin so request failures are validated, classified, redacted, and surfaced consistently on Windows and macOS. The feature will move AI Markup failure handling away from the current workspace-level HTTP/string parsing path into a layered design: domain models for diagnostic state and classification, application command/query orchestration, infrastructure adapters for configured OpenAI-compatible chat completions, sanitized troubleshooting capture, and UI integration through the existing DAMA panel, the existing gear icon menu, and preferences surface. The same release will also extend the existing gear icon menu implemented in `DAMAWorkspaceAccessPluginExtension#createMenuBar()` and `createOptionsMenu()` so its order is `Preferences...`, `User Manual`, `About`, where `User Manual` opens `https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing` and `About` shows the installed version plus full release notes from a single packaged release-notes source shared with extension descriptor generation.

## Technical Context

**Language/Version**: Java 8 source/bytecode baseline running inside Oxygen XML Editor / SDK 27.1.0.3+ on Java 17  
**Primary Dependencies**:
- Oxygen XML SDK 27.1.0.3+ (provided)
- Java Swing for DAMA panel and preferences UI
- `HttpURLConnection` for OpenAI-compatible HTTP calls
- `org.json` for robust request/response/error JSON parsing
- Java ResourceBundle with TEI XML translations

**Storage**:
- Oxygen `WSOptionsStorage` and `secretOption` for endpoint/model/key preferences
- In-memory diagnostic session state during DAMA workflow
- User-initiated exported diagnostic package written to local file on demand
- Packaged release-notes resource stored under `src/main/resources/` as the canonical content source for both the existing gear menu's `About` action and extension descriptor generation

**Testing**:
- JUnit 4
- Mockito 4.11.0
- AssertJ 3.24.2
- Automated translation-bundle completeness verification for all supported languages
- Maven Surefire (`mvn test`)

**Target Platform**: Oxygen XML Editor 27.1.0.3+ on Windows and macOS desktop

**Project Type**: Desktop Oxygen plugin in an existing Maven single-project codebase

**Performance Goals**:
- Visible processing/failure feedback in the DAMA panel within 3 seconds of invocation
- No EDT blocking during validation, network calls, classification, or export preparation
- Equivalent diagnostic classification, materially equivalent corrective guidance, and the same success-state behavior across Windows/macOS for the same request inputs

**Constraints**:
- Must preserve Java 8 compilation compatibility
- Must use asynchronous execution with UI updates on `SwingUtilities.invokeLater()`
- Must preserve equivalent successful workflow behavior on Windows and macOS for the same request conditions
- Must preserve diagnostics-only scope for request handling: no automatic endpoint/model/request mutation
- Must shut down background executors during `applicationClosing()`
- Must keep plugin version in Maven project metadata and release-note content in one packaged resource reused by the existing gear-menu `About` action and extension descriptor generation
- Must reuse the existing gear icon menu and keep `Preferences...`, `User Manual`, and `About` in that order
- Must extend the existing options-menu code path in `DAMAWorkspaceAccessPluginExtension#createMenuBar()` / `createOptionsMenu()` instead of introducing a second support menu location
- Must route the `User Manual` action to `https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing`
- Must support OpenAI-hosted and OpenAI-compatible endpoints configured in DAMA
- Must validate credential presence/shape, model identifier, endpoint base URL, and proxy-related connection settings before network execution when possible
- Must redact secrets in UI, logs, in-memory troubleshooting records, and exported diagnostic packages
- Must use i18n for all user-facing text in `translation.xml`

**Scale/Scope**:
- 1 existing DAMA AI Markup workflow refactor in touched areas
- 1 new diagnostic aggregate and supporting value objects
- 1 command path for AI Markup diagnostics plus 2 query paths for export assembly and packaged release-note loading
- 2 external contract artifacts (chat completions subset + diagnostic export schema)
- Estimated 15-22 production files touched/added
- Estimated 24-34 unit/integration/specification tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Gate

### ✅ Principle I: Event Storming First
**Status**: PASS
- The feature spec includes an explicit Event Storming section covering actors, commands, events, policies, and external boundaries for the AI Markup diagnostic flow.

### ✅ Principle II: Problem Frames for Requirements
**Status**: PASS
- The feature spec includes an explicit Problem Frames section separating problem domain, solution domain, and shared phenomena.

### ✅ Principle III: Clean Architecture
**Status**: PASS WITH REMEDIATION IN TOUCHED AREA
- Current AI Markup logic in `workspace/DAMAWorkspaceAccessPluginExtension.java` mixes UI orchestration, preference access, HTTP calls, request construction, response parsing, and error formatting.
- This feature will remediate the touched AI Markup path by introducing domain/application/infrastructure collaborators instead of extending the mixed workspace implementation.

### ✅ Principle IV: Test-Driven Development (BDD + TDD)
**Status**: PASS WITH POST-DELIVERY FOLLOW-UP
- Spec contains Given-When-Then acceptance scenarios.
- User-story tasks are structured test-first for diagnostic classification, redaction, UI behavior, and export behavior, with explicit specification, unit, and integration labels in the task IDs.
- Delivered work also included foundational scaffolding tasks (`T004`-`T009`) that were completed without standalone RED-phase tasks. Phase 8 follow-up work records the characterization and verification backfill needed for full constitution traceability without rewriting delivered history.

### ✅ Principle V: Domain-Driven Design (DDD)
**Status**: PASS
- Ubiquitous language is explicit: AI Markup Diagnostic Result, Troubleshooting Record, OpenAI-Compatible Service Configuration, Failure Classification.

### ✅ Principle VI: Command-Query Separation (CQRS)
**Status**: PASS WITH REFACTOR EXPECTATION
- Existing AI Markup path currently returns UI-ready strings from workspace code.
- This feature will introduce a command to run diagnostics and a query/view-mapping path to obtain display/export data, avoiding new CQRS drift in touched files.

### ✅ Principle VII: Defensive Programming
**Status**: PASS WITH EXPLICIT LIFECYCLE TRACEABILITY
- Plan includes multi-layer validation: configuration, request construction, HTTP status/body parsing, classification confidence, redaction, export sanitization.
- Delivered implementation also shuts down the diagnostics executor during `applicationClosing()`. Phase 8 follow-up work adds explicit task/test traceability for that lifecycle requirement.

### ✅ Principle VIII: Async-First Design
**Status**: PASS WITH EXPLICIT LIFECYCLE TRACEABILITY
- Network calls and export preparation remain background work via existing executor/`CompletableFuture`.
- UI updates remain marshaled to the EDT.
- Phase 8 follow-up work adds explicit verification for executor lifecycle cleanup and successful workflow parity in addition to failure-path async behavior.

### ✅ Principle IX: Comprehensive i18n
**Status**: PASS
- New diagnostic summaries, guidance messages, export labels, and fallback messages will be added in all supported languages.
- Phase 2/Polish verification includes an automated translation-bundle completeness test so new keys cannot ship missing in any supported language.

### ✅ Principle X: Continuous Verification
**Status**: PASS WITH POST-DELIVERY FOLLOW-UP
- The delivered branch was verified with `mvn test`, including unit tests for classification/redaction, integration-style tests around the touched AI Markup workflow, story verification gates, and a final regression run recorded in `quickstart.md`.
- The original task list did not explicitly represent immediate verification after every shared scaffolding edit. Phase 8 follow-up work preserves that remaining traceability gap as scheduled work instead of leaving it implicit.

### Post-Design Re-Check

### ✅ Architecture Alignment
**Status**: PASS
- Phase 1 design introduces:
  - **Domain Layer**: diagnostic aggregate, failure category enum, sanitized record models, validation/redaction services
  - **Application Layer**: run-diagnostics command and export/query mapping
  - **Infrastructure Layer**: OpenAI-compatible HTTP client, response parser, export writer, workspace/preferences integration

### ✅ Java Baseline Alignment
**Status**: PASS
- Design remains on Java 8 source/target and uses `HttpURLConnection` plus existing `org.json`.

### ✅ i18n / Async / Defensive Alignment
**Status**: PASS
- No design decision requires bypassing translation files, EDT rules, validation gates, or executor lifecycle cleanup.

### Gate Result
**PASS WITH POST-DELIVERY FOLLOW-UP**: Delivered behavior remains accepted, and the remaining constitution traceability hardening is tracked explicitly in Phase 8 tasks instead of being left implicit.

## Project Structure

### Documentation (this feature)

```text
specs/002-ai-api-diagnostics/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openai-compatible-chat-completions.yaml
│   └── diagnostic-export.schema.json
└── tasks.md
```

### Source Code (repository root)

```text
Models/Gemini2.5/dila-ai-markup-plugin/
├── src/
│   ├── main/
│   │   ├── java/com/dila/dama/plugin/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── AiMarkupDiagnosticSession.java
│   │   │   │   │   ├── DiagnosticFailureCategory.java
│   │   │   │   │   ├── DiagnosticStatus.java
│   │   │   │   │   ├── ExportedDiagnosticPackage.java
│   │   │   │   │   ├── MarkupServiceConfiguration.java
│   │   │   │   │   └── SanitizedTroubleshootingRecord.java
│   │   │   │   └── service/
│   │   │   │       ├── DiagnosticClassifier.java
│   │   │   │       ├── RequestValidationService.java
│   │   │   │       └── SecretRedactor.java
│   │   │   ├── application/
│   │   │   │   ├── command/
│   │   │   │   │   └── RunAiMarkupDiagnosticsCommand.java
│   │   │   │   └── query/
│   │   │   │       ├── BuildDiagnosticExportQuery.java
│   │   │   │       └── LoadReleaseNotesQuery.java
│   │   │   ├── infrastructure/
│   │   │   │   ├── api/
│   │   │   │   │   ├── OpenAiCompatibleChatClient.java
│   │   │   │   │   ├── OpenAiErrorResponse.java
│   │   │   │   │   └── RequestTraceSnapshot.java
│   │   │   │   ├── export/
│   │   │   │   │   └── DiagnosticExportWriter.java
│   │   │   │   ├── logging/
│   │   │   │   │   └── SanitizedDiagnosticLogger.java
│   │   │   │   └── release/
│   │   │   │       └── ReleaseNotesResourceLoader.java
│   │   │   ├── preferences/
│   │   │   │   └── DAMAOptionPagePluginExtension.java
│   │   │   ├── workspace/
│   │   │   │   └── DAMAWorkspaceAccessPluginExtension.java
│   │   │   └── util/
│   │   │       └── PluginLogger.java
│   │   └── resources/
│   │       ├── i18n/translation.xml
│   │       └── release-notes.xhtml
│   └── test/java/com/dila/dama/plugin/
│       ├── domain/
│       │   ├── model/
│       │   └── service/
│       ├── application/
│       │   ├── command/
│       │   └── query/
│       └── infrastructure/
│           ├── api/
│           ├── export/
│           ├── i18n/
│           ├── release/
│           └── workspace/
```

**Structure Decision**: Reuse the existing Maven plugin module under `Models/Gemini2.5/dila-ai-markup-plugin/` and keep new feature logic in domain/application/infrastructure packages, while limiting `workspace/` and `preferences/` changes to wiring and UI integration for diagnostics plus the existing gear-menu support actions implemented in `DAMAWorkspaceAccessPluginExtension#createMenuBar()` / `createOptionsMenu()`.

## Verification Strategy

- Write and fail specification, unit, and integration tests before implementation in each user story.
- Run story-scoped `mvn test` verification immediately after completing US1, US2, US3, and US4 before advancing to the next story.
- Add an automated translation-bundle completeness test covering all supported languages whenever new `translation.xml` keys are introduced.
- Record story-gate and full-regression evidence in `quickstart.md`; the delivered branch already includes those verification results.
- Use Phase 8 follow-up work to add explicit verification for diagnostics-only non-mutation, successful Windows/macOS parity, executor shutdown lifecycle handling, and measurable success-criteria evidence that were only implicit in the original delivery tasks.
- Finish with a full-module `mvn test` regression run after contracts, quickstart documentation, and release-note generation workflow are aligned with the implemented behavior.

## Complexity Tracking

No Java-baseline exception is required for this feature. Post-delivery follow-up tasks are retained to close constitution traceability gaps around foundational RED coverage, executor lifecycle verification, diagnostics-only non-mutation coverage, successful cross-platform parity, and explicit success-criteria measurement evidence.




