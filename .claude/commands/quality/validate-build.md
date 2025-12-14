---
description: Comprehensive pre-commit validation (Gradle, tests, Spring Boot, Vaadin, frontend)
argument-hint: ""
allowed-tools: Bash, Read, Grep, Glob
---

**⚠️ CRITICAL EXECUTION INSTRUCTION**: This command requires you to EXECUTE actual validation steps using the Bash tool. DO NOT just show example output or documentation. You MUST run real Gradle commands and npm commands, and report actual results.

## Purpose

This command validates the entire NextSkip stack (Gradle, Spring Boot, Vaadin, React) to ensure:
- Backend code compiles without errors
- All backend tests pass
- All frontend tests pass
- Quality checks complete successfully
- Build artifacts are generated
- **Application starts successfully with no runtime exceptions**
- No obvious issues that would break the build

## When to Use

- ✅ **Before committing changes** - Pre-commit safety check
- ✅ **After merging branches** - Ensure integration is clean
- ✅ **Before creating pull requests** - Verify PR is ready for review
- ✅ **When troubleshooting builds** - Validate environment

## Validation Workflow

Execute these steps IN ORDER. Use the Bash tool to run each command and report actual results:

### Step 1: Git Status Check

**Execute**: `git status`

**Report**:
- Modified files (if any)
- Untracked files (if any)
- Working tree state (clean/dirty)

### Step 2: Frontend Tests

**Execute**: `npm run test:run`

This runs all frontend tests including:
- Unit tests (priority calculation algorithm)
- Component tests (BentoCard, BentoGrid)
- Accessibility tests (WCAG 2.1 AA compliance)
- Integration tests

**Report**:
- Test result (PASS or FAIL)
- Test count (X passing)
- Test duration (in seconds or milliseconds)
- Any test failures with error messages

**Expected**: All tests passing in ~1-2 seconds

### Step 3: Gradle Clean Build

**Execute**: `time ./gradlew clean build`

This runs the full build including:
- Compilation (Java 25 bytecode)
- Backend test execution (all tests)
- Quality checks (Checkstyle, PMD, SpotBugs, JaCoCo)
- JAR packaging

**Report**:
- Build result (SUCCESS or FAILURE)
- Build duration (in seconds)
- Compilation warnings/errors
- Test results (X passing)
- Quality violation counts:
  - Checkstyle (main + test)
  - PMD (main + test)
  - SpotBugs status
- Any build failures with error messages

**Expected**: BUILD SUCCESSFUL in 10-15 seconds

### Step 4: Verify Build Artifacts

**Execute**: `ls -lh build/libs/*.jar`

**Report**:
- JAR filename (should be `nextskip-0.0.1-SNAPSHOT.jar`)
- JAR file size
- Existence of test reports at `build/reports/tests/test/`

### Step 5: Runtime Validation (Required)

**⚠️ CRITICAL**: Starting the application is REQUIRED, not optional. Runtime exceptions (dependency injection issues, configuration errors, bean initialization failures) will NOT be caught by tests alone.

**Step 5a: Clear Port 8080**

**Execute**: `lsof -ti :8080 | xargs kill -9`

(This will silently succeed even if no process is running)

**Step 5b: Start Application in Background**

**Execute**: `./gradlew bootRun > /tmp/bootrun.log 2>&1 &`

**Step 5c: Wait and Check Logs**

**Execute**: `sleep 15 && tail -100 /tmp/bootrun.log`

**Report**:
- Startup result (SUCCESS or FAILURE)
- Startup time (in seconds)
- Any runtime exceptions or errors
- Spring Boot version and port
- Vite frontend compilation status
- TypeScript errors (should be 0)
- Application URL confirmation

**Step 5d: Stop Application**

**Execute**: `lsof -ti :8080 | xargs kill -9`

**Expected**:
- Application starts in 5-10 seconds
- No runtime exceptions
- TypeScript: 0 errors
- "Started NextSkipApplication" message in logs

## Summary Report Format

After executing ALL validation steps, provide this structured summary using ACTUAL results:

