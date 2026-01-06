# Feature Specification: Ref to Link Action

**Feature Branch**: `001-ref-to-link-action`
**Created**: 2026-01-05
**Status**: Draft
**Input**: User description: "Add a new action '<ref> to link' under Actions menu of DILA AI Markup Assistant (DAMA) to convert Tripitaka references inside <ref> elements to CBETA online links"

---

## Clarifications

### Session 2026-01-05

- Q: How should the system handle CBETA API failures (unreachable, slow, errors)? → A: Retry timeouts automatically, then show error with option to retry manually via Convert button
- Q: Where should the CBETA API endpoint URL be configured? → A: Configurable in DAMA plugin preferences (like existing API key settings)
- Q: Is the `<c>` (column) element required for API conversion? → A: Optional; convert with canon, volume, page only; column adds precision if present

---

## Event Storming Analysis

### Domain Events (Orange)

| Event | Description |
|-------|-------------|
| **ReferenceSelected** | User selects `<ref>` element in Oxygen editor |
| **RefToLinkActionInvoked** | User clicks [Actions]-[<ref> to link] menu |
| **SelectionDisplayed** | Selected `<ref>` shown in infoArea |
| **ReferenceComponentsExtracted** | Canon, volume, page, column parsed from `<ref>` |
| **ConvertButtonClicked** | User retries conversion (optional) |
| **CBETAAPICallInitiated** | System calls CBETA API |
| **CBETAAPIResponseReceived** | API returns link(s) or error |
| **LinkDisplayedInResult** | Generated link shown in resultArea |
| **ReplaceButtonDisplayed** | Replace button becomes visible |
| **ReplaceButtonClicked** | User confirms replacement |
| **PtrElementsReplaced** | Old `<ptr>` elements removed, new one inserted |
| **CheckedAttributeUpdated** | `checked` attribute set to "2" |
| **ReplacementCompleted** | Success message shown, Replace button hidden |
| **ConversionFailed** | Error occurred (invalid selection, API error, etc.) |

### Commands (Blue)

| Command | Trigger | Result |
|---------|---------|--------|
| **SelectRefToLinkAction** | Menu click | Action invoked, selection displayed |
| **ExtractReferenceComponents** | Action invoked | Components parsed from XML |
| **ConvertReferenceToLink** | Action invoked (auto) or Convert button | CBETA API called |
| **ReplaceLinks** | Replace button | Document updated |

### Actors (Yellow)

- **Markup Editor**: Human user editing Buddhist text markup in Oxygen XML Editor

### Aggregates

- **ReferenceConversionSession**: Tracks conversion workflow state (selected `<ref>`, extracted components, generated link, operation status)

### Policies (Lilac)

| Policy | Enforcement |
|--------|-------------|
| Selection must be valid `<ref>` element | Validate XML structure before processing |
| Reference must contain required components | Check for `<canon>`, `<v>`, `<w>`, `<p>` before API call; `<c>` is optional |
| Call API with raw `<ref>` | No local normalization; pass selected XML as-is |
| API client uses system proxy + headers | Honor OS proxy settings and send User-Agent/Accept-Charset |
| CBETA API must return valid URL | Cannot enable Replace without successful response |
| Replace updates checked attribute | Always set `checked="2"` on success |
| Preserve non-ptr content | Only replace `<ptr>` elements, keep reference text |

### External Systems (Pink)

| System | Role |
|--------|------|
| **Oxygen XML Editor** | Source of selection, target for replacement |
| **CBETA API** | External service converting Tripitaka references to URLs |

---

## Problem Frames Analysis

### Problem Domain (Real-world entities)

| Entity | Description |
|--------|-------------|
| **Tripitaka Reference** | Citation to Buddhist canon text (canon, volume, page, column) |
| **CBETA Online Resource** | Web page displaying Buddhist text at specific location |
| **TEI XML Document** | Structured markup document being edited |
| **`<ref>` Element** | XML container holding citation information and links |
| **`<ptr>` Element** | XML element with hyperlink to online resource |
| **Reference Components** | `<canon>`, `<v>`, `<w>`, `<p>`, `<c>`, `<l>` elements within `<ref>` |
| **Verification Status** | `checked` attribute (0=unchecked, 2=linkRegened) |

### Solution Domain (Technical components)

| Component | Description |
|-----------|-------------|
| **DAMA Plugin** | DILA AI Markup Assistant in Oxygen |
| **Actions Menu** | UI menu with available operations |
| **infoArea** | Displays selection/context |
| **resultArea** | Displays conversion results |
| **Replace Button** | Applies changes to document |
| **CBETA API Client** | HTTP client for external API |
| **Reference Parser** | Extracts components from `<ref>` XML |
| **Link Generator** | Constructs `<ptr>` element with URL |

