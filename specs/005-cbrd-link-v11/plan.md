# Implementation Plan: Restore Ref-to-Link against CBRD v1.1.0

**Branch**: `005-cbrd-link-v11` | **Date**: 2026-08-03 | **Spec**: [spec.md](./spec.md)

**Status**: Complete — implemented and validated 2026-08-03. 376 tests, 0 failures, 2 skipped (opt-in live probe); `infrastructure.api` coverage 90.0% → 91.5%; one production file changed (+34/-11). Execution notes, including five deviations from this plan, are in [tasks.md](./tasks.md) → Notes.

**Input**: Feature specification from `/specs/005-cbrd-link-v11/spec.md`

## Summary

Ref-to-Link is non-functional in production. CBRD moved `/cbrd/link` from
`GET ?q=<xml>` (v1.0.0) to `POST` with a JSON body (v1.1.0); the plugin still
issues the GET, no route matches, and every conversion fails with
`CBRD API error: HTTP 404` — a transport message for what editors read as a
citation problem.

The fix is small and confined: inside `CBRDAPIClient.executeOnce`, stop
assembling a query string and start writing a JSON body. Response handling,
the response DTO, the seven i18n keys the client raises, the retry policy, the Replace rewrite,
and the URL preference are all unchanged — verified against the live service,
which returns HTTP 200 for every documented outcome.

The larger part of the work is making sure this cannot happen silently again.
The current `/link` tests **positively assert the broken shape** and use a
private test double that cannot observe the HTTP method or the request body, so
they were structurally incapable of catching the drift and would fail on the
fix. They move to the shared capture seam built for 004, gain method/body
assertions, and are joined by a two-layer conformance guard (offline version
pin, opt-in live probe).

## Technical Context

**Language/Version**: Java 8 (`source`/`target`/`release` = 8) — constitution
Technology Standards; the Oxygen 27.1.0.3+ host runs Java 17 but that does not
raise the plugin baseline.

**Primary Dependencies**: Oxygen SDK 27.1.0.3+ (provided), `org.json` 20240303
(compile). No new runtime dependency. No new test dependency (see research D3).

**Storage**: N/A — no persistence. Plugin preferences are unchanged.

**Testing**: JUnit 4.13.2, Mockito 4.11.0, AssertJ 3.24.2, Hamcrest 2.2. HTTP is
exercised through the injected `HttpUrlConnectionFactory` seam; the shared
`CapturingConnectionFactory` records method, headers, body, timeouts, and
disconnection.

**Target Platform**: Oxygen XML Editor desktop plugin (Windows/macOS/Linux),
Java Swing UI.

**Project Type**: Desktop plugin — single Maven module at
`Models/Gemini2.5/dila-ai-markup-plugin`.

**Performance Goals**: Unchanged. Existing connect/read timeouts and the
retry-on-timeout-only policy are preserved; no new network round trips are
introduced on the conversion path.

**Constraints**:
- `mvn test` MUST remain offline-runnable and deterministic. The live contract
  probe is opt-in behind an environment flag.
- Java 8 language level — no `HttpClient`, no `var`, no new switch forms.
- All I/O stays off the EDT (Principle VIII); this change does not alter
  threading.