```
## NextSkip Build Validation Report

📊 Git Status: [Clean / X modified, Y untracked files]
🎨 Frontend Tests: [SUCCESS/FAILURE] ([actual count]/90 passing, [actual duration])
🔨 Backend Build: [SUCCESS/FAILURE] ([actual duration]s)
✅ Backend Tests: [actual count]/91 passing ([test duration]s)
📋 Quality Violations:
  - Checkstyle: [actual] main, [actual] test
  - PMD: [actual] main, [actual] test
  - SpotBugs: [actual status]
📦 Artifacts: [actual JAR name] ([actual size])
🚀 Runtime Validation: [SUCCESS/FAILURE] (started in [X]s, TypeScript: [N] errors)

🎯 Overall Result: [✅ READY TO COMMIT / ❌ NEEDS FIXES]

[Provide specific recommendations based on actual results]
```

### Example of Actual Report (Not Documentation):

```
## NextSkip Build Validation Report

📊 Git Status: 1 modified (BentoCard.tsx)
🎨 Frontend Tests: SUCCESS (all passing, ~900ms)
🔨 Backend Build: SUCCESS (~12s)
✅ Backend Tests: all passing (~7s)
📋 Quality Violations:
  - Checkstyle: low warnings main, moderate warnings test
  - PMD: moderate violations main and test
  - SpotBugs: exit code 1 (non-blocking)
📦 Artifacts: nextskip-0.0.1-SNAPSHOT.jar (~82MB)
🚀 Runtime Validation: SUCCESS (started in ~7s, TypeScript: 0 errors)

🎯 Overall Result: ✅ READY TO COMMIT

All validation checks passed. Quality violations are within acceptable limits.
Frontend accessibility tests (WCAG 2.1 AA) passing.
Application starts without runtime exceptions.
```

## Troubleshooting

**Port 8080 Already in Use**:
```bash
lsof -ti :8080 | xargs kill -9
```

**Stale Build Artifacts**:
```bash
./gradlew clean
```

**Frontend Test Failures**:
- Check for import path issues (should use `Frontend/` alias)
- Verify vitest.config.ts is correctly configured
- Check that test files are in `frontend/tests/` directory
- Run `npm test` in watch mode to see detailed errors

**Backend Test Failures**:
- Use `/java-test-debugger` to investigate specific test failures
- Check recent git changes that might have broken tests

**Quality Check Failures**:
- Checkstyle/PMD violations are reported but don't fail the build (ignoreFailures = true)
- SpotBugs may show Java 25 compatibility warnings (non-blocking)

**npm Command Not Found**:
- Ensure Node.js and npm are installed
- Run `npm install` to install dependencies

**Runtime Startup Failures**:
- Check `/tmp/bootrun.log` for full error messages and stack traces
- Common issues:
  - Bean initialization failures (dependency injection errors)
  - Configuration property errors (missing or invalid application.yml values)
  - Database connection issues (if using external DB)
  - Port conflicts (use `lsof -ti :8080` to check)
- Verify all required environment variables are set
- Check that gradle.properties settings are compatible with the application

## Key Principles for Execution

1. **Use Bash tool** - Execute commands with `Bash` tool, don't simulate
2. **Report real data** - Show actual build times, test counts, violation numbers from command output
3. **Be specific** - If failures occur, include actual error messages and stack traces
4. **Provide evidence** - Include relevant snippets from command output
5. **Give clear verdict** - State clearly: READY TO COMMIT or NEEDS FIXES
6. **Run frontend tests first** - Catch quick failures early before running longer Gradle build

## Success Criteria

- ✅ Frontend tests complete with all tests passing
- ✅ Backend build completes with SUCCESS status
- ✅ All backend tests pass
- ✅ JAR artifact is generated (~80MB)
- ✅ Quality violations within acceptable range (non-blocking)
- ✅ **Application starts successfully without runtime exceptions**
- ✅ TypeScript: 0 errors
- ✅ No critical compilation errors

## Related Commands

After successful validation:
- `/commit` - Create conventional commit for your changes

If validation fails:
- `/java-test-debugger` - Debug specific backend test failures
- `/find-refactor-candidates` - Identify code quality issues
- `/plan-tests` - Plan additional test coverage

## Notes

**Frontend Test Details**:
- Tests located in: `frontend/tests/`
- Test configuration: `vitest.config.ts`
- Test setup: `frontend/test/setup.ts`
- Coverage target: 80%+ statements/functions/lines

**Backend Test Details**:
- Tests located in: `src/test/java/`
- Test configuration: `build.gradle` (JUnit 5 platform)
- Coverage target: 80%+ (JaCoCo)
