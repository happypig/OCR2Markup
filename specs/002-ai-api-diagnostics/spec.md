# Feature Specification: Cross-Platform API Diagnostics

**Feature Branch**: `002-ai-api-diagnostics`  
**Created**: 2026-03-27  
**Status**: Draft  
**Input**: User description: "Add a cross-platform API diagnostics and request-hardening feature for the DILA AI Markup plugin so AI Markup behaves consistently on Windows and macOS. The feature must detect and clearly report OpenAI API failures, especially 400 vs 401 errors, invalid or inaccessible model names, endpoint/model mismatches, missing or malformed API keys, proxy-related configuration issues, and malformed request payloads. The plugin must log the sanitized request metadata and full error response body without exposing secrets, and display actionable user-facing guidance in the DAMA UI. The feature must preserve non-blocking UI behavior, use the existing Java plugin architecture, and support Oxygen XML Editor on both Windows and macOS. Include acceptance criteria for consistent behavior across operating systems, diagnostic quality, and safe redaction of API credentials."

## Clarifications

### Session 2026-03-27

- Q: Should diagnostics scope cover only OpenAI-hosted endpoints or any OpenAI-compatible endpoint configured in DAMA? → A: Scope diagnostics to OpenAI and other OpenAI-compatible endpoints configured in DAMA.
- Q: Should this feature only diagnose failures, or should it also auto-correct settings or retry with alternate request shapes? → A: Keep this feature diagnostics-only, without automatic correction or compatibility fallbacks.
- Q: Should cross-platform consistency require identical wording, or equivalent meaning with platform-specific wording when needed? → A: Require equivalent diagnostic meaning and next-step guidance, while allowing platform-specific wording for OS-specific troubleshooting.
- Q: Should the DAMA panel show the full sanitized service error body or a concise actionable summary? → A: Show a concise actionable summary in the DAMA panel and keep the fuller sanitized error body in the troubleshooting record.
- Q: Should troubleshooting records remain local-only, or should users be able to export them manually for support? → A: Users can export troubleshooting records manually for support sharing.

## Event Storming

### Actors

- Markup Editor
- Support Maintainer
- Configured OpenAI-Compatible Service
- Local Plugin Runtime

### Commands

- Invoke AI Markup
- Validate AI Markup Configuration
- Build Chat Completion Request
- Classify Returned Failure
- Capture Troubleshooting Record
- Export Sanitized Diagnostics

### Domain Events

- AI Markup Invoked
- Configuration Validation Failed
- Request Payload Validation Failed
- Request Sent To Service
- Service Rejected Request
- Failure Classified
- Concise Diagnostic Displayed
- Troubleshooting Record Sanitized
- Troubleshooting Record Exported
- Successful AI Markup Completed Without Diagnostic Warning

### Policies

- When required configuration is missing or malformed, classify the failure locally before any outbound request is attempted.
- When a request fails, classify it into a user-meaningful category before displaying guidance.
- When diagnostics are shown in the DAMA panel, expose only a concise actionable summary.
- When troubleshooting details are stored or exported, apply the same redaction policy used for logs and visible messages.
- When equivalent failure conditions occur on Windows and macOS, preserve the same classification and equivalent next-step guidance.
- When an AI Markup operation is already in progress, prevent overlapping diagnostics from producing ambiguous results for the same user action.

### External System Boundaries

- OpenAI-hosted chat completions endpoints
- Other OpenAI-compatible chat completions endpoints configured in DAMA
- Local file system destination chosen by the user for manual diagnostic export
- Oxygen XML Editor workspace, editor, and preferences environment

## Problem Frames

### Problem Domain

- Markup editors need to understand why AI Markup failed without reading raw HTTP responses.
- Support maintainers need sanitized but sufficiently detailed troubleshooting evidence.
- Service providers may reject requests because of credentials, model access, payload shape, endpoint compatibility, proxy/network interference, or transient service conditions.
- Windows and macOS environments may differ in proxy routing, local settings, and OS-specific troubleshooting steps.

### Solution Domain

- The DAMA panel displays concise guidance and progress/failure status.
- The plugin validates configuration and request shape before sending outbound requests.
- Diagnostic classification maps request and service failures to user-meaningful categories.
- Troubleshooting records and manual exports preserve sanitized evidence for support workflows.

