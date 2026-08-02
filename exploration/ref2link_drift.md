# Exploration: `<ref> to link` vs. live CBRD API (spec drift)

**Date:** 2026-08-02
**Source URL (live):** https://cbss.dila.edu.tw/cbrd/openapi#tag/link
**Codebase contract:** `specs/001-ref-to-link-action/contracts/cbrd-api.yaml`
**Source root:** `Models\Gemini2.5\dila-ai-markup-plugin\`

## Summary

The codebase's vendor contract is pinned to CBRD **v1.0.0 (`GET /link?q=...`)**, but the live server is now **v1.1.0 (`POST /link` with JSON body)**. The plugin's `CBRDAPIClient` still issues a GET; the server no longer defines a GET route for `/cbrd/link`, so Rails returns **HTTP 404**.

This is the cause of the user-facing error:

```
Action selected: <ref> to link
Selected text: <ref><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>
Text length: 71 characters
CBRD API error: HTTP 404
```

The 404 is **NOT** a per-citation "no match" signal (the API's documented way to say "no match" is HTTP 200 with empty `found`, mapped to `error.no.results`). It indicates the endpoint/route itself is missing — i.e. a method-mismatch / routing error at the HTTP layer.

---

## 1. Live OpenAPI snapshot (v1.1.0)

Retrieved from `https://cbss.dila.edu.tw/cbrd/openapi.json` (the `/openapi` HTML viewer renders this JSON via JS; raw JSON is served from `openapi.json`).

Canonically relevant fragment (CJK mojibake in the description strings is from the shell retrieval and is not reproduced here):

```json
{
  "openapi": "3.1.0",
  "info": { "title": "CBRD API", "version": "1.1.0" },
  "servers": [{ "url": "/cbrd", "description": "Production CBRD server" }],
  "tags": [
    { "name": "Link",    "description": "CBETA Online link generation" },
    { "name": "Parse",   "description": "Parse free text into <ref> XML" }
  ],
  "paths": {
    "/link": {
      "post": {
        "tags": ["Link"],
        "summary": "Generate CBETA link(s) from <ref> XML in the request body",
        "operationId": "postCbetaLink",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": { "$ref": "#/components/schemas/LinkRequest" },
              "example": {
                "q": "<ref xml:id=\"r26\">…<canon>???</canon>…<v>?</v>…<p>??</p>…<c>?</c>…<l>?</l>~<l>?</l>??</ref>"
              }
            },
            "application/x-www-form-urlencoded": {
              "schema": { "$ref": "#/components/schemas/LinkRequest" }
            }
          }
        },
        "responses": { "200": { "$ref": "#/components/responses/LinkResponse" } }
      }
    },
    "/parse": {
      "post": {
        "tags": ["Parse"],
        "summary": "Parse free text into a reference",
        "description": "Requires Bearer token; accepts free text and returns <ref> XML; max 4,000 characters.",
        "operationId": "parseReference",
        "security": [{ "BearerAuth": [] }],
        "requestBody": { "required": true, "content": { "application/json": {
          "schema": { "$ref": "#/components/schemas/ParseRequest" },
          "example": { "text": "(T 1442),??23,?869???871 ?", "lang": "zh" }
        }}},
        "responses": {
          "200": { "description": "Parsed reference", "content": { "application/xml": { ... }}},
          "400": { ... text_is_required  ...},
          "401": { ... unauthorized      ...},
          "422": { ... invalid_model_output ...},
          "502": { ... openai_unavailable ...},
          "503": { ... parse_api_not_configured / openai_credentials_unavailable / openai_rate_limited ...}
        }
      }
    }
  },
  "components": {
    "securitySchemes": {
      "BearerAuth": {
        "type": "http", "scheme": "bearer",
        "description": "PARSE_API_TOKEN — Rails encrypted credentials, shared token"
      }
    },
    "schemas": {
      "LinkRequest": {
        "type": "object",
        "required": ["q"],
        "properties": { "q": { "type": "string", "description": "Raw <ref> XML string" }},
        "additionalProperties": false
      },
      "LinkSuccess": {
        "type": "object",
        "required": ["success", "found"],
        "properties": {
          "success": { "type": "boolean", "const": true },
          "found": { "type": "array", "items": { "type": "string", "format": "uri" }}
        },
        "additionalProperties": false
      },
      "LinkFailure": {
        "type": "object",
        "required": ["success"],
        "properties": {
          "success": { "type": "boolean", "const": false },
          "msg":   { "type": "string" },
          "error": { "type": "string" }
        },
        "anyOf": [{ "required": ["msg"] }, { "required": ["error"] }],
        "additionalProperties": false
      },
      "LinkResponse": { "oneOf": [
        { "$ref": "#/components/schemas/LinkSuccess" },
        { "$ref": "#/components/schemas/LinkFailure" }
      ]}
    },
    "responses": {
      "LinkResponse": {
        "description": "Always returns HTTP 200 regardless of internal processing result; consult `success` field.",
        "content": { "application/json": {
          "schema": { "$ref": "#/components/schemas/LinkResponse" },
          "examples": {
            "success": { "value": { "success": true, "found": ["https://cbetaonline.dila.edu.tw/T04n0202_p0376b04"] }},
            "failure": { "value": { "success": false, "msg": "…no matching reference…" }}
          }
        }}
      }
    }
  }
}
```

