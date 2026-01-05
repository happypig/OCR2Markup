# Quick Start Guide: Ref to Link Action

**Branch**: `001-ref-to-link-action` | **Date**: 2026-01-05

---

## Overview

This guide provides a quick reference for developers implementing the Ref to Link feature. For complete details, see:
- [spec.md](spec.md) - Feature specification
- [plan.md](plan.md) - Implementation plan
- [research.md](research.md) - Research findings
- [data-model.md](data-model.md) - Data model and validation rules

---

## Architecture at a Glance

```
+------------------------------+
| UI Layer (Swing)             |
| RefToLinkActionListener      |
+------------------------------+
               |
+------------------------------+
| Application Layer            |
| ConvertReferenceCommand      |
+------------------------------+
               |
+------------------------------+
| Domain Layer                 |
| ReferenceParser              |
| TripitakaComponents          |
| RefElementRewriter           |
+------------------------------+
               |
+------------------------------+
| Infrastructure Layer         |
| CBRDAPIClient (raw <ref> XML)|
+------------------------------+
```


---

## Key Classes

### Domain Layer (`com.dila.dama.plugin.domain`)

**Value Objects**:
- `TripitakaComponents` - Raw components from XML (immutable)
- `TransformedComponents` - Legacy API-ready components (not used in raw-XML API path)

**Aggregate Root**:
- `ReferenceConversionSession` - Tracks conversion workflow state

**Services**:
- `ReferenceParser` - Parse `<ref>` XML to extract components
- `ComponentTransformer` - Legacy transformer (not used before API call)
- `RefElementRewriter` - Rewrite `<ref>` contents for replacement (ptr/checked)

**Exceptions**:
- `InvalidReferenceException` - Thrown when XML parsing fails
- `TransformationException` - Legacy transformer exception (not used before API call)

### Application Layer (`com.dila.dama.plugin.application`)

**Commands**:
- `ConvertReferenceCommand` - Orchestrates entire workflow

### Infrastructure Layer (`com.dila.dama.plugin.infrastructure`)

**API Client**:
- `CBRDAPIClient` - HTTP client for CBRD API
- `CBRDResponse` - DTO for API response
- `CBRDAPIException` - Thrown on API errors

**UI**:
- `RefToLinkActionListener` - Action listener (inner class in DAMAWorkspaceAccessPluginExtension)

---

## Workflow Summary

```
1. User selects <ref> element in editor
2. User clicks [Actions] -> [<ref> to link]
3. System displays selection in infoArea
4. System validates/parses XML -> TripitakaComponents
5. System displays "Calling CBRD API..." and disables [Convert] while request is in-flight
6. System calls CBRD API immediately with the raw <ref> XML (no local normalization)
7. System displays URL or error in resultArea
8. System shows [Replace] button on success (Convert remains available for retry)
9. User clicks [Replace]
10. System updates document (remove existing <ptr>, insert new <ptr> as first child, set checked="2")
11. System shows success message
```


---

## CBRD API Quick Reference

**Endpoint**: `https://cbss.dila.edu.tw/dev/cbrd/link`

**Request**:
```
GET /dev/cbrd/link?q=<ref><canon>T</canon><v>25</v>.<w>1514</w></ref>
Headers:
  Referer: CBRD@dila.edu.tw
  User-Agent: DILA-AI-Markup/0.4.0
  Accept-Charset: UTF-8
```

**Response** (Success):
```json
{
  "success": true,
  "found": ["https://cbetaonline.dila.edu.tw/T1514"]
}
```

**Response** (No Results):
```json
{
  "success": true,
  "found": []
}
```

**Response** (Error):
```json
{
  "success": false,
  "error": "Error message"
}
```

---

## Component Transformations

No local normalization is applied before calling the API. The selected `<ref>` XML is sent as-is, and the CBRD service handles canon/numeral variations.

---

## Required Components

| Component | Required | Description |
|-----------|----------|-------------|
| `<canon>` | ✅ Yes | Canon name or code |
| `<v>` | ✅ Yes | Volume number |
| `<p>` | ✅ Yes | Page number |
| `<w>` | ⬜ No | Work number (recommended) |
| `<c>` | ⬜ No | Column indicator |
| `<l>` | ⬜ No | Line number |

