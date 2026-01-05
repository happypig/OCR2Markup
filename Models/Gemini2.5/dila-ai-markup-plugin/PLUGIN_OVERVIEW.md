# DILA AI Markup Plugin - Implementation Overview

**Version:** 0.4.0
**Framework:** Oxygen XML Author Plugin (Java-based)
**Build System:** Maven
**Target Platform:** Oxygen XML SDK 27.1.0.3
**Java Compatibility:** Java 8 (1.8)
**Last Updated:** Jan 5 2026

> **📘 For Users**: Looking for installation instructions, user guide, and features? See [README.MD](README.MD)
>
> **🔧 This Document**: Technical implementation details for developers and maintainers
>
> **🏗️ For Architects**: Looking for formal design specifications with UML and OCL? See [RUP.MD](docs/RUP.MD)

---

## 1. Project Structure

### 1.1 Standard Maven Structure

```
dila-ai-markup-plugin/
├── src/
│   ├── main/
│   │   ├── java/                    # Java source code
│   │   │   └── com/dila/dama/plugin/
│   │   │       ├── preferences/     # Options/preferences UI
│   │   │       ├── utf8/            # UTF-8 validation service
│   │   │       ├── util/            # Utility classes (logging)
│   │   │       └── workspace/       # Plugin entry point & UI
│   │   └── resources/               # Plugin resources
│   │       ├── i18n/                # Internationalization files
│   │       │   └── translation.xml
│   │       ├── images/              # UI icons (if any)
│   │       ├── plugin.xml           # Plugin descriptor
│   │       └── extension.xml        # Extension configuration
│   └── test/
│       ├── java/                    # Test code
│       │   └── com/dila/dama/plugin/
│       │       ├── i18n/            # Localization tests
│       │       ├── test/            # General tests
│       │       ├── utf8/            # UTF-8 service tests
│       │       └── workspace/       # Workspace extension tests
│       └── resources/               # Test resources
│           ├── i18n/
│           └── test-files/
├── pom.xml                          # Maven build configuration
├── assembly.xml                     # Plugin packaging configuration
└── dilaAIMarkupPlugin.xml           # Root plugin descriptor (generated)
```

### 1.2 Build Output Structure (Required by Maven)

```
target/                              # Maven build output (required, auto-generated)
├── classes/                         # Compiled Java classes
├── test-classes/                    # Compiled test classes
├── maven-archiver/                  # Maven metadata
├── maven-status/                    # Build status tracking
├── antrun/                          # Ant plugin output
└── dilaAIMarkupPlugin.zip           # Final plugin package
```

### 1.3 Extra Folders (Not Required by Maven)

#### `.github/` - CI/CD Configuration
- **Purpose**: GitHub-specific automation and history
- **Required by Maven**: ❌ No
- **Can be deleted**: ✅ Yes (if not using GitHub CI/CD)
- **Contents**:
  - `java-upgrade/` - Historical upgrade notes and planning
  - Migration planning documents

#### `.settings/` - Eclipse IDE Configuration
- **Purpose**: Eclipse IDE-specific project settings
- **Required by Maven**: ❌ No
- **Can be deleted**: ✅ Yes (if not using Eclipse)
- **Note**: Managed by Eclipse IDE, should be in `.gitignore`

#### `dilaAIMarkupPlugin/` - Legacy Packaging Structure
- **Purpose**: Old-style plugin packaging (pre-Maven structure)
- **Required by Maven**: ❌ No
- **Can be deleted**: ⚠️ Maybe (verify if assembly.xml references it)
- **Contents**:
  - `i18n/` - Duplicate of resources
  - `images/` - Duplicate of resources
  - `lib/` - Empty or old JARs
- **Recommendation**: Can be removed if `src/main/resources/` has all needed files

#### `lib/` - External Libraries
- **Purpose**: Manual dependency management
- **Required by Maven**: ❌ No (Maven handles dependencies via pom.xml)
- **Can be deleted**: ✅ Yes (Maven downloads dependencies automatically)
- **Note**: Should be empty if using Maven correctly

#### `test-files/` (root level) - Ad-hoc Test Data
- **Purpose**: Manual test files outside Maven structure
- **Required by Maven**: ❌ No
- **Can be deleted**: ⚠️ Maybe (check if tests reference it)
- **Better location**: `src/test/resources/test-files/`
- **Contents**: UTF-8/UTF-16 test files for manual testing