---

## 2. Codebase contract snapshot (v1.0.0)

From `D:\project\OCR2Markup\specs\001-ref-to-link-action\contracts\cbrd-api.yaml`:

```yaml
openapi: 3.0.3
info:
  title: CBRD (CBETA Reference Detection) API
  version: 1.0.0   # OLD
servers:
  - url: https://cbss.dila.edu.tw/cbrd
paths:
  /link:
    get:           # OLD — GET, not POST
      summary: Convert Tripitaka reference to CBETA link
      parameters:
        - name: q
          in: query           # OLD — query string, URL-encoded
          required: true
          schema:
            type: string
            format: xml
            example: '<ref><canon>T</canon><v>25</v>.<w>1514</w></ref>'
        - name: Referer       # OLD — Referer header required for /link
          in: header
          required: true
          schema: { enum: [CBRD@dila.edu.tw] }
      responses:
        '200': ...oneOf success/empty/failure styles without strict oneOf...
        '400': Bad request - missing or invalid parameters
        '401': Unauthorized - missing or invalid Referer header
        '500': Internal server error
        '504': Gateway timeout
components:
  schemas:
    CBRDResponse: { required: [success, found], ... }   # OLD — no strict oneOf, no additionalProperties:false
    ErrorResponse: { required: [error] }
  securitySchemes:
    RefererHeader: { apiKey, in: header, name: Referer }
security:
  - RefererHeader: []
```

`CBRDAPIClient.java` (production code) is built against this v1.0.0 contract:

| Concern | Code |
|---|---|
| Method | `conn.setRequestMethod("GET")` |
| URL | `apiUrl + "?q=" + URLEncoder.encode(refXml, "UTF-8")` (CBRDAPIClient.java:48-50,107-148) |
| Referer | hardcoded fallback `CBRD@dila.edu.tw` (DAMAWorkspaceAccessPluginExtension.java:614) |
| User-Agent | `DILA-AI-Markup/0.4.2` (CBRDAPIClient.java:116) |
| Non-200 handling | throw `CBRDAPIException("error.api.http", status)` for any non-200 (CBRDAPIClient.java:122-125) — source of `CBRD API error: HTTP 404` |

---

## 3. Diff table

Every breaking difference between the codebase contract / client and the live v1.1.0 API.

