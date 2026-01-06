# Research Findings: Ref to Link Action

**Branch**: `001-ref-to-link-action` | **Date**: 2026-01-05 | **Spec**: [spec.md](spec.md)

---

## Executive Summary

This document consolidates research findings from three key areas needed for implementing the Ref to Link feature:
1. **CBETA API Specification** - Understanding how to convert Tripitaka references to CBETA URLs
2. **XML Parsing Approaches** - Best practices for extracting reference components from `<ref>` elements
3. **Oxygen SDK Integration** - Established patterns from the existing DAMA plugin

---

## Research Area 1: CBETA API Specification

### Decision

**Use CBRD (CBETA Reference Detection) REST API at `https://cbss.dila.edu.tw/dev/cbrd/link`**

### Rationale

CBETA provides a formal REST API endpoint specifically designed for converting Tripitaka references to CBETA online URLs. This API accepts XML-formatted reference components and returns validated CBETA links, eliminating the need for manual URL construction and ensuring correctness.

### Key Findings

**CBRD API Endpoint:**
```
https://cbss.dila.edu.tw/dev/cbrd/link
```

**Request Method:** HTTP GET

**Query Parameter:** `q` (XML-formatted reference)

**Example Request:**
```
GET https://cbss.dila.edu.tw/dev/cbrd/link?q=<ref><canon>T</canon><v>25</v>.<w>1514</w></ref>
```

**Example Response:**
```json
{
  "success": true,
  "found": ["https://cbetaonline.dila.edu.tw/T1514"]
}
```

**Required HTTP Headers:**
- `Referer: CBRD@dila.edu.tw` (REQUIRED - API authentication/tracking)

### API Contract

**Request Format:**

| Component | Description | Example | Required |
|-----------|-------------|---------|----------|
| Query Parameter `q` | XML reference element | `<ref><canon>T</canon><v>25</v>.<w>1514</w></ref>` | Yes |
| HTTP Header `Referer` | Client identifier | `CBRD@dila.edu.tw` | Yes |

**XML Reference Structure in Query:**
```xml
<ref>
  <canon>T</canon>       <!-- Canon code (T, X, etc.) - REQUIRED -->
  <v>25</v>              <!-- Volume number - REQUIRED -->
  <w>1514</w>            <!-- Work number - OPTIONAL -->
  <p>917</p>             <!-- Page number - OPTIONAL -->
  <c>a</c>               <!-- Column (a/b/c) - OPTIONAL -->
  <l>01</l>              <!-- Line - OPTIONAL -->
</ref>
```

**Response Format:**

```json
{
  "success": true,           // Boolean indicating API call success
  "found": [                 // Array of CBETA URLs (may contain multiple results)
    "https://cbetaonline.dila.edu.tw/T1514"
  ]
}
```

**Success Response:**
- `success: true` - API call succeeded
- `found: [...]` - Array of CBETA URLs (typically one result)

**Error Response (Hypothetical - needs validation):**
```json
{
  "success": false,
  "error": "Error message"
}
```

### XML Component Transformations (Pre-API)

Before calling the API, the plugin must transform `<ref>` element components from the document format to API format:

| Document Format | API Format | Transformation Required |
|----------------|------------|------------------------|
| `<canon>大正蔵</canon>` | `<canon>T</canon>` | Map canon name to code: 大正蔵 → T |
| `<canon>続蔵</canon>` | `<canon>X</canon>` | Map canon name to code: 続蔵 → X |
| `<v>二四</v>` | `<v>24</v>` | Convert CJK numerals to Arabic: 二四 → 24 |
| `<v>一・一六</v>` | `<v>1.16</v>` | Convert CJK numerals with punctuation |
| `<p>九一七</p>` | `<p>917</p>` | Convert CJK numerals to Arabic |
| `<c>上</c>` | `<c>a</c>` | Map position: 上→a, 中→b, 下→c |
| `<c>左上</c>` | `<c>a</c>` | Map position: left-upper → a |
| `<l>〇一</l>` | `<l>01</l>` | Convert CJK numerals |

**Canon Code Mapping:**
- `大正蔵` (Taishō Tripiṭaka) → `T`
- `続蔵` (Zoku Zōkyō) → `X`
- Additional canons may exist and need to be added to mapping table

**Column Position Mapping:**
- `上` (upper) → `a`
- `中` (middle) → `b`
- `下` (lower) → `c`
- `左上` (upper left) → `a`
- `右上` (upper right) → may vary (needs validation)

### Required Components

Based on API example and spec.md FR-003:
- **Required**: `<canon>`, `<v>` (volume)
- **Recommended for accuracy**: `<w>` (work number)
- **Optional (adds precision)**: `<p>` (page), `<c>` (column), `<l>` (line)

### Configuration Requirements

Based on FR-014, the plugin should allow configuration of:
- **API Base URL**: `https://cbss.dila.edu.tw/dev/cbrd/link` (configurable for different environments)
- **Referer Header Value**: `CBRD@dila.edu.tw` (configurable if needed)
- **Timeout**: API call timeout in milliseconds (default 10000ms per attempt)
- **Retry Policy**: Automatic retries on timeout (default: 3 attempts with exponential backoff), manual retry via Convert button per FR-013

### Implementation Approach

**Create an API Client Service:**

