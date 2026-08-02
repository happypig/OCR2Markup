# Constitution Amendment — 2026-08-03

**Version**: 1.0.0 → **1.1.0** (MINOR)
**Ratified**: 2025-12-26 (unchanged) | **Last Amended**: 2026-08-03
**Approved by**: jeffwu@dila.edu.tw (project owner), 2026-08-03
**Constitution**: `.specify/memory/constitution.md`

Two changes, made together because they are the same kind of defect: in each case the
constitution asserted something the project does not do. The fix is the document, not a
workaround in the code.

---

## Change A — Principle VI gains a third category

### Before

Principle VI ("Command-Query Separation (CQRS)") offered exactly two categories:

- **Commands** — mutate state, return void/result. *"Commands MUST NOT return domain data."*
- **Queries** — *"read-only, no side effects, cacheable."*

### The problem

A read against a third-party service fits neither. It returns domain data, so it is not a
Command. It cannot be assumed cacheable — the remote answer can change, the service can be down,
the call can be rate-limited — so it is not a Query.

This was not hypothetical or marginal. **Both** of the project's remote operations fell in the
gap, and each had been recorded as an approved, time-boxed deviation:

| Operation | Endpoint | Returns | Recorded in |
|---|---|---|---|
| `RunAiMarkupDiagnosticsCommand.execute` | `POST /cbrd/parse` | markup | `specs/004-cbrd-parse-endpoint/plan.md` → Complexity Tracking |
| `ConvertReferenceCommand.execute` | `POST /cbrd/link` | a CBETA URL | `specs/005-cbrd-link-v11/plan.md` → Complexity Tracking |

Two of two operations violating a principle is evidence the categories are wrong, not the code.
The 005 plan made this explicit and attached an expiry clause: the deviation must not be renewed
a third time without this amendment, because a principle deviated from by default has quietly
become optional.

The gap also blocked remediation work. Backlog item 2 — splitting `RunAiMarkupDiagnosticsCommand`
into a properly-typed pair — could not proceed, because there was no correct type to split it
into. Any split would have encoded a guess in class names.

### After

A third category, **Remote Read**: returns data from an external system, changes no local state,
is not cacheable. It MUST NOT modify local state, MAY return domain data, and MUST NOT be cached
without an explicit documented invalidation policy.

The amendment also carries one rule that is not merely definitional. A Remote Read's failure modes
are part of its contract: it MUST distinguish "the external system answered, and the answer is
negative" from "the external system could not be reached." That is drawn directly from the 005
outage, where a routing error and an unresolvable citation both reached the editor as
`CBRD API error: HTTP 404`, sending people to hunt for network faults that did not exist.

### Deliberate choices

- **Additive.** Command and Query definitions are untouched. Nothing that was legal became
  illegal.
- **`ConvertReferenceCommand` was removed from the Command example list.** It had been listed as
  an exemplary Command since ratification while actually being a Remote Read — the misclassification
  was baked into the illustration.
- **Naming is SHOULD, not MUST.** Two Remote Reads are named `…Command` for historical reasons.
  Making the name mandatory would have created two fresh violations on the day the amendment
  landed, which is precisely the failure being corrected. Classification is by behaviour, not
  suffix; renaming is tracked separately.

---

## Change B — the coverage figure becomes true and enforced

### Before

> **Enforcement**: Automated: Pre-commit hooks, CI pipeline (tests, coverage ≥80%, security scan)

### The problem

The project has never met 80%, and nothing has ever checked. Coverage was first measured on
2026-08-02, seven months after ratification. Measured again 2026-08-03 across 376 tests:

| Metric | Value |
|---|---|
| Instructions | **59.6%** (7,462 / 12,511) |
| Branches | **45.4%** (533 / 1,173) |

JaCoCo is wired for `prepare-agent` and `report` only. There is no `check` goal, so the mandated
number was never enforced in either direction.

A mandated number the build never verifies is a defect however it is resolved. Its real cost is
not the missing coverage — it is that it teaches readers the document records intentions rather
than facts, which devalues every other clause in it.

### After

A ratchet floor of **59% instructions / 45% branches**, enforced by a JaCoCo `check` goal that
fails the build. The floor MUST be raised when exceeded by a durable margin and MUST NOT be
lowered.

Stated explicitly as a ratchet, not a target: it records the level already reached so it cannot be
lost, and says nothing about the level worth aiming for. Choosing 59/45 rather than 59.6/45.4
leaves a small margin so that ordinary refactoring does not redden the build.

### Why the number is low, and why that supports Principle III

| Layer | Coverage |
|---|---|
| `infrastructure.export` | 100% |
| `application.query` | 96% |
| `infrastructure.api` | **91.5%** |
| `application.command` | 90% |
| `domain.service` | 85% |
| `domain.model` | 68% |
| `preferences`, `util` | 69% |
| **`workspace`** | **35.4%** ← `DAMAWorkspaceAccessPluginExtension`, ~1800 lines of Swing/Oxygen UI |
| `infrastructure.release` | 8.3% |

The domain, application and infrastructure layers are well covered. The aggregate is dragged down
almost entirely by the UI class. That is evidence *for* Principle III (Clean Architecture), not
against it: logic kept out of the UI gets tested, logic that leaks into it does not. Raising the
floor is therefore mostly a matter of moving logic out of the UI — not of writing Swing tests.

---

## Impact analysis

| Area | Impact |
|---|---|
| Existing code | **None required.** Change A reclassifies two operations that already behave correctly; Change B lowers a floor that was never enforced. No source file must change to comply. |
| `specs/004-cbrd-parse-endpoint/plan.md` | Its deviation is superseded. Left as historical record — 004 is merged. |
| `specs/005-cbrd-link-v11/plan.md` | Its Constitution Check should be re-run and the Complexity Tracking table **deleted rather than honoured**, per that plan's expiry clause. 005 has not merged; this amendment lands first, deliberately. |
| Backlog item 2 | **Unblocked.** `RunAiMarkupDiagnosticsCommand` can now be classified and split. |
| Backlog items 1 and 3 | **Closed** by this amendment. |
| `pom.xml` | **Follow-up required.** The `check` goal is not wired yet. Until it is, the ratchet is stated but not enforced — the same defect in a smaller size. |
| Templates | No changes needed. The Constitution Check in `plan-template.md` is generated per-feature from this file rather than hardcoding a principle list; `spec-template.md` and `tasks-template.md` align with Principle IV as before. |

## Versioning rationale

**MINOR (1.0.0 → 1.1.0).** One principle materially expanded and one governance clause corrected.
Nothing was removed, and nothing previously compliant became non-compliant — which rules out
MAJOR. Both changes alter substance rather than wording, which rules out PATCH.

## Governance checklist

- [x] Documented in `specs/constitution-amendment-2026-08-03.md` (this file)
- [x] Impact analysis completed (above)
- [x] Approval recorded — jeffwu@dila.edu.tw, project owner, 2026-08-03
- [x] Version bumped 1.0.0 → 1.1.0; footer `Last Amended` updated
- [x] Sync Impact Report updated at the head of `constitution.md`
- [x] Template propagation reviewed — no changes required
- [ ] JaCoCo `check` goal wired into `pom.xml` — **follow-up, not part of this document change**