---

## Validation Rules

### Selection Validation
- Must be non-null, non-empty
- Must start with "<ref"
- Must be valid XML

### XML Validation
- Must contain `<canon>` with non-empty text
- Must contain `<v>` with non-empty text
- Must contain `<p>` with non-empty text

### API Validation
- Response must be valid JSON
- `success` field must exist
- `found` field must be array

---

## Error Handling

**Strategy**: Errors are captured in session state, not thrown to UI.

**Terminal Error States**:
- `PARSING_FAILED` - XML parsing error
- `API_FAILED` - API call error
- `NO_RESULTS` - No URLs found (not an error, valid state)

**User Actions**:
- All errors display immediately in resultArea
- User can click [Convert] again to retry (FR-013)
- Automatic retries on timeout (up to 3 attempts with exponential backoff), then show timeout error

---

## UI Integration

**Follow Existing Patterns**:
1. **Action Listener**: Inner class in `DAMAWorkspaceAccessPluginExtension`
2. **Operation Type**: Add `REF_TO_LINK` to `OperationType` enum
3. **Selection**: Use `fetchSelectedRefToLinkText()` for validation
4. **Auto Convert**: Trigger API call immediately on action; keep [Convert] for retry
5. **Result Display**: Write URL/error to resultArea and show Replace on success
6. **Async Operations**: Use existing executor with `CompletableFuture`
7. **UI Updates**: Wrap in `SwingUtilities.invokeLater()`

---

## Configuration

**Stored in Oxygen's WSOptionsStorage**:
- `cbrd.api.url` - CBRD API endpoint (default: `https://cbss.dila.edu.tw/dev/cbrd/link`)
- `cbrd.referer.header` - Referer header value (default: `CBRD@dila.edu.tw`)
- `cbrd.timeout` - API timeout in ms (default: `3000`, per attempt)

**Retry behavior (fixed)**:
- Up to 3 attempts on timeout with exponential backoff (250ms, 500ms)
- Per-attempt timeout scales by attempt (timeout, timeout*2, timeout*3)

---

## i18n Keys to Add

**Menu/Actions**:
- `menuItem.ref.to.link` - "<ref> to link"
- `action.ref.to.link.selected` - "Action: <ref> to link selected"

**Info Messages**:
- `selected.ref` - "Selected <ref> element"
- `ref.components.extracted` - "Reference components extracted:"
- `calling.cbrd.api` - "Calling CBRD API..."

**Success Messages**:
- `success.link.generated` - "CBETA link generated successfully"
- `success.replacement.complete` - "Replacement complete"

**Error Messages**:
- `error.no.selection` - "No text selected"
- `error.invalid.xml` - "Selected text is not valid XML"
- `error.not.ref.element` - "Selected text is not a <ref> element"
- `error.missing.canon` - "Missing required <canon> element"
- `error.missing.volume` - "Missing required <v> (volume) element"
- `error.missing.page` - "Missing required <p> (page) element"
- `error.unknown.canon` - "Unknown canon: {0}"
- `error.invalid.numerals` - "Invalid numeral format: {0}"
- `error.api.timeout` - "CBRD API timeout (after retries)"
- `error.api.connection` - "Cannot connect to CBRD API"
- `error.no.results` - "No CBETA links found for this reference"

---

## Testing Strategy

### Unit Tests
- `TripitakaComponentsTest` - Value object validation
- `TransformedComponentsTest` - Legacy value object validation (no longer in API path)
- `ReferenceParserTest` - XML parsing with various inputs
- `ComponentTransformerTest` - Legacy transformation logic (not used before API call)
- `ReferenceConversionSessionTest` - State transitions

### Integration Tests
- `CBRDAPIClientTest` - API call with mock HTTP responses
- `ConvertReferenceCommandTest` - Full workflow with mocks
- `RefToLinkActionListenerTest` - UI workflow with Oxygen SDK mocks

### Test Data
- Valid `<ref>` elements with all components
- Minimal `<ref>` elements (canon, volume, page only)
- Invalid `<ref>` elements (missing required components)
- Various canon formats (大正蔵, T, 続蔵, X)
- Various numeral formats (CJK, Arabic, mixed)
- Mock CBRD API responses (success, no results, errors)

