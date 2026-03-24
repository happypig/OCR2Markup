# Planning Summary: Ref to Link Action

**Branch**: `001-ref-to-link-action` | **Date**: 2026-01-05 | **Status**: ✅ Planning Complete

---

## ✅ `/speckit.plan` Execution Complete

### Branch
**`001-ref-to-link-action`**

### Implementation Plan
📄 [plan.md](plan.md)

---

## 📦 Generated Artifacts

### Phase 0: Research ✅
📄 **[research.md](research.md)** (1,200+ lines)
- **CBETA API**: CBRD REST API at `https://cbss.dila.edu.tw/cbrd/link`
  - GET request with XML query parameter `q`
  - Requires `Referer: CBRD@dila.edu.tw` header
  - Returns JSON: `{"success": true, "found": ["url"]}`
- **XML Parsing**: Java DOM (`DocumentBuilder`)
- **Oxygen SDK**: Existing DAMA plugin patterns

### Phase 1: Design & Contracts ✅
📄 **[data-model.md](data-model.md)** (900+ lines)
- 4 Domain entities with complete field definitions
- 2 Domain services with transformation logic
- State machine with 9 states and terminal conditions
- Comprehensive validation rules (4 layers)
- i18n message keys (14 error messages, 3 success messages)
- Example workflow with real data

📄 **[contracts/cbrd-api.yaml](contracts/cbrd-api.yaml)** (OpenAPI 3.0)
- Complete API specification for CBRD API
- Request/response schemas with examples
- Security scheme (Referer header)
- Error response definitions
- 5 response scenarios documented

📄 **[quickstart.md](quickstart.md)** (400+ lines)
- Developer quick reference guide
- Architecture diagram (text-based)
- 13-step workflow summary
- Transformation tables (canon, numerals, columns)
- 6-phase development checklist
- 7 common pitfalls
- Performance targets

📄 **Agent Context Updated**
- ✅ [CLAUDE.md](../../CLAUDE.md) - Claude Code context
- ✅ [AGENTS.md](../../AGENTS.md) - Codex CLI context

---

## 🔑 Key Highlights

### Critical API Discovery
**CBRD (CBETA Reference Detection) REST API** officially documented:
```
Endpoint: https://cbss.dila.edu.tw/cbrd/link
Method: GET with ?q=<ref>...</ref>
Auth: Referer: CBRD@dila.edu.tw
Response: {"success": true, "found": ["url"]}
```

**Impact**: This was a critical correction during planning. Initial research suggested no formal API existed, but user clarification revealed the official CBRD API, significantly simplifying the implementation approach.

### Architecture Summary
```
UI Layer (Swing)
   ↓
Application Layer (ConvertReferenceCommand)
   ↓
Domain Layer (ReferenceParser, ComponentTransformer)
   ↓
Infrastructure Layer (CBRDAPIClient)
```

**Clean Architecture**: Three-layer separation ensures:
- Domain logic is technology-agnostic
- Application layer orchestrates workflow
- Infrastructure handles external dependencies

### Required Transformations

| From Document | To API | Transformation | Example |
|--------------|--------|----------------|---------|
| Canon Names | Canon Codes | Mapping table | 大正蔵 → T, 続蔵 → X |
| CJK Numerals | Arabic Numerals | Character conversion | 二四 → 24, 一・一六 → 1.16 |
| Column Positions | Column Codes | Position mapping | 上 → a, 中 → b, 下 → c, 左上 → a |

**Implementation Complexity**: The transformation layer is a key component that bridges the gap between document format (human-readable Chinese/Japanese text) and API format (standardized codes).

---

## 📊 Constitution Check: 10/10 ✅

All constitutional principles verified and documented:

- ✅ **Principle I: Event Storming First**
  - 14 domain events, 4 commands, 1 aggregate identified
  - Complete event flow from selection to replacement

- ✅ **Principle II: Problem Frames for Requirements**
  - Problem/solution domains separated
  - Transformation problem type identified
  - Shared phenomena documented

- ✅ **Principle III: Clean Architecture**
  - Three layers: Domain, Application, Infrastructure
  - Clear dependency direction (outer → inner)
  - Domain logic isolated from frameworks