### Shared Phenomena

- Selected text and AI Markup invocation context
- Stored API key, model identifier, endpoint base URL, and proxy-related connection settings
- Outbound chat completions request payload and returned status/body
- Diagnostic category, confidence, and corrective guidance shown to the user
- Sanitized troubleshooting record content shared with support

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Diagnose AI Markup Failures Clearly (Priority: P1)

A markup editor using AI Markup needs immediate, understandable feedback when a request to the configured OpenAI-compatible service fails, so they can tell whether the problem is authentication, model access, request formatting, endpoint compatibility, or service configuration instead of seeing a generic failure.

**Why this priority**: Clear diagnosis is the minimum valuable outcome. Without it, users cannot tell whether they should fix their settings, retry later, or report a bug.

**Independent Test**: Trigger AI Markup failures that represent different categories of request problems and verify the DAMA panel shows distinct, actionable guidance for each failure type without exposing secrets.

**Acceptance Scenarios**:

1. **Given** a user invokes AI Markup and the service rejects the request as unauthorized, **When** the failure is returned to the DAMA panel, **Then** the user sees a message that clearly indicates a credential problem and explains the next corrective action.
2. **Given** a user invokes AI Markup and the service rejects the request because the configured model is invalid or inaccessible, **When** the failure is returned, **Then** the DAMA panel identifies the model configuration as the likely cause and tells the user what to verify.
3. **Given** a user invokes AI Markup and the service rejects the request as malformed or incompatible with the selected endpoint, **When** the failure is returned, **Then** the DAMA panel distinguishes it from an authentication failure and provides targeted guidance instead of a generic error.

---

### User Story 2 - Keep Diagnostics Consistent Across Windows and macOS (Priority: P2)

A markup editor working on either Windows or macOS needs the same AI Markup request to produce equivalent outcomes and equivalent diagnostic messages so that configuration problems can be reproduced and resolved consistently across operating systems.

**Why this priority**: The current user pain is platform inconsistency. Resolving that inconsistency is necessary to make support and troubleshooting credible.

**Independent Test**: Execute the same AI Markup request and the same failure conditions on Windows and macOS and verify that the resulting classifications, user guidance, and visible workflow behavior remain equivalent.

**Acceptance Scenarios**:

1. **Given** the same AI Markup configuration and the same selected text on Windows and macOS, **When** both environments encounter the same request failure category, **Then** both environments display the same diagnostic classification and equivalent next-step guidance, even if some wording differs to explain platform-specific troubleshooting.
2. **Given** the same AI Markup configuration and the same selected text on Windows and macOS, **When** both environments succeed, **Then** the visible user workflow completes without platform-specific warning messages or unexplained deviations.
3. **Given** one environment is missing a required configuration value or is using an incompatible network path, **When** AI Markup is invoked, **Then** the user receives a platform-appropriate but semantically equivalent message describing the missing or conflicting configuration.

---

### User Story 3 - Preserve Safe Troubleshooting Records (Priority: P3)

A maintainer or support engineer needs enough diagnostic information to investigate AI Markup request failures while ensuring sensitive values such as credentials are never exposed in logs or user-facing messages.

**Why this priority**: Good diagnostics are only useful if they are safe to share and safe to keep. Redaction is essential for supportability.

**Independent Test**: Trigger a failed AI Markup request and inspect user-visible diagnostics and captured troubleshooting records to verify that sensitive values are redacted while the remaining metadata is sufficient for analysis.

**Acceptance Scenarios**:

1. **Given** AI Markup records request diagnostics after a failure, **When** a maintainer reviews the troubleshooting record, **Then** request metadata and the fuller sanitized service error body are available for analysis without exposing API keys or other secrets.
2. **Given** a user shares the visible DAMA error message with support, **When** the message is reviewed, **Then** it contains a concise actionable summary without revealing credentials, secret tokens, or the full service error body.
3. **Given** a failure occurs during background processing, **When** diagnostics are captured, **Then** the editor remains responsive and the troubleshooting record still includes enough information to distinguish request, configuration, and service-side problems.
4. **Given** a user needs help from support, **When** they choose to share diagnostics, **Then** they can export the troubleshooting record manually in a form that preserves sanitized details without exposing secrets.

---

### Edge Cases

