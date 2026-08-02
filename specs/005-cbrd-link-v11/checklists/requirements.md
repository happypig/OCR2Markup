# Specification Quality Checklist: Restore Ref-to-Link against CBRD v1.1.0

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-03
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs) — *qualified, see Notes 1*
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders — *qualified, see Notes 1*
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

- [x] All functional requirements have clear acceptance criteria — *fixed in iteration 1, see Notes 2*
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification — *qualified, see Notes 1*

## Notes

**1. On "no implementation details" — qualified pass, deliberately.**

This feature's subject *is* an external interface. The facts that the citation
travels in the request body under the key `q`, that the service returns all
outcomes as a successful exchange, and that a previously-required header is now
optional are properties of the **problem domain** — the vendor's contract, which
the project does not control — not solution choices. Removing them would make
the requirements untestable and would recreate exactly the condition that caused
this outage: a specification that does not record what the service actually
expects.

Two genuine leaks were kept on purpose and should be read as constraints, not
design:

- **FR-003** ("use the project's existing JSON facility rather than hand-rolled
  string escaping") names a class of solution. It is retained because
  `exploration/ref2link_drift.md` §9.2 identifies hand-rolled escaping as a
  concrete hazard for this payload specifically — `<ref>` XML carrying quoted
  attributes and CJK text is the input that breaks naive escapers.
- **i18n message keys** (`error.no.results`, `error.api.failed`,
  `error.api.http`) appear in FR-004, FR-005, and US2. They are the stable
  identifiers for three distinct user-visible behaviours, and naming them is the
  only unambiguous way to state "these three outcomes must not be conflated" —
  which is the entire point of User Story 2.

**2. Iteration 1 finding — fixed.**

First pass failed *"All functional requirements have clear acceptance
criteria"*: FR-006, FR-008, FR-009, FR-011, FR-012, FR-013, and FR-014 stated a
requirement without Given-When-Then. Constitution Principle IV requires BDD
acceptance criteria on **all** functional requirements, not only the
behavioural ones. Criteria were added to all seven; FR-011 received two (one for
regression coverage, one for the preference-override path).

**3. Live-verified inputs.**

Unusually for a spec at this stage, the external facts are not assumptions. They
were checked against the production service on 2026-08-03: GET returns 404, POST
with a JSON body returns 200 with a resolved URL, an incomplete citation returns
200 with `success: false`, and the live contract declares version 1.1.0 with
`post` as the only method on `/link` and no `security` requirement. The spec's
Assumptions section is correspondingly short — what would normally be assumed is
recorded as evidence instead.

**4. Constitution Principle VI (CQRS) will be contested at plan time.**

Not a spec defect, but the planner should expect it: `ConvertReferenceCommand`
returns domain data (a URL), which Principle VI forbids of a Command, while
Query requires "read-only, no side effects, cacheable" — and a remote citation
lookup is not cacheable. This is the second feature to hit the same wall (004
recorded the first, for `RunAiMarkupDiagnosticsCommand`). Per `CLAUDE.md`, the
category gap is a pending constitution amendment, not a defect in this feature.
Expect a documented deviation in `plan.md` → Complexity Tracking.

- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
