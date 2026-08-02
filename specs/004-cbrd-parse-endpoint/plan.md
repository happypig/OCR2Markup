# Implementation Plan: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Branch**: `004-cbrd-parse-endpoint` | **Date**: 2026-07-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-cbrd-parse-endpoint/spec.md`

## Summary

The plugin's AI Markup action currently calls an OpenAI-compatible chat completions endpoint
directly from each editor's machine using client-stored credentials and fine-tuned model names.
This plan replaces that call with a single POST to the DILA-hosted CBRD Parse endpoint
(`https://cbss.dila.edu.tw/cbrd/parse`), removes the six obsolete OpenAI preference fields
and the client-side system prompt, and adds three new preference fields: CBRD Parse endpoint
URL, shared bearer token (secure
storage), and request timeout. The existing review-then-replace workflow, undo history,
async executor lifecycle, and diagnostic-export machinery are preserved. New behavior is
added for client-side input pre-validation (FR-019), ignore-second-invocation concurrency
with in-flight selected XML feedback (FR-015), and cancel-on-close (FR-020).

## Technical Context

**Language/Version**: Java 8 (source/target/release = 8) running inside Oxygen XML Editor / SDK 27.1.0.3+ on Java 17

**Primary Dependencies**: Oxygen XML SDK 27.1.0.3 (provided), `org.json:json:20240303` (runtime), JUnit 4.13.2, Mockito 4.11.0, AssertJ 3.24.2, Hamcrest 2.2 (test)

**Storage**: Oxygen `WSOptionsStorage` plugin preferences; secure credential storage via `getSecretOption`/`setSecretOption`. No database/filesystem.

**Testing**: `mvn test` (JUnit 4, Mockito, AssertJ). Surefire runs `*Test`/`*Tests`, excludes `*IntegrationTest`. Headless AWT, parallel methods.

**Target Platform**: Oxygen XML Editor / SDK 27.1.0.3+ desktop plugin (Java Swing)

**Project Type**: Oxygen XML Editor plugin (desktop-app)

**Performance Goals**: Processing feedback in the result area within 500 ms; success/failure no later than timeout + 500 ms; editor responsive throughout (NFR-002). Timeout default 30,000 ms — the pre-feature AI Markup timeout. The 10,000 ms Ref-to-Link lookup default is deliberately NOT reused: `/cbrd/parse` performs a server-side model transformation, so a 10 s ceiling would regress the success path (FR-016).

**Constraints**:
- Java 8 bytecode baseline; no Java 11+ APIs (constitution Technology Standards)
- `HttpURLConnection` only (no `HttpClient`)
- Do not block the EDT; all UI updates via `SwingUtilities.invokeLater`
- Shared executor (`Executors.newFixedThreadPool(2)`) reused; shutdown in `applicationClosing()`
- Token never displayed/logged except as non-reversible fingerprint
- New i18n keys must ship in `en_US`, `zh_CN`, `zh_TW`
- Existing UTF-8 validation, tag removal, Ref-to-Link actions unchanged
- 002 diagnostics features preserved (only AI Markup OpenAI call path + preferences removed)

**Scale/Scope**: Single-user desktop editor plugin; one parse request per AI Markup invocation; selections up to 4,000 characters.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Event Storming First | Pass | `spec.md` Event Storming section defines actors, commands, domain events, policies, external boundaries. |
| II. Problem Frames | Pass | `spec.md` Problem Frames section separates problem domain, solution domain, shared phenomena. |
| III. Clean Architecture | Pass | New `CbrdParseApiClient` lives in `infrastructure/api`; new domain configuration object and error classifier live in `domain/`; command orchestration stays in `application/`; UI stays in `workspace/`. Dependencies point inward. |
| IV. TDD (BDD + TDD) | Pass | All 22 functional requirements carry Given-When-Then acceptance criteria (FR-003 → US2/4, FR-012 → US4/8, FR-016 → US1/7 + US2/5, FR-017 → US2/6, FR-018 → US4/10, FR-021 → US4/11, FR-022 → US4/9). `tasks.md` carries explicit 🚨 TEST GATE / 🎯 COMPLETION GATE tasks and `[T-SPEC]`/`[T-UNIT]`/`[T-INTEGRATION]` classification, with GREEN order Domain → Application → Infrastructure. Test seams: `*ForTests`, `CapturingConnectionFactory`. |
| V. DDD | Pass | New domain terms: `CbrdParseConfiguration`, `ParseError`, `DocumentLanguageResolver`; reuses `ReferenceConversionSession`, `SanitizedTroubleshootingRecord`. |
| VI. CQRS | **Deviation (tracked)** | `RunAiMarkupDiagnosticsCommand.execute` returns markup XML — domain data — which Principle VI forbids for Commands. The class also mutates nothing: the only state change is `replaceSelectionWithText` in the Replace action. Inherited from `002-ai-api-diagnostics`; this feature makes it the primary path. See Complexity Tracking. |
| VII. Defensive Programming | Pass | Null-check Oxygen SDK returns; validate token, endpoint URL, selection, timeout; disconnect HTTP connection in `finally`; map all 9 service error codes + generic + connectivity. |
| VIII. Async-First | Pass | Reuses shared executor, `CompletableFuture.supplyAsync`, `SwingUtilities.invokeLater`; adds cancel-on-close (FR-020). |
| IX. Comprehensive i18n | Pass | New keys added to `translation.xml` in all three languages; `TranslationBundleCompletenessTest` enforces. |
| X. Continuous Verification | Pass | `mvn test` after every code change; tests written first. |

