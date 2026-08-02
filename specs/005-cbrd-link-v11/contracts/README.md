# Contracts: Restore Ref-to-Link against CBRD v1.1.0

**Feature**: `005-cbrd-link-v11` | **Date**: 2026-08-03

## There is deliberately no YAML file in this directory

The canonical vendored contract is, and stays:

```
specs/001-ref-to-link-action/contracts/cbrd-api.yaml
```

It is **updated in place** by this feature from v1.0.0 to v1.1.0. A second copy
is not created here.

Spec Kit convention would put a contract artifact under the feature directory.
Following that literally would leave two `cbrd-api.yaml` files — one authoritative,
one a snapshot that nothing updates. That is precisely the structure that caused
the outage this feature fixes, reproduced one level up. The conformance guard
(research D3) reads one path; there must be one file at it. See research D5.

## The delta this feature applies

Verified against `https://cbss.dila.edu.tw/cbrd/openapi.json` on 2026-08-03.

| Aspect | v1.0.0 (vendored, stale) | v1.1.0 (live) | Breaking |
|---|---|---|---|
| `openapi` | `3.0.3` | `3.1.0` | no |
| `info.title` | `CBRD (CBETA Reference Detection) API` | `CBRD API` | no |
| `info.version` | `1.0.0` | `1.1.0` | no — but it is the drift signal |
| `servers[0].url` | `https://cbss.dila.edu.tw/cbrd` | `/cbrd` (relative) | no — same effective base |
| **`/link` method** | `get` | **`post` only** | **YES — root cause** |
| **Citation transport** | `q` query parameter, URL-encoded | **`q` member of a JSON request body** | **YES** |
| **Request `Content-Type`** | none (no body) | `application/json` or `application/x-www-form-urlencoded` | **YES** |
| `Referer` header | required; `401` documented | not required — `/link` declares no `security` | no (soft) |
| Documented responses | `200`, `400`, `401`, `500`, `504` | **`200` only** | no (soft) |
| Response schema | `CBRDResponse`, loose | `oneOf(LinkSuccess, LinkFailure)`, `additionalProperties: false`, `const` booleans | no (stricter, field-compatible) |
| `found` | always required | present only on success | no (soft) |
| Failure fields | ad-hoc `error` or `msg` | `LinkFailure` with `anyOf([msg],[error])` | no (soft) |

## Schemas the updated file must declare

```
LinkRequest  : object, required [q], properties { q: string }, additionalProperties false
LinkSuccess  : object, required [success, found], success const true,
               found array of uri strings, additionalProperties false
LinkFailure  : object, required [success], success const false,
               anyOf [required msg] | [required error], additionalProperties false
LinkResponse : oneOf [LinkSuccess, LinkFailure]
               "Always returns HTTP 200 regardless of internal processing result;
                consult the success field."
```

## Observed behaviour, for the record

These are live responses, not examples copied from the vendor's document:

```http
GET /cbrd/link?q=<ref><canon>T</canon><v>25</v></ref>
→ HTTP 404                                            # the production bug

POST /cbrd/link   Content-Type: application/json; charset=UTF-8
{"q":"<ref><canon>T</canon><v>4</v><w>202</w><p>376</p><c>b</c><l>4</l></ref>"}
→ HTTP 200  {"success":true,"found":["https://cbetaonline.dila.edu.tw/T04n0202_p0376b04"]}

POST /cbrd/link   (incomplete citation)
→ HTTP 200  {"success":false,"msg":"經號或頁碼 至少要有一個"}
```

The third case is the one worth internalising: a citation the service cannot
process is still a **successful exchange**. Anything that reports it as a
transport error is repeating this outage in a new costume.

## What consumes this contract

| Consumer | How |
|---|---|
| `CBRDContractConformanceTest` (new) | Asserts the vendored `info.version` equals the version constant the client is built against, and that the described request shape matches what the client sends. Offline, always runs. |
| `CBRDLiveContractProbeTest` (new) | Fetches the live document and compares `info.version` and the `/link` method set. Skipped unless the opt-in environment flag is set. |
| Human readers | `exploration/ref2link_drift.md` — full investigation; read §9 "Verified corrections" before §6. |