#### `docs/` - Documentation
- **Purpose**: Project documentation and guides
- **Required by Maven**: ❌ No
- **Can be deleted**: ❌ No (valuable project knowledge)
- **Should be kept**: ✅ Yes (contains implementation history)

---

## 2. Plugin Architecture

### 2.1 Core Components

#### Entry Point: `DAMAWorkspaceAccessPlugin`
- Implements `WorkspaceAccessPlugin` from Oxygen SDK
- Registers the workspace access extension

#### Main Extension: `DAMAWorkspaceAccessPluginExtension`
- Implements `WorkspaceAccessPluginExtension`
- Creates plugin UI with Swing components
- Integrates all features:
  - AI Markup processing (LLM API integration)
  - Tag removal functionality
  - UTF-8 validation and conversion workflow
  - Options/preferences integration

#### Options Page: `DAMAOptionPagePluginExtension`
- Extends `OptionPagePluginExtension`
- Provides preferences UI for:
  - API Key configuration
  - Model selection (ft.parse.model, ft.detect.model)
  - Settings storage via Oxygen's WSOptionsStorage

#### UTF-8 Service: `UTF8ValidationService`
- Pure Java implementation (95% accuracy)
- Features:
  - Strict UTF-8 validation using CharsetDecoder
  - BOM detection (UTF-8, UTF-16 LE/BE)
  - UTF-16 pattern recognition
  - Automatic encoding detection
  - File conversion with backup creation
  - File size limits (50MB) for safety

#### Utility: `PluginLogger`
- Centralized logging framework
- Multiple log levels (DEBUG, INFO, WARN, ERROR)
- Debug mode via environment (`DILA_DEBUG=true`) or system property (`-Ddila.debug=true`)
- UTF-8 encoding support for log output

### 2.2 Feature Summary

| Feature | Implementation | Status |
|---------|---------------|--------|
| **AI Markup** | Native Java HTTP client with LLM API integration | ✅ Complete |
| **Tag Removal** | Regex-based text processing | ✅ Complete |
| **UTF-8 Check/Convert** | Enhanced Java validation service (95% accuracy) | ✅ Complete |
| **Options Dialog** | Swing-based preferences page | ✅ Complete |
| **Internationalization** | XML-based resource bundles (69 keys, 32 used) | ✅ Complete |
| **Background Processing** | CompletableFuture + ExecutorService | ✅ Complete |

---

## 3. Documentation Files

### 3.1 Core Documentation

#### [GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md)
**Purpose**: Git branching strategy for UTF-8 implementation
**Summary**:
- Documents the evolution from JavaScript (90% accuracy) to Java (95% accuracy)
- Branch structure:
  - `main`: Current stable version
  - `feature/utf8-tool-js`: JavaScript implementation (archived)
  - `feature/utf8-tool-java`: Java migration branch (merged)
- Decision criteria: accuracy, reliability, performance, maintainability

#### [JAVASCRIPT-TO-JAVA-MIGRATION-COMPLETE.md](docs/JAVASCRIPT-TO-JAVA-MIGRATION-COMPLETE.md)
**Purpose**: Complete migration success documentation
**Summary**:
- **All 5 phases completed**:
  1. Core UI Framework Migration (JavaScript → Java Swing)
  2. UTF-8 Tools Migration (integrated with native UI)
  3. AI Markup Migration (LLM API integration)
  4. Tag Removal & Options (text processing)
  5. JavaScript Cleanup (legacy code archived)
- **Key Achievements**:
  - Eliminated JS-Java bridge issues
  - Native Swing integration
  - Better performance and reliability
  - JAR size: 81,310 bytes (from 54,399 bytes)

#### [MIGRATION-COMPLETE.md](docs/MIGRATION-COMPLETE.md)
**Purpose**: UTF-8 tool specific migration requirements and completion
**Summary**:
- **All 8 requirements fulfilled**:
  1. ✅ All existing UI preserved
  2. ✅ UTF-8 UI integrated consistently
  3. ✅ Java implementation (95% accuracy)
  4. ✅ Plugin configs minimally changed (version 0.2.4 → 0.3.0)
  5. ✅ DAMAWorkspaceAccessPlugin verified
  6. ✅ File corruption prevention (backups, size limits, validation)
  7. ✅ Check workflow with statistics display
  8. ✅ Conversion results in result area
