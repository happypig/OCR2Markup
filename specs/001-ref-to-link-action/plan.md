# Implementation Plan: Ref to Link Action

**Branch**: `001-ref-to-link-action` | **Date**: 2026-01-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-ref-to-link-action/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Add a new action "<ref> to link" under Actions menu in DILA AI Markup Assistant (DAMA) that converts Tripitaka references inside `<ref>` elements to CBETA online links. The action follows the existing plugin pattern: user selects a `<ref>` element, clicks the action, system extracts reference components (canon, volume, page, column), calls CBETA API to generate link, displays result, and allows user to replace old `<ptr>` elements with the API-generated URL. The `checked` attribute is updated to "2" upon successful replacement.

## Technical Context

**Language/Version**: Java 8 (1.8+) - compatible with Oxygen XML SDK 27.1.0.3+
**Primary Dependencies**:
- Oxygen XML SDK 27.1.0.3+ (provided)
- Java Swing (UI components)
- `HttpURLConnection` (Java 8+) for CBRD API calls
- Java ResourceBundle with TEI XML (i18n)
- JSON parsing library (for CBRD API response)

**External API**:
- **CBRD API**: `https://cbss.dila.edu.tw/dev/cbrd/link` (CBETA Reference Detection)
- **Authentication**: HTTP Referer header `CBRD@dila.edu.tw`
- **Request**: GET with XML query parameter
- **Response**: JSON with `success` boolean and `found` array of URLs

**Storage**:
- Configuration stored in Oxygen's `WSOptionsStorage` (CBRD API endpoint URL, Referer header value)
- No persistent data storage required (stateless operations)

**Testing**:
- JUnit 4
- Mockito 4.11.0 (mocking Oxygen SDK, HTTP client)
- AssertJ 3.24.2 (fluent assertions)

**Target Platform**: Oxygen XML Editor 27.0+ on Windows/macOS/Linux desktop

**Project Type**: Desktop plugin (extends existing DAMA plugin codebase)

**Performance Goals**:
- API call response within configured timeout window (default 3 seconds per attempt, up to 3 attempts plus backoff) (SC-003)
- Complete workflow (selection → replacement) under 30 seconds (SC-001)
- Non-blocking UI during API calls (async operations)

**Constraints**:
- MUST integrate with existing DAMAWorkspaceAccessPluginExtension architecture
- MUST follow existing UI pattern (infoArea, resultArea, Replace button)
- MUST support 3 languages (en_US, zh_CN, zh_TW)
- MUST validate Oxygen SDK returns for null (defensive programming)
- MUST use CompletableFuture for async HTTP calls (never block EDT)

**Scale/Scope**:
- Single new menu action in existing plugin
- 1 new action listener class
- 1 new reference parser service (domain layer)
- 1 new CBETA API client (infrastructure layer)
- Estimated ~500-800 lines of production code
- Estimated 15-25 tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Principle I: Event Storming First
**Status**: PASS
- Spec contains complete Event Storming analysis (14 events, 4 commands, 1 aggregate, 5 policies, 2 external systems)

### ✅ Principle II: Problem Frames for Requirements
**Status**: PASS
- Spec contains Problem Frames analysis (problem/solution domains, shared phenomena, transformation problem identified)

### ✅ Principle III: Clean Architecture
**Status**: PASS (requires verification in Phase 1)
- Plan will implement:
  - **Domain Layer**: `ReferenceParser` (extract components), `CBETAReference` (value object)
  - **Application Layer**: `ConvertReferenceCommand` (orchestrates conversion workflow)
  - **Infrastructure Layer**: `CBETAAPIClient` (HTTP), `RefToLinkActionListener` (UI event handler)

