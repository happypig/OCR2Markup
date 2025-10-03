# Git Workflow: UTF-8 Tool Implementation Strategy

## Branch Structure

### `main` branch
- **Status**: Contains the committed JavaScript UTF-8 tool implementation (v0.2.4)
- **Features**: Self-contained validation, 90% accuracy, production-ready
- **Last Commit**: `c5c82da` - feat: Implement UTF-8 Check/Convert Tool v0.2.4

### `feature/utf8-tool-js` branch
- **Purpose**: Preserve the stable JavaScript implementation
- **Status**: Reference branch for the working JavaScript solution
- **Use Case**: Rollback point and comparison baseline
- **Implementation**: JavaScript with Rhino engine, self-contained validation

### `feature/utf8-tool-java` branch (CURRENT)
- **Purpose**: Experimental Java conversion using StandalonePluginWorkspace API
- **Status**: Development branch for enhanced accuracy implementation
- **Target**: 95% accuracy using Java CharsetDecoder or ICU4J
- **Implementation**: Java with native Oxygen SDK integration

## Workflow Strategy

### Phase 1: Preserve Working Solution ✅
```bash
git add .
git commit -m "feat: Implement UTF-8 Check/Convert Tool v0.2.4"
git branch feature/utf8-tool-js
```

### Phase 2: Experiment with Java Implementation (CURRENT)
```bash
git checkout -b feature/utf8-tool-java
# Develop Java version using StandalonePluginWorkspace API
# Focus on enhanced accuracy and native integration
```

### Phase 3: Future Merge Strategy
```bash
# Option A: Java implementation proves superior
git checkout main
git merge feature/utf8-tool-java
git branch -d feature/utf8-tool-js

# Option B: JavaScript implementation remains optimal
git checkout main
# Keep current implementation, archive Java branch

# Option C: Hybrid approach
# Merge best features from both implementations
```

## Implementation Comparison

| Aspect | JavaScript (Current) | Java (Experimental) |
|--------|---------------------|-------------------|
| **Accuracy** | 90% | Target: 95% |
| **Dependencies** | Self-contained | ICU4J/Tika potential |
| **Integration** | Rhino engine | Native Oxygen SDK |
| **Maintenance** | Simple | More complex |
| **Performance** | Good | Potentially better |

## Decision Criteria

The final implementation will be chosen based on:
1. **Accuracy**: UTF-8 detection precision
2. **Reliability**: Error handling and edge cases
3. **Performance**: Processing speed and memory usage
4. **Maintainability**: Code complexity and dependencies
5. **Integration**: Compatibility with Oxygen SDK

## Current Status
- ✅ JavaScript implementation: Complete and functional
- 🔄 Java implementation: In development on `feature/utf8-tool-java`
- ⏳ Performance comparison: Pending
- ⏳ Final merge decision: Pending

## Usage Commands
```bash
# Switch to JavaScript version
git checkout feature/utf8-tool-js

# Switch to Java experimental version  
git checkout feature/utf8-tool-java

# Return to main (current JavaScript)
git checkout main

# View all branches
git branch -a
```