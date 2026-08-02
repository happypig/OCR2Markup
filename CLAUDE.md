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

**Coverage now 59.6% instructions / 45.4% branches** (first measured 2026-08-02
at 59.5% / 45.1%). The constitution used to mandate ≥ 80%, which the project had
never met and the build had never checked; **v1.1.0 replaced it** with a ratchet
floor of 59% / 45% that must be raised when exceeded and never lowered.
`mvn test` writes `target/site/jacoco/index.html`.

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

JaCoCo is still **measure-only** — `prepare-agent` and `report`, no `check` goal.
The threshold question is now settled (59% / 45%, constitution v1.1.0); wiring
the goal is backlog item 5. Until that lands the floor is stated but unenforced.

## Next up

1. **Re-run 005's Constitution Check and delete its deviation table.** The
   amendment landed 2026-08-03 (v1.1.0, Remote Read category), which is what that
   table was waiting for. Per its own expiry clause it should be **deleted rather
   than honoured** — `specs/005-cbrd-link-v11/plan.md` → Complexity Tracking.
   Principle VI then passes cleanly and 005 carries no deviation into history.
2. **Merge `005`.** Branch `005-cbrd-link-v11`, complete and validated. Do this
   after step 1, which is the whole reason the amendment was scheduled before the
   merge rather than after it.
3. **`006` — single-source plugin version.** `src/main/java` is not
   resource-filtered, so `${project.version}` cannot reach it. Two hardcoded
   `User-Agent` literals drift as a result; `CBRDAPIClient`'s still reads
   `DILA-AI-Markup/0.4.2`, three releases stale. Marked `TODO(005)` in
   `CbrdParseApiClient`. 005 already converted the *test* assertion to invariant
   form, so this bump will not turn the suite red.

## Backlog

Two open items. Each names where it gets done and what closes it — **delete the
entry when its closing condition is met.** A stale backlog is worse than none:
this file once claimed T055/T057 were open for an hour after they were signed
off, and anyone reading it went looking for finished work.

Kept here rather than in a commit message or a closed document, because those are
where the previous round of these rotted. If this list ever exceeds ~5 items,
move it to GitHub Issues (`gh` is authenticated, `/speckit-taskstoissues`
converts) — an always-loaded file has a real cost per session.

| # | Item | Where | Closes when |
|---|------|-------|-------------|
| 2 | Command/query/remote-read split | `/speckit-specify` (**unblocked** 2026-08-03) | feature merges |
| 5 | Wire the JaCoCo `check` goal | `pom.xml` | build fails below the floor |

> Items 1 and 3 (coverage threshold, Principle VI category) **closed 2026-08-03** by
> constitution **v1.1.0** — see `specs/constitution-amendment-2026-08-03.md`.

2. **Split `RunAiMarkupDiagnosticsCommand` into properly-typed operations.**
   No longer blocked: v1.1.0 added the **Remote Read** category, so it can finally
   be classified correctly instead of the split encoding a guess. Was recorded as
   an approved deviation in `specs/004-cbrd-parse-endpoint/plan.md`. The name is
   misleading too — it is the primary AI Markup path, not a diagnostic.
   `ConvertReferenceCommand` is the same case and can be done in the same pass;
   note that under v1.1.0 the `…Command` suffix on a Remote Read is a SHOULD, not
   a violation, so this is tidying rather than compliance work.

5. **The coverage ratchet is stated but not enforced.** v1.1.0 sets a floor of
   59% instructions / 45% branches and requires a JaCoCo `check` goal to enforce
   it. The goal is not wired yet — `pom.xml` still has only `prepare-agent` and
   `report`. Until it is, the amendment has reproduced the original defect at a
   smaller size: a number the build does not verify. Current actual: 59.6% / 45.4%.

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
