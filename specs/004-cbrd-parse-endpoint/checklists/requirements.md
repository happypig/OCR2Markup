# Specification Quality Checklist: Replace AI Markup with Server-Hosted CBRD Parse Endpoint

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-07-21
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Seven clarifications were settled during the specify session on 2026-07-21: (1) remove the manual OpenAI client path entirely; (2) store the shared DILA parse token in the editor's secure credential storage; (3) infer `lang` from the document's `xml:lang` rather than add a new user-facing language preference; (4) ignore a second invocation while one is in flight and show the in-flight selected XML; (5) read `xml:lang` from the document root element only; (6) pre-validate empty and too-long selections client-side; (7) cancel and discard an in-flight request when the document or editor closes. All seven are recorded in the spec's Clarifications section and reflected in the functional requirements.
- **2026-08-01 (`/speckit.analyze` remediation)**: "All functional requirements have clear acceptance criteria" was ticked in error — FR-003, FR-012, FR-016, FR-017, and FR-018 had no Given-When-Then scenario. Scenarios were added (US1/7, US2/4-6, US4/8-11) and two requirements were introduced (FR-021 malformed endpoint URL, FR-022 diagnostics export preserved). The ticks above were re-verified against the amended spec on that date.
- The DILA CBRD Parse endpoint URL (`https://cbss.dila.edu.tw/cbrd/parse`) and the DILA CBRD OpenAPI reference are treated as user-facing product facts the requester named explicitly, not as implementation detail. They appear only where they describe the user-visible service the plugin talks to.
- The spec deliberately avoids naming Oxygen-specific APIs, Java types, Swing widgets, HTTP libraries, or other implementation choices. Those belong in `plan.md`, `research.md`, and `data-model.md` from the next phase.
- This feature supersedes the `003-aws-sso-secrets` scope for the AI Markup credential surface only. The full corporate SSO scope of `003` remains its own feature; this spec's Notes section records the expected relationship so a future `003` landing can layer SSO on top of the minimal preference surface introduced here.