### ✅ Principle IV: Test-Driven Development (BDD + TDD)
**Status**: PASS
- Spec contains BDD acceptance scenarios in Given-When-Then format
- Tasks will follow test-first structure (T###-T-SPEC → T###-T-UNIT → T###-T-INTEGRATION → Implementation)

### ✅ Principle V: Domain-Driven Design (DDD)
**Status**: PASS
- Spec uses ubiquitous language: Tripitaka Reference, CBETA Link, Reference Components, Verification Status
- Aggregate identified: `ReferenceConversionSession`
- Value Objects planned: `CBETAReference`, `TripitakaComponents`
- Domain Service planned: `ReferenceParser`, `LinkGenerator`

### ✅ Principle VI: Command-Query Separation (CQRS)
**Status**: PASS
- Command: `ConvertReferenceCommand` (mutates document, returns void/result)
- Queries not needed for this feature (stateless operations)

### ✅ Principle VII: Defensive Programming
**Status**: PASS (requires implementation verification)
- Plan includes null checks for Oxygen SDK returns
- Multi-layer validation: selection → XML parsing → API response → replacement
- Error handling at each stage with i18n messages

### ✅ Principle VIII: Async-First Design
**Status**: PASS (requires implementation verification)
- API calls will use `CompletableFuture`
- UI updates via `SwingUtilities.invokeLater()`
- Never block EDT during HTTP operations

### ✅ Principle IX: Comprehensive i18n
**Status**: PASS (requires implementation)
- All user-facing strings will use existing i18n system
- Menu labels, button text, error messages, success messages
- 3 languages: en_US, zh_CN, zh_TW
- Translation keys to be added to `translation.xml`

### ✅ Principle X: Continuous Verification
**Status**: PASS
- `mvn test` after each change
- Tests must pass before marking tasks complete

### ✅ Research Complete (see [research.md](research.md))

1. **CBETA API Specification**: ✅ RESOLVED
   - **Decision**: Use CBRD REST API at `https://cbss.dila.edu.tw/dev/cbrd/link`
   - **API Contract**: GET request with XML query parameter `q`, requires `Referer: CBRD@dila.edu.tw` header
   - **Response**: JSON with `{"success": true, "found": ["url"]}`
   - **Authentication**: HTTP Referer header (no credentials needed)

2. **Reference Component Parsing**: ✅ RESOLVED
   - **Decision**: Use Java DOM (`DocumentBuilder`) to parse `<ref>` elements
   - **Two-phase approach**: Oxygen Text mode API returns String → DOM parses String to extract child elements
   - **Transformation required**: Map canon names (大正蔵→T), convert CJK numerals (二四→24), map column positions (上→a)

3. **Oxygen SDK Integration**: ✅ RESOLVED
   - **Decision**: Follow existing DAMA plugin patterns
   - **Action Listener**: Inner class implementing `ActionListener`, follows AI Markup pattern
   - **Selection Handling**: Use `fetchSelectedText()` method (reuse existing infrastructure)
   - **Document Modification**: Use existing `ReplaceButtonActionListener` pattern
   - **Async Operations**: Use `CompletableFuture` with shared executor, UI updates via `SwingUtilities.invokeLater()`

## Project Structure

### Documentation (this feature)

```text
specs/001-ref-to-link-action/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (pending)
├── data-model.md        # Phase 1 output (pending)
├── quickstart.md        # Phase 1 output (pending)
├── contracts/           # Phase 1 output (pending)
│   └── cbeta-api.yaml   # CBETA API contract (OpenAPI)
├── checklists/
│   └── requirements.md  # Spec quality checklist (complete)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

Based on existing plugin structure from README.MD and PLUGIN_OVERVIEW.md:

```text
Models/Gemini2.5/dila-ai-markup-plugin/
├── src/
│   ├── main/
│   │   ├── java/com/dila/dama/plugin/
│   │   │   ├── domain/                    # NEW: Domain layer for this feature
│   │   │   │   ├── model/
│   │   │   │   │   ├── CBETAReference.java              # Value Object
│   │   │   │   │   ├── TripitakaComponents.java         # Value Object
│   │   │   │   │   └── ReferenceConversionSession.java  # Aggregate
│   │   │   │   └── service/
│   │   │   │       ├── ReferenceParser.java             # Domain Service
│   │   │   │       └── LinkGenerator.java               # Domain Service (if needed)
│   │   │   │
│   │   │   ├── application/               # NEW: Application layer
│   │   │   │   └── command/
│   │   │   │       └── ConvertReferenceCommand.java     # Command handler
│   │   │   │
│   │   │   ├── infrastructure/            # NEW: Infrastructure layer
│   │   │   │   ├── api/
│   │   │   │   │   └── CBETAAPIClient.java              # HTTP client
│   │   │   │   └── ui/
│   │   │   │       └── RefToLinkActionListener.java     # UI event handler
│   │   │   │
│   │   │   ├── preferences/               # EXISTING: Update for CBETA API config
│   │   │   │   └── DAMAOptionPagePluginExtension.java  # Add CBETA endpoint field
│   │   │   │
│   │   │   ├── workspace/                 # EXISTING: Update for new action
│   │   │   │   └── DAMAWorkspaceAccessPluginExtension.java  # Add menu item
│   │   │   │
│   │   │   └── util/                      # EXISTING: Shared utilities
│   │   │       └── PluginLogger.java      # Logging
│   │   │
│   │   └── resources/
│   │       ├── i18n/
│   │       │   └── translation.xml        # UPDATE: Add new translation keys
│   │       ├── plugin.xml
│   │       └── extension.xml
│   │
│   └── test/
│       └── java/com/dila/dama/plugin/
│           ├── domain/
│           │   ├── model/
│           │   │   ├── CBETAReferenceTest.java
│           │   │   ├── TripitakaComponentsTest.java
│           │   │   └── ReferenceConversionSessionTest.java
│           │   └── service/
│           │       └── ReferenceParserTest.java
│           │
│           ├── application/
│           │   └── command/
│           │       └── ConvertReferenceCommandTest.java
│           │
│           └── infrastructure/
│               ├── api/
│               │   └── CBETAAPIClientTest.java
│               └── ui/
│                   └── RefToLinkActionListenerTest.java
```

**Structure Decision**: Extending existing DAMA plugin with Clean Architecture layers. New domain/application/infrastructure packages isolate the Ref to Link feature while integrating with existing workspace and preferences infrastructure. This maintains backward compatibility and follows the constitution's layered architecture requirement.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. All 10 constitution principles are followed.
