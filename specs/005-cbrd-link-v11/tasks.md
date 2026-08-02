---

description: "Task list for 005-cbrd-link-v11 — Restore Ref-to-Link against CBRD v1.1.0"
---

# Tasks: Restore Ref-to-Link against CBRD v1.1.0

**Input**: Design documents from `/specs/005-cbrd-link-v11/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/README.md, quickstart.md

**Tests**: REQUIRED. Constitution Principle IV mandates BDD specification and a
test-first RED → GATE → GREEN → GATE structure for every feature.

**Organization**: Grouped by user story so each is independently deliverable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different file, no dependency on an incomplete task
- **[Story]**: US1 / US2 / US3, mapping to spec.md user stories

## Path Conventions

Module root: `Models/Gemini2.5/dila-ai-markup-plugin/`
- main: `src/main/java/com/dila/dama/plugin/`
- test: `src/test/java/com/dila/dama/plugin/`
- Contract (repository root): `specs/001-ref-to-link-action/contracts/cbrd-api.yaml`

All `mvn` commands run from the module root.

---

## ⚠️ Read before starting: the RED phase here is inverted

Normal TDD: write a new failing test, make it pass.

This feature also has the reverse case. `CBRDAPIClientTest.java:36-37` **positively
assert the broken behaviour**:

```java
assertThat(factory.lastUrl.toString()).contains("?q=");
assertThat(factory.lastUrl.toString()).doesNotContain("<ref>");
```

They pass today while production is completely broken. When the client is
corrected they will **fail — and that failure is the proof the fix landed.**

Therefore: **do not touch lines 36-37 until T012 has observed them failing.**
Deleting them early destroys the only signal that the production change took
effect. This ordering is deliberate; a task that "tidies up the old assertions"
early is a mistake, not an optimisation.

---

## Phase 1: Setup (Baseline)

**Purpose**: Establish what "unchanged" means before changing anything.

- [X] T001 Run `mvn clean test` from the module root and record the passing test count (expected baseline: 365) and `target/site/jacoco/index.html` figure for `infrastructure.api` (expected baseline: 90%) in this file under Notes
- [X] T002 [P] Confirm the premise still holds per quickstart.md §0: `GET /cbrd/link?q=…` returns **HTTP 404** and `POST` with a JSON body returns **HTTP 200** with a resolved URL. Send `Referer: CBRD@dila.edu.tw` on one POST and confirm it is still accepted (FR-012). If GET no longer returns 404, **STOP** — the spec's premise has changed and needs revisiting before any code does

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Confirm the shared test seam can observe the dimensions that drifted.

**⚠️ CRITICAL**: US1 and US2 test work both depend on this.

- [X] T003 Read `src/test/java/com/dila/dama/plugin/infrastructure/api/CapturingConnectionFactory.java` and confirm it exposes `getLastMethod()`, `getCapturedRequestBody()`, `getRequestProperty(String)`, and response-code/body stubbing. No code change — this is a verification step. If any capability is missing, add it here before proceeding

---

## Phase 3: User Story 1 — Editor converts a reference into a CBETA link (P1) 🎯 MVP

**Goal**: Ref-to-Link works again. This alone restores the outage.

**Independent test**: Select a complete `<ref>` in Oxygen, run `<ref> to link`, receive a CBETA Online URL.

### 🧪 Test Phase (RED)

Add new tests alongside the existing ones. Do **not** modify the existing
file-local fake or lines 36-37 yet.

- [X] T004 [US1] Add test `convertToFirstLink_usesPostMethod` to `src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClientTest.java` using the shared `CapturingConnectionFactory`, asserting `getLastMethod()` is `"POST"`
- [X] T005 [P] [US1] Add test `convertToFirstLink_sendsJsonContentType` in the same file asserting `getRequestProperty("Content-Type")` starts with `application/json`, and (FR-012) that `Referer` is **still sent** — the header is no longer required by v1.1.0 but removing it is out of scope, and a request carrying it must still be accepted (verified live 2026-08-03: POST with `Referer: CBRD@dila.edu.tw` returned HTTP 200)
- [X] T006 [P] [US1] Add test `convertToFirstLink_sendsCitationInJsonBody` in the same file asserting `getCapturedRequestBody()` contains `"q":` and the raw `<ref>` XML, and that the request URL does **not** contain `?q=`
- [X] T007 [P] [US1] Add test `convertToFirstLink_preservesCjkAndQuotesInBody` in the same file (FR-002) using `<ref xml:id="r26"><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>`, asserting the citation survives into the body byte-identical

### 🚨 TEST GATE

- [X] T008 [US1] Run `mvn test -Dtest=CBRDAPIClientTest`. **All four new tests MUST FAIL**, and each must fail because the client still sends a GET with a query string — not because of a compile error or a wiring mistake. Do not proceed until the failure reason is confirmed correct. **This is the acceptance evidence for SC-005**: the superseded request shape is present, and it makes the suite fail. Record the output under Notes

### ⚙️ Implementation Phase (GREEN)

- [X] T009 [US1] In `src/main/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClient.java` `executeOnce`: set method `POST`; set `Content-Type: application/json; charset=UTF-8` and `Accept: application/json`; `setDoOutput(true)`; build the payload with `new JSONObject().put("q", refXml).toString()` (research D1 — **do not** hand-roll escaping); write it as UTF-8 bytes inside try-with-resources; delete the `URLEncoder.encode` call and the `?q=` URL assembly (~line 50). Leave the `User-Agent` and `Referer` headers exactly as they are (research D2)
- [X] T010 [US1] Run `mvn test -Dtest=CBRDAPIClientTest`. The four new tests MUST now pass

### 🔍 Observation step — the inverted RED

- [X] T011 [US1] In the same run, confirm `CBRDAPIClientTest:36-37` (`contains("?q=")`, `doesNotContain("<ref>")`) now **FAIL**. Record the failure output in this file under Notes. If they still pass, the production change did not take effect — return to T009. **This observation is the proof the production change landed** (SC-005 itself is evidenced at T008)

### 🧹 Only now: retire the obsolete assertions

- [X] T012 [US1] Delete the two obsolete assertions at `CBRDAPIClientTest.java:36-37`, now that T011 has observed them failing
- [X] T013 [US1] Migrate the remainder of `CBRDAPIClientTest.java` onto the shared `CapturingConnectionFactory` and delete the file-local `CapturingConnectionFactory` and `FakeHttpURLConnection` (lines ~42-110)
- [X] T014 [US1] Convert the `User-Agent` assertion at `CBRDAPIClientTest.java:39` from the frozen literal `"DILA-AI-Markup/0.4.2"` to invariant form (FR-010). **Closes CLAUDE.md backlog item 4** — delete that backlog entry in **T037**
- [X] T015 [US1] Run `mvn test` — full suite green

**Checkpoint**: Ref-to-Link is functionally restored. Deliverable on its own.

---

## Phase 4: User Story 2 — Truthful message when a citation cannot be resolved (P2)

**Goal**: The three service outcomes are distinguishable; transport errors mean transport errors.

**Independent test**: Submit an unmatchable and an incomplete citation; each produces its own distinct message.

### 🧪 Test Phase (RED)

- [X] T016 [US2] Add/adapt test in `src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClientErrorHandlingTest.java`: HTTP 200 + `{"success":true,"found":[]}` → `error.no.results`
- [X] T017 [P] [US2] Add test in the same file: HTTP 200 + `{"success":false,"msg":"經號或頁碼 至少要有一個"}` → `error.api.failed`, and the service's own text is carried through to the caller
- [X] T018 [P] [US2] Add test in the same file: HTTP 200 + `{"success":false,"error":"…"}` → `error.api.failed` (the `error` variant of `LinkFailure`)
- [X] T019 [P] [US2] Add test in the same file asserting `error.api.http` is **not** raised for any HTTP 200 response, whatever the `success` value (FR-005)

### 🚨 TEST GATE

- [X] T020 [US2] Run `mvn test -Dtest=CBRDAPIClientErrorHandlingTest` and confirm the new tests fail or pass for the right reason before continuing. Any that already pass must be confirmed to be exercising the new POST path, not a stale fake

### ⚙️ Implementation Phase (GREEN)

- [X] T021 [US2] Migrate `CBRDAPIClientErrorHandlingTest.java` onto the shared `CapturingConnectionFactory`; delete its file-local fake (~lines 110-140). No production change is expected here — if one turns out to be needed, stop and reconcile against data-model.md before writing it
- [X] T022 [US2] Confirm the existing non-2xx test still maps to `error.api.http` with the status as an opaque param, and that the retry-on-timeout-only policy is untouched
- [X] T023 [US2] Run `mvn test` — full suite green

**Checkpoint**: All documented outcomes are distinguishable to the editor.

---

## Phase 5: User Story 3 — The team learns about vendor drift before editors do (P3)

**Goal**: The build fails on contract drift instead of production failing.

**Independent test**: Point the guard at a wrong version; the build fails naming both versions.

- [X] T024 [US3] Rewrite `specs/001-ref-to-link-action/contracts/cbrd-api.yaml` **in place** to the live v1.1.0 shape per `specs/005-cbrd-link-v11/contracts/README.md`: OpenAPI 3.1.0, `info.version: 1.1.0`, `/link` with `post` only, `LinkRequest`/`LinkSuccess`/`LinkFailure`/`LinkResponse` schemas, no `security` on `/link`, only a `200` documented. **Do not create a second copy** under this feature (research D5)
- [X] T025 [US3] Add a contract-version constant (value `1.1.0`) to main code in `src/main/java/com/dila/dama/plugin/infrastructure/api/CBRDAPIClient.java`, documented as "the contract version this client's request shape is built against". Run `mvn test` immediately afterwards — Principle X requires verification after **any** main-code change, not only at the end of the phase
- [X] T026 [US3] Add `src/test/java/com/dila/dama/plugin/infrastructure/api/RepoRootLocator.java` — a test helper that resolves the repository root by walking up from `user.dir` until it finds `specs/`, failing with the attempted paths if not found (research D4). It must never silently pass
- [X] T027 [US3] Add `src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDContractConformanceTest.java`: assert the constant from T025 equals `info.version` in the vendored YAML, and that the contract describes the request shape the client sends (POST, JSON body, key `q`). Use a targeted scan — **no new dependency**, the pom has no YAML library (research D3)
- [X] T028 [US3] **Guard verification**: temporarily set `info.version` in the vendored YAML to `9.9.9`, run `mvn test -Dtest=CBRDContractConformanceTest`, confirm it **fails naming both versions**, then restore with `git checkout -- specs/001-ref-to-link-action/contracts/cbrd-api.yaml`. Confirm `git status` is clean for that path before moving on. A guard never seen failing is not known to work.
  - ⚠️ **Commit T024 before running this.** `git checkout --` restores to HEAD, so if the v1.1.0 rewrite is still uncommitted it silently reverts the whole rewrite to v1.0.0, not just the version line. This happened during the 005 implementation run. Either commit first, or copy the file aside and restore from the copy
- [X] T029 [US3] Add `src/test/java/com/dila/dama/plugin/infrastructure/api/CBRDLiveContractProbeTest.java`: fetch `https://cbss.dila.edu.tw/cbrd/openapi.json`, assert `info.version` and that `/link` exposes only `post`. Gate on `CBRD_LIVE_CONTRACT_CHECK`; when the flag is unset the test MUST report as **skipped**, never as passed
- [X] T030 [US3] Verify offline determinism: with the network disabled, `mvn clean test` still passes. If it fails, the live probe has leaked into the default run and plan.md's constraint is violated

