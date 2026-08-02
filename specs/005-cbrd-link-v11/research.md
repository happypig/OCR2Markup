# Phase 0 Research: Restore Ref-to-Link against CBRD v1.1.0

**Feature**: `005-cbrd-link-v11` | **Date**: 2026-08-03

The external contract facts for this feature were resolved before planning, by
probing the production service rather than by reading documentation. They are
recorded in `exploration/ref2link_drift.md` and re-verified 2026-08-03; they are
**not** open questions and are not repeated here.

Four genuine decisions remained. Each is recorded below with what was rejected.

---

## D1. How the request body reaches the connection

**Decision**: Build the payload with `new JSONObject().put("q", refXml).toString()`
and write it as UTF-8 bytes to `conn.getOutputStream()` inside a try-with-resources,
after `setDoOutput(true)`. No new dependency, no new abstraction.

**Rationale**: `org.json` 20240303 is already a **compile-scope** dependency
(`pom.xml:99`) and is already the house pattern in `CbrdParseApiClient`. The
payload is `<ref>` XML carrying double quotes (`xml:id="r26"`), CJK text, and
full-width punctuation — precisely the input class that breaks naive escapers.

**Alternatives considered**:

- *Hand-rolled `escapeJson` handling `"`, `\`, and control characters* — proposed
  by the investigation's §6a item 1 and **explicitly retracted by its own §9.2**.
  Rejected: it re-implements a solved problem against the exact input that defeats
  such implementations, and the correct library is already on the compile path.
- *`x-www-form-urlencoded`* — the live contract accepts it as a second body type.
  Rejected: JSON is the documented primary, matches the `/parse` client's shape,
  and re-introducing URL encoding is how this feature's bug looked in the first
  place.

## D2. Whether to keep sending the `Referer` header

**Decision**: Keep sending it, unchanged.

**Rationale**: v1.1.0 declares no `security` on `/link`, so it is no longer
required — verified live: a POST with no `Referer` returned 200 with a resolved
URL. But it is harmless, it is what deployed installations already send, and
removing it would be an unforced behavioural change inside an outage fix. FR-011
requires everything outside request construction to stay put.

**Alternatives considered**:

- *Remove it as dead weight* — rejected: it widens the blast radius of a hotfix
  for no user-visible gain, and the older contract required it, so any
  not-yet-upgraded CBRD deployment would break.

## D3. Shape of the contract-conformance guard

**Decision**: Two layers.

1. **Offline, always-on**: a constant in main code records the contract version
   the client is built against (`1.1.0`). A test asserts that the vendored
   `cbrd-api.yaml` declares the same version, and that the client's request shape
   (method `POST`, `Content-Type: application/json`, body key `q`) matches what
   that contract describes. Deterministic, no network, no new dependency.
2. **Live, opt-in**: a probe that fetches `https://cbss.dila.edu.tw/cbrd/openapi.json`
   and asserts `info.version` and that `/link` still exposes only `post`. Skipped
   unless an environment flag is set (e.g. `CBRD_LIVE_CONTRACT_CHECK=1`), so
   `mvn test` stays offline and deterministic per the constraint.

**Rationale**: Layer 1 catches the failure mode this repository actually suffers —
the vendored contract and the code drifting apart — and costs nothing. Layer 2 is
the only mechanism that catches *vendor-initiated* drift, which is what happened
here; but a suite that fails when the network is down or DILA is doing maintenance
is a suite people learn to ignore, which would recreate the problem in a new form.

**Alternatives considered**:

- *Parse the YAML properly with SnakeYAML* — rejected **for now**: the pom has no
  YAML library at all (junit, mockito, assertj, hamcrest, json), so this means a
  new test-scoped dependency to read two scalar values. A targeted scan for
  `info.version` and the `/link` method key is enough for the assertions we
  actually make. Revisit if the guard grows to compare whole schemas.
- *Live probe always on* — rejected: violates the offline/deterministic constraint
  and makes the build fail for reasons unrelated to the change under test.
- *No guard, rely on the updated unit assertions* — rejected: that is exactly
  today's situation. Unit assertions pin what the client *does*, not whether the
  vendor still agrees.

## D4. Locating the vendored contract from inside the Maven module

**Decision**: A small test helper resolves the repository root by walking up from
`user.dir` until it finds the `specs/` directory, then reads
`specs/001-ref-to-link-action/contracts/cbrd-api.yaml`. If the walk fails, the
test **fails loudly** with the attempted paths — it must never silently pass.

**Rationale**: The Maven module lives three levels below the repository root
(`Models/Gemini2.5/dila-ai-markup-plugin`), while the vendored contract lives at
the root under `specs/`. Working directory differs between `mvn test` from the
module, `mvn test` from the root, and an IDE run. Walking up to a known marker is
cwd-tolerant and dependency-free.

**Alternatives considered**:

- *Copy the contract into `src/test/resources` at build time via
  `maven-resources-plugin`* — cleanest classpath story, and the natural answer if
  the guard grows. Rejected for now: it adds build configuration to serve a single
  file, and the copy step is one more thing that can silently go stale.
- *Hardcode `../../../specs/...`* — rejected: passes under `mvn test` from the
  module and breaks everywhere else, including the IDE. A guard that breaks
  depending on how you run it is worse than no guard, because it trains people to
  ignore the failure.
- *Keep a second copy of the contract inside the module* — rejected outright.
  Duplicate copies of a contract drifting apart is the defect this feature exists
  to fix.

## D5. Where the updated contract file lives

**Decision**: One file, updated in place at
`specs/001-ref-to-link-action/contracts/cbrd-api.yaml`. This feature's
`contracts/` directory holds a README recording the delta and naming the canonical
path — **not** a second copy of the YAML.

**Rationale**: Spec Kit convention puts contract artifacts under the feature
directory, but following that literally here would leave two `cbrd-api.yaml`
files, one of which is authoritative and one of which is a snapshot. That is the
same structure that produced this outage, at one remove. The guard in D3 reads a
single path; there must be a single file at it.

**Alternatives considered**:

- *Write the new YAML under `specs/005-cbrd-link-v11/contracts/` and copy it over
  001's during implementation* — rejected: leaves a stale duplicate behind
  permanently, and nothing would ever update it again.
- *Move the contract to a neutral top-level location* — reasonable, but out of
  scope for an outage fix; it would break the 001 spec's internal references for
  no benefit to this feature.
