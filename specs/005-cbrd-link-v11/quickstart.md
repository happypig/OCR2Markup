# Quickstart: Validating Ref-to-Link against CBRD v1.1.0

**Feature**: `005-cbrd-link-v11` | **Date**: 2026-08-03

How to prove this feature works. Run from the Maven module unless stated
otherwise:

```
D:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin
```

## Prerequisites

- JDK capable of `source`/`target` 8 (the build enforces the Java 8 baseline)
- Maven 3.x
- Oxygen XML Editor 27.1.0.3+ for the manual gate
- Network access **only** for the two optional live checks. The suite itself must
  pass offline.

## 0. Confirm the bug before fixing it

Independent of the plugin, one command shows the outage and its fix:

```bash
curl -s -o /dev/null -w "GET  -> HTTP %{http_code}\n" \
  -G --data-urlencode 'q=<ref><canon>T</canon><v>25</v></ref>' \
  https://cbss.dila.edu.tw/cbrd/link

curl -s -w "\nPOST -> HTTP %{http_code}\n" -X POST \
  -H 'Content-Type: application/json; charset=UTF-8' \
  -d '{"q":"<ref><canon>T</canon><v>4</v><w>202</w><p>376</p><c>b</c><l>4</l></ref>"}' \
  https://cbss.dila.edu.tw/cbrd/link
```

**Expected**:

```
GET  -> HTTP 404
{"success":true,"found":["https://cbetaonline.dila.edu.tw/T04n0202_p0376b04"]}
POST -> HTTP 200
```

If the GET returns anything but 404, stop — the premise of this feature has
changed and the spec needs revisiting before any code does.

## 1. RED gate — watch the old assertions fail

Principle IV requires tests to be seen failing before the implementation makes
them pass. This feature has an unusually literal version of that: two existing
assertions **pin the broken behaviour** and must fail once the client is
corrected.

```bash
mvn test -Dtest=CBRDAPIClientTest
```

**Expected at the RED gate**: failures on the assertions that the request URL
`contains("?q=")` and `doesNotContain("<ref>")`. Seeing these fail is the
evidence that the client now sends the new shape. A green run here means the
client was not actually changed.

Do not proceed to rewrite the assertions until they have been observed failing.

## 2. GREEN gate — full offline suite

```bash
mvn clean test
```

**Expected**:

- `BUILD SUCCESS`, 0 failures, 0 errors
- Test count **above** the 365 baseline recorded in `CLAUDE.md` (this feature
  adds conformance coverage)
- `target/site/jacoco/index.html` written; `infrastructure.api` coverage should
  not regress below its 90% baseline

**Determinism check** — the suite must not depend on the network:

```bash
# disconnect, or block DNS, then:
mvn clean test
```

Still `BUILD SUCCESS`. If it fails while offline, the live probe has leaked into
the default run and the constraint in plan.md is violated.

## 3. Contract guards

**Offline guard (always runs, part of step 2):**

```bash
mvn test -Dtest=CBRDContractConformanceTest
```

Verify it actually guards, by breaking it on purpose: change `info.version` in
`specs/001-ref-to-link-action/contracts/cbrd-api.yaml` to `9.9.9`, re-run, and
confirm the test **fails naming both versions**. Restore the file afterwards.
A guard never seen failing is not known to work.

**Live probe (opt-in):**

```bash
CBRD_LIVE_CONTRACT_CHECK=1 mvn test -Dtest=CBRDLiveContractProbeTest
```

```powershell
$env:CBRD_LIVE_CONTRACT_CHECK=1; mvn test -Dtest=CBRDLiveContractProbeTest
```

**Expected**: passes against the live service today. Without the flag, the same
command reports the test as skipped — not passed.

## 4. Package the plugin

```bash
mvn clean install
```

**Expected**: `dilaAIMarkupPlugin.zip` and `dilaAIMarkupPlugin.xml` at the module
root. The assembly is bound to `install`, not `package` — `mvn package` will not
produce them.

## 5. Oxygen gate (FR-014, SC-008) — required for completion

Unit tests cannot close this feature. Every HTTP test injects a fake connection,
and Oxygen installs its own URL stream handler that rewrites non-2xx responses
into `HttpExceptionWithDetails`. In 004 all 354 tests passed while that behaviour
was breaking production. See `exploration/ref2link_drift.md` §9.3.

1. Install `dilaAIMarkupPlugin.zip` in Oxygen and restart.
2. Open a TEI document and select a complete reference, e.g.
   `<ref><canon>T</canon><v>4</v><w>202</w><p>376</p><c>b</c><l>4</l></ref>`
3. Run **`<ref> to link`**.

| # | Selection | Expected result |
|---|---|---|
| 1 | Complete citation (above) | A `cbetaonline.dila.edu.tw` URL is shown; **Replace** is enabled |
| 2 | The original bug report's citation — `<ref><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>` | A documented outcome, **never** `CBRD API error: HTTP 404`. CJK and full-width punctuation survive the round trip |
| 3 | Incomplete citation, e.g. `<ref><canon>T</canon><v>25</v></ref>` | The service's own explanation via `error.api.failed` — not a transport error |
| 4 | Click **Replace** after outcome 1 | The `<ref>` element is rewritten with the URL, exactly as before this feature |

Record the observation (screenshot or the diagnostics export) before sign-off.
Outcomes 1, 2, and 3 have **never** been reachable in production, because every
request failed at routing first — this is their first real exercise.

## 6. Regression sweep

Confirm nothing outside request construction moved:

| Behaviour | How to check |
|---|---|
| Selection validation | Select a non-`<ref>` fragment → the existing validation message, unchanged |
| Retry policy | Existing timeout tests still pass; a routing/validation failure is **not** retried |
| URL preference | Set a custom CBRD URL in preferences → the request goes there; no preference is reset or migrated |
| i18n | All keys resolve in en_US, zh_CN, zh_TW; no new key was added |
| Version bump safety | Bump `<version>` in `pom.xml`, run `mvn test` → no test fails on a version string (FR-010). Revert |

## Definition of done

- [ ] Step 1 failures observed before the assertions were rewritten
- [ ] `mvn clean test` green, offline, above the 365 baseline
- [ ] Conformance guard seen failing on a deliberately wrong version, then restored
- [ ] Live probe passes when opted in, skips when not
- [ ] All four Oxygen outcomes confirmed and recorded
- [ ] Regression sweep clean