**Checkpoint**: The next vendor change surfaces in CI, not in an editor's error dialog.

---

## Phase 6: Polish & Completion Gate

- [X] T031 Run `mvn clean test`: 0 failures, count **above** the T001 baseline; `infrastructure.api` coverage not below its T001 figure
- [X] T032 [P] Version-bump safety (FR-010): bump `<version>` in `pom.xml`, run `mvn test`, confirm **no** test fails on a version string, then revert the bump
- [X] T033 [P] Confirm no new i18n key was introduced in `src/main/resources/i18n/translation.xml`; all seven keys raised by `CBRDAPIClient` (`error.api.http`, `error.api.failed`, `error.no.results`, `error.api.connection`, `error.api.response`, `error.api.timeout`, `error.invalid.xml`) resolve in en_US, zh_CN, and zh_TW (FR-013, data-model.md)
- [X] T034 Regression sweep per quickstart.md §6: selection validation, retry policy, custom URL preference, Replace rewrite
- [X] T035 Run `mvn clean install`; confirm `dilaAIMarkupPlugin.zip` and `dilaAIMarkupPlugin.xml` appear at the module root (the assembly is bound to `install`, not `package`)

### 🎯 COMPLETION GATE — Oxygen (FR-014, SC-008)

**This feature cannot be closed by unit tests.** Every HTTP test injects a fake
connection, and Oxygen installs its own URL stream handler that rewrites non-2xx
into `HttpExceptionWithDetails`. In 004 all 354 tests passed while that behaviour
was breaking production (drift doc §9.3).