- ✅ **Principle IV: Test-Driven Development**
  - BDD acceptance scenarios in Given-When-Then format
  - Test-first structure planned for tasks
  - Unit, integration, and E2E tests specified

- ✅ **Principle V: Domain-Driven Design**
  - Ubiquitous language: Tripitaka Reference, CBETA Link, Reference Components
  - Value objects: `TripitakaComponents`, `TransformedComponents`
  - Aggregate root: `ReferenceConversionSession`
  - Domain services: `ReferenceParser`, `ComponentTransformer`

- ✅ **Principle VI: Command-Query Separation**
  - Command: `ConvertReferenceCommand` (mutates session state)
  - Queries not needed (stateless operations)

- ✅ **Principle VII: Defensive Programming**
  - Multi-layer validation (selection → parsing → transformation → API)
  - Null checks for Oxygen SDK returns
  - Error handling with i18n messages at each stage

- ✅ **Principle VIII: Async-First Design**
  - API calls use `CompletableFuture`
  - UI updates wrapped in `SwingUtilities.invokeLater()`
  - Never block Event Dispatch Thread

- ✅ **Principle IX: Comprehensive i18n**
  - 3 languages: en_US, zh_CN, zh_TW
  - 17 translation keys defined
  - All user-facing messages internationalized

- ✅ **Principle X: Continuous Verification**
  - JUnit 4 + Mockito + AssertJ test stack
  - `mvn test` after each change
  - Tests must pass before task completion

---

## 🎯 Next Steps

### 1. Generate Implementation Tasks
```bash
/speckit.tasks
```
This will create `tasks.md` with:
- Dependency-ordered tasks
- Test-first structure (SPEC → UNIT → INTEGRATION → Implementation)
- Estimated 15-25 tasks

### 2. Implementation Order

**Phase 1: Domain Layer** (TDD)
- `TripitakaComponents` value object
- `TransformedComponents` value object
- `ReferenceConversionSession` aggregate root
- `ReferenceParser` service
- `ComponentTransformer` service
- Domain exceptions
- Unit tests for all domain classes

**Phase 2: Infrastructure Layer**
- `CBRDAPIClient` HTTP client
- `CBRDResponse` DTO
- `CBRDAPIException`
- JSON parsing logic
- Integration tests with mock HTTP

**Phase 3: Application Layer**
- `ConvertReferenceCommand` command handler
- Workflow orchestration
- Integration tests

**Phase 4: UI Layer**
- Add `REF_TO_LINK` to `OperationType` enum
- `RefToLinkActionListener` inner class
- Menu item integration
- UI event handlers
- i18n translation keys
- UI integration tests

**Phase 5: Configuration**
- CBRD API configuration in preferences
- `WSOptionsStorage` integration
- Default configuration values

**Phase 6: End-to-End Testing**
- Real Oxygen XML Editor testing
- Real CBRD API testing
- Error scenario testing
- Performance testing (<3s API response)
- User acceptance testing

---

## 📈 Complexity Estimate

### Code Volume
- **Production Code**: ~500-800 lines
  - Domain layer: ~200 lines
  - Infrastructure layer: ~150 lines
  - Application layer: ~100 lines
  - UI layer: ~150 lines
  - Configuration: ~50 lines

- **Test Code**: ~800-1200 lines
  - Unit tests: ~400 lines
  - Integration tests: ~300 lines
  - UI tests: ~200 lines
  - E2E tests: ~100 lines

### Time Estimate
- **Implementation**: 3-5 days (with TDD)
  - Domain layer: 1 day
  - Infrastructure layer: 1 day
  - Application + UI layer: 1-2 days
  - Configuration + i18n: 0.5 day

- **Testing**: 2-3 days
  - Unit + integration: 1 day
  - UI + E2E: 1-2 days

- **Total**: 5-8 days (1-1.5 weeks)