```java
public class CBRDAPIClient {
    private String apiUrl = "https://cbss.dila.edu.tw/dev/cbrd/link";
    private String refererHeader = "CBRD@dila.edu.tw";
    private int timeout = 10000; // 10 seconds per attempt

    /**
     * Call CBRD API to convert reference to CBETA link
     *
     * @param components Transformed reference components
     * @return CBETA URL or null if not found
     * @throws CBRDAPIException if API call fails
     */
    public String convertToLink(TripitakaComponents components)
            throws CBRDAPIException {
        // Build XML query: <ref><canon>T</canon><v>25</v>.<w>1514</w></ref>
        String xmlQuery = buildXmlQuery(components);

        // URL-encode query parameter
        String encodedQuery = URLEncoder.encode(xmlQuery, "UTF-8");

        // Build full URL
        String fullUrl = apiUrl + "?q=" + encodedQuery;

        // Make HTTP GET request with Referer header
        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Referer", refererHeader);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        // Parse JSON response
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String jsonResponse = readResponse(conn);
            CBRDResponse response = parseJson(jsonResponse);

            if (response.isSuccess() && response.getFound().length > 0) {
                return response.getFound()[0]; // Return first URL
            } else {
                return null; // No results found
            }
        } else {
            throw new CBRDAPIException("API returned " + responseCode);
        }
    }

    private String buildXmlQuery(TripitakaComponents components) {
        StringBuilder xml = new StringBuilder("<ref>");
        xml.append("<canon>").append(components.getCanon()).append("</canon>");
        xml.append("<v>").append(components.getVolume()).append("</v>");
        if (components.hasWork()) {
            xml.append(".<w>").append(components.getWork()).append("</w>");
        }
        // Add page, column, line if present...
        xml.append("</ref>");
        return xml.toString();
    }
}
```

**Required Transformation Services:**
1. **Canon Mapper**: Map canon names to codes (大正蔵 → T, 続蔵 → X)
2. **Numeral Converter**: Parse Chinese/Japanese numerals to Arabic numerals
3. **Column Converter**: Convert column positions (上/中/下 → a/b/c)
4. **XML Builder**: Construct API query XML from components

### Authentication

**Referer Header Required:** `CBRD@dila.edu.tw`

This appears to be a simple authentication/tracking mechanism. The API requires this specific Referer header value to accept requests.

### Error Handling

**API-level errors:**
1. **Connection timeout**: Network unreachable, slow response (exceeds configured timeout window)
2. **HTTP errors**: 4xx (bad request), 5xx (server error)
3. **Malformed response**: Invalid JSON, unexpected structure
4. **No results**: `success: true` but `found: []` (empty array)
5. **API error**: `success: false` with error message

**Application-level errors:**
1. **Validation errors**: Missing required components before API call
2. **Transformation errors**: Unable to map canon/column/numerals
3. **URL encoding errors**: Invalid characters in XML query

**Error Display (per FR-013):**
- Show error immediately in resultArea
- Retry automatically on timeout (up to 3 attempts with exponential backoff)
- User can click [Convert] again to manually retry after errors

### Performance Requirements

From SC-003:
- **API call response**: Within configured timeout window (default 10 seconds per attempt, up to 3 attempts plus backoff)
- **Timeout configuration**: Set to 10000ms per attempt by default
- **Async execution**: Call API in background thread to avoid blocking UI (Principle VIII)

### Alternatives Considered

1. **Direct URL construction** - Rejected: API provides validation and handles edge cases
2. **Screen scraping CBETA website** - Rejected: unreliable, fragile, violates best practices
3. **Using external conversion service** - Rejected: CBETA provides official API
4. **CBRD REST API** - ✅ **CHOSEN**: Official, reliable, maintained by CBETA/DILA

---

## Research Area 2: XML Parsing Approaches

### Decision

**Use Java DOM (`javax.xml.parsers.DocumentBuilder`) to parse `<ref>` elements.**

### Rationale

The DAMA plugin uses Oxygen's Text mode API, which returns selected text as a String. We need to parse this String as XML to extract child elements like `<canon>`, `<v>`, `<p>`, etc. DOM is the best choice because:
1. Simple, familiar API for extracting specific child elements
2. Perfect for small XML fragments (a single `<ref>` element)
3. Standard Java library (no additional dependencies)
4. Provides full random access to navigate parent/child relationships
5. Existing DAMA plugin already uses DOM for translation file parsing

### Comparison of XML Parsing Approaches

#### DOM (Document Object Model) ✅ RECOMMENDED

**Pros:**
- Full random access to any element
- Easy navigation (parent, child, sibling relationships)
- Simple API for extracting specific child elements
- Standard Java library
- Ideal for small XML fragments

**Cons:**
- Higher memory usage for large documents (not relevant for `<ref>` fragments)

**Example from existing code ([TranslationConsistencyTest.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\test\java\com\dila\dama\plugin\test\TranslationConsistencyTest.java:75-88)):**
```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
Document doc = builder.parse(new File(TRANSLATION_FILE));

NodeList keyNodes = doc.getElementsByTagName("key");
for (int i = 0; i < keyNodes.getLength(); i++) {
    Element keyElement = (Element) keyNodes.item(i);
    String keyValue = keyElement.getAttribute("value");
}
```