**Scale/Scope**: One production method (`CBRDAPIClient.executeOnce`), two
existing test classes, one vendored contract file, and two new tests. No change
to the domain or application layers.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Gate | Status |
|---|---|---|
| I. Event Storming First | Domain events unchanged — this is a transport correction inside an existing flow (`ReferenceSelected → APICallSucceeded`). No new events, actors, or aggregates. | **PASS** (no new domain surface) |
| II. Problem Frames | Problem domain (the vendor's contract) and solution domain (request construction) are cleanly separated, and the shared phenomenon — the wire request — is exactly what the new assertions observe. | **PASS** |
| III. Clean Architecture | The change is entirely in the Infrastructure layer. Domain and Application layers are untouched; no new dependency points outward. | **PASS** |
| IV. TDD (BDD + TDD) | All 14 FRs carry BDD criteria (spec.md). Task ordering must follow RED → TEST GATE → GREEN → COMPLETION GATE. The RED phase is unusually literal here: the existing assertions must be seen failing against the new client before they are rewritten. | **PASS** (enforced in tasks.md) |
| V. DDD | Ubiquitous language preserved — 參考文獻/Reference, 典籍/Canon, Hyperlink. No generic `Request`/`Response` names introduced; the contract's own `LinkRequest`/`LinkResponse` terms stay confined to the contract artifact. | **PASS** |
| VI. Command / Query / Remote Read | `ConvertReferenceCommand.execute` performs a **Remote Read**: it returns data from an external system (`POST /cbrd/link`), changes no local state, and is not cached. It satisfies that category's rules, including the requirement that a Remote Read distinguish "the external system answered negatively" from "could not be reached" — which is User Story 2 of this feature. The `…Command` suffix is a SHOULD, not a violation. | **PASS** |
| VII. Defensive Programming | Preserved and slightly extended: the output stream is written in try-with-resources; null/blank citation handling is unchanged; the payload is built through `JSONObject` rather than string concatenation. | **PASS** |
| VIII. Async-First | Unchanged. The conversion already runs off the EDT and reports back via `SwingUtilities.invokeLater`. This feature does not touch threading. | **PASS** |
| IX. Comprehensive i18n | No new user-facing strings expected. All seven keys the client raises keep their semantics (`error.api.http` narrows: it is no longer reachable for outcomes the service returns normally). Service-supplied text (e.g. `經號或頁碼 至少要有一個`) is passed through as data, not translated. If any key is added, all three languages ship in the same change. | **PASS** |
| X. Continuous Verification | `mvn test` after every change; the feature is not complete until the suite is green **and** the behaviour is confirmed in a live Oxygen session (FR-014, SC-008). | **PASS** |

**Technology Standards**: no Java baseline change, no new runtime dependency, no
JavaScript bridge, UTF-8 throughout. **PASS**

**Post-Phase 1 re-check**: no violations. The design adds no project, no layer,
and no dependency.

**Re-checked 2026-08-03 against constitution v1.1.0**: the single deviation this
plan originally recorded — Principle VI having no category for a non-cacheable
remote read — no longer exists. The amendment added **Remote Read**, and
`ConvertReferenceCommand` classifies cleanly as one. **All 10 principles pass.**
The Complexity Tracking table was deleted rather than honoured, per its own
expiry clause.

## Project Structure

### Documentation (this feature)

```text
specs/005-cbrd-link-v11/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output — D1..D5 decisions
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output — validation guide
├── contracts/
│   └── README.md        # Phase 1 output — canonical contract path + v1.0.0→v1.1.0 delta
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
Models/Gemini2.5/dila-ai-markup-plugin/
├── pom.xml                                             # unchanged — no new dependency
└── src/
    ├── main/java/com/dila/dama/plugin/
    │   ├── infrastructure/api/
    │   │   ├── CBRDAPIClient.java                      # CHANGED — request construction only
    │   │   ├── CBRDResponse.java                       # unchanged
    │   │   ├── CBRDAPIException.java                   # unchanged
    │   │   ├── HttpUrlConnectionFactory.java           # unchanged — seam already in place
    │   │   └── CbrdParseApiClient.java                 # unchanged — reference for status recovery
    │   ├── application/command/ConvertReferenceCommand.java   # unchanged
    │   ├── domain/service/RefElementRewriter.java             # unchanged
    │   └── workspace/DAMAWorkspaceAccessPluginExtension.java  # unchanged — URL default still correct
    └── test/java/com/dila/dama/plugin/infrastructure/api/
        ├── CBRDAPIClientTest.java                      # CHANGED — migrate to shared seam
        ├── CBRDAPIClientErrorHandlingTest.java         # CHANGED — migrate to shared seam
        ├── CapturingConnectionFactory.java             # unchanged — shared seam, reused
        ├── RepoRootLocator.java                        # NEW — walks up to find specs/, fails loudly
        ├── CBRDContractConformanceTest.java            # NEW — offline version/shape guard
        └── CBRDLiveContractProbeTest.java              # NEW — opt-in live probe

specs/001-ref-to-link-action/contracts/
└── cbrd-api.yaml                                       # CHANGED — v1.0.0/GET → v1.1.0/POST (canonical, single copy)
```

**Structure Decision**: No structural change. This is a targeted correction
inside the existing single-module layout, touching one production class in
`infrastructure/api` plus its tests. The two new test classes sit alongside the
existing ones in the mirrored test tree. The vendored contract is updated **in
place** at its existing path rather than copied under this feature's
`contracts/` directory — see research D5; a second copy of a contract is the
defect this feature exists to remove.

## Complexity Tracking

> **Empty — no deviations.** All 10 principles pass under constitution v1.1.0.

This plan originally carried one deviation: `ConvertReferenceCommand.execute`
returns domain data, which Principle VI forbade of a Command, while Query
required cacheability that a remote citation lookup cannot offer. Neither
category fitted.

That table is gone because the gap was closed, not because it was waived.
Constitution **v1.1.0** (2026-08-03,
`specs/constitution-amendment-2026-08-03.md`) added the **Remote Read**
category, and the operation now classifies correctly.

Recorded here because the sequence is the point. The deviation was written with
an expiry clause stating it must not be renewed a third time — this was the
second instance, after 004's for `RunAiMarkupDiagnosticsCommand`. The amendment
was then deliberately scheduled **before this branch merges rather than after**,
so the clause could be discharged instead of inherited. Two of two operations
violating a principle was evidence the categories were wrong; the fix belonged
in the constitution, and 005 merges carrying no deviation at all.
