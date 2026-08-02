# OCR2Markup Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-05

## Active Technologies

- Java 8 (1.8+) - compatible with Oxygen XML SDK 27.1.0.3+ (001-ref-to-link-action)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Java 8 (1.8+) - compatible with Oxygen XML SDK 27.1.0.3+

## Code Style

Java 8 (1.8+) - compatible with Oxygen XML SDK 27.1.0.3+: Follow standard conventions

## Recent Changes

- 001-ref-to-link-action: Added Java 8 (1.8+) - compatible with Oxygen XML SDK 27.1.0.3+

<!-- MANUAL ADDITIONS START -->

## Where the code actually is

The generated "Project Structure" above is a placeholder. The real Maven module is:

```text
Models/Gemini2.5/dila-ai-markup-plugin/
  src/main/java/com/dila/dama/plugin/{domain,application,infrastructure,workspace,preferences}/
  src/main/resources/i18n/translation.xml      # all user-facing text, 3 languages
  src/test/java/...                            # mirrors main
```

Build from that directory. `mvn clean test` runs the suite; `mvn clean install`
also produces `dilaAIMarkupPlugin.zip` + `dilaAIMarkupPlugin.xml` at the module
root (the assembly is bound to `install`, not `package`).

## Current state — 2026-08-02

Plugin **0.5.0**. Feature `004-cbrd-parse-endpoint` is complete, manually
validated in Oxygen, and merged to `main`: AI Markup now calls the DILA CBRD
Parse endpoint instead of an OpenAI-compatible endpoint on the editor's machine.
**365 tests, 0 failures**, all 86 tasks signed off.

**Coverage baseline (first ever measured, 2026-08-02): 59.5% instructions,
45.1% branches.** The constitution's Enforcement section mandates ≥ 80%, so the
project is well short of its own rule and always has been — nothing measured it
until now. `mvn test` writes `target/site/jacoco/index.html`.

Where the gap is, and why it matters architecturally:

```
infrastructure.export  100%     application.command   90%     domain.service   85%
application.query       96%     infrastructure.api    90%     domain.model     68%
preferences             69%     util                  69%
workspace            35.4%   ← DAMAWorkspaceAccessPluginExtension, ~1800 lines
infrastructure.release  8.3%
```

The domain/application/infrastructure layers 004 built sit at 85-100%. The
number is dragged down almost entirely by `workspace`, the Swing/Oxygen UI
class. That is evidence for Principle III rather than against it: logic that
stays out of the UI gets tested, logic that leaks into it does not.

JaCoCo is deliberately **measure-only** — no `check` goal. Wiring an 80%
threshold today would fail the build immediately. Decide the threshold once
someone has looked at the number.

## Next up

1. **`005` — Ref-to-Link v1.1.0 drift (urgent).** Ref-to-Link is broken in
   production: CBRD moved `/link` from GET to POST+JSON, the plugin still issues
   GET, and editors see `CBRD API error: HTTP 404`. Full investigation, with a
   verified-corrections section, is in `exploration/ref2link_drift.md` — read §9
   before implementing §6.
2. **`006` — single-source plugin version.** `src/main/java` is not
   resource-filtered, so `${project.version}` cannot reach it. Two hardcoded
   `User-Agent` literals drift as a result; `CBRDAPIClient`'s has been stale
   since 0.4.3. Marked `TODO(005)` in `CbrdParseApiClient`.

If both run on one branch, Phase A (005) must **merge** before Phase B (006)
starts — otherwise an outage fix waits on hygiene work.

## Backlog

Four open items. Each names where it gets done and what closes it — **delete the
entry when its closing condition is met.** A stale backlog is worse than none:
this file once claimed T055/T057 were open for an hour after they were signed
off, and anyone reading it went looking for finished work.

None of these block 005 or 006. Kept here rather than in a commit message or a
closed document, because those are where the previous round of these rotted. If
this list ever exceeds ~5 items, move it to GitHub Issues (`gh` is authenticated,
`/speckit-taskstoissues` converts) — an always-loaded file has a real cost per
session.

| # | Item | Where | Closes when |
|---|------|-------|-------------|
| 1 | Coverage threshold | `/speckit-constitution` | amendment lands |
| 2 | Command/query split | `/speckit-specify` (blocked on 3) | feature merges |
| 3 | Principle VI category | `/speckit-constitution` (with 1) | amendment lands |
| 4 | Stale User-Agent literal | 005 `tasks.md` | 005 merges |