#### SAX (Simple API for XML) ❌ NOT RECOMMENDED

**Pros:**
- Low memory footprint
- Fast for large documents

**Cons:**
- Forward-only parsing (can't revisit elements)
- Complex handler callbacks
- No random access
- Overkill for small fragments

**Example found in [SAXParserHandler.java](d:\project\OCR2Markup\oxygenSDKSamples\oxygen-sample-plugins\oxygen-sample-plugin-broken-links-checker\src\main\java\dila\brokenlinkschecker\impl\SAXParserHandler.java)** - Shows event-driven parsing but unnecessary complexity for our use case.

#### StAX (Streaming API for XML) ❌ NOT RECOMMENDED

**Pros:**
- Low memory usage
- Pull-based (more intuitive than SAX)

**Cons:**
- Still streaming (no random access)
- More complex than DOM for simple extraction
- Overkill for small fragments

### Implementation: Two-Phase Approach

**Phase 1: Selection Extraction (Oxygen API)**

```java
// Get selected text from Oxygen editor (Text mode)
WSTextEditorPage textPage = getTextEditorPage();
String selectedXml = textPage.getSelectedText();

// Validate selection contains a <ref> element
if (!selectedXml.trim().startsWith("<ref")) {
    throw new InvalidSelectionException("Selection must be a <ref> element");
}
```

**Phase 2: XML Parsing (Java DOM)**

```java
// Parse the selected XML string with DOM
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setNamespaceAware(true); // Important for TEI XML
factory.setValidating(false);
factory.setFeature(
    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
    false);
DocumentBuilder builder = factory.newDocumentBuilder();

// Wrap in root element for valid XML parsing
String xmlToParse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"
                  + selectedXml + "</root>";

Document doc = builder.parse(new InputSource(new StringReader(xmlToParse)));
Element refElement = (Element) doc.getElementsByTagName("ref").item(0);

// Extract child elements
String canon = getChildElementText(refElement, "canon");
String volume = getChildElementText(refElement, "v");
String page = getChildElementText(refElement, "p");
String column = getChildElementText(refElement, "c"); // Optional
```

**Helper Method:**

```java
/**
 * Safely extract text content from a child element.
 * Returns null if element not found.
 */
private String getChildElementText(Element parent, String childTagName) {
    NodeList children = parent.getElementsByTagName(childTagName);
    if (children.getLength() > 0) {
        Element child = (Element) children.item(0);
        String text = child.getTextContent();
        return (text != null) ? text.trim() : null;
    }
    return null;
}
```

### Domain Service: ReferenceParser

**File**: `src/main/java/com/dila/dama/plugin/domain/service/ReferenceParser.java`

```java
package com.dila.dama.plugin.domain.service;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.io.StringReader;

/**
 * Domain service for parsing Tripitaka reference components from <ref> elements.
 * Uses Java DOM to extract child elements like <canon>, <v>, <p>, <c>.
 */
public class ReferenceParser {

    private final DocumentBuilder documentBuilder;

    public ReferenceParser() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // TEI XML uses namespaces
        factory.setValidating(false);
        factory.setFeature(
            "http://apache.org/xml/features/nonvalidating/load-external-dtd",
            false);
        // XXE Protection
        factory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl",
            true);
        factory.setFeature(
            "http://xml.org/sax/features/external-general-entities",
            false);
        factory.setFeature(
            "http://xml.org/sax/features/external-parameter-entities",
            false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        this.documentBuilder = factory.newDocumentBuilder();
    }

    /**
     * Parse a <ref> element and extract Tripitaka reference components.
     *
     * @param refXml The XML string containing a <ref> element
     * @return TripitakaComponents value object with extracted values
     * @throws InvalidReferenceException if parsing fails or required elements missing
     */
    public TripitakaComponents parseReference(String refXml)
            throws InvalidReferenceException {

        // Defensive: Limit input size (prevent DoS)
        if (refXml.length() > 10_000) { // 10KB max
            throw new InvalidReferenceException("Selected text too large");
        }

        try {
            // Wrap in root element for valid XML parsing
            String wrappedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"
                              + refXml + "</root>";

            Document doc = documentBuilder.parse(
                new InputSource(new StringReader(wrappedXml))
            );

            // Get the <ref> element
            NodeList refNodes = doc.getElementsByTagName("ref");
            if (refNodes.getLength() == 0) {
                throw new InvalidReferenceException(
                    "No <ref> element found in selection");
            }

            Element refElement = (Element) refNodes.item(0);

            // Extract required components (defensive: handle missing elements)
            String canon = getChildElementText(refElement, "canon");
            String volume = getChildElementText(refElement, "v");
            String work = getChildElementText(refElement, "w"); // Optional
            String page = getChildElementText(refElement, "p");

            // Extract optional components
            String column = getChildElementText(refElement, "c");
            String line = getChildElementText(refElement, "l");

            // Validate required components
            if (canon == null || canon.trim().isEmpty()) {
                throw new InvalidReferenceException(
                    "Missing required <canon> element");
            }
            if (volume == null || volume.trim().isEmpty()) {
                throw new InvalidReferenceException(
                    "Missing required <v> (volume) element");
            }
            if (page == null || page.trim().isEmpty()) {
                throw new InvalidReferenceException(
                    "Missing required <p> (page) element");
            }

            // Build value object
            return new TripitakaComponents(canon, volume, work, page, column, line);

        } catch (Exception e) {
            throw new InvalidReferenceException(
                "Failed to parse reference: " + e.getMessage(), e);
        }
    }

    private String getChildElementText(Element parent, String childTagName) {
        NodeList children = parent.getElementsByTagName(childTagName);
        if (children.getLength() > 0) {
            Element child = (Element) children.item(0);
            String text = child.getTextContent();
            return (text != null) ? text.trim() : null;
        }
        return null;
    }
}
```

### Value Object: TripitakaComponents

**File**: `src/main/java/com/dila/dama/plugin/domain/model/TripitakaComponents.java`

```java
package com.dila.dama.plugin.domain.model;

import java.util.Objects;

/**
 * Value object representing Tripitaka reference components.
 * Immutable and validated upon construction.
 */
public final class TripitakaComponents {

    private final String canon;   // Required: e.g., "続蔵", "大正蔵"
    private final String volume;  // Required: e.g., "一・一六"
    private final String work;    // Optional: work number
    private final String page;    // Required: e.g., "二四九"
    private final String column;  // Optional: e.g., "左上" (left-upper)
    private final String line;    // Optional: line indicator

    public TripitakaComponents(String canon, String volume, String work,
                               String page, String column, String line) {
        // Defensive validation
        if (canon == null || canon.trim().isEmpty()) {
            throw new IllegalArgumentException("Canon cannot be null or empty");
        }
        if (volume == null || volume.trim().isEmpty()) {
            throw new IllegalArgumentException("Volume cannot be null or empty");
        }
        if (page == null || page.trim().isEmpty()) {
            throw new IllegalArgumentException("Page cannot be null or empty");
        }

        this.canon = canon.trim();
        this.volume = volume.trim();
        this.work = (work != null) ? work.trim() : null;
        this.page = page.trim();
        this.column = (column != null) ? column.trim() : null;
        this.line = (line != null) ? line.trim() : null;
    }

    // Getters
    public String getCanon() { return canon; }
    public String getVolume() { return volume; }
    public String getWork() { return work; }
    public String getPage() { return page; }
    public String getColumn() { return column; }
    public String getLine() { return line; }

    public boolean hasColumn() { return column != null && !column.isEmpty(); }
    public boolean hasLine() { return line != null && !line.isEmpty(); }
    public boolean hasWork() { return work != null && !work.isEmpty(); }

    @Override
    public String toString() {
        return String.format("TripitakaComponents{canon='%s', volume='%s', " +
                           "page='%s', work='%s', column='%s', line='%s'}",
                           canon, volume, page, work, column, line);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripitakaComponents that = (TripitakaComponents) o;
        return canon.equals(that.canon) &&
               volume.equals(that.volume) &&
               page.equals(that.page) &&
               Objects.equals(work, that.work) &&
               Objects.equals(column, that.column) &&
               Objects.equals(line, that.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canon, volume, work, page, column, line);
    }
}
```

### Best Practices: Defensive Programming

Following Principle VII from constitution:

**1. Validate Input Before Parsing**
```java
if (selectedXml == null || selectedXml.trim().isEmpty()) {
    throw new InvalidSelectionException("No text selected");
}

if (!selectedXml.trim().startsWith("<ref")) {
    throw new InvalidSelectionException("Selection must be a <ref> element");
}
```

**2. Handle Missing Elements Safely**
- Always check `NodeList.getLength()` before accessing items
- Return null for missing optional elements
- Throw descriptive exceptions for missing required elements

**3. Security: Disable External Entity Processing (XXE Protection)**
- Set all XXE prevention features on `DocumentBuilderFactory`
- Disable DTD loading
- Disable entity expansion

**4. Limit Input Size (Prevent DoS)**
- Maximum 10KB for a `<ref>` element
- Check size before parsing

**5. Reuse Parser Instance**
- Create one `DocumentBuilder` and reuse it
- Store as instance field in `ReferenceParser`

### Alternatives Considered

1. **SAX Parsing** - Rejected: Overkill for small fragments, complex callback handlers
2. **StAX Parsing** - Rejected: Unnecessary complexity for simple extraction
3. **Oxygen Author API (AuthorNode)** - Rejected: DAMA uses Text mode, not Author mode
4. **XPath Queries** - Considered: More powerful but unnecessary for simple child element access
5. **Java DOM** - ✅ **CHOSEN**: Simple, reliable, perfect for small fragments

---

## Research Area 3: Oxygen SDK Integration Patterns

### Decision

**Follow existing DAMA plugin patterns for action listeners, selection handling, async operations, and document modification.**

### Rationale

The DAMA plugin ([DAMAWorkspaceAccessPluginExtension.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java)) has established patterns for all aspects of plugin development. Following these patterns ensures:
1. Consistency with existing actions (AI Markup, Tag Removal)
2. Reuse of existing infrastructure (i18n, logging, error handling)
3. Maintainability and code familiarity for future developers

### Key Patterns Identified

#### Pattern 1: Action Listener (Inner Class)

**Location**: [DAMAWorkspaceAccessPluginExtension.java:337-404](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L337-L404)

```java
/**
 * AI Markup action listener
 */
private class AIMarkupActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        PluginLogger.info("[AIMarkupActionListener]AI Markup action triggered");

        // 1. Set operation context
        currentOperation = OperationType.AI_MARKUP;

        // 2. Clear previous results
        infoArea.setText(i18n("action.ai.markup.selected") + "\n\n");
        resultArea.setText("");
        hideAllButtons();

        // 3. Get selected text
        String selectedText = fetchSelectedText(resultArea);
        if (selectedText.isEmpty()) {
            return;
        }

        // 4. Display selection info
        infoArea.append(i18n("selected.text", selectedText) + "\n");

        // 5. Process in background (async)
        CompletableFuture.supplyAsync(() -> processAIMarkup(selectedText), executor)
            .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                setResultWithReplaceButton(result);
            }))
            .exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> {
                    setResultInformational(
                        i18n("error.processing", throwable.getMessage()));
                });
                return null;
            });
    }
}
```

**Key Points:**
1. **Inner class** within main plugin extension
2. **Operation context tracking** with `OperationType` enum
3. **UI state management**: clear results, hide buttons
4. **Selection validation** with `fetchSelectedText()`
5. **Async processing** with `CompletableFuture`
6. **Error handling** with `.exceptionally()`
7. **i18n** for all user-facing messages
8. **Logging** with context markers `[ClassName]`

#### Pattern 2: Selection Handling

**Location**: [DAMAWorkspaceAccessPluginExtension.java:638-668](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L638-L668)

```java
/**
 * Fetch selected text from current editor
 */
private String fetchSelectedText(JTextArea area) {
    try {
        WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
            PluginWorkspace.MAIN_EDITING_AREA);

        if (editorAccess != null) {
            WSEditorPage pageAccess = editorAccess.getCurrentPage();

            if (pageAccess instanceof WSTextEditorPage) {
                WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;
                String selectedText = textPage.getSelectedText();

                if (selectedText != null && !selectedText.trim().isEmpty()) {
                    PluginLogger.info("[fetchSelectedText]Text mode: " + selectedText);
                    return selectedText;
                } else {
                    area.append(i18n("no.text.selected") + "\n");
                    PluginLogger.warn("[fetchSelectedText]No text selected");
                    return "";
                }
            } else {
                area.append(i18n("not.text.mode") + "\n");
                PluginLogger.warn("[fetchSelectedText]Not Text mode");
                return "";
            }
        }

        area.append(i18n("no.editor.open") + "\n");
        PluginLogger.warn("[fetchSelectedText]No open editor");
        return "";

    } catch (Exception e) {
        area.append(i18n("error.fetching.text", e.getMessage()) + "\n");
        PluginLogger.error("[fetchSelectedText]Error: " + e.getMessage());
        return "";
    }
}
```

**Key Points:**
1. **Oxygen API chain**: `pluginWorkspaceAccess → getCurrentEditorAccess() → getCurrentPage() → WSTextEditorPage`
2. **Type checking**: Always check `instanceof WSTextEditorPage` before casting
3. **Comprehensive error handling**: Handle null editor, non-text mode, no selection
4. **User feedback**: Append error messages to provided text area
5. **Logging**: Log all states (success, warnings, errors)
6. **Return empty string**: On any error, return `""` (not null)

#### Pattern 3: Document Modification

**Location**: [DAMAWorkspaceAccessPluginExtension.java:495-541](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L495-L541)

```java
/**
 * Replace button action listener
 */
private class ReplaceButtonActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        PluginLogger.info("[ReplaceButtonActionListener]Replace triggered");

        String resultText = resultArea.getText();
        if (resultText == null || resultText.trim().isEmpty()) {
            return;
        }

        try {
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
                PluginWorkspace.MAIN_EDITING_AREA);

            if (editorAccess != null) {
                WSEditorPage pageAccess = editorAccess.getCurrentPage();

                if (pageAccess instanceof WSTextEditorPage) {
                    WSTextEditorPage textPage = (WSTextEditorPage) pageAccess;

                    // Get selection bounds
                    int selectionStart = textPage.getSelectionStart();
                    int selectionEnd = textPage.getSelectionEnd();

                    if (selectionStart != selectionEnd) {
                        // Two-step replacement
                        textPage.deleteSelection();

                        int currentOffset = textPage.getCaretOffset();
                        javax.swing.text.Document doc = textPage.getDocument();
                        doc.insertString(currentOffset, resultText, null);

                        PluginLogger.info("[ReplaceButtonActionListener]Replaced");

                        // Reset operation context
                        currentOperation = OperationType.NONE;

                        infoArea.append(i18n("text.replaced"));
                        hideAllButtons();
                    }
                }
            }
        } catch (Exception ex) {
            PluginLogger.error("[ReplaceButtonActionListener]Error: " + ex.getMessage());
            setResultInformational(i18n("error.replacing.text", ex.getMessage()));
        }
    }
}
```

**Key Points:**
1. **Two-step replacement**: Delete selection first, then insert at caret position
2. **Document API**: Use `textPage.getDocument()` and `doc.insertString()`
3. **Selection validation**: Verify `selectionStart != selectionEnd`
4. **State reset**: Reset `currentOperation` after success
5. **Error handling**: Separate try-catch for document operations

#### Pattern 4: Async Operations

**Location**: [DAMAWorkspaceAccessPluginExtension.java:54](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L54)

```java
// Shared executor for all background operations
private final ExecutorService executor = Executors.newFixedThreadPool(2);

// Usage pattern
CompletableFuture.supplyAsync(() -> processOperation(input), executor)
    .thenAccept(result -> SwingUtilities.invokeLater(() -> {
        updateUI(result);
    }))
    .exceptionally(throwable -> {
        SwingUtilities.invokeLater(() -> {
            displayError(throwable.getMessage());
        });
        return null;
    });
```

**Key Points:**
1. **Shared executor**: Fixed thread pool (size 2) for all async operations
2. **Supply async**: Use `CompletableFuture.supplyAsync(() -> ..., executor)`
3. **UI updates in EDT**: Wrap ALL UI updates in `SwingUtilities.invokeLater()`
4. **Error handling**: Use `.exceptionally()` to catch and handle errors
5. **Return null in exceptionally**: Always return null in the exceptionally handler
6. **Executor lifecycle**: Shut down executor in `applicationClosing()`

#### Pattern 5: Result Handling

**Location**: [DAMAWorkspaceAccessPluginExtension.java:691-746](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L691-L746)

```java
/**
 * Set result text and show replace button if result is valid
 */
private void setResultWithReplaceButton(String result) {
    String safeResult = (result != null) ? result : "";
    PluginLogger.info("[setResultWithReplaceButton]Setting result (length: "
                      + safeResult.length() + ")");

    boolean isValid = isValidResultForReplacement(safeResult);
    resultArea.setText(safeResult);

    if (isValid) {
        showReplaceButton();
    } else {
        hideAllButtons();
    }
}

/**
 * Display informational result without action buttons
 */
private void setResultInformational(String result) {
    String safeResult = (result != null) ? result : "";
    PluginLogger.info("[setResultInformational]Setting result (length: "
                      + safeResult.length() + ")");
    resultArea.append(safeResult);
    hideAllButtons();
}

/**
 * Check if result is valid for replacement
 */
private boolean isValidResultForReplacement(String result) {
    if (result == null || result.trim().isEmpty()) {
        return false;
    }

    if (isErrorMessage(result)) {
        return false;
    }

    // Use operation context
    if (currentOperation == OperationType.AI_MARKUP ||
        currentOperation == OperationType.TAG_REMOVAL) {
        return true;
    }

    return false;
}
```

**Key Points:**
1. **Three result types**: Replace button, informational (no buttons)
2. **Null safety**: Always convert null to empty string
3. **Context-based validation**: Use `currentOperation` enum
4. **Error detection**: Use `isErrorMessage()` to detect errors
5. **Logging**: Log every result update with length

#### Pattern 6: Internationalization

**Location**: [DAMAWorkspaceAccessPluginExtension.java:600-633](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L600-L633)

```java
/**
 * Get internationalized string
 */
private String i18n(String key) {
    try {
        if (resources != null) {
            java.lang.reflect.Method getMessageMethod =
                resources.getClass().getMethod("getMessage", String.class);
            return (String) getMessageMethod.invoke(resources, key);
        }
        PluginLogger.warn("[i18n]Resources not available: " + key);
        return key;
    } catch (Exception e) {
        PluginLogger.error("[i18n]Error for key " + key + ": " + e.getMessage());
        return key;
    }
}

/**
 * Get internationalized string with parameter substitution
 */
private String i18n(String key, Object... params) {
    try {
        String message = i18n(key);
        if (params != null && params.length > 0) {
            // Simple parameter substitution using {0}, {1}, etc.
            for (int i = 0; i < params.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(params[i]));
            }
        }
        return message;
    } catch (Exception e) {
        PluginLogger.error("[i18n]Error formatting key " + key);
        return key;
    }
}
```

**Usage Examples:**
```java
// Simple key
infoArea.setText(i18n("action.ai.markup.selected") + "\n\n");

// With single parameter
infoArea.append(i18n("selected.text", selectedText) + "\n");

// With multiple parameters
info.append(i18n("found.issues", issueCount, totalCount) + "\n");
```

**Key Points:**
1. **Fallback to key**: If translation fails, return the key itself
2. **Parameter substitution**: Use `{0}`, `{1}` placeholders
3. **Reflection usage**: Use reflection to call `getMessage()` on resource bundle
4. **Error handling**: Log errors but don't throw exceptions
5. **Consistent naming**: Dot-separated keys (e.g., `action.ref.to.link.selected`)

#### Pattern 7: Operation Context Tracking

**Location**: [DAMAWorkspaceAccessPluginExtension.java:59-68](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java#L59-L68)

```java
/**
 * Operation type enum for tracking current operation context
 */
private enum OperationType {
    NONE,           // No operation in progress
    AI_MARKUP,      // AI Markup operation
    TAG_REMOVAL,    // Tag Removal operation
    UTF8_CHECK,     // UTF-8 Check operation
    UTF8_CONVERT,   // UTF-8 Conversion operation
    REF_TO_LINK     // NEW: Ref to Link operation
}

// Current operation context
private OperationType currentOperation = OperationType.NONE;
```

**Usage:**
```java
// Set at start of action
currentOperation = OperationType.REF_TO_LINK;

// Check in validation
if (currentOperation == OperationType.REF_TO_LINK) {
    return true;
}

// Reset after completion
currentOperation = OperationType.NONE;
```

**Key Points:**
1. **Enum for type safety**: Use enum instead of string constants
2. **Single field**: One `currentOperation` field tracks global state
3. **Set at start**: Set operation type at the beginning of each action
4. **Reset on completion**: Reset to NONE after successful operation
5. **Use in validation**: Check operation type to determine valid states

#### Pattern 8: Logging

**Location**: Throughout [DAMAWorkspaceAccessPluginExtension.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java)

```java
// Method entry with context
PluginLogger.info("[AIMarkupActionListener]AI Markup action triggered");

// Success with details
PluginLogger.info("[ReplaceButtonActionListener]Text replaced successfully");

// Warnings for expected issues
PluginLogger.warn("[ReplaceButtonActionListener]No text to replace");

// Errors with exception messages
PluginLogger.error("[ReplaceButtonActionListener]Error: " + ex.getMessage());

// Debug information
PluginLogger.debug("[applicationStarted]Debug mode: " + PluginLogger.isDebugEnabled());
```

**Key Points:**
1. **Context markers**: Use `[MethodName]` or `[ClassName]` prefix
2. **Four levels**: info, debug, warn, error
3. **Info for flow**: Log major state transitions and user actions
4. **Debug for details**: Log detailed information only in debug mode
5. **Warn for expected errors**: Use warn for recoverable issues
6. **Error for exceptions**: Log all caught exceptions with messages
7. **Include details**: Add relevant data (lengths, counts, states)

### Implementation Recommendations for Ref to Link Action

Based on the established patterns, here's how the Ref to Link action should be implemented:

**1. Add Operation Type**
```java
private enum OperationType {
    NONE,
    AI_MARKUP,
    TAG_REMOVAL,
    UTF8_CHECK,
    UTF8_CONVERT,
    REF_TO_LINK        // NEW
}
```

**2. Create Action Listener**
```java
private class RefToLinkActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        PluginLogger.info("[RefToLinkActionListener]Ref to Link action triggered");

        currentOperation = OperationType.REF_TO_LINK;

        infoArea.setText(i18n("action.ref.to.link.selected") + "\n\n");
        resultArea.setText("");
        hideAllButtons();

        String selectedText = fetchSelectedText(resultArea);
        if (selectedText.isEmpty()) {
            return;
        }

        infoArea.append(i18n("selected.ref") + ": " + selectedText + "\n\n");

        // Parse synchronously (fast operation, no async needed)
        try {
            ReferenceParser parser = new ReferenceParser();
            TripitakaComponents components = parser.parseReference(selectedText);

            infoArea.append(i18n("ref.components.extracted") + "\n");
            infoArea.append("  " + i18n("canon") + ": " + components.getCanon() + "\n");
            infoArea.append("  " + i18n("volume") + ": " + components.getVolume() + "\n");
            infoArea.append("  " + i18n("page") + ": " + components.getPage() + "\n");
            if (components.hasColumn()) {
                infoArea.append("  " + i18n("column") + ": " + components.getColumn() + "\n");
            }

            // Show Convert button
            showConvertButton();

        } catch (InvalidReferenceException ex) {
            setResultInformational(i18n("error.parsing.ref", ex.getMessage()));
        }
    }
}
```

**3. Add Menu Item**
```java
// In createMenuBar() method
JMenu menuActions = new JMenu(i18n("menu.actions"));
JMenuItem menuItemActionAIMarkup = new JMenuItem(i18n("menuItem.ai.markup"));
JMenuItem menuItemActionTagRemoval = new JMenuItem(i18n("menuItem.tag.removal"));
JMenuItem menuItemRefToLink = new JMenuItem(i18n("menuItem.ref.to.link")); // NEW

menuActions.add(menuItemActionAIMarkup);
menuActions.add(menuItemActionTagRemoval);
menuActions.add(menuItemRefToLink); // NEW
menuBar.add(menuActions);

menuItemRefToLink.addActionListener(new RefToLinkActionListener()); // NEW
```

**4. Add i18n Keys**

In `src/main/resources/i18n/translation.xml`:
```xml
<key value="menuItem.ref.to.link">
    <val lang="en_US">&lt;ref&gt; to link</val>
    <val lang="zh_CN">&lt;ref&gt; 转链接</val>
    <val lang="zh_TW">&lt;ref&gt; 轉連結</val>
</key>

<key value="action.ref.to.link.selected">
    <val lang="en_US">Action: &lt;ref&gt; to link selected</val>
    <val lang="zh_CN">操作：已选择 &lt;ref&gt; 转链接</val>
    <val lang="zh_TW">操作：已選擇 &lt;ref&gt; 轉連結</val>
</key>

<key value="selected.ref">
    <val lang="en_US">Selected &lt;ref&gt; element</val>
    <val lang="zh_CN">已选择的 &lt;ref&gt; 元素</val>
    <val lang="zh_TW">已選擇的 &lt;ref&gt; 元素</val>
</key>

<key value="ref.components.extracted">
    <val lang="en_US">Reference components extracted:</val>
    <val lang="zh_CN">已提取引用组件：</val>
    <val lang="zh_TW">已提取引用組件：</val>
</key>

<key value="error.parsing.ref">
    <val lang="en_US">Error parsing reference: {0}</val>
    <val lang="zh_CN">解析引用时出错：{0}</val>
    <val lang="zh_TW">解析引用時出錯：{0}</val>
</key>
```

**5. Reuse Existing Infrastructure**
- Use existing `ReplaceButtonActionListener` for document modification
- Use existing `fetchSelectedText()` for selection handling
- Use existing `setResultWithReplaceButton()` for result display
- Use existing `i18n()` methods for internationalization
- Use existing logging patterns with `PluginLogger`

### Alternatives Considered

1. **Create standalone plugin** - Rejected: Should extend existing DAMA plugin
2. **Use Author mode APIs** - Rejected: DAMA uses Text mode
3. **Implement custom result handler** - Rejected: Reuse `setResultWithReplaceButton()`
4. **Async parsing** - Rejected: Parsing is fast enough for synchronous execution
5. **Follow existing patterns** - ✅ **CHOSEN**: Ensures consistency and maintainability

---

## Summary of Decisions

| Research Area | Decision | Key Rationale |
|---------------|----------|---------------|
| **CBETA API** | Use CBRD REST API at `https://cbss.dila.edu.tw/dev/cbrd/link` | Official API provided by CBETA/DILA; handles validation and edge cases |
| **XML Parsing** | Java DOM with `DocumentBuilder` | Simple, standard library, perfect for small fragments |
| **SDK Integration** | Follow existing DAMA plugin patterns | Ensures consistency, reuses infrastructure |

---

## Implementation Checklist

### Phase 0 (Research) ✅ COMPLETE
- ✅ Research CBETA API specification
- ✅ Analyze XML parsing approaches
- ✅ Review Oxygen SDK integration patterns
- ✅ Document findings in research.md

### Phase 1 (Design - Next Steps)
- ⬜ Create data-model.md (entity definitions, validation rules, state transitions)
- ⬜ Generate contracts/cbeta-api.yaml (OpenAPI schema for URL building)
- ⬜ Create quickstart.md
- ⬜ Update agent context

### Phase 2 (Implementation - See tasks.md)
- ⬜ Create domain layer: `ReferenceParser`, `TripitakaComponents`, `CBETAReference`
- ⬜ Create application layer: `ConvertReferenceCommand`
- ⬜ Create infrastructure layer: `CBETAUrlBuilder`, `RefToLinkActionListener`
- ⬜ Update existing classes: `DAMAWorkspaceAccessPluginExtension`, `translation.xml`
- ⬜ Write unit tests for all new classes
- ⬜ Write integration tests for full workflow
- ⬜ Update documentation

---

## References

### Codebase Files Analyzed

- [DAMAWorkspaceAccessPluginExtension.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\workspace\DAMAWorkspaceAccessPluginExtension.java) - Main plugin class (1309 lines)
- [UTF8ValidationService.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\utf8\UTF8ValidationService.java) - Service layer pattern
- [TranslationConsistencyTest.java](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\src\test\java\com\dila\dama\plugin\test\TranslationConsistencyTest.java) - DOM parsing example
- [ref-cases.json](d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\xml-w-link\ref-cases.json) - CBETA URL patterns (1869 lines)
- [CopyAttributes.java](d:\project\OCR2Markup\oxygenSDKSamples\oxygen-sample-eclipse-plugin\src\main\java\dila\eclipse\action\CopyAttributes.java) - Author mode example
- [SAXParserHandler.java](d:\project\OCR2Markup\oxygenSDKSamples\oxygen-sample-plugins\oxygen-sample-plugin-broken-links-checker\src\main\java\dila\brokenlinkschecker\impl\SAXParserHandler.java) - SAX parsing example

### Specification Files

- [spec.md](d:\project\OCR2Markup\specs\001-ref-to-link-action\spec.md) - Feature specification
- [plan.md](d:\project\OCR2Markup\specs\001-ref-to-link-action\plan.md) - Implementation plan
- [constitution.md](d:\project\OCR2Markup\.specify\memory\constitution.md) - Project principles

### Java Libraries

**XML Parsing:**
- `javax.xml.parsers.DocumentBuilderFactory`
- `javax.xml.parsers.DocumentBuilder`
- `org.w3c.dom.Document`
- `org.w3c.dom.Element`
- `org.w3c.dom.NodeList`
- `org.xml.sax.InputSource`

**Oxygen SDK:**
- `ro.sync.exml.workspace.api.editor.WSEditor`
- `ro.sync.exml.workspace.api.editor.page.WSEditorPage`
- `ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage`

**Concurrency:**
- `java.util.concurrent.CompletableFuture`
- `java.util.concurrent.ExecutorService`
- `java.util.concurrent.Executors`
- `javax.swing.SwingUtilities`

---

## Conclusion

All research for Phase 0 is complete. The findings provide clear guidance for implementing the Ref to Link feature:

1. **CBETA URL Construction**: Build URLs directly using the pattern `https://cbetaonline.dila.edu.tw/[lang]/[canon][volume]n[work]_p[page][column][line]` with appropriate component transformations
2. **XML Parsing**: Use Java DOM to parse `<ref>` elements and extract child components
3. **Plugin Integration**: Follow existing DAMA patterns for action listeners, selection handling, async operations, and UI updates

The implementation will be straightforward by reusing existing infrastructure and following established patterns.