### Shared Phenomena

| Phenomenon | Direction | Description |
|------------|-----------|-------------|
| Selection | PD → SD | User highlights `<ref>`, Oxygen provides selection |
| Display | SD → PD | infoArea shows text to user |
| Reference Text | PD → SD | Parser extracts XML elements |
| API Request | SD → External | CBETA API receives query |
| API Response | External → SD | HTTP response with URL |
| Generated Link | SD → PD | resultArea displays URL |
| Replacement | SD → PD | Oxygen modifies document |

### Problem Type

**Transformation Problem**: Convert `<ref>` element with Tripitaka reference text to updated `<ref>` with correct `<ptr href="..."/>` and `checked="2"`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Convert Reference to Link (Priority: P1)

A markup editor working with Buddhist texts needs to verify and update links within `<ref>` elements. They select a `<ref>` element containing Tripitaka reference information (canon, volume, page, column and line) and use the "<ref> to link" action to automatically generate the correct CBETA online URL based on the reference text.

**Why this priority**: This is the core functionality - without the ability to convert references to links, the entire feature has no value. This enables editors to efficiently validate and correct reference links in Buddhist text markup.

**Independent Test**: Can be fully tested by selecting a `<ref>` element in Oxygen XML Editor, invoking the action, clicking Convert, and verifying the CBETA API returns a valid link that matches the Tripitaka reference.

**Acceptance Scenarios**:

1. **Given** a user has selected a `<ref>` element containing Tripitaka reference text like `<canon>続蔵</canon><v>一・一六</v>、<p>二四九</p><c>左上</c>`, **When** they click [Actions]-[<ref> to link], **Then** the infoArea displays the selected actions and the selected `<ref>` element and the system extracts the reference components for processing.

2. **Given** the infoArea shows the selected actions and the selected `<ref>` element, **When** the action is invoked, **Then** the system calls the CBETA API with the selected `<ref>` XML (no local normalization) and displays the generated link in the resultArea.

3. **Given** the resultArea displays the converted link, **When** the user reviews and confirms the link is correct, **Then** the [Replace] button becomes visible for the user to apply changes.

---

### User Story 2 - Replace Old Links with New Links (Priority: P2)

After the CBETA API returns the correct link for a Tripitaka reference, the editor wants to replace the existing `<ptr>` elements inside the `<ref>` with the newly generated link and mark the reference as verified.

**Why this priority**: This completes the workflow by allowing users to apply the converted links back to the document. Without this, users would need to manually copy/paste the results.

**Independent Test**: Can be tested by having a converted link in resultArea and clicking [Replace] to verify the `<ref>` element is updated with new `<ptr href="..."/>` and the `checked` attribute changes to "2".

**Acceptance Scenarios**:

1. **Given** the resultArea contains a valid CBETA link and the [Replace] button is visible, **When** the user clicks [Replace], **Then** the old `<ptr>` elements inside the selected `<ref>` are replaced with a new `<ptr>` element containing the CBETA API-generated URL inserted as the first child of `<ref>`.

2. **Given** the replacement is about to occur, **When** the [Replace] button is clicked, **Then** the `checked` attribute of the `<ref>` element is updated from its current value to "2" to indicate verification.

3. **Given** the replacement has completed successfully, **When** the operation finishes, **Then** the [Replace] button is hidden and a success message is displayed in the infoArea.

---

### User Story 3 - Handle Invalid or Missing References (Priority: P3)

When the selected `<ref>` element contains incomplete reference information or the CBETA API cannot find a matching link, the editor receives clear feedback about what went wrong.

**Why this priority**: Error handling ensures users understand failures and can take corrective action. This improves user experience but is not core functionality.

**Independent Test**: Can be tested by selecting a `<ref>` with missing or invalid reference components and verifying appropriate error messages appear.

**Acceptance Scenarios**:

1. **Given** a user selects text that is not a valid `<ref>` element, **When** they click [Actions]-[<ref> to link], **Then** an error message is displayed in the resultArea explaining the selection must be a valid `<ref>` element.

2. **Given** a `<ref>` element is selected but lacks required reference components (canon, volume, page), **When** the action is invoked, **Then** the resultArea displays an error message specifying which components are missing.

3. **Given** the CBETA API returns no results or an error, **When** the API response is received, **Then** the resultArea displays an informative message and the [Replace] button remains hidden (user can retry via [Convert]).