- **Safety Features**:
  - Automatic `.utf8backup` files
  - 50MB file size limit
  - Path validation
  - Permission checks
  - Strict validation with CharsetDecoder

### 3.2 Technical Documentation

#### [TEST_FAILURE_FIXING_GUIDE.md](docs/TEST_FAILURE_FIXING_GUIDE.md)
**Purpose**: Systematic methodology for analyzing and fixing test failures
**Summary**:
- **Failure Categories**:
  - Assertion failures (expected vs actual)
  - Mock verification failures
  - Timeout failures (performance issues)
  - Exception handling failures
  - Null pointer exceptions
  - Test environment interference
- **Fix Strategies**: Step-by-step analysis and resolution patterns
- **Real Examples**: UTF8ValidationService, DAMAWorkspaceAccessPluginExtension, LocalizationTest
- **Best Practices**: Incremental testing, defensive programming, test isolation

#### [TEST_FAILURE_QUICK_REFERENCE.md](docs/TEST_FAILURE_QUICK_REFERENCE.md)
**Purpose**: Quick lookup reference for common test issues (not read but inferred)

#### [translationKeyAnalysis.md](docs/translationKeyAnalysis.md)
**Purpose**: Translation key usage analysis
**Summary**:
- **Total keys**: 69 (100%)
- **Used keys**: 32 (46.4%)
- **Unused keys**: 37 (53.6%)
- **Used by**:
  - DAMAWorkspaceAccessPluginExtension.java: 28 keys
  - DAMAOptionPagePluginExtension.java: 4 keys
- **Unused categories**:
  - Legacy UI messages (13 keys)
  - Options dialog messages (4 keys)
  - UTF-8 conversion detailed dialog (19 keys)
- **Recommendations**: Clean up or document for future use

#### [UTF8_Tool_Implementation_Comparison.md](docs/UTF8_Tool_Implementation_Comparison.md)
**Purpose**: Comprehensive comparison across 4 implementation phases
**Summary**:
- **Phase 1 (BK)**: JavaScript bridge via DAMAOptionPagePluginExtension
- **Phase 2 (Enhanced)**: JavaScript bridge via dedicated UTF8ValidationService
- **Phase 3 (Legacy)**: Same as Enhanced
- **Phase 4 (Java)**: Native Java with CharsetDecoder
- **Key Improvements**:
  - BOM detection (UTF-8, UTF-16 LE/BE)
  - UTF-16 pattern recognition (null byte patterns)
  - Prevents false positives and data corruption
  - Background processing with non-blocking UI
  - Professional error handling and progress feedback
- **User Experience Evolution**:
  - Blocking UI → Non-blocking responsive interface
  - Basic tool → Professional enterprise-grade application
  - Direct replacement → Confirmed workflow with backups
  - Generic errors → Specific, actionable error messages

#### [Session of Copilot CLI.txt](docs/Session%20of%20Copilot%20CLI.txt)

**Purpose**: Historical development notes (AI pair programming session)

#### [REFACTORING_SUMMARY.md](docs/REFACTORING_SUMMARY.md)

**Purpose**: PluginLogger utility extraction refactoring documentation

**Summary**:

- **Date**: October 10, 2025
- **Refactoring**: Extracted debug utilities into centralized `PluginLogger` class
- **Changes**:
  - Created `PluginLogger.java` with multiple log levels (DEBUG, INFO, WARN, ERROR)
  - Updated `DAMAWorkspaceAccessPluginExtension.java` (-40 lines)
  - Updated `DAMAOptionPagePluginExtension.java` (-9 lines)
  - Added UTF-8 encoding support for all log output
- **Benefits**:
  - DRY principle: Eliminated ~40 lines of duplicate code
  - Enhanced functionality: Multiple log levels, exception logging, class context
  - Consistency: Uniform logging format across all classes
  - Maintainability: Single source of truth for logging
- **Debug Mode**:
  - Environment variable: `DILA_DEBUG=true`
  - System property: `-Ddila.debug=true`
