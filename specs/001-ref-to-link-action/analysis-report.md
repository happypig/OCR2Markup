## Specification Analysis Report

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|----|----------|----------|-------------|---------|----------------|
| C1 | Constitution Alignment | CRITICAL | `.specify/memory/constitution.md:128`, `specs/001-ref-to-link-action/plan.md:14` | Plan targets Java 8 while constitution mandates Java 25 (may downgrade to 21). | Align plan/spec to constitution or file a constitution amendment per governance. |
| C2 | Constitution Alignment | CRITICAL | `.specify/memory/constitution.md:78`, `specs/001-ref-to-link-action/plan.md:94` | Plan allows command to return a result; constitution forbids commands returning domain data. | Redesign command/return type or amend constitution with justification. |
| C3 | Constitution Alignment | CRITICAL | `specs/001-ref-to-link-action/spec.md:124`, `specs/001-ref-to-link-action/spec.md:214`, `specs/001-ref-to-link-action/spec.md:215` | FR-014/FR-015 lack explicit BDD acceptance scenarios, violating Principle IV. | Add Given/When/Then scenarios covering preferences + proxy/headers. |
| I1 | Inconsistency | HIGH | `specs/001-ref-to-link-action/spec.md:197`, `specs/001-ref-to-link-action/spec.md:174`, `specs/001-ref-to-link-action/data-model.md:413` | `<p>` is optional in FR-003 but required in user story and data-model validation. | Decide required vs optional and align spec/data-model/tests/tasks. |
| I2 | Inconsistency | HIGH | `specs/001-ref-to-link-action/spec.md:205`, `specs/001-ref-to-link-action/data-model.md:313`, `specs/001-ref-to-link-action/data-model.md:320` | Spec requires sending raw `<ref>` as-is; data-model describes constructing XML from transformed components. | Choose raw vs normalized request; update spec/plan/tasks accordingly. |
| I3 | Inconsistency | HIGH | `specs/001-ref-to-link-action/spec.md:213`, `specs/001-ref-to-link-action/tasks.md:103` | Spec mandates auto-retry on timeout; tasks state "no auto-retry." | Clarify retry policy and align tasks + data-model. |
| G1 | Coverage Gap | MEDIUM | `specs/001-ref-to-link-action/spec.md:230`, `specs/001-ref-to-link-action/spec.md:231`, `specs/001-ref-to-link-action/tasks.md:155` | SC-001/SC-002 have no explicit tasks/tests for 30s workflow or 95% accuracy. | Add perf/accuracy tests or mark criteria as aspirational with rationale. |
| G2 | Coverage Gap | MEDIUM | `specs/001-ref-to-link-action/spec.md:215`, `specs/001-ref-to-link-action/tasks.md:66` | FR-015 requires proxy + User-Agent/Accept-Charset; tasks mention only Referer/timeout/encoding. | Add explicit tasks/tests for proxy and headers. |
| A1 | Ambiguity | LOW | `specs/001-ref-to-link-action/spec.md:212` | "Appropriate error messages" is vague. | Define required message keys/format in BDD scenarios. |
| T1 | Terminology Drift | LOW | `specs/001-ref-to-link-action/spec.md:205`, `specs/001-ref-to-link-action/plan.md:25`, `specs/001-ref-to-link-action/tasks.md:66` | "CBETA API" vs "CBRD API" naming varies across artifacts. | Standardize naming and add a glossary note if both are valid. |

**Coverage Summary Table:**

| Requirement Key | Has Task? | Task IDs | Notes |
|-----------------|-----------|----------|-------|
| fr-add-ref-to-link-menu | Yes | T026 | - |
| fr-display-selected-ref | Yes | T028 | - |
| fr-extract-ref-components | Yes | T013, T020, T021 | `<p>` requirement inconsistency (see I1). |
| fr-auto-call-api-and-retry-button | Yes | T027, T028, T029 | - |
| fr-call-api-with-ref-as-is | Yes | T023, T024 | Spec vs data-model conflict (see I2). |
| fr-display-cbeta-link | Yes | T029 | - |
| fr-show-replace-button | Yes | T029 | - |
| fr-replace-ptr-elements | Yes | T033, T035 | - |
| fr-update-checked-attribute | Yes | T033, T035 | - |
| fr-hide-replace-after | Yes | T036 | - |
| fr-validate-ref-selection | Yes | T020, T040 | - |
| fr-display-error-messages | Yes | T040-T043, T045 | FR-012 ambiguity (see A1). |
| fr-timeout-retry | Yes | T023, T044 | Retry policy inconsistency (see I3). |
| fr-configure-api-endpoint | Yes | T007, T008 | - |
| fr-honor-proxy-and-headers | Partial | T023 | Missing proxy/UA/charset tasks (see G2). |
| sc-workflow-under-30s | No | - | No perf task (see G1). |
| sc-95pct-accuracy | No | - | No accuracy task (see G1). |
| sc-timeout-feedback-10s | Yes | T023, T044 | - |
| sc-ui-pattern-consistent | Yes | T002, T026-T028 | - |
| sc-zero-data-loss | Yes | T031, T033, T035 | - |

**Constitution Alignment Issues:**
- C1 (Java version mismatch)
- C2 (CQRS command returns result)
- C3 (Missing BDD acceptance for FR-014/FR-015)

**Unmapped Tasks:** None detected.

**Metrics:**

- Total Requirements: 20
- Total Tasks: 48
- Coverage %: 90%
- Ambiguity Count: 1
- Duplication Count: 0
- Critical Issues Count: 3

**Next Actions**

- Resolve CRITICAL constitution conflicts before `/speckit.implement`.
- Align spec/data-model/tasks for `<p>` requiredness, request construction, and retry policy.
- Add tasks/tests for SC-001/SC-002 and FR-015 (proxy/headers).

Would you like me to suggest concrete remediation edits for the top 3 issues?