| Aspect | Codebase (v1.0.0) | Live (v1.1.0) | Breaking? | Effect on plugin |
|---|---|---|---|---|
| **`/link` HTTP method** | `GET` | **`POST`** | **YES — root cause of the 404** | Rails routes only `POST /link`; GET → `No route matches [GET]` → **404 Not Found** |
| **`q` transport** | query string, **URL-encoded** XML | **JSON body** `{"q":"<ref>…</ref>"}` (or `x-www-form-urlencoded`) | YES | `URLEncoder.encode(refXml)` is now wrong; should be a raw JSON string |
| **Request `Content-Type`** | (none — GET) | `application/json` (or `application/x-www-form-urlencoded`) | YES | `conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")` must be set, and `setDoOutput(true)` + body write required |
| **`Referer` header** | **required** (`Referer: CBRD@dila.edu.tw`) | **not required** for `/link` (no `security` on `/link` in v1.1.0) | Soft (backward-compatible) | Plugin can keep sending it (harmless); v1.1.0 has no `401` documented for `/link` |
| **OpenAPI version** | 3.0.3 | 3.1.0 | No (declaration only) | — |
| **`info.version`** | 1.0.0 | 1.1.0 | No (version bump) | Vendor contract pin is stale |
| **`info.title`** | `CBRD (CBETA Reference Detection) API` | `CBRD API` | No | — |
| **`servers[0].url`** | `https://cbss.dila.edu.tw/cbrd` | `/cbrd` (relative) | No | Same effective base URL |
| **Response: `success`** | boolean (free) | boolean **`const:true` in LinkSuccess / `const:false` in LinkFailure** | Soft (compatible, more strict) | Existing `if (resp.success)` logic still works |
| **Response: `found`** | always present (`required: [success, found]`) | present **only on success** | Soft | Existing `found[0]` access still works for success path |
| **Response: error fields** | ad-hoc `error` or `msg` (any of) | **`oneOf (LinkSuccess \| LinkFailure)`**; LinkFailure has **`anyOf ([msg],[error])`** — i.e. exactly one of `msg`/`error` | Soft | Existing `if (resp.msg != null) … else if (resp.error != null)` logic still works |
| **`additionalProperties`** | unrestricted | **`false`** (strict, no extra fields) | No effect on client | — |
| **`Error responses 4xx/5xx documented for `/link`** | 400, 401, 500, 504 | **none** (only 200 documented; failures return 200 with `success:false`) | Soft | A Rails routing/validation error before reaching the controller will still surface as 404/422 |
| **`/parse` endpoint** | (not in this contract; covered separately by spec `004-cbrd-parse-endpoint`) | Published in same doc (`POST /parse`, Bearer, `application/xml` response, 400/401/422/502/503) | No effect on `/link` path | Confirms `004` is now a first-party part of the CBRD API surface |

---

## 4. Why HTTP 404 specifically

The live spec defines `/link` as **`POST` only**. On a Rails app (which CBRD is — described as Rails in spec `004-cbrd-parse-endpoint` and consistent with `PARSE_API_TOKEN` being a Rails encrypted-credentials token in the live OpenAPI), when a request hits `GET /cbrd/link` and no matching GET route exists:

- Rails' router raises `ActionController::RoutingError` ("No route matches [GET] \"/cbrd/link\"")
- In production this is rendered as **404 Not Found** (not 400/422/500)
- The request never reaches the `postCbetaLink` controller action, so the documented "always return 200 with `success:false` on failure" contract does not apply

This exactly matches the user's observed `CBRD API error: HTTP 404`. Because `CBRDAPIClient` (CBRDAPIClient.java:122-125) throws `CBRDAPIException("error.api.http", 404)` for **any** non-200, it surfaces as the generic `error.api.http` i18n template (`"CBRD API error: HTTP {0}"` → `"CBRD API error: HTTP 404"`).

Critically:
- The 404 is **NOT** "the CBRD service couldn't resolve this reference." That case is returned as **HTTP 200 with `success:false`** (`error.api.failed` key) or **HTTP 200 with empty `found:[]`** (`error.no.results` key).
- The retry logic (`executeWithRetries`, CBRDAPIClient.java:65-105) **only retries on timeouts**, so a 404 propagates immediately — no backoff.

---

## 5. Reproducing the working request (after fix)

For the user's example citation, the correct v1.1.0 request is:

```http
POST /cbrd/link HTTP/1.1
Host: cbss.dila.edu.tw
Content-Type: application/json; charset=UTF-8
Accept: application/json
User-Agent: DILA-AI-Markup/0.4.2
Referer: CBRD@dila.edu.tw   # Optional in v1.1.0; harmless to retain