**Gate result**: PASS with one tracked deviation (Principle VI) — see Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/004-cbrd-parse-endpoint/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── contracts/
│   └── openapi.yaml     # CBRD Parse endpoint contract
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (Maven project root: `Models/Gemini2.5/dila-ai-markup-plugin`)

```text
src/main/java/com/dila/dama/plugin/
├── workspace/
│   └── DAMAWorkspaceAccessPluginExtension.java   # AI Markup action, concurrency, async, replace, close hook
├── preferences/
│   └── DAMAOptionPagePluginExtension.java        # Remove OpenAI rows; add CBRD Parse rows
├── application/
│   └── command/
│       └── RunAiMarkupDiagnosticsCommand.java    # Retarget to CBRD Parse client/config
├── domain/
│   ├── model/
│   │   ├── CbrdParseConfiguration.java           # NEW: endpoint URL, token fingerprint, timeout
│   │   ├── ParseError.java                       # NEW: enum of 9 service error codes
│   │   └── AiMarkupDiagnosticSession.java        # Reused state machine
│   └── service/
│       ├── CbrdParseErrorClassifier.java          # NEW: maps ParseError -> guidance key
│       ├── DocumentLanguageResolver.java          # NEW: xml:lang on document root -> zh|jp
│       ├── RequestValidationService.java          # Add CbrdParseConfiguration overload
│       └── DiagnosticClassifier.java              # Retarget: retire OpenAI branches
├── infrastructure/
│   └── api/
│       ├── CbrdParseApiClient.java               # NEW: POST JSON, Bearer auth, HttpURLConnection
│       ├── CbrdParseRequest.java                 # NEW: {text, lang} — no other fields
│       ├── CbrdParseResponse.java                # NEW: success/failure result type
│       └── HttpUrlConnectionFactory.java         # Reused
└── (removed)
    ├── domain/model/MarkupServiceConfiguration.java   # OpenAI-shaped; retire
    ├── infrastructure/api/OpenAiCompatibleChatClient.java  # OpenAI client; retire
    └── infrastructure/api/OpenAiErrorResponse.java        # OpenAI error parsing; retire

src/main/resources/
├── plugin.xml
└── i18n/translation.xml                          # Add new keys (en_US, zh_CN, zh_TW)

src/test/java/com/dila/dama/plugin/
└── (mirror structure for new client/config/classifier tests)
```

**Structure Decision**: The plan keeps the existing clean-architecture layout. New CBRD Parse
classes are siblings of the OpenAI-era classes they replace. No new top-level packages are
introduced. The OpenAI-shaped `MarkupServiceConfiguration` and `OpenAiCompatibleChatClient`
are retired rather than overloaded, keeping the domain model honest.

### Retirement Sequencing (build-safety constraint)

Twelve files reference the three OpenAI-era classes being retired. Deleting them in Phase 2
would leave the module uncompilable until Phase 3, so `mvn test` could not run — violating
Principle X. The retirement is therefore two-step:

1. **Additive phase (Phase 2)**: introduce the CBRD Parse types; add a
   `validate(CbrdParseConfiguration)` overload to `RequestValidationService` and a CBRD Parse
   path to `AiMarkupDiagnosticSession`/`DiagnosticClassifier` *alongside* the OpenAI ones.
   Only genuinely unreferenced dead code (`processAIMarkup`, `createJSONRequest`,
   `parseOpenAIResponse` — verified: no callers) is removed here.
2. **Deletion phase (Phase 7)**: once every call site is retargeted, delete
   `MarkupServiceConfiguration`, `OpenAiCompatibleChatClient`, `OpenAiErrorResponse`, the
   OpenAI overloads, `OpenAiCompatibleChatClientTest`, and the five obsolete i18n keys —
   updating the seven remaining test classes and `LocalizationTest` in the same step.

`mvn test` must be green at the end of every task in both phases.

## Complexity Tracking

| Violation | Why needed | Simpler alternative rejected because | Approval |
|-----------|------------|--------------------------------------|----------|
| Principle VI (CQRS): `RunAiMarkupDiagnosticsCommand.execute` returns domain data (the marked-up XML via `Result.getMarkupResult()`) | The AI Markup workflow is review-then-replace: the transformation result must reach the UI for review *before* any state change. The only state mutation happens later, in the Replace action (`replaceSelectionWithText`). Renaming to a Query in this feature would ripple through 29 references across 6 files, 4 of which are `002`'s already-green workspace test classes. | Splitting into `ParseSelectedReferenceQuery` (returns markup) + `ReplaceSelectionCommand` (mutates, returns status) is the constitutionally clean shape and is the right target. It is deferred because it re-opens `002`'s tested surface inside a feature whose stated contract (spec.md Out of Scope) is to preserve that surface unchanged. Note also that `/cbrd/parse` is a POST that consumes server-side model budget and is not cacheable, so it does not cleanly satisfy Principle VI's Query definition (*"read-only, no side effects, cacheable"*) either — the constitution lacks a category for a non-cacheable remote read. | **Approved 2026-08-01** — jeffwu@dila.edu.tw (feature owner), recorded during `/speckit.analyze` remediation. Deviation is time-boxed: see Follow-up. |

**Follow-up (required)**: file a refactor item to split the command into a query + command pair
once this feature ships, and raise the missing "non-cacheable remote read" category with
`/speckit.constitution` so Principle VI can classify this operation without a deviation.
