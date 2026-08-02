<!--
=== CONSTITUTION SYNC IMPACT REPORT ===
Version Change: 1.0.0 → 1.1.0
Type: MINOR (one principle materially expanded, one governance clause corrected;
      nothing removed, nothing redefined incompatibly)

Modified Principles:
- VI. "Command-Query Separation (CQRS)" → "Command, Query, and Remote Read Separation"
  Added a third category. The two-way split was empirically wrong: a remote read returns
  domain data (not a Command) and is not cacheable (not a Query). Both of the project's
  remote operations fell in that gap and had been recorded as approved deviations (004 for
  RunAiMarkupDiagnosticsCommand, 005 for ConvertReferenceCommand). Command and Query
  definitions are unchanged; ConvertReferenceCommand was moved out of the Command example
  list, where it had been misclassified since ratification.

Modified Sections:
- Governance → Enforcement. "coverage ≥80%" replaced by a ratchet floor of 59% instructions
  / 45% branches, enforced by a JaCoCo check goal. The 80% was never met and never checked;
  measured 2026-08-03 it stood at 59.6% / 45.4%.

Added Sections: None
Removed Sections: None

Templates Requiring Updates:
✅ .specify/templates/plan-template.md - Constitution Check is generated per-feature from this
   file; no hardcoded principle list to update
✅ .specify/templates/spec-template.md - BDD acceptance criteria still align with Principle IV
✅ .specify/templates/tasks-template.md - Test-first structure still aligns with Principle IV

Follow-up TODOs:
- Wire the JaCoCo `check` goal into
  Models/Gemini2.5/dila-ai-markup-plugin/pom.xml at the floor above. Until then the ratchet is
  stated but not enforced, which is the same defect in a smaller size.
- Re-run the Constitution Check in specs/005-cbrd-link-v11/plan.md and DELETE its Complexity
  Tracking deviation table rather than honouring it, per that plan's own expiry clause.
- Backlog item 2 (split RunAiMarkupDiagnosticsCommand) is unblocked: it can now be classified.

Amendment record: specs/constitution-amendment-2026-08-03.md
Date: 2026-08-03
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

### VI. Command, Query, and Remote Read Separation

Every application-layer operation MUST be classified as exactly one of three kinds:

- **Commands** (mutate local state; return void or a status result): ConfigureAPICommand
- **Queries** (read local state; no side effects; cacheable): GetConversionHistoryQuery, GetAPIStatusQuery
- **Remote Reads** (return data from an external system; change no local state; not cacheable): ConvertReferenceCommand (POST /cbrd/link), RunAiMarkupDiagnosticsCommand (POST /cbrd/parse)

Rules per kind:

- Commands MUST NOT return domain data.
- Queries MUST NOT modify state, and MUST be safe to serve from a cache.
- Remote Reads MUST NOT modify local state; MAY return domain data; and MUST NOT be cached
  without an explicit, documented invalidation policy. Their failure modes are part of their
  contract: a Remote Read MUST distinguish "the external system answered, and the answer is
  negative" from "the external system could not be reached."

UI event handlers MUST only invoke one of these three.

A class SHOULD be named for its kind. Two existing Remote Reads are named `…Command` for
historical reasons; renaming them is tracked separately and is NOT a violation of this principle.
Classification is by behaviour, not by suffix.

**Rationale**: Simplifies reasoning about state changes, enables caching, prepares for event
sourcing. The third kind exists because the two-way split was empirically wrong for this codebase:
a remote citation lookup returns domain data (so it is not a Command) and cannot be assumed
cacheable (so it is not a Query). Both of the project's remote operations fell in that gap, and
each was recorded as an approved deviation — 004 for `RunAiMarkupDiagnosticsCommand`, 005 for
`ConvertReferenceCommand`. Two of two operations violating a principle is evidence that the
categories were wrong, not the code. The Remote Read rule about failure modes is drawn from the
005 outage, where a routing error and an unresolvable citation reached the editor as the same
message.

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
- Host Runtime Platform: Oxygen XML Editor / SDK 27.1.0.3+ running on Java 17
- Plugin Language Baseline: Java 8 source and bytecode compatibility unless a feature plan explicitly approves a higher baseline
- Build: Maven 3.x
- Platform: Oxygen XML SDK 27.1.0.3+
- UI: Java Swing (native desktop)
- HTTP: `HttpURLConnection` for Java 8 compatibility; `HttpClient` may be used only if the feature plan raises the plugin baseline
- Testing: JUnit 4, Mockito 4.11.0, AssertJ 3.24.2
- i18n: Java ResourceBundle with TEI XML files
- Encoding: UTF-8 everywhere

**Java Version Policy**:
- Distinguish between the Java version required by the Oxygen host application and the Java version used to compile the plugin
- The Oxygen host runtime requirement does NOT by itself permit raising the plugin compilation target
- Default rule: plugin code MUST compile with `source=1.8`, `target=1.8`, and `release=8`
- A feature MAY raise the plugin baseline only when the implementation plan documents:
  - the required Java feature or library
  - the compatibility impact on supported Oxygen deployments
  - the migration and verification strategy
  - explicit approval in the plan's Complexity Tracking section

**Compatibility Rule**:
- Prefer the lowest practical plugin Java baseline that remains compatible with supported Oxygen deployments
- New code MUST NOT introduce Java language features, standard-library APIs, or dependencies that break the approved plugin baseline
- If a higher Java baseline is adopted, all affected documentation, build configuration, CI validation, and feature plans MUST be updated in the same change

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
- Automated: Pre-commit hooks, CI pipeline (tests, coverage ratchet, security scan)

**Coverage ratchet**: the build MUST enforce a minimum of **59% instructions** and **45%
branches**, via a JaCoCo `check` goal that fails the build — not a figure recorded in a report
that nobody reads. The floor MUST be raised whenever it is exceeded by a durable margin, and MUST
NOT be lowered. It is a ratchet, not a target: it records the level already reached so that it
cannot be lost, and says nothing about the level worth aiming for.

This replaces a mandated `≥80%` that the project never met and the build never checked. Measured
2026-08-03 across 376 tests: 59.6% instructions, 45.4% branches. A mandated number the build never
verifies is a defect in whichever direction it is resolved, because it teaches readers that the
document describes intentions rather than facts.

Where the gap is argues *for* Principle III rather than against it: the domain, application and
infrastructure layers sit at 85–100% (`infrastructure.api` is 91.5%), while `workspace`
(`DAMAWorkspaceAccessPluginExtension`, ~1800 lines of Swing/Oxygen UI) is 35.4% and
`infrastructure.release` is 8.3%. Logic kept out of the UI gets tested; logic that leaks into it
does not. Raising the floor is therefore mostly a question of moving logic out of the UI, not of
writing UI tests.
- Human: Code reviewers verify architecture, BDD scenarios, i18n
- Consequences: Minor violations → fix request; Major violations → PR rejection; Repeated violations → architecture review meeting

**Complexity Justification**: Any principle violation MUST be justified in `specs/[feature]/plan.md` Complexity Tracking table with architect approval.

---

**Version**: 1.1.0 | **Ratified**: 2025-12-26 | **Last Amended**: 2026-08-03