{"q":"<ref><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>"}
```

Expected responses (per v1.1.0 `LinkResponse`):

```json
// Success (LinkSuccess) — at least one matching URL
{ "success": true, "found": ["https://cbetaonline.dila.edu.tw/T..."] }

// Success, no match (LinkSuccess, empty array) → error.no.results
{ "success": true, "found": [] }

// Failure (LinkFailure) → error.api.failed with msg or error
{ "success": false, "msg":   "..." }
{ "success": false, "error": "..." }
```

All three return **HTTP 200**, so the existing branch logic in `CBRDAPIClient` (`success` → first URL; `found==[] && success==true` → `error.no.results`; `success==false` → `error.api.failed`) continues to work unchanged. **Only the request construction needs to change.**

---

## 6. Required code changes

### a. `CBRDAPIClient` — switch GET → POST with JSON body

File: `src\main\java\com\dila\dama\plugin\infrastructure\api\CBRDAPIClient.java`

In `convertToFirstLink`/`executeOnce`:

1. Stop URL-encoding the XML. Build a JSON payload directly:
   ```java
   String payload = "{\"q\":\"" + escapeJson(refXml) + "\"}";
   ```
   (Use the project's existing JSON utility if one exists; otherwise a minimal `escapeJson` that handles `"`, `\`, control chars.)
2. Open `HttpsURLConnection` with POST:
   ```java
   conn.setRequestMethod("POST");
   conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
   conn.setRequestProperty("Accept", "application/json");
   conn.setDoOutput(true);
   try (OutputStream os = conn.getOutputStream()) {
       os.write(payload.getBytes(StandardCharsets.UTF_8));
   }
   ```
3. Drop the `?q=…` query-string assembly.
4. Keep `User-Agent: DILA-AI-Markup/0.4.2` and (optionally) `Referer: CBRD@dila.edu.tw` headers — both remain harmless and identical to today.
5. Non-200 handling can stay as-is (`error.api.http`, status). But note: per v1.1.0, the server should return 200 even on failure, so 404 should disappear post-fix. If a 404 persists after the POST switch, that would point to a different operational issue (wrong base URL, server down).
6. Optionally migrate off `HttpsURLConnection` to the project's HTTP client if one is used elsewhere (search the codebase for a shared HTTP utility).

### b. Response envelope — no change needed at the field level

The v1.0.0 → v1.1.0 response shape differences (`oneOf` strictness, `const` booleans, `additionalProperties:false`, `found` optional on failure) are all **more strict** than v1.0.0; the existing `CBRDResponse` parsing logic still works:

- `CBRDAPIClient.executeOnce` reads `success`, `found`, `msg`, `error` — all still present and named identically.
- `CBRDResponse.getFirstUrl()` (CBRDResponse.java:38-40) for success-with-URLs — still works.
- `error.no.results` / `error.api.failed` branches — still work.

### c. Tests

| File | Required update |
|---|---|
| `CBRDAPIClientErrorHandlingTest.java` | Update `FakeConnectionFactory` so the request URI no longer contains `?q=…`; assert POST + JSON body instead. All error-key assertions (`error.api.http`, `error.no.results`, `error.api.failed`) remain valid. |
| `CBRDAPIClientSuccessFlowTest.java` (if exists) | Same: assert POST + JSON body, and that response `found[0]` is returned on `success=true`. |
| Any "q=" string assertions (search for `q=` and `URLEncoder`) | Replace with JSON body assertions. |
| Spec contract: `specs/001-ref-to-link-action/contracts/cbrd-api.yaml` | Replace vendor contract with the v1.1.0 spec (OpenAPI 3.1.0, POST, `LinkRequest/LinkResponse` schemas). |
| `specs/001-ref-to-link-action/data-model.md` | Update i18n keys table only if a new error category is added. The three existing keys (`error.api.http`, `error.no.results`, `error.api.failed`) are unchanged in semantics. |
| `DAMAWorkspaceAccessPluginExtension.java:613-615` (preference defaults) | The default CBRD API URL `https://cbss.dila.edu.tw/cbrd/link` is still correct (only the method changed, not the URL). No preference change needed. |