- **Build Status**: ✅ Maven Clean Compile SUCCESS

#### [RUP.MD](docs/RUP.MD)

**Purpose**: Rational Unified Process (RUP) Object-Oriented Design Document

**Summary**:

- **Version**: 0.3.1-SNAPSHOT
- **Date**: October 10, 2025
- **Methodology**: RUP with UML and OCL
- **Contents**:
  1. **Design Principles**: SOLID principles, architectural layers (4 tiers)
  2. **Class Diagrams**: Core architecture, relationships with multiplicity, UI hierarchy
  3. **Class Specifications**: Detailed attributes, methods, stereotypes, responsibilities
  4. **Sequence Diagrams**:
     - Application startup sequence
     - AI markup processing sequence
     - UTF-8 validation and conversion sequence
     - Tag removal sequence
  5. **State Diagrams**: Plugin lifecycle, conversion operation, UI button states
  6. **OCL Constraints**:
     - System-level invariants
     - Pre/post conditions for all major operations
     - Derived attributes
  7. **Design Patterns**:
     - Singleton (DAMAWorkspaceAccessPlugin)
     - Strategy (encoding detection)
     - Command (menu actions)
     - Template Method (file processing)
     - Observer (button state management)
     - Value Object (ConversionSuccess/Failure)
  8. **Package Structure**: Dependencies and responsibilities
  9. **Deployment View**: Component deployment, runtime environment
- **Technical Details**:
  - 3 packages: workspace, preferences, utf8
  - 4 classes in utf8 package (service + 3 result types)
  - Comprehensive OCL constraints for validation
  - Thread safety and performance considerations
- **Documentation Quality**: Enterprise-grade design documentation with formal constraints

### 3.3 Legacy Code Archive

#### `docs/jsCopy/`
**Contents**: Archived JavaScript implementations
- `dila-ai-markup-bk.js` - Original backup version
- `dila-ai-markup-enhanced.js` - Enhanced version
- `dila-ai-markup-legacy.js` - Final JavaScript version before Java migration

### 3.4 Behavior-Driven Development Specifications

#### Source: [README.BK.MD](docs/README.BK.MD) - Plugin Specifications (Gherkin BDD)

**Purpose**: Comprehensive behavior specifications in Gherkin syntax for test automation and acceptance criteria

**Coverage**: 6 features, 18+ scenarios covering all major functionality

**Testing Value**:
- **Executable Specifications**: Can be used with Cucumber, SpecFlow, or Behave for automated testing
- **Comprehensive Coverage**: 18+ scenarios covering happy paths, error cases, and edge conditions
- **Edge Case Documentation**: BOM detection, large files, rate limiting, malformed responses
- **Acceptance Criteria**: Clear definition of expected behavior for QA validation
- **Regression Testing**: Scenarios can be automated to prevent regressions

#### Feature 1: AI-Powered TEI XML Markup Assistant (4 scenarios)

**Scenarios**:
1. Successful AI markup of selected text (happy path)
2. AI markup with invalid API configuration (error handling)
3. AI markup with network connectivity issues (resilience)
4. User reviews and edits AI-generated markup (interactive workflow)

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: AI-Powered TEI XML Markup Assistant
  As a TEI XML document editor
  I want to automatically markup references and citations using AI
  So that I can efficiently process scholarly documents with proper semantic tagging

  Background:
    Given the DILA AI Markup Plugin is installed in Oxygen XML Editor
    And the plugin is properly configured with valid API credentials
    And I have a TEI XML document open in the editor

  Scenario: Successful AI markup of selected text
    Given I have selected unmarked text containing potential references
    When I click "AI Markup" from the plugin menu
    Then the plugin should send the text to the configured AI model
    And the AI response should contain properly tagged XML elements
    And the result should be displayed in the plugin's result area
    And a "Replace" button should be available
    When I click the "Replace" button
    Then the original selected text should be replaced with the AI-marked version
    And the change should be recorded in the document's undo history

  Scenario: AI markup with invalid API configuration
    Given the API key is not configured or is invalid
    When I attempt to use AI markup functionality
    Then an error message should be displayed in the info panel
    And the user should be guided to check the plugin configuration
    And no changes should be made to the document

  Scenario: AI markup with network connectivity issues
    Given the API key is valid but network connectivity fails
    When I attempt to use AI markup functionality
    Then a network error message should be displayed
    And the user should be able to retry the operation
    And the plugin should not crash or become unresponsive

  Scenario: User reviews and edits AI-generated markup
    Given AI markup has been generated and displayed in the result area
    When I modify the markup text in the result area
    And I click the "Replace" button
    Then the edited version should be used for replacement
    And the original AI response should be preserved for reference