---

### Edge Cases

- **Incomplete selection**: User selects only part of a `<ref>` element - system should detect incomplete XML and prompt user to select the full element
- **Multiple `<ptr>` elements**: `<ref>` contains multiple existing `<ptr>` elements - all should be replaced with the single new link
- **Missing checked attribute**: `<ref>` has no `checked` attribute - should add `checked="2"` on replacement
- **Already verified**: `<ref>` has `checked="2"` already - re-processing should be allowed, attribute remains "2"
- **No reference text**: `<ref>` contains only `<ptr>` elements, no Tripitaka reference - error message should explain what's needed
- **Canon format variations**: Different canon abbreviations (T, X, 大正蔵, 卍續藏) in various formats - API handles variations; no local normalization

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST add a new menu item "<ref> to link" under the [Actions] menu in DILA AI Markup Assistant
- **FR-002**: System MUST display the selected `<ref>` element in the infoArea when the action is invoked
- **FR-003**: System MUST extract Tripitaka reference components from within the `<ref>` element:
  - `<canon>` - the canon/collection name (e.g., "続蔵", "大正蔵") *(required)*
  - `<v>` - volume number *(required)*
  - `<w>` - work number *(optional)*
  - `<p>` - page number *(optional)*
  - `<c>` - column indicator (left/right, upper/lower) *(optional, adds precision if present)*
  - `<l>` - line indicator *(optional, adds precision if present)*
- **FR-004**: System MUST trigger the CBETA API call immediately when the action is invoked, and provide a [Convert] button for retry
- **FR-005**: System MUST call the CBETA API with the selected `<ref>` XML as-is (no local normalization) to obtain the corresponding online link
- **FR-006**: System MUST display the CBETA API-generated link in the resultArea
- **FR-007**: System MUST show the [Replace] button after successful link generation
- **FR-008**: System MUST replace existing `<ptr>` elements inside the `<ref>` with a new `<ptr>` element containing the CBETA-generated URL, inserted as the first child of `<ref>`, when [Replace] is clicked
- **FR-009**: System MUST update the `checked` attribute of the `<ref>` element to "2" upon successful replacement
- **FR-010**: System MUST hide the [Replace] button after replacement is complete, consistent with other actions
- **FR-011**: System MUST validate that the selected text is a properly formed `<ref>` element before processing
- **FR-012**: System MUST display appropriate error messages when reference conversion fails
- **FR-013**: System MUST retry CBRD API calls on timeouts (up to 3 attempts with exponential backoff) and then show the timeout error; user can retry manually by clicking [Convert] again
- **FR-014**: System MUST allow CBETA API endpoint URL to be configured in DAMA plugin preferences
- **FR-015**: System MUST honor system proxy settings when calling the CBETA API and set a User-Agent and UTF-8 Accept-Charset header

### Key Entities

- **Reference Element (`<ref>`)**: XML element containing Tripitaka citation with attributes `xml:id` and `checked`, containing `<ptr>` link elements and reference text elements
- **Pointer Element (`<ptr>`)**: XML element with `href` attribute linking to CBETA online resources
- **Tripitaka Reference Components**: Elements (`<canon>`, `<v>`, `<w>`, `<p>`, `<c>`, `<l>`) identifying a location in Buddhist canon texts
- **CBETA Link**: URL pointing to CBETA online database (e.g., `https://cbetaonline.dila.edu.tw/X01n0008_p0261b12`)

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can convert a Tripitaka reference to a CBETA link in under 30 seconds from selection to replacement
- **SC-002**: 95% of valid Tripitaka references successfully convert to correct CBETA links
- **SC-003**: Users receive clear feedback within the configured timeout window (default 10 sec per attempt, up to 3 attempts plus backoff) after clicking [Convert]
- **SC-004**: The action follows the same UI pattern as existing actions (AI Markup, Tag Removal), requiring no additional user training
- **SC-005**: Zero data loss - original `<ref>` element content outside of `<ptr>` elements is preserved during replacement

---

## Assumptions

- The CBETA API is available and provides an endpoint to convert Tripitaka references to URLs
- The Tripitaka reference format within `<ref>` elements follows a consistent structure with `<canon>`, `<v>`, `<w>`, `<p>`, `<c>`, and `<l>` elements
- Users have network connectivity to reach the CBETA API
- The `checked` attribute convention (0 = unchecked, 2 = linkRegened) is an established workflow in this markup system
- The CBETA API response format includes a URL or sufficient information to construct a CBETA online link
