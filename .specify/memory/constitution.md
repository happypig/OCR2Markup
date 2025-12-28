<!--
=== CONSTITUTION SYNC IMPACT REPORT ===
Version Change: INITIAL → 1.0.0
Type: MAJOR (Initial constitution ratification)

Modified Principles: N/A (Initial version)

Added Sections:
- Core Principles (10 principles)
- Technology Standards
- Governance

Removed Sections: N/A (Initial version)

Templates Requiring Updates:
✅ .specify/templates/plan-template.md - Constitution Check section aligns
✅ .specify/templates/spec-template.md - BDD acceptance criteria aligns with Principle IV
✅ .specify/templates/tasks-template.md - Test-first structure aligns with Principle V

Follow-up TODOs: None

Date: 2025-12-26
-->

# DILA AI Markup Plugin Constitution

## Core Principles

### I. Event Storming First

All feature requirements MUST begin with Event Storming to discover domain events, commands, actors, aggregates, policies, and external system boundaries before any design or implementation.

**Rationale**: Reveals the true domain model by focusing on what actually happens, surfacing hidden requirements and integration points.

### II. Problem Frames for Requirements

Every feature MUST be analyzed through Problem Frames to separate problem domain (real-world entities) from solution domain (technical components) and identify shared phenomena (interactions).

**Rationale**: Prevents premature design decisions and ensures requirements are testable and complete.

### III. Clean Architecture

The codebase MUST maintain layered architecture with dependencies pointing inward:
- **Domain Layer** (innermost): Pure business logic, framework-independent, 100% unit testable
- **Application Layer**: Commands (writes) and Queries (reads), orchestrates domain operations
- **Infrastructure Layer**: UI, HTTP clients, file I/O, Oxygen SDK integration

**Rationale**: Enables testing business logic in isolation and allows framework migration without rewriting core logic.

### IV. Test-Driven Development (BDD + TDD)

**BDD (Specification Level)**: All features MUST be specified using Given-When-Then scenarios before implementation. ALL functional requirements MUST include explicit BDD acceptance criteria.

**TDD (Implementation Level)**: Domain layer functions MUST follow Red-Green-Refactor cycle. Tests written first, verified failing (RED), then implementation makes them pass (GREEN).

**Test-First Task Structure**: All tasks MUST follow this sequence:
1. 🧪 Test Phase (RED): Write T###-T-SPEC, T###-T-UNIT, T###-T-INTEGRATION - MUST FAIL
2. 🚨 TEST GATE: Cannot proceed until all tests written and failing
3. ⚙️ Implementation Phase (GREEN): Implement Domain → Application → Infrastructure
4. 🎯 COMPLETION GATE: All tests MUST pass before marking feature complete

**Rationale**: BDD ensures shared understanding with stakeholders; TDD ensures code correctness; test-first gates prevent skipping tests.

### V. Domain-Driven Design (DDD)

The domain model MUST reflect ubiquitous language from TEI XML, Buddhist studies, and plugin development. Use domain-specific terminology (參考文獻/Reference, 典籍/Canon, 超連結/Hyperlink) instead of generic terms (Request, Response, Data).

Core domain concepts: Aggregates (ReferenceConversionSession, PluginConfiguration), Value Objects (CBETAReference, TEIMarkup, APICredentials), Domain Services (ReferenceParser, LinkGenerator, MarkupBuilder), Domain Events (ReferenceSelected, APICallSucceeded, MarkupGenerated).

**Rationale**: Creates shared language between developers and domain experts, making implicit concepts explicit.

### VI. Command-Query Separation (CQRS)

Application layer MUST separate:
- **Commands** (mutate state, return void/result): ConvertReferenceCommand, ConfigureAPICommand
- **Queries** (read-only, no side effects, cacheable): GetConversionHistoryQuery, GetAPIStatusQuery

Commands MUST NOT return domain data. Queries MUST NOT modify state. UI event handlers MUST only invoke commands or queries.

**Rationale**: Simplifies reasoning about state changes, enables caching, prepares for event sourcing.

