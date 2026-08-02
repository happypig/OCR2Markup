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

Plugin **0.5.0**. Feature `004-cbrd-parse-endpoint` is complete and committed:
AI Markup now calls the DILA CBRD Parse endpoint instead of an OpenAI-compatible
endpoint on the editor's machine. 365 tests, 0 failures.

Two items remain open on 004, both recorded in
`specs/004-cbrd-parse-endpoint/tasks.md` under "Implementation Progress":
T055 (quickstart scenarios need a live Oxygen and a real token) and T057
(coverage ≥ 80% is unmeasurable — no coverage plugin in the build).

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
at specs/004-cbrd-parse-endpoint/plan.md
<!-- SPECKIT END -->