- The configured credential is present but malformed, expired, or associated with a different account than the configured model.
- The configured model exists but cannot be used from the selected request path.
- The selected text is valid for AI Markup, but hidden platform-specific settings cause request delivery differences between Windows and macOS.
- The service returns an empty or partially structured error body.
- The service returns a generic request failure that cannot be mapped confidently to one cause.
- A proxy or network intermediary alters the request path or strips headers before the request reaches the service.
- The user retries the action multiple times while a previous request is still in progress.
- The system detects a likely fixable misconfiguration but, by design, only reports corrective guidance and does not change settings or rewrite the request automatically.
- A user exports troubleshooting details for support review and expects the export to remain sanitized.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST classify AI Markup request failures into user-meaningful categories, including authentication problems, invalid or inaccessible model configuration, malformed request problems, incompatible request path problems, connectivity/proxy problems, and unknown service failures.
- **FR-002**: System MUST display user-facing guidance for each failure category that explains the likely cause and the next action the user should take.
- **FR-003**: System MUST distinguish unauthorized failures from malformed-request failures so that users are not told to replace credentials when the real problem is request compatibility.
- **FR-004**: System MUST detect when required configuration values for AI Markup are missing or malformed before attempting the request, and those required values MUST include at minimum credential presence and shape, model identifier, endpoint base URL, and any configured proxy-related connection settings used by the plugin runtime.
- **FR-005**: System MUST surface when the configured model appears invalid, inaccessible, or incompatible with the selected request path.
- **FR-006**: System MUST apply diagnostics consistently for OpenAI-hosted and other OpenAI-compatible endpoints configured in DAMA.
- **FR-007**: System MUST capture troubleshooting records for AI Markup failures that include sanitized request metadata and the returned service error body.
- **FR-008**: System MUST redact API keys, secret tokens, and equivalent sensitive values from troubleshooting records and user-facing messages.
- **FR-009**: System MUST present a concise actionable summary in the DAMA panel and reserve the fuller sanitized service error body for troubleshooting records.
- **FR-010**: System MUST preserve equivalent diagnostic behavior on Windows and macOS for the same request conditions, while allowing platform-specific wording only when it clarifies OS-specific troubleshooting steps.
- **FR-011**: System MUST keep the DAMA user interface responsive while requests are in progress and while diagnostics are captured.
- **FR-012**: System MUST prevent overlapping AI Markup operations from producing ambiguous or conflicting diagnostic output for the same user action.
- **FR-013**: System MUST provide a generic fallback diagnostic message when the failure cannot be classified confidently, while still preserving sanitized troubleshooting context.
- **FR-014**: Users MUST be able to tell from the DAMA panel whether a failure is most likely caused by credentials, model access, request compatibility, service availability, or local configuration.
- **FR-015**: System MUST remain diagnostics-only for this feature and MUST NOT automatically modify DAMA settings, credentials, selected models, endpoint selections, or request shapes as part of failure handling.
- **FR-016**: System MUST allow users to export troubleshooting records manually for support sharing, and exported records MUST preserve the same redaction rules as on-screen diagnostics.

### Functional Requirement Acceptance Criteria