### VII. Defensive Programming

ASSUME NOTHING, VALIDATE EVERYTHING.

- Check ALL Oxygen SDK returns (editor, page, workspace) for null
- Validate at multiple layers: Configuration → Input → HTTP → Parsing → Exception
- Use try-with-resources for ALL I/O operations
- Shutdown executors in `applicationClosing()`
- Translate technical errors to user-friendly i18n messages

**Rationale**: Prevents crashes, data loss, and poor user experience in diverse production environments.

### VIII. Async-First Design

ALL I/O operations MUST be asynchronous with non-blocking UI:
- NEVER block EDT (Event Dispatch Thread)
- Use `CompletableFuture` for background work
- ALWAYS update Swing components via `SwingUtilities.invokeLater()`
- Track operation state to prevent concurrent conflicts
- Shutdown `ExecutorService` properly

**Rationale**: Blocking EDT freezes Oxygen UI. Async-first ensures smooth user experience during slow API calls or large file processing.

### IX. Comprehensive i18n

ALL user-facing text MUST be internationalized via TEI XML translation files.

Support English (en_US), Simplified Chinese (zh_CN), Traditional Chinese (zh_TW) for: UI labels, error messages, success messages, help text, system prompts.

Automated tests MUST verify ALL keys exist in ALL language files. Pull requests adding user-visible strings MUST include translations for ALL supported languages.

**Rationale**: DILA serves international Buddhist studies community. Comprehensive i18n is non-negotiable for user adoption.

### X. Continuous Verification

Code changes MUST be validated immediately through automated testing:
- Auto-test after edits (`mvn test` after ANY code change)
- Verify before proceeding (check results before marking complete)
- Fix-test cycle: Fix → Test → Verify → Next
- Incomplete without verification

**Rationale**: Immediate verification catches regressions early and ensures confidence in each incremental change. Testing should be reflexive, not requested.

## Technology Standards

**Pure Java Architecture** (NO JavaScript/Java bridges):
- Language: Java 25 (may downgrade to Java 21 for compatibility)
- Build: Maven 3.x
- Platform: Oxygen XML SDK 27.1.0.3+
- UI: Java Swing (native desktop)
- HTTP: `HttpURLConnection` or `HttpClient` (Java 11+)
- Testing: JUnit 4, Mockito 4.11.0, AssertJ 3.24.2
- i18n: Java ResourceBundle with TEI XML files
- Encoding: UTF-8 everywhere

**Project Structure**:
- `src/main/java/.../domain/` - Domain Layer (pure logic)
- `src/main/java/.../application/` - Application Layer (commands/queries)
- `src/main/java/.../infrastructure/` - Infrastructure Layer (UI/HTTP/File/Oxygen)
- `src/test/java/` - Test mirror structure

**Rationale**: JavaScript bridge (v0.2.0) had stability issues. Pure Java ensures reliability and native Oxygen SDK integration.

## Governance

This constitution supersedes all development practices. Amendments require:
1. Documentation in `specs/constitution-amendment-[date].md`
2. Impact analysis and approval (unanimous for MAJOR, majority for MINOR/PATCH)
3. Version bump: MAJOR (breaking), MINOR (additive), PATCH (clarifications)
4. Template propagation to `.specify/templates/`

**Compliance Review**:
- Per PR: Check constitution compliance
- Monthly: Audit technical debt
- Quarterly: Review Event Storming/Problem Frames docs
- Annually: Full constitution review

**Enforcement**:
- Automated: Pre-commit hooks, CI pipeline (tests, coverage ≥80%, security scan)
- Human: Code reviewers verify architecture, BDD scenarios, i18n
- Consequences: Minor violations → fix request; Major violations → PR rejection; Repeated violations → architecture review meeting

**Complexity Justification**: Any principle violation MUST be justified in `specs/[feature]/plan.md` Complexity Tracking table with architect approval.

---

**Version**: 1.0.0 | **Ratified**: 2025-12-26 | **Last Amended**: 2025-12-26