### d. Backward-compatibility / preference migration

No preference migration is needed: the user-facing URL preference (`KEY_CBRD_API_URL`) is unchanged, and the method change is hard-coded inside `CBRDAPIClient`, not exposed to the user. Users on older plugin builds will keep getting 404 until they upgrade.

---

## 7. File & line index (for the implementer)

All source paths absolute.

| Concern | Path |
|---|---|
| Codebase vendor contract (v1.0.0, stale) | `D:\project\OCR2Markup\specs\001-ref-to-link-action\contracts\cbrd-api.yaml` |
| HTTP client (GET, `?q=…`) — **must change to POST + JSON body** | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\infrastructure\api\CBRDAPIClient.java:41-148` |
| Non-200 throw site (`error.api.http`, status 404) | `CBRDAPIClient.java:122-125` |
| Retry logic (only timeouts; 404 not retried) | `CBRDAPIClient.java:65-105, 161-182` |
| Response DTO (`success` / `found` / `error` / `msg`) — unchanged at field level | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\infrastructure\api\CBRDResponse.java:10-64` |
| `CBRDAPIException` (carries message key + status param) | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\infrastructure\api\CBRDAPIException.java:1-27` |
| Command orchestration (catches API exception) | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\application\command\ConvertReferenceCommand.java:19-34` |
| i18n template `error.api.http` → `"CBRD API error: HTTP {0}"` | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\resources\i18n\translation.xml:483-487` |
| Other i18n keys (`error.no.results`, `error.api.failed`) — unchanged | `translation.xml:498-502` and around |
| Plugin preference defaults (URL unchanged) | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java:613-615` |
| Action listener ("Action selected: <ref> to link") — unchanged | `DAMAWorkspaceAccessPluginExtension.java:551-577, 637-674` |
| Selection validation (`<ref>` parse) — unchanged | `DAMAWorkspaceAccessPluginExtension.java:1033-1076`, `ReferenceParser.java:13-91` |
| Rewriter (Replace button) — unchanged | `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\domain\service\RefElementRewriter.java:15-85` |
| Error key list for styling the result area | `DAMAWorkspaceAccessPluginExtension.java:1351-1365` |
| Non-200 error-handling unit test (uses 500; same branch as 404) | `Models\Gemini2.5\dila-ai-markup-plugin\src\test\java\com\dila\dama\plugin\infrastructure\api\CBRDAPIClientErrorHandlingTest.java:19-30` |
| Empty-`found` test (`error.no.results`) | `CBRDAPIClientErrorHandlingTest.java:57-64` |
| `success:false` test (`error.api.failed`) | `CBRDAPIClientErrorHandlingTest.java:42-54` |
| Separately documented `/parse` endpoint (spec `004`, AI Markup path, not `<ref> to link`) | `specs\004-cbrd-parse-endpoint\spec.md`; `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\infrastructure\api\CbrdParseApiClient.java` |

---

## 8. Conclusion & action

- **Root cause:** Spec drift. Server bumped `/link` from `GET` (v1.0.0) to `POST` JSON-body (v1.1.0). The plugin's `CBRDAPIClient` still issues GET → Rails returns 404 → user sees `CBRD API error: HTTP 404`.
- **Not the cause:** The reference itself, the column-range syntax (`―<p>二</p><c>上</c>`), or the `大正` (vs `T`) canon code. All those remain server-side concerns and would surface as 200-with-empty-found or 200-with-`success:false`, never as 404.
- **Minimum fix:** Change `CBRDAPIClient` to POST a JSON body `{"q":"<ref>…</ref>"}` with `Content-Type: application/json`, drop the URL-encoding of the XML, keep the existing `User-Agent` and `Referer` headers (harmless), and update the unit tests to assert POST + JSON body. Update the vendor contract `cbrd-api.yaml` to v1.1.0 to prevent future drift.
- **No data-model changes required:** the three existing i18n keys (`error.api.http`, `error.no.results`, `error.api.failed`) and the `CBRDResponse` DTO field parsing all remain valid under v1.1.0. The response envelope is more strict but field-compatible.

