# Research Findings: Cross-Platform API Diagnostics

**Branch**: `002-ai-api-diagnostics` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)

---

## Executive Summary

This research resolves the planning decisions needed to implement AI Markup diagnostics without breaking the plugin's Java/Oxygen compatibility rules:

1. Keep the feature on the Java 8 plugin baseline and reuse `HttpURLConnection`
2. Replace ad hoc string parsing with structured JSON parsing and explicit failure classification
3. Preserve diagnostics-only scope while still allowing manual export of sanitized troubleshooting records
4. Support OpenAI-hosted and OpenAI-compatible endpoints through DAMA configuration, not hardcoded assumptions

---

## Research Area 1: Java/Oxygen Compatibility Strategy

### Decision

**Keep implementation on Java 8 source/bytecode and use `HttpURLConnection` plus existing `org.json`.**

### Rationale

The constitution now explicitly distinguishes the Oxygen host runtime (Java 17) from the plugin compilation baseline (Java 8). The existing plugin Maven configuration already enforces `source=1.8`, `target=1.8`, and `release=8`, so using `HttpClient` or newer Java APIs would create unnecessary compatibility drift.

### Alternatives considered

- **Raise plugin baseline to Java 11/17**: Rejected because the feature does not require newer language/runtime APIs and the migration cost is unrelated to diagnostics.
- **Keep manual string-based JSON extraction**: Rejected because the feature centers on reliable failure parsing and redaction.

---

## Research Area 2: Request Construction and Error Parsing

### Decision

**Introduce structured request validation and JSON-based success/error parsing for OpenAI-compatible chat completions responses.**

### Rationale

Current AI Markup code in `DAMAWorkspaceAccessPluginExtension` builds request JSON manually and parses success responses with string searching. That is too fragile for classifying malformed requests, endpoint mismatches, or partially structured error bodies. The repo already depends on `org.json`, which is sufficient for Java 8-compatible request/error parsing and avoids new dependencies.

### Alternatives considered

- **Manual string matching only**: Rejected due to brittle parsing and poor support for classification confidence.
- **Add a heavier JSON binding library**: Rejected because `org.json` is already present and adequate for the bounded payloads in this feature.

---

## Research Area 3: Endpoint Scope and Compatibility

### Decision

**Support OpenAI-hosted and other OpenAI-compatible endpoints configured in DAMA, with diagnostics validating compatibility rather than assuming one fixed server.**

### Rationale

The clarified spec explicitly includes OpenAI-compatible endpoints. The feature should therefore validate endpoint path assumptions, model/endpoint compatibility, and configuration completeness even when the request target is not the default OpenAI host. That requires endpoint configuration to be treated as part of `MarkupServiceConfiguration`.

### Alternatives considered

- **OpenAI-hosted only**: Rejected by clarification because it would force rework if the plugin is pointed at compatible gateways.
- **Current hardcoded endpoint only**: Rejected because it conflicts with the clarified scope and undermines the value of diagnostics.

---

## Research Area 4: Diagnostics Scope and UX

### Decision

**Keep the feature diagnostics-only, show concise actionable summaries in the DAMA panel, and keep fuller sanitized details in troubleshooting records.**

### Rationale

This choice reduces implementation risk and keeps the feature within the clarified scope. Users need actionable guidance, not raw payload dumps, while maintainers still need deeper sanitized context for support and verification.

### Alternatives considered

- **Automatic local correction of settings**: Rejected because it mutates configuration and expands the feature beyond diagnostics.
- **Automatic request-shape or endpoint fallback**: Rejected because it introduces hidden behavior and complicates acceptance testing.
- **Show full sanitized error body in UI**: Rejected because it increases noise and leakage risk for normal users.

---

## Research Area 5: Troubleshooting Record Retention and Export

### Decision

**Maintain troubleshooting records in memory for the active workflow and allow manual export of a sanitized diagnostic package when the user chooses to share it.**

### Rationale

The spec now requires manual support export but does not require background persistence across sessions. In-memory session state plus explicit export keeps privacy manageable and avoids accidental retention of sensitive metadata.

### Alternatives considered

- **Local-only with no export**: Rejected because it weakens supportability.
- **Automatic persistence across sessions**: Rejected because it adds retention/privacy questions not needed for this feature.

---

## Research Area 6: Cross-Platform Parity

### Decision

**Require equivalent classification and next-step guidance across Windows and macOS, while allowing wording to differ only when OS-specific troubleshooting needs to be explained.**

### Rationale

Exact text identity would be unnecessarily rigid, but allowing free divergence would make support diagnostics hard to compare. Semantic parity is the correct planning target.

### Alternatives considered

- **Identical wording on all platforms**: Rejected because platform-specific network/proxy advice may need different wording.
- **Loosely similar messages per OS**: Rejected because it weakens testability.

---

## Resulting Design Constraints

1. New code must stay Java 8-compatible.
2. The touched AI Markup path must move toward layered command/query orchestration.
3. Secrets must be redacted consistently in UI, logs, records, and exported packages.
4. Export is user-initiated and sanitized.
5. The feature must classify at least these families of failure:
   - credentials/authentication
   - invalid/inaccessible model
   - malformed request
   - endpoint/path compatibility
   - proxy/connectivity
   - unknown service failure

---

## Planning Readiness

All Phase 0 research questions relevant to this feature are resolved. No remaining `NEEDS CLARIFICATION` items block data modeling, contract definition, or quickstart preparation.