### Risk Factors
- **Low Risk**: Domain and application layers (pure Java, no external dependencies)
- **Medium Risk**: Infrastructure layer (HTTP client, JSON parsing, error handling)
- **Medium Risk**: UI layer (Oxygen SDK integration, thread safety)
- **High Risk**: CJK numeral conversion (complex logic, edge cases)
- **High Risk**: CBRD API behavior (external dependency, assumptions need validation)

---

## 📚 Documentation Structure

```
specs/001-ref-to-link-action/
├── spec.md                    ✅ Feature specification (Event Storming, Problem Frames, BDD scenarios)
├── plan.md                    ✅ Implementation plan (Technical Context, Constitution Check)
├── research.md                ✅ Research findings (1200+ lines)
│                                 - CBRD API specification
│                                 - XML parsing approaches
│                                 - Oxygen SDK integration patterns
├── data-model.md              ✅ Data model (900+ lines)
│                                 - 4 entities, 2 services
│                                 - State machine (9 states)
│                                 - Validation rules
│                                 - i18n message keys
├── quickstart.md              ✅ Quick start guide (400+ lines)
│                                 - Architecture overview
│                                 - Workflow summary
│                                 - Transformation tables
│                                 - Development checklist
├── PLAN_SUMMARY.md            ✅ This file (planning summary)
├── contracts/
│   └── cbrd-api.yaml          ✅ OpenAPI 3.0 spec for CBRD API
└── checklists/
    └── requirements.md        ✅ Spec quality checklist
```

**Total Documentation**: ~3,400+ lines across 8 files

---

## 🔬 Technical Deep Dive

### CBRD API Integration

**Endpoint**: `https://cbss.dila.edu.tw/dev/cbrd/link`

**Request Example**:
```http
GET /dev/cbrd/link?q=%3Cref%3E%3Ccanon%3ET%3C%2Fcanon%3E%3Cv%3E25%3C%2Fv%3E.%3Cw%3E1514%3C%2Fw%3E%3C%2Fref%3E
Referer: CBRD@dila.edu.tw
```

**Decoded Query**:
```xml
<ref>
  <canon>T</canon>
  <v>25</v>
  <w>1514</w>
</ref>
```

**Response Example**:
```json
{
  "success": true,
  "found": ["https://cbetaonline.dila.edu.tw/T1514"]
}
```

**Error Scenarios**:
1. **Timeout** (configured window with retries): Network unreachable or slow response
2. **HTTP 400**: Invalid XML query format
3. **HTTP 401**: Missing or invalid Referer header
4. **HTTP 500**: Server error
5. **Success with empty results**: Valid request but no matching URLs
6. **Success: false**: Processing error with error message

### State Machine

```
INITIALIZED → PARSING → TRANSFORMING → CALLING_API → COMPLETED → REPLACED
                ↓           ↓              ↓
          PARSING_FAILED TRANSFORM_FAILED API_FAILED
                                           ↓
                                      NO_RESULTS
```

**Terminal States**:
- `PARSING_FAILED`: Invalid XML structure
- `TRANSFORM_FAILED`: Cannot map canon/numerals/columns
- `API_FAILED`: Network or HTTP error
- `NO_RESULTS`: No CBETA URLs found (valid state)
- `REPLACED`: Success - changes applied to document

**Non-Terminal States**:
- `INITIALIZED`: Session created, ready to parse
- `PARSING`: Extracting components from XML
- `TRANSFORMING`: Converting to API format
- `CALLING_API`: HTTP request in progress
- `COMPLETED`: URL ready, waiting for user to click Replace

### Data Flow

```
1. User Selection (Oxygen Editor)
   ↓
2. Raw XML String
   "<ref><canon>続蔵</canon><v>一・一六</v>..."
   ↓
3. ReferenceParser (Domain)
   TripitakaComponents(canon="続蔵", volume="一・一六", page="二四九", column="左上")
   ↓
4. ComponentTransformer (Domain)
   TransformedComponents(canonCode="X", volume="1.16", page="249", column="a")
   ↓
5. CBRDAPIClient (Infrastructure)
   HTTP GET https://cbss.dila.edu.tw/dev/cbrd/link?q=<ref><canon>X</canon>...
   ↓
6. CBRD API Response
   {"success": true, "found": ["https://cbetaonline.dila.edu.tw/X0116_p0249a"]}
   ↓
7. Display in resultArea (UI)
   ↓
8. User clicks Replace
   ↓
9. Document Update (Oxygen Editor)
   <ref checked="2"><ptr href="https://cbetaonline.dila.edu.tw/X0116_p0249a"/></ref>
```