```

</details>

#### Feature 2: UTF-8 File Validation and Conversion (5 scenarios)

**Scenarios**:
1. Scan directory for non-UTF-8 files
2. Convert non-UTF-8 files to UTF-8 with backup
3. Handle encoding detection for common formats (Windows-1252, GBK, Big5)
4. UTF-8 validation with BOM detection (FF FE or FE FF)
5. Large file and directory processing (50MB limit, progress feedback)

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: UTF-8 File Validation and Conversion
  As a document processor working with international content
  I want to validate and convert file encodings to UTF-8
  So that all text files are properly encoded for XML processing

  Background:
    Given the DILA AI Markup Plugin is installed
    And I have access to the UTF-8 validation tools

  Scenario: Scan directory for non-UTF-8 files
    Given I have a directory containing files with mixed encodings
    When I select the directory for UTF-8 validation
    Then the plugin should recursively scan all text files
    And it should identify files that are not valid UTF-8
    And it should display a list of non-UTF-8 files with their paths
    And it should show the total count of problematic files

  Scenario: Convert non-UTF-8 files to UTF-8 with backup
    Given I have identified non-UTF-8 files in my directory
    When I choose to convert them to UTF-8
    Then the plugin should create backup files with .utf8backup extension
    And it should detect the source encoding automatically
    And it should convert each file to UTF-8 encoding
    And it should preserve the original file content and structure
    And it should report the conversion results with success/failure counts

  Scenario: Handle encoding detection for common formats
    Given I have files encoded in Windows-1252, GBK, or Big5
    When I run UTF-8 validation
    Then the plugin should correctly identify these encoding formats
    And it should distinguish them from UTF-8 files
    And it should handle encoding conversion appropriately for each format

  Scenario: UTF-8 validation with BOM detection
    Given I have files with UTF-16 byte order marks (BOM)
    When I run UTF-8 validation
    Then the plugin should detect UTF-16 BOM patterns (FF FE or FE FF)
    And it should correctly identify these files as non-UTF-8
    And it should handle the conversion while removing or handling BOMs properly

  Scenario: Large file and directory processing
    Given I have directories with hundreds of files including large files
    When I run UTF-8 validation
    Then the plugin should process files efficiently without memory issues
    And it should skip binary files automatically
    And it should handle files larger than 50MB appropriately
    And it should provide progress feedback for long operations
```

</details>

#### Feature 3: Multi-Language Support and Localization (3 scenarios)

**Scenarios**:
1. Interface language switching (Scenario Outline with 3 examples: en_US, zh_CN, zh_TW)
2. Fallback to English for missing translations
3. UTF-8 character handling in all languages

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: Multi-Language Support and Localization
  As an international user of the plugin
  I want the interface to display in my preferred language
  So that I can use the plugin effectively in my native language

  Background:
    Given the DILA AI Markup Plugin supports multiple languages
    And the translation files are properly configured

  Scenario Outline: Interface language switching
    Given Oxygen XML Editor is configured for <locale>
    When I open the plugin interface
    Then all menu items should display in <language>
    And all button labels should display in <language>
    And all error messages should display in <language>
    And all tooltips should display in <language>

    Examples:
      | locale | language               |
      | en_US  | English                |
      | zh_CN  | Simplified Chinese     |
      | zh_TW  | Traditional Chinese    |

  Scenario: Fallback to English for missing translations
    Given a translation key is missing in the current language bundle
    When the plugin needs to display that message
    Then it should fall back to the English version
    And it should not crash or display raw translation keys
    And it should log the missing translation for developer reference

  Scenario: UTF-8 character handling in all languages
    Given I am using Chinese language interface
    When I process XML documents with Chinese characters
    Then the plugin should handle Chinese text correctly in all operations
    And UTF-8 validation should work properly with Chinese content
    And AI markup should preserve Chinese characters accurately