**1 and 3 are one conversation — do them in a single amendment pass.** Both are
cases where the constitution asserts something the project does not do, so the
fix is the constitution, not a workaround. Governance requires
`specs/constitution-amendment-[date].md`, impact analysis, a version bump, and
template propagation; expect MINOR (1.1.0) since both are additive/clarifying.

1. **Coverage threshold.** Measuring is done — 59.5% instructions, 45.1%
   branches. Enforcing is not: JaCoCo has no `check` goal. Either lower the
   Enforcement line to a figure that is true today and ratchet it up, or drop it.
   A mandated number the build never checks is the defect, in whichever
   direction it gets resolved. Wiring 80% as-is fails the build immediately.

3. **Principle VI needs a category for a non-cacheable remote read.** Both
   `/cbrd/parse` and `/cbrd/link` are POSTs that return data, change no local
   state, and cannot be cached — neither Command ("MUST NOT return domain data")
   nor Query ("read-only, no side effects, cacheable") fits.

   This is **2 of 2**, not a one-off: `RunAiMarkupDiagnosticsCommand.execute`
   returns markup, and `ConvertReferenceCommand.execute` returns
   `ConvertReferenceResult.getUrl()`. Two of two commands violating a principle
   is evidence the categories are wrong, not the code.

   **This has now happened.** 2026-08-03: 005's Constitution Check hit the same
   wall and recorded the predicted *second* deviation, in
   `specs/005-cbrd-link-v11/plan.md` → Complexity Tracking. That plan carries an
   **expiry clause**: the deviation MUST NOT be renewed a third time without this
   amendment landing, because a principle deviated from by default has quietly
   become optional.

   **Scheduled: before `005` merges, while the branch is in review** (decided
   2026-08-03). Not after. Two reasons the later slot was rejected: an amendment
   parked behind a merge slips behind 006, and 005 would land in permanent
   history carrying a deviation it did not need to. Implementation of 005 goes
   first — an outage fix does not wait on governance — but the amendment lands
   before the branch does, and 005's Constitution Check is then re-run clean.

   Concrete shape when it is written: add a third category alongside Command and
   Query — *Remote Read: returns data from an external system, changes no local
   state, cannot be assumed cacheable. MUST NOT modify local state; MAY return
   domain data; MUST NOT be cached without an explicit documented policy.* That
   one addition covers both existing offenders and unblocks item 2.

2. **Split `RunAiMarkupDiagnosticsCommand` into a query + command pair.**
   Blocked on 3 — it cannot be classified correctly until the category exists.
   Already recorded as an approved, time-boxed deviation in
   `specs/004-cbrd-parse-endpoint/plan.md` → Complexity Tracking → Follow-up.
   The name is misleading too: it is the primary AI Markup path, not a
   diagnostic. `ConvertReferenceCommand` needs the same treatment.

4. **`CBRDAPIClientTest:39` pins `"DILA-AI-Markup/0.4.2"`.** A correct version
   bump turns that test red, so the suite currently defends the bug. 005 rewrites
   lines 36-39 of that exact block anyway (`?q=` → JSON body), so fix it there as
   the invariant form, not another frozen string. If 005 somehow skips it, 006
   removes the literal entirely.

## Standing rules this codebase earned the hard way

- **Retire additively.** Introduce the replacement, migrate every call site,
  delete last. 004 had 12 files referencing 3 classes; deleting first would have
  left the module uncompilable for four phases with `mvn test` unrunnable
  (violating Principle X).
- **Assert invariants, not literals.** `CBRDAPIClientTest:39` pins
  `"DILA-AI-Markup/0.4.2"`. When someone bumps the version correctly, that test
  goes red — so the suite now defends the bug. Prefer
  `isEqualTo("DILA-AI-Markup/" + version)`.
- **Fakes cannot see host behaviour.** Every HTTP test injects a fake
  connection. Oxygen installs its own URL stream handler, so a non-2xx response
  throws `HttpExceptionWithDetails` instead of returning a status — all 354
  tests passed while that bug was live. Verify transport behaviour in Oxygen or
  against a real server, not only against a fake.
- **`.gitignore` does not apply to tracked files.** This repo had four ignore
  entries that never took effect and one that hid a stale orphan. If a file is
  tracked, ignoring it is a no-op; use `git rm --cached`.
- **Spec Kit resolves the feature from `.specify/feature.json`, not the branch.**
  It is gitignored. Set `SPECIFY_FEATURE_DIRECTORY` per session so parallel
  agents don't redirect each other.

<!-- MANUAL ADDITIONS END -->

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/005-cbrd-link-v11/plan.md
<!-- SPECKIT END -->
