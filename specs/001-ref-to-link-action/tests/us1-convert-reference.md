# US1 Acceptance Test Matrix: Convert Reference to Link

Source: `specs/001-ref-to-link-action/spec.md` (User Story 1 acceptance scenarios).

## Preconditions

- Oxygen XML Editor in Text mode.
- DAMA plugin view open.
- A TEI/XML document containing a complete `<ref>...</ref>` element is open.
- Network access to the configured CBRD endpoint (or a test double).

## Test Cases

### US1-1: Invoke action displays selection and prepares conversion

**Given** a user has selected a full `<ref>` element containing Tripitaka reference components  
**When** they click `[Actions] -> [<ref> to link]`  
**Then** infoArea displays the selected action and the selected `<ref>` element  
**And** the system extracts reference components for processing  
**And** the Convert button is visible (Replace hidden).

### US1-2: Convert calls CBRD API and displays generated link

**Given** the infoArea shows the selected action and the selected `<ref>` element  
**When** the user clicks `[Convert]`  
**Then** the system calls the CBRD API using extracted/transformed components  
**And** resultArea displays the generated CBETA URL  
**And** Replace button becomes visible.

### US1-3: Replace button appears only after success

**Given** the resultArea displays a converted link  
**When** the user reviews the link  
**Then** Replace is visible for applying changes  
**And** Convert remains available for manual retry (per FR-013).

