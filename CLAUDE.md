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

## Current state — 2026-08-03

Plugin **0.5.0, not yet released**. Two features are in it.

`004-cbrd-parse-endpoint` — complete, manually validated in Oxygen, merged to
`main`: AI Markup calls the DILA CBRD Parse endpoint instead of an
OpenAI-compatible endpoint on the editor's machine.

`005-cbrd-link-v11` — complete on branch `005-cbrd-link-v11`, all 38 tasks
signed off including the Oxygen gate. Ref-to-Link had been failing for **every**
reference with `CBRD API error: HTTP 404` because CBRD moved `/link` from
`GET ?q=` to `POST` + JSON body and the plugin still issued the GET. The client
now POSTs `{"q":"<ref>…</ref>"}`. One production file changed (+34/-11); the
response DTO, i18n keys, retry policy, Replace rewrite and URL preference are
untouched. Because 0.5.0 is unreleased, 005 folded into its release notes rather
than taking a version of its own — **there is no 0.5.1**.

**376 tests, 0 failures, 2 skipped** (the 2 are `CBRDLiveContractProbeTest`,
opt-in behind `CBRD_LIVE_CONTRACT_CHECK`; without the flag they report as
*skipped*, never passed, so `mvn test` stays offline and deterministic).

Coverage **59.6% instructions** (`infrastructure.api` 90.0% → **91.5%**).

**Coverage baseline (first measured 2026-08-02): 59.5% instructions,
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

1. **Constitution amendment — due before `005` merges, not after.** Backlog items
   1 and 3 in a single `/speckit-constitution` pass. 005's Constitution Check
   recorded the *second* Principle VI deviation (see Backlog 3), scoped to 005
   only and carrying an expiry. Landing the amendment while 005 is in review lets
   that Check be re-run clean and the deviation table deleted rather than
   honoured. Parked behind the merge instead, it slips behind 006.
2. **Merge `005`.** Branch `005-cbrd-link-v11`, complete and validated.
3. **`006` — single-source plugin version.** `src/main/java` is not
   resource-filtered, so `${project.version}` cannot reach it. Two hardcoded
   `User-Agent` literals drift as a result; `CBRDAPIClient`'s still reads
   `DILA-AI-Markup/0.4.2`, three releases stale. Marked `TODO(005)` in
   `CbrdParseApiClient`. 005 already converted the *test* assertion to invariant
   form, so this bump will not turn the suite red.

## Backlog

Three open items. Each names where it gets done and what closes it — **delete the
entry when its closing condition is met.** A stale backlog is worse than none:
this file once claimed T055/T057 were open for an hour after they were signed
off, and anyone reading it went looking for finished work.

Item 3 now gates the `005` **merge** (see Next up); 1 rides along with it in the
same amendment pass. Kept here rather than in a commit message or a
closed document, because those are where the previous round of these rotted. If
this list ever exceeds ~5 items, move it to GitHub Issues (`gh` is authenticated,
`/speckit-taskstoissues` converts) — an always-loaded file has a real cost per
session.

| # | Item | Where | Closes when |
|---|------|-------|-------------|
| 1 | Coverage threshold | `/speckit-constitution` | amendment lands |
| 2 | Command/query split | `/speckit-specify` (blocked on 3) | feature merges |
| 3 | Principle VI category | `/speckit-constitution` (with 1) | amendment lands |

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

## Standing rules this codebase earned the hard way

- **Retire additively.** Introduce the replacement, migrate every call site,
  delete last. 004 had 12 files referencing 3 classes; deleting first would have
  left the module uncompilable for four phases with `mvn test` unrunnable
  (violating Principle X).
- **Assert invariants, not literals.** `CBRDAPIClientTest` used to pin
  `"DILA-AI-Markup/0.4.2"` exactly, so a correct version bump would turn it red —
  the suite defended the stale header. 005 replaced it with a shape assertion
  (`startsWith("DILA-AI-Markup/")` plus a semver pattern). The wider case is the
  same feature's `/link` tests, which asserted `contains("?q=")`: a positive
  assertion of a vendor's current shape cannot fail when the vendor drifts, only
  when you fix the client. Assert the rule, not the current output.
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