```

</details>

#### Feature 4: Plugin Configuration and Preferences (3 scenarios)

**Scenarios**:
1. Configure API credentials and models
2. Validate API configuration
3. Handle missing or invalid configuration

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: Plugin Configuration and Preferences
  As a plugin user
  I want to configure API settings and preferences
  So that I can customize the plugin behavior for my workflow

  Background:
    Given I have access to Oxygen XML Editor preferences
    And the DILA AI Markup Assistant preferences page is available

  Scenario: Configure API credentials and models
    Given I open the plugin preferences page
    When I enter a valid OpenAI API key
    And I specify the fine-tuned model names for parsing and detection
    And I save the preferences
    Then the settings should be stored securely in Oxygen's options storage
    And the API key should be encrypted/protected
    And the model settings should be available for plugin operations

  Scenario: Validate API configuration
    Given I have entered API configuration in preferences
    When I test the API connection through the plugin
    Then the plugin should verify the API key is valid
    And it should confirm the specified models are accessible
    And it should provide clear feedback on configuration status

  Scenario: Handle missing or invalid configuration
    Given the plugin preferences are not configured or invalid
    When I attempt to use AI markup functionality
    Then the plugin should detect the missing configuration
    And it should provide clear guidance on what needs to be configured
    And it should offer a direct link to the preferences page
```

</details>

#### Feature 5: Error Handling and Recovery (3 scenarios)

**Scenarios**:
1. Handle API rate limiting
2. Handle malformed API responses
3. Recovery from plugin exceptions

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: Error Handling and Recovery
  As a plugin user
  I want the plugin to handle errors gracefully
  So that I can continue working even when issues occur

  Scenario: Handle API rate limiting
    Given I make multiple rapid API requests
    When the API returns a rate limit error
    Then the plugin should display an appropriate message
    And it should suggest waiting before retrying
    And it should not lose the user's work or selections

  Scenario: Handle malformed API responses
    Given the API returns an invalid or incomplete response
    When the plugin processes the response
    Then it should detect the malformed response
    And it should display a meaningful error message
    And it should not crash or corrupt the document

  Scenario: Recovery from plugin exceptions
    Given an unexpected error occurs in plugin operations
    When the error is caught by the plugin's exception handling
    Then the error should be logged with sufficient detail
    And the user should receive a friendly error message
    And the plugin should remain functional for subsequent operations
    And the document should remain in a consistent state
```

</details>

#### Feature 6: Document Integration and Workflow (3 scenarios)

**Scenarios**:
1. Text selection and replacement workflow
2. Handle complex XML structures
3. Batch processing workflow

<details>
<summary>View Gherkin specification</summary>

```gherkin
Feature: Document Integration and Workflow
  As a TEI XML document editor
  I want seamless integration with my document editing workflow
  So that I can efficiently process documents without disruption

  Scenario: Text selection and replacement workflow
    Given I have a TEI XML document with unmarked references
    When I select a paragraph containing potential citations
    And I trigger AI markup processing
    Then the plugin should preserve the document structure
    And it should maintain proper XML formatting
    And the replacement should integrate seamlessly with existing markup
    And undo/redo functionality should work correctly

  Scenario: Handle complex XML structures
    Given I select text that spans multiple XML elements
    When I apply AI markup
    Then the plugin should handle complex nested structures correctly
    And it should preserve existing XML attributes and namespaces
    And it should generate valid XML markup that integrates properly

  Scenario: Batch processing workflow
    Given I have multiple sections that need markup
    When I process them sequentially with the plugin
    Then each operation should be independent and reliable
    And the plugin state should be properly maintained between operations
    And document changes should be tracked correctly in the undo history
```

</details>

---

## 4. Build and Packaging

### 4.1 Maven Build Process

```bash
# Compile source code
mvn clean compile

# Run tests
mvn test

