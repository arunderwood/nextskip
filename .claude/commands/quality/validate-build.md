---
description: Comprehensive pre-commit validation (Gradle, tests, Spring Boot, Vaadin)
allowed-tools: Read, Grep, Glob, Bash
---

## Purpose

This command validates the entire NextSkip stack (Gradle, Spring Boot, Vaadin) to ensure:
- Code compiles without errors
- All tests pass
- Spring Boot application starts successfully
- Vaadin frontend builds correctly
- No obvious issues that would break the build

## When to Use

- ✅ **Before committing changes** - Pre-commit safety check
- ✅ **After merging branches** - Ensure integration is clean
- ✅ **Before creating pull requests** - Verify PR is ready for review
- ✅ **When troubleshooting "works on my machine" issues** - Validate environment

## Usage

Simply invoke this command:

```
/validate-build
```

Or in conversation:
```
User: Validate the build
```

## What This Command Does

This command delegates to the **gradle-build-validator** agent to execute a systematic validation workflow:

### 1. Git Status Check
- Report any uncommitted changes
- List untracked files that should be committed (new source files)
- Warn if working tree is dirty

### 2. Gradle Clean Build
- Run `./gradlew clean build`
- Report build time
- Capture and display warnings
- Expected: Build SUCCESS

### 3. Test Execution
- Run `./gradlew test`
- Expected: 60/60 passing
- Report any failures with stack traces
- Display test execution time

### 4. Spring Boot Startup Validation
- Check for port 8080 conflicts (kill if necessary)
- Start application with `./gradlew bootRun`
- Monitor logs for successful startup
- Verify "Started NextskipApplication" message appears
- Gracefully stop application

### 5. Frontend Build Validation
- Verify `node_modules/` exists (run `npm install` if missing)
- Check for `vite.generated.ts` and `index.html`
- Verify no frontend console errors
- Confirm Vaadin build artifacts present

### 6. Summary Report

The validator will provide a summary like:

```
## NextSkip Build Validation Report

✅ Git Status: Clean working tree
✅ Build: SUCCESS (1m 23s)
✅ Tests: 60/60 passing (12s)
✅ Spring Boot: Starts successfully (8s)
✅ Frontend: Builds without errors

🎯 Overall Result: ✅ READY TO COMMIT

All validation checks passed. The build is healthy and ready for commit.
```

Or if issues are found:

```
## NextSkip Build Validation Report

✅ Git Status: Clean working tree
✅ Build: SUCCESS (1m 23s)
❌ Tests: 58/60 passing (2 failures)
  - NoaaSwpcClientTest.testFetchSolarIndices_Success
  - HamQslClientTest.testFetchBandConditions_Success

🎯 Overall Result: ❌ NEEDS FIXES

Please fix the test failures before committing.
Recommendation: Use /java-test-debugger to investigate failures.
```

## Expected Behavior

The command will:
1. Run the gradle-build-validator agent with full validation workflow
2. Present a clear summary of results (✅ for pass, ❌ for fail)
3. Provide actionable recommendations if issues are found
4. Give a final verdict: "READY TO COMMIT" or "NEEDS FIXES"

## Troubleshooting

**Port 8080 Already in Use**:
- The validator automatically kills conflicting processes
- If this fails, manually run: `lsof -ti :8080 | xargs kill -9`

**Tests Failing**:
- Use `/java-test-debugger` command to investigate root cause
- Check recent git changes that might have broken tests

**Frontend Build Issues**:
- Run `npm install` to ensure dependencies are present
- Verify Node.js version is 18+ with `node --version`
- Check `package.json` exists in project root

**Spring Boot Won't Start**:
- Check application logs in `/tmp/nextskip-boot.log`
- Look for exceptions or configuration errors
- Verify `application.properties` is correct

## Related Commands

- `/java-test-debugger` - Debug specific test failures
- `/plan-tests` - Plan additional test coverage
- `/find-refactor-candidates` - Identify code quality issues
- `/commit` - Create conventional commit after validation passes

## Example Workflow

```
User: I've finished implementing NOAA client improvements