- **FR-001**: Given representative authentication, model-access, malformed-payload, endpoint-mismatch, proxy/connectivity, and unknown-service failures, when AI Markup diagnostics run, then each failure is assigned to the expected user-meaningful category.
- **FR-002**: Given a classified failure, when the DAMA panel displays guidance, then the message states the likely cause and the next user action without exposing secrets.
- **FR-003**: Given one unauthorized failure and one malformed-request failure, when both are diagnosed, then the credential guidance and request-compatibility guidance remain distinct.
- **FR-004**: Given missing or malformed credentials, model identifier, endpoint base URL, or proxy-related settings, when AI Markup is invoked, then the plugin rejects the request locally and tells the user exactly which configuration area to correct.
- **FR-005**: Given a configured model that is invalid, inaccessible, or incompatible with the selected request path, when diagnostics run, then the user is told to verify model access or model-to-endpoint compatibility.
- **FR-006**: Given equivalent failures from an OpenAI-hosted endpoint and another configured OpenAI-compatible endpoint, when diagnostics run, then both are classified and reported through the same diagnostic model.
- **FR-007**: Given an AI Markup failure, when the troubleshooting record is captured, then it contains sanitized request metadata and the sanitized service error body needed for support analysis.
- **FR-008**: Given secrets in configuration values, headers, request bodies, or returned error content, when diagnostics are shown, logged, or exported, then all secrets are masked or removed.
- **FR-009**: Given a failed request, when the DAMA panel is updated, then it shows only a concise actionable summary and omits the fuller sanitized error body reserved for troubleshooting records.
- **FR-010**: Given equivalent request conditions on Windows and macOS, when the same failure occurs, then both platforms show the same diagnostic classification and materially equivalent guidance, allowing only OS-specific troubleshooting wording to differ.
- **FR-011**: Given AI Markup validation, network execution, or diagnostic capture is in progress, when the user continues interacting with the editor, then the editor remains responsive and visible status or failure feedback appears within the stated time target.
- **FR-012**: Given an AI Markup operation is already running, when the user invokes the action again, then the plugin prevents overlapping diagnostics from producing conflicting output for the same action.
- **FR-013**: Given a failure cannot be classified confidently, when diagnostics complete, then the DAMA panel shows a generic fallback message and the troubleshooting record preserves the sanitized uncertainty context.
- **FR-014**: Given any supported failure category, when the user reads the DAMA panel summary, then they can tell whether the most likely cause is credentials, model access, request compatibility, service availability, or local configuration.
- **FR-015**: Given a likely misconfiguration is detected, when diagnostics complete, then the plugin provides corrective guidance without changing settings, credentials, model selection, endpoint selection, or request shape automatically.
- **FR-016**: Given a user manually exports diagnostics, when the export is created, then the file remains sanitized and contains enough support-triage context to identify the failure category without unsanitized local logs.

### Key Entities *(include if feature involves data)*

- **AI Markup Diagnostic Result**: The user-visible outcome of an AI Markup request, including the failure category, confidence of classification, actionable guidance, and whether the user can retry immediately.
- **Sanitized Troubleshooting Record**: A support-oriented record of request metadata and returned error content with secrets removed or masked.
- **Exported Diagnostic Package**: A user-initiated shareable form of the troubleshooting record that preserves sanitized failure context for support analysis.
- **Markup Service Configuration**: The collection of values that determine how AI Markup reaches the configured OpenAI-compatible service, including credentials, model identifier, endpoint base URL, and proxy-related connection path details.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In at least 95% of sampled AI Markup failures from the defined failure categories, users can identify the likely corrective action from the DAMA panel without needing to inspect raw logs.
- **SC-002**: Equivalent AI Markup request conditions on Windows and macOS produce the same diagnostic classification and materially equivalent corrective guidance in 100% of validation scenarios.
- **SC-003**: In 100% of sampled troubleshooting records generated for this feature, secrets remain redacted while the remaining metadata is sufficient to distinguish the major failure categories.
- **SC-004**: Users receive visible feedback that an AI Markup request is processing or has failed within 3 seconds of invoking the action, without the editor becoming unresponsive.
- **SC-005**: In at least 90% of sampled known AI Markup request failures, a maintainer can identify the most likely root-cause category from the sanitized diagnostic output within 5 minutes and without access to unsanitized local logs.
- **SC-006**: In 100% of sampled manual diagnostic exports, the exported package matches the documented schema, remains sanitized, and gives support enough information to identify the failure category without requiring unsanitized local logs.

## Assumptions

- AI Markup already exists as a user-visible DAMA workflow and this feature improves its reliability and diagnosability rather than replacing it.
- The plugin continues to support both Windows and macOS installations used by the project.
- Users and support maintainers benefit from diagnostics that are precise enough to guide action but safe enough to share.
- The configured OpenAI-compatible service may reject requests for multiple reasons even when network connectivity exists.
- DAMA may be configured to use OpenAI-hosted or other OpenAI-compatible endpoints, and this feature covers both.
- Users will apply any recommended corrective actions manually rather than relying on automatic repair in this feature.
- Users may choose to export sanitized troubleshooting details manually when seeking support.
- Existing DAMA workflows already provide a place to present user-facing messages about request progress and failure.
- UI responsiveness means the editor remains interactive, processing/failure status appears within the stated time target, and diagnostic capture continues asynchronously without blocking the Event Dispatch Thread.
