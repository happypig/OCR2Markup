# US2 Acceptance Test Matrix: Replace Old Links with New Links

Source: `specs/001-ref-to-link-action/spec.md` (User Story 2 acceptance scenarios).

## Preconditions

- US1 has produced a valid CBETA URL in resultArea.
- Replace button is visible.

## Test Cases

### US2-1: Replace removes all existing `<ptr>` and inserts one new `<ptr>`

**Given** the resultArea contains a valid CBETA link and Replace is visible  
**When** the user clicks `[Replace]`  
**Then** all existing `<ptr>` elements inside the selected `<ref>` are removed  
**And** exactly one new `<ptr href="..."/>` is inserted using the generated URL  
**And** the new `<ptr>` is inserted as the first child inside `<ref>`  
**And** all non-`<ptr>` children and text in `<ref>` are preserved.

### US2-2: Replace sets `checked="2"`

**Given** the replacement is about to occur  
**When** Replace is clicked  
**Then** the `<ref>` element has `checked="2"` after replacement (added or updated).

### US2-3: UI updates after success

**Given** replacement completed successfully  
**When** the operation finishes  
**Then** Replace is hidden  
**And** a success message is displayed in infoArea.
