# US3 Acceptance Test Matrix: Handle Invalid or Missing References

Source: `specs/001-ref-to-link-action/spec.md` (User Story 3 acceptance scenarios).

## Preconditions

- Oxygen XML Editor in Text mode.
- DAMA plugin view open.

## Test Cases

### US3-1: Invalid selection (not XML or not `<ref>`)

**Given** a user selects text that is not valid XML  
**When** they click `[Actions] -> [<ref> to link]`  
**Then** resultArea shows an invalid XML error  
**And** Convert is not shown.

**Given** a user selects a valid XML element that is not `<ref>`  
**When** they click `[Actions] -> [<ref> to link]`  
**Then** resultArea shows a not-`<ref>` error  
**And** Convert is not shown.

### US3-2: Missing required components

**Given** a user selects a `<ref>` missing `<canon>` or `<v>` or `<p>`  
**When** they click `[Convert]`  
**Then** resultArea shows the corresponding missing component error  
**And** Replace remains hidden  
**And** Convert remains available for manual retry.

### US3-3: API failures and no results

**Given** a valid `<ref>` selection  
**When** the CBRD API times out after retries or returns a non-200 response  
**Then** resultArea shows the matching API error message  
**And** Replace remains hidden  
**And** Convert remains available for manual retry.

**Given** a valid `<ref>` selection  
**When** the CBRD API returns `success: true` with `found: []`  
**Then** resultArea shows a no-results message  
**And** Replace remains hidden  
**And** Convert remains available for manual retry.