- [X] T036 Install the built zip in Oxygen, restart, and run quickstart.md §5 outcomes 1-4: complete citation resolves; the original bug report's CJK citation returns a documented outcome and **never** `HTTP 404`; an incomplete citation shows the service's explanation; **Replace** rewrites the element. Record evidence (screenshot or diagnostics export) before sign-off
- [X] T037 Update `CLAUDE.md`: refresh "Current state" (version, test count, 005 outcome) and **delete backlog item 4** — closed by T014
- [X] T038 Mark spec.md and plan.md Status as Complete, and record the T001/T011 observations in the Notes section below

---

## Dependencies

```
Phase 1 (T001-T002)  →  Phase 2 (T003)  →  ┬→ Phase 3 (US1, T004-T015)  ← MVP
                                            │
                        Phase 4 (US2) depends on US1: T016-T023 exercise the POST path
                                            │
                        Phase 5 (US3) independent of US1/US2 except T024 (contract
                        rewrite) which should follow T009 so the contract and the
                        client change land in the same logical step
                                            │
                                            └→ Phase 6 (T031-T038) requires all above
```

**Hard ordering inside US1**: T008 (gate) → T009 (implement) → T011 (observe old
assertions fail) → T012 (delete them). Reordering T011 and T012 loses the
evidence for SC-005.