---

## Development Checklist

### Phase 1: Domain Layer
- [ ] Create `TripitakaComponents` value object
- [ ] Create `TransformedComponents` value object (legacy; not used before API call)
- [ ] Create `ReferenceConversionSession` aggregate
- [ ] Create `ReferenceParser` service
- [ ] Create `ComponentTransformer` service (legacy; not used before API call)
- [ ] Create domain exceptions
- [ ] Write unit tests for all domain classes

### Phase 2: Infrastructure Layer
- [ ] Create `CBRDAPIClient`
- [ ] Create `CBRDResponse` DTO
- [ ] Create `CBRDAPIException`
- [ ] Implement HTTP request handling
- [ ] Implement JSON parsing
- [ ] Write integration tests with mock HTTP

### Phase 3: Application Layer
- [ ] Create `ConvertReferenceCommand`
- [ ] Implement workflow orchestration
- [ ] Write integration tests

### Phase 4: UI Layer
- [ ] Add `REF_TO_LINK` to `OperationType` enum
- [ ] Create `RefToLinkActionListener` inner class
- [ ] Add menu item to Actions menu
- [ ] Implement UI event handlers
- [ ] Add i18n translation keys
- [ ] Write UI integration tests

### Phase 5: Configuration
- [ ] Add CBRD API configuration to preferences page
- [ ] Implement `WSOptionsStorage` integration
- [ ] Add default configuration values

### Phase 6: End-to-End Testing
- [ ] Test with real Oxygen XML Editor
- [ ] Test with real CBRD API
- [ ] Test all error scenarios
- [ ] Test with various `<ref>` formats
- [ ] Performance testing (SC-003: <3s API response)
- [ ] User acceptance testing

---

## Common Pitfalls

1. **Don't block EDT**: Always use `CompletableFuture` for API calls
2. **Always wrap UI updates**: Use `SwingUtilities.invokeLater()` for all UI updates from background threads
3. **Validate Oxygen SDK nulls**: `WSEditor`, `WSEditorPage`, etc. can be null
4. **URL encode query parameter**: CBRD API query must be URL-encoded
5. **Preserve reference text**: Only replace `<ptr>` elements, keep all other content in `<ref>`
6. **Insert `<ptr>` first**: New `<ptr>` should be the first child inside `<ref>`
7. **Handle empty results**: `found: []` is success, not an error
8. **Set Referer header**: API requires `Referer: CBRD@dila.edu.tw`
9. **Respect system proxy settings**: HTTP client should honor OS proxy configuration

---

## Performance Targets

From Success Criteria:
- **SC-001**: Complete workflow in <30 seconds
- **SC-003**: API response within configured timeout window (default 3 seconds per attempt, up to 3 attempts plus backoff)
- **SC-005**: Zero data loss during replacement

**Configuration**:
- Per-attempt timeout: 3000ms (3 seconds) by default
- Connection timeout: matches per-attempt timeout
- Read timeout: matches per-attempt timeout
- Timeout retries: 3 attempts with 250ms/500ms backoff

---

## Next Steps

1. Read [data-model.md](data-model.md) for complete entity definitions
2. Review [contracts/cbrd-api.yaml](contracts/cbrd-api.yaml) for API contract
3. Study existing DAMA plugin patterns in `DAMAWorkspaceAccessPluginExtension.java`
4. Implement domain layer first (TDD approach)
5. Follow task order in `tasks.md` (to be generated by `/speckit.tasks`)

---

## References

- [spec.md](spec.md) - Feature specification with Event Storming and Problem Frames
- [plan.md](plan.md) - Implementation plan with architecture and constitution check
- [research.md](research.md) - Research findings (CBRD API, XML parsing, Oxygen SDK)
- [data-model.md](data-model.md) - Complete data model with validation rules
- [contracts/cbrd-api.yaml](contracts/cbrd-api.yaml) - CBRD API OpenAPI specification
- [DAMAWorkspaceAccessPluginExtension.java](../../Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java) - Existing plugin code

---

## Questions?

If you encounter issues or have questions:
1. Check the constitution at `.specify/memory/constitution.md`
2. Review existing plugin code for patterns
3. Consult research.md for CBRD API details
4. Refer to data-model.md for validation rules