---

## 🎓 Key Learnings

### 1. Specification Quality
- **Event Storming** provided clear understanding of domain events and workflow
- **Problem Frames** helped separate problem domain (Buddhist texts) from solution domain (plugin code)
- **BDD scenarios** give concrete acceptance criteria for testing

### 2. Research Phase Value
- Initial assumption about "no API" was corrected by user input
- Research prevented wasted effort on URL construction logic
- Oxygen SDK pattern analysis saved time by reusing existing infrastructure

### 3. Clean Architecture Benefits
- Domain layer is completely independent of Oxygen SDK and HTTP libraries
- Easy to test domain logic in isolation
- Can swap HTTP client or JSON parser without affecting domain
- UI changes don't impact business logic

### 4. State Machine Clarity
- 9 states provide clear tracking of conversion progress
- Terminal states make error handling explicit
- Non-terminal states enable UI progress indicators
- State transitions enforce workflow invariants

### 5. i18n Early Planning
- Defining translation keys during planning ensures consistency
- 3 languages (en_US, zh_CN, zh_TW) require careful message design
- Error messages must be helpful in all languages

---

## ⚠️ Risks & Mitigations

### Risk 1: CBRD API Assumptions
**Risk**: API behavior may differ from documentation

**Mitigation**:
- Early integration testing with real API
- Mock responses for unit tests
- Configurable API endpoint for different environments
- Comprehensive error handling for unexpected responses

### Risk 2: CJK Numeral Conversion
**Risk**: Complex conversion logic with edge cases

**Mitigation**:
- TDD approach with comprehensive test cases
- Document all conversion rules in data-model.md
- Extensible mapping tables for new patterns
- Clear error messages when conversion fails

### Risk 3: Oxygen SDK Thread Safety
**Risk**: UI updates from background threads

**Mitigation**:
- Always use `SwingUtilities.invokeLater()` for UI updates
- Use `CompletableFuture` for async operations
- Follow existing DAMA plugin patterns exactly
- Integration tests with mock Oxygen SDK

### Risk 4: Canon/Column Mapping Completeness
**Risk**: Unknown canons or column positions in documents

**Mitigation**:
- Extensible mapping tables
- Clear error messages for unknown values
- Logging for unknown patterns (helps discover new cases)
- User can report issues to add new mappings

### Risk 5: Performance (configured API timeout with retries)
**Risk**: CBRD API may be slow or unreachable

**Mitigation**:
- Configurable timeout (default 10000ms per attempt)
- Async execution (never block UI)
- Clear timeout error message
- Automatic retry on timeouts (up to 3 attempts with backoff)
- Manual retry via Convert button (after error)

---

## ✨ Summary

**Planning Phase Complete**: All research, design, and contracts are documented and ready for implementation. The feature follows Clean Architecture principles with clear separation of concerns across domain, application, and infrastructure layers.

**Key Decision**: Using official CBRD REST API instead of manual URL construction ensures reliability and correctness. This was a critical correction made during planning based on user input.

**Ready for**: Task generation with `/speckit.tasks` to create implementation roadmap with dependency-ordered, test-first tasks.

**Documentation Quality**: 3,400+ lines of comprehensive documentation across 8 files ensure developers have all information needed for implementation.

**Constitutional Compliance**: 10/10 principles verified - Event Storming, Problem Frames, Clean Architecture, TDD, DDD, CQRS, Defensive Programming, Async-First, i18n, Continuous Verification.

**Estimated Effort**: 5-8 days (1-1.5 weeks) for complete implementation with TDD approach, including domain layer, infrastructure, UI, and comprehensive testing.

---

**Date**: 2026-01-05
**Branch**: `001-ref-to-link-action`
**Next Command**: `/speckit.tasks`
