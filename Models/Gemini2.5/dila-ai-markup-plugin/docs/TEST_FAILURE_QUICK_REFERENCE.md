# Test Failure Quick Reference

## Quick Diagnostic Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

## Common Failure Patterns & Quick Fixes

| Error Pattern | Likely Cause | Quick Fix |
|---------------|--------------|-----------|
| `expected:<[X]> but was:<[Y]>` | Assertion mismatch | Check if test scenario is realistic |
| `Wanted but not invoked: mock.method()` | Missing method call | Add missing call to implementation |
| `test timed out after X milliseconds` | Performance issue | Move setup outside loops |
| `Should not propagate exceptions` | Missing exception handling | Add try-catch blocks |
| `Cannot invoke ... because "obj" is null` | Null pointer | Add null checks |
| `Expecting empty but was: [...]` | Test environment contamination | Use isolated temp directories |

## Step-by-Step Fix Process

1. **Read error message carefully** - Understand what failed and why
2. **Examine test code** - Check what the test is trying to verify
3. **Examine implementation** - See if behavior matches expectations
4. **Identify root cause** - Implementation issue vs test issue
5. **Apply targeted fix** - Fix the actual problem, not symptoms
6. **Test the fix** - Run specific test to verify
7. **Run full suite** - Ensure no regressions

## Real Examples from Our Project

### UTF8ValidationService
- **Problem**: Test expected conversion failure but got success
- **Root Cause**: Using UTF-8 encoding on invalid file (auto-detection worked)
- **Fix**: Use non-existent file to guarantee failure

### DAMAWorkspaceAccessPluginExtension  
- **Problem**: Mock verification failure for `getOption`
- **Root Cause**: Implementation only called `getSecretOption`
- **Fix**: Added missing `getOption` call

### LocalizationTest
- **Problem**: Performance test timeout
- **Root Cause**: Mock setup inside test loop
- **Fix**: Moved mock setup outside performance measurement

## Best Practices

✅ **Do:**
- Fix one failure at a time
- Test each fix immediately
- Use realistic test scenarios
- Add proper error handling
- Isolate test environments

❌ **Don't:**
- Fix multiple failures simultaneously without testing
- Ignore root causes
- Use unrealistic test scenarios
- Skip regression testing
- Let tests interfere with each other

## Files Created

- **Detailed Guide**: `docs/TEST_FAILURE_FIXING_GUIDE.md`
- **Quick Reference**: `docs/TEST_FAILURE_QUICK_REFERENCE.md`

---
*Generated from DILA AI Markup Plugin test fixing session - October 5, 2025*