# Create plugin package
mvn clean install
```

### 4.2 Packaging Configuration ([assembly.xml](assembly.xml))

**Purpose**: Defines how to package the plugin for Oxygen XML Author

**Structure**:
```xml
<assembly>
  <id>plugin</id>
  <formats><format>zip</format></formats>
  <baseDirectory>dilaAIMarkupPlugin</baseDirectory>

  <!-- Include compiled JAR -->
  <files>
    <file>
      <source>target/${project.build.finalName}.jar</source>
      <outputDirectory>lib</outputDirectory>
    </file>
  </files>

  <!-- Include resources (plugin.xml, i18n, images) -->
  <fileSets>
    <fileSet>
      <directory>src/main/resources</directory>
      <outputDirectory>/</outputDirectory>
    </fileSet>
  </fileSets>

  <!-- Include runtime dependencies (exclude Oxygen SDK) -->
  <dependencySets>
    <dependencySet>
      <outputDirectory>lib</outputDirectory>
      <excludes>
        <exclude>com.oxygenxml:*</exclude>
      </excludes>
    </dependencySet>
  </dependencySets>
</assembly>
```

### 4.3 Build Output

**Final Package**: `target/dilaAIMarkupPlugin.zip`
**Installation**: Extract to Oxygen plugins directory

**Package Contents**:
```
dilaAIMarkupPlugin/
├── lib/
│   └── dila-ai-markup-plugin-0.4.0.jar
├── i18n/
│   └── translation.xml
├── images/
├── plugin.xml
└── extension.xml
```

### 4.4 Maven Plugins Used

| Plugin | Purpose |
|--------|---------|
| `maven-enforcer-plugin` | Validates JAVA_HOME is set |
| `maven-compiler-plugin` | Compiles Java 8 source code |
| `maven-surefire-plugin` | Runs unit tests with parallel execution |
| `maven-assembly-plugin` | Packages plugin ZIP file |
| `maven-resources-plugin` | Copies resources and renames extension.xml |
| `maven-antrun-plugin` | Renames extension.xml → dilaAIMarkupPlugin.xml |

---

## 5. Testing Infrastructure

### 5.1 Test Categories

| Test Class | Purpose | Count |
|------------|---------|-------|
| `LocalizationTest` | Resource bundle loading and i18n | Multiple |
| `ResourceBundleIntegrationTest` | Integration with Oxygen's i18n system | Multiple |
| `StandaloneLocalizationTest` | Standalone i18n testing | Multiple |
| `TranslationXmlValidatorTest` | XML translation file validation | Multiple |
| `TranslationConsistencyTest` | Translation key consistency | Multiple |
| `UTF8ValidationServiceTest` | UTF-8 validation and conversion | Multiple |
| `DAMAWorkspaceAccessPluginExtensionTest` | Workspace extension functionality | Multiple |

**Total Tests**: 99
**Status**: ✅ All passing (0 failures, 0 errors)

### 5.2 Test Dependencies

```xml
<!-- JUnit 4 -->
<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
  <version>4.13.2</version>
  <scope>test</scope>
</dependency>

<!-- Mockito for mocking -->
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <version>4.11.0</version>
  <scope>test</scope>
</dependency>

<!-- AssertJ for fluent assertions -->
<dependency>
  <groupId>org.assertj</groupId>
  <artifactId>assertj-core</artifactId>
  <version>3.24.2</version>
  <scope>test</scope>
</dependency>

<!-- Hamcrest for matchers -->
<dependency>
  <groupId>org.hamcrest</groupId>
  <artifactId>hamcrest</artifactId>
  <version>2.2</version>
  <scope>test</scope>