---

## 9. Why the existing tests failed to catch the drift

Five cascading reasons, each confirmed by reading the actual test source. (All paths under `Models\Gemini2.5\dila-ai-markup-plugin\src\test\java\com\dila\dama\plugin\infrastructure\api\`.)

### 9.1 The `/link` success-flow test **explicitly pins** the wrong behavior

`CBRDAPIClientTest.java:36-37` asserts the very things that are now broken:

```java
assertThat(factory.lastUrl.toString()).contains("?q=");           // asserts the GET query-string shape — wrong in v1.1.0
assertThat(factory.lastUrl.toString()).doesNotContain("<ref>");   // asserts the XML is URL-encoded out — wrong in v1.1.0
```

These aren't silent omissions — they are **positive assertions of the v1.0.0 contract**. They won't fail when the vendor drifts; they only fail if someone *changes the client to match the new contract*, which is exactly the change we want. The test suite is, in effect, blocking the fix and green-checking the bug.

### 9.2 The `/link` test fake cannot observe the HTTP method

The `/link` tests use a **private** `CapturingConnectionFactory` and `FakeHttpURLConnection` (CBRDAPIClientTest.java:42-110) — **not** the shared `CapturingConnectionFactory` (infrastructure/api/CapturingConnectionFactory.java) used by the `004` Parse tests. The private fake overrides only:

- `setRequestProperty`, `getRequestProperty`
- `getResponseCode` (hardcoded)
- `getInputStream`, `getErrorStream`, `connect`, `disconnect`, `usingProxy`

It does **NOT** override `setRequestMethod`. So when the production client calls `conn.setRequestMethod("GET")` (CBRDAPIClient.java:109), that call is a no-op in tests. There is no `lastMethod` recorded, no method assertion possible — the test surface is structurally blind to the dimension that drifted.

By contrast the shared seam (`CapturingConnectionFactory.java:100-101`) explicitly records `lastMethod`, and `CbrdParseApiClientTest.java:49` already uses it to assert `POST`. The `/parse` team built the right seam in `004`; the `/link` flow was never migrated to it.

### 9.3 The fake returns the canned status regardless of request shape

Both `/link` fakes make `getResponseCode()` return a hardcoded number (CBRDAPIClientTest.java:84-86; CBRDAPIClientErrorHandlingTest.java:134-139) and `getInputStream()`/`getErrorStream()` return a hardcoded body — none of which inspect method, path, query, body, or headers. So a malformed GET against a real Rails server that returns 404 has no analogue in the unit test: any request shape gets the stubbed `200`. The unit tests prove the client's *branch logic*, but never exercise the client against something that responds like the real server.

### 9.4 The error-handling test treats status codes as opaque

`CBRDAPIClientErrorHandlingTest.convertToFirstLink_non200_reportsHttpErrorKey` (lines 19-30) hardcodes an HTTP 500 and asserts:

```java
assertThat(apiEx.getMessageKey()).isEqualTo("error.api.http");
assertThat(apiEx.getParams()).containsExactly(500);
```

The contract here is **literally** "any non-200 → `error.api.http:<status>`". Status is passed through as an opaque param. So:

- A 404 would yield `params: [404]` and pass the same way 500 does.
- The test does not assert what 404 *means* semantically (routing error vs. legitimate not-found), only that the key is `error.api.http`.
- There is no test that distinguishes "404 because the endpoint moved methods" from "404 because the citation doesn't exist (which the contract says should never even produce 404 — it should produce 200 with empty `found`)". The `error.no.results` branch (line 57-64) is driven by `200 + found:[]`, never crossing paths with a 404 path.

### 9.5 No contract-conformance test exists

Searching the entire test tree for any conformance artifact (live-server probe, schema diff, version-bump check, OpenAPI validation, integration test that hits `cbss.dila.edu.tw`):

```
matches for: cbrd-api.yaml | openapi | contracts/cbrd | schemaValidation 
            | swagger-parse | contractConformance | liveServer | IntegrationTest
  ParseErrorTest:9           // comment only
  CbrdParseRequestBodyTest:12 // comment only
  CbrdParseApiClientTest:13  // comment only
```

Everything is a **comment** ("wire codes come from contracts/openapi.yaml"). Nothing:

- Loads and parses the vendored `specs/001-ref-to-link-action/contracts/cbrd-api.yaml`.
- Fetches `https://cbss.dila.edu.tw/cbrd/openapi.json` and compares it to the vendored copy.
- Asserts `info.version == 1.0.0` against the live doc (would have caught the bump to `1.1.0`).
- Sends an offline-formatted request to the live endpoint and checks it's accepted.
- Generates a client stub from the YAML and asserts the production client's request matches.

The vendored contract is committed as **documentation**, not as a test input.

### 9.6 Summary table — assertion self-audit

| Drift that occurred | Test that **would** have caught it | Status today |
|---|---|---|
| `GET /link` → `POST /link` | `assertThat(factory.getLastMethod()).isEqualTo("POST")` in `CBRDAPIClientTest` | **Impossible** — the private fake doesn't override `setRequestMethod`, so `lastMethod` is never recorded. |
| Query string `?q=…` → JSON body `{"q":"…"}` | `assertThat(factory.getCapturedRequestBody()).contains("\"q\":")` | **Impossible** — the private fake doesn't capture the request body (only `lastUrl` + `lastRequestProperties`). |
| URL-encoded XML in query → raw XML in JSON body | removal of `contains("?q=")` + removal of `doesNotContain("<ref>")` at CBRDAPIClientTest.java:36-37 | **Backwards** — these two asserts *pin* the broken shape and would (correctly) fail after the fix. As long as they remain green, the broken shape passes. |
| OpenAPI version bump `1.0.0 → 1.1.0` | Periodic test fetching `openapi.json` and asserting `info.version` | **Doesn't exist** — no test touches the live server. |
| Method-mismatch 404 from Rails router | End-to-end (live integration / smoke) test | **Doesn't exist**. |
| Wire-shape disambiguation between 404 (routing) and other non-200s | Test sending 404 + asserting a distinct i18n key | **Impossible** — `error.api.http` is deliberately opaque by status; that is the contract. |

### 9.7 Root structural lesson

The `/link` unit tests **encode the vendor-contract assumptions as positive assertions** (`?q=`, URL-encoded XML, no-method-assert, no-body-assert). When a vendor drifts:

- A positive assertion against the *old* shape only fails if you **change the client to match the new contract** — i.e. it fails on the fix, not on the drift.
- The tests are in lockstep with the v1.0.0 client; they green-flag the bug because the bug *is* the behavior they assert.
- The shared `CapturingConnectionFactory` infrastructure (record method + body) was built for the `004` Parse client and never propagated back to the `001` Link flow.

So the existing tests can't catch the drift because (1) the assertion seam for `/link` can't see the dimension that drifted (no method/body capture), (2) the assertions present actively **ratify** the drifted-against shape, and (3) no test ever asks the live server "are you still the shape I think you are?". A live-API contract-conformance probe, or even migrating the `/link` tests to the shared seam and adding `lastMethod == "POST"` + JSON-body asserts, would have surfaced this immediately.

### 9.8 Minimum recommended remediation in tests

1. Migrate `CBRDAPIClientTest` to use the shared `infrastructure/api/CapturingConnectionFactory` (the `004` seam). Delete the file-local `CapturingConnectionFactory` and `FakeHttpURLConnection` at lines 42-110.
2. Add `assertThat(factory.getLastMethod()).isEqualTo("POST")`.
3. Add `assertThat(factory.getRequestProperty("Content-Type")).startsWith("application/json")`.
4. Add `assertThat(factory.getCapturedRequestBody()).contains("\"q\":\"").doesNotContain("?q=")`.
5. Remove the now-obsolete assertions `contains("?q=")` and `doesNotContain("<ref>")` at lines 36-37.
6. Add a `CBRDContractConformanceTest` (a long-running / nightly test bears the live-server dependency, or a short test that loads `cbrd-api.yaml` and asserts the production client's expected wire-shape against it) — at minimum pin `info.version` so a version bump surfaces as a deliberate, reviewed change rather than silent slip.
7. Optionally, add an env-flagged live-smoke test that does an actual `POST https://cbss.dila.edu.tw/cbrd/link` with `{"q":"<ref><canon>T</canon><v>25</v></ref>"}` and asserts an HTTP 200 back — this is the only mechanism that catches arbitrary vendor drift on its own, not just the dimensions we remember to assert.

---

## 9. Verified corrections (2026-08-02, added before commit)

The analysis above is sound and its root cause holds. Three points were checked
against the current code on branch `004-cbrd-parse-endpoint` and need amending
before anyone implements §6 — two of them would send an implementer down a
wrong path.

### 9.1 §6a item 6 is already done — no HTTP client migration needed

> "Optionally migrate off `HttpsURLConnection` to the project's HTTP client if
> one is used elsewhere (search the codebase for a shared HTTP utility)."

That shared utility exists and `CBRDAPIClient` already uses it:

```java
// CBRDAPIClient.java:23-29
private final HttpUrlConnectionFactory connectionFactory;
public CBRDAPIClient(String apiUrl, String refererHeaderValue, int timeoutMs,
                     HttpUrlConnectionFactory connectionFactory) { ... }
```

`CBRDAPIClientTest` and `CBRDAPIClientErrorHandlingTest` already inject fakes
through it, and `CbrdParseApiClient` (004) uses the same seam. There is nothing
to migrate; the change is confined to the request construction inside
`executeOnce`.

### 9.2 §6a item 1 — do NOT hand-roll `escapeJson`

> "(Use the project's existing JSON utility if one exists; otherwise a minimal
> `escapeJson` that handles `"`, `\`, control chars.)"

`org.json` is already a compile-scope dependency and is already used in main
code (`CbrdParseApiClient`, and the OpenAI client before its retirement). Use:

```java
String payload = new JSONObject().put("q", refXml).toString();
```

Hand-rolled escaping is a poor fit here specifically: the payload is `<ref>` XML
containing double quotes and CJK text, which is exactly the input that breaks
naive escapers. The house pattern is `JSONObject`.

### 9.3 A caution the fix should carry: fakes cannot see host-installed handlers

Every HTTP test in this repository injects a fake `HttpURLConnection`. That is
fine for request shape, but it cannot reproduce runtime behaviour that the host
application introduces.

This is not hypothetical. During 004 Phase 8/9 a rejected token was being
reported as a connectivity failure, and the cause was that **Oxygen installs its
own URL stream handler**: a non-2xx response throws
`ro.sync.net.protocol.http.HttpExceptionWithDetails` with message
`"NNN Reason for: <url>"` instead of the JDK's
`"Server returned HTTP response code: NNN for URL: ..."`. Every one of the 354
tests passed throughout, because the fakes never behaved that way. It was found
only in a live diagnostics export.

Implications for this drift fix:

- The §4 reasoning ("Rails renders `ActionController::RoutingError` as 404") is
  still the right diagnosis, but inside Oxygen the 404 may not arrive as a clean
  `getResponseCode()` at all — it may surface as `HttpExceptionWithDetails`.
- After the GET→POST fix, `/link` should return HTTP 200 for every documented
  outcome, so this path should stop mattering for Ref-to-Link. Confirm that in
  Oxygen, not only against a unit test.
- If a non-200 path is retained for `/link`, reuse the status-recovery logic
  already written for the parse client
  (`CbrdParseApiClient.recoverStatusFromException`) rather than writing a second
  one.

### 9.4 Scope note

`CBRDAPIClient` also hardcodes `User-Agent: DILA-AI-Markup/0.4.2`, stale since
the plugin reached 0.4.3 and now two releases behind at 0.5.0. It was left
untouched by 004 on purpose: FR-017 requires Ref-to-Link behaviour to be
unchanged by that feature, and the header is part of that behaviour. `CBRDAPIClientTest:39`
asserts the stale literal, which is why the drift was never caught — the test
pins the output instead of the rule. Both that literal and the one in
`CbrdParseApiClient` are scheduled for the single-source-version follow-up.