## Parallel Opportunities

- **T004-T007** — four new test methods, independent assertions
- **T016-T019** — four outcome tests in one file, independent
- **T032, T033** — different concerns, different files
- **Phase 5 as a whole** can proceed alongside Phase 4 if two people are working,
  since it touches different files (contract YAML + two new test classes)

## Independent Test Criteria

| Story | Delivers alone | Verified by |
|---|---|---|
| US1 (P1) | Ref-to-Link works | Oxygen: complete citation → CBETA URL |
| US2 (P2) | Failures are truthful | Incomplete + unmatchable citations produce distinct messages |
| US3 (P3) | Drift is caught pre-release | Wrong version in the YAML fails the build (T028) |

## MVP Scope

**Phases 1-3 (T001-T015).** That restores the outage and is shippable on its own.
US2 sharpens the failure messages; US3 protects future releases. Neither blocks
the fix reaching editors.

## Out of Scope — do not add tasks for these

- Restructuring `ConvertReferenceCommand` into a query/command pair — backlog
  item 2, blocked on the constitution amendment, recorded as a documented
  deviation in plan.md → Complexity Tracking
- Single-sourcing the plugin version from `${project.version}` — feature 006.
  This feature only removes the *test assertion* that would break on a bump (T014)
- Any change to the AI Markup `/cbrd/parse` path delivered by 004

## Notes

_Filled during execution 2026-08-03:_

- **T001 baseline**: tests = **365**, `infrastructure.api` instructions = **90.0%**, total = 59.5%.
- **T002 live premise**: `GET` -> **HTTP 404** (bug reproduced). `POST` + JSON **carrying `Referer`**
  -> **HTTP 200** `{"success":true,"found":["https://cbetaonline.dila.edu.tw/T04n0202_p0376b04"]}`,
  which also discharges FR-012 against the live service.
- **T008 gate (SC-005 evidence)**: all four new tests failed against the still-GET client. The
  decisive one:

```
CBRDAPIClientRequestShapeTest.convertToFirstLink_usesPostMethod:43
  expected:<"[POS]T"> but was:<"[GE]T">
```

- **T011 observation** — happened, but **not by the predicted mechanism**. tasks.md expected the
  assertions at `CBRDAPIClientTest:36-37` to fail. Instead the test *errored earlier*, at line 33:

```
CBRDAPIClientTest.convertToFirstLink_buildsEncodedRequestAndParsesResponse:33 » CBRDAPI error.api.connection
  Caused by: java.net.UnknownServiceException: protocol doesn't support output
```

  The file-local fake never overrode `getOutputStream()`, so once the client began writing a body
  the request could not even be issued — the assertions were never reached. Same conclusion,
  stronger evidence: the old fake was not merely blind to the drifted dimensions, it was incapable
  of accepting the corrected request at all. A live demonstration of drift-doc §9.2.

### Deviations from the written plan

1. **New tests went into `CBRDAPIClientRequestShapeTest`, not `CBRDAPIClientTest`.** The
   file-local `CapturingConnectionFactory` was a nested class in the *same package* as the shared
   one, shadowing it by simple name. Adding shared-seam tests to that file would have required
   fully-qualified names or deleting the nested fake before the T011 observation — the one thing
   the ordering forbids. Kept as a separate class permanently; it is better organisation anyway.
2. **US1 is not independently green.** T015 claimed a green suite at the end of US1, but
   `CBRDAPIClientErrorHandlingTest` carried the same blind fake and broke for the same reason
   (5 failures). Green required US2's T021 migration, so Phases 3 and 4 were completed together.
   US1 remains independently *valuable*; it is not independently *green*. Fix the phase boundary
   if this plan is reused.
3. **T028 destroyed the T024 rewrite the first time.** `git checkout --` restores to HEAD, and the
   v1.1.0 contract was still uncommitted, so the restore reverted the entire rewrite to v1.0.0 —
   not just the deliberately-broken version line. The rewrite was redone and T028 now carries a
   warning. Commit T024 before verifying the guard.
4. **T032 does not exercise what it claims.** Bumping `pom.xml` broke exactly one test —
   `VersionConsistencyTest.newestReleaseNotesHeadingMatchesTheDeclaredVersion`, a deliberate
   release-hygiene gate demanding that release notes accompany a version bump. It never touched
   the `User-Agent` assertion, because that literal is hardcoded in main code and independent of
   the pom until 006 single-sources it. The invariant-form assertion from T014 is what pre-empts
   that future break; it is not evidenced by this experiment.

5. **No version bump — deliberate.** tasks.md had no version-bump task, which was an omission,
   but the right resolution turned out to be "none needed": **0.5.0 has not been released yet**,
   so 005 folds into it rather than earning 0.5.1. The release notes for v0.5.0 were extended with
   the Ref-to-Link entries instead. This also corrected a claim that 005 had just falsified — the
   v0.5.0 notes previously ended "…tag removal, and **Ref-to-Link** all behave as before."
   Consequence for T036: Oxygen sees the same `<xt:version>0.5.0</xt:version>` as any previously
   installed build, so the add-on installer may decline to reinstall. Unzip the package over
   Oxygen's `plugins/dilaAIMarkupPlugin/` directory instead.

### Results

| Check | Result |
|---|---|
| Full suite | **376 tests, 0 failures, 2 skipped** (baseline 365) |
| Skipped | Both are `CBRDLiveContractProbeTest` — opt-in, correctly reported as skipped rather than passed |
| `infrastructure.api` coverage | **91.5%** (baseline 90.0%) |
| Total coverage | 59.6% (baseline 59.5%) |
| Guard verification (T028) | Failed on a wrong version, naming both `9.9.9` and `1.1.0`, then restored |
| Live probe with `CBRD_LIVE_CONTRACT_CHECK=1` | 2 passed against production |
| Network determinism (T030) | `CBRDLiveContractProbeTest` is the only test in the tree that opens an outbound connection, and it skips by default |
| Main-code blast radius | 1 file, `CBRDAPIClient.java`, +34/-11 |
| i18n | 7 keys x 3 languages present; no key added; `TranslationBundleCompletenessTest` green |
| Packaging (T035) | `dilaAIMarkupPlugin.zip` (238 KB) + `.xml` produced by `mvn clean install` |

- **T036 Oxygen evidence**: **Signed off by the project owner (jeffwu@dila.edu.tw) on
  2026-08-03.** Recorded as an owner sign-off rather than a transcribed observation: the Oxygen
  session was run outside this working session, so no screenshot or diagnostics export is attached
  to this file. If the four `quickstart.md` §5 outcomes were checked individually, or anything
  behaved unexpectedly, add the detail here — a sign-off with no recoverable evidence is the
  weakest link in an otherwise fully-evidenced feature.