</dependency>
```

---

## 6. Key Technical Achievements

### 6.1 Migration Benefits

| Aspect | Before (JavaScript) | After (Java) | Improvement |
|--------|-------------------|--------------|-------------|
| **Accuracy** | 90% | 95% | +5% |
| **UI Responsiveness** | Blocking (freezes) | Non-blocking (async) | ✅ Major |
| **Error Handling** | Basic try-catch | Comprehensive categorization | ✅ Major |
| **Data Safety** | No backups | Automatic .utf8backup files | ✅ Critical |
| **BOM Detection** | ❌ None | ✅ UTF-8, UTF-16 LE/BE | ✅ Critical |
| **UTF-16 Detection** | ❌ None | ✅ Pattern recognition | ✅ Critical |
| **Performance** | JavaScript bridge overhead | Native Java | ✅ Improved |
| **Maintainability** | JS-Java bridge complexity | Pure Java | ✅ Simplified |

### 6.2 Code Quality Improvements

**From [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)**:
- Extracted `PluginLogger` utility class (~240 lines)
- Eliminated ~40 lines of duplicate debug code
- Added multiple log levels (DEBUG, INFO, WARN, ERROR)
- Centralized debug mode detection
- Improved consistency across all plugin classes

### 6.3 Safety Features

1. **File Corruption Prevention**:
   - Automatic backups before conversion
   - File size limits (50MB)
   - Read/write permission checks
   - Strict CharsetDecoder validation

2. **Error Recovery**:
   - Graceful handling of missing files
   - Continues processing on individual failures
   - Detailed error reporting per file

3. **User Workflow Safety**:
   - Two-stage workflow (Check → Review → Convert)
   - Cancel capability at any stage
   - Progress feedback during operations

---

## 7. Development History

### 7.1 Version Timeline

| Version | Status | Key Features |
|---------|--------|--------------|
| **0.2.4** | Legacy | JavaScript implementation, 90% UTF-8 accuracy |
| **0.4.0** | Current | Ref-to-Link action, CBRD preferences, improved validation |
| **0.3.0** | Previous | Pure Java, 95% accuracy, BOM detection, enterprise UX |

### 7.2 Major Milestones

1. **Initial Implementation**: JavaScript-based plugin with Rhino engine
2. **UTF-8 Service Extraction**: Created dedicated validation service
3. **Java Migration Phase 1**: Core UI framework (Swing components)
4. **Java Migration Phase 2**: UTF-8 tools integration
5. **Java Migration Phase 3**: AI Markup and LLM API integration
6. **Java Migration Phase 4**: Tag removal and options
7. **Java Migration Phase 5**: JavaScript cleanup and finalization
8. **Testing & Refinement**: 99 tests, systematic test failure fixing

---

## 8. Configuration Files

### 8.1 Plugin Descriptor ([src/main/resources/plugin.xml](src/main/resources/plugin.xml))
- Defines plugin metadata (name, version, description)
- Registers plugin workspace access point
- Declares internationalization resources

### 8.2 Extension Descriptor ([src/main/resources/extension.xml](src/main/resources/extension.xml))
- Registers options page extension
- Configures plugin extension points

### 8.3 Translation File ([src/main/resources/i18n/translation.xml](src/main/resources/i18n/translation.xml))
- 69 total translation keys
- Supports multiple languages (en_US, zh_TW, etc.)
- Organized by feature area (UI, actions, messages, errors)

---

## 9. Future Considerations

### 9.1 Potential Enhancements

From documentation analysis:

1. **Use Unused Translation Keys**: Implement detailed UTF-8 conversion dialog using 19 unused keys
2. **Enhanced Options Feedback**: Use 4 unused options dialog keys for better user feedback
3. **Additional Logging**: PluginLogger already supports file logging, rotation could be added
4. **Performance Metrics**: Add timing information to logs
5. **Remote Logging**: Send logs to remote server for support

### 9.2 Cleanup Opportunities

1. **Remove Unused Translation Keys**: 37 keys (53.6%) could be removed if not planned for future use
2. **Clean Extra Folders**: Remove `.github/`, `.settings/`, `lib/`, legacy `dilaAIMarkupPlugin/` if not needed
3. **Consolidate Test Files**: Move root `test-files/` to `src/test/resources/test-files/`

---

## 10. Summary

The DILA AI Markup Plugin is a mature, well-architected Oxygen XML Author plugin that has successfully migrated from a JavaScript-based implementation to a pure Java implementation. The current version (0.4.0) demonstrates enterprise-grade features including:

- **Professional UTF-8 validation** with 95% accuracy and BOM/UTF-16 detection
- **Ref-to-Link conversion** with safe `<ptr>` replacement and CBRD integration
- **Non-blocking UI** with background processing and progress feedback
- **Data safety** through automatic backups and comprehensive error handling
- **Complete test coverage** with 99 passing tests
- **Clean architecture** with centralized logging and proper separation of concerns
- **Comprehensive documentation** tracking implementation evolution and decision rationale

The plugin follows Maven best practices, has a clear project structure, and is ready for production deployment to assist users with AI-powered markup, tag processing, and UTF-8 encoding management in Oxygen XML Author.
