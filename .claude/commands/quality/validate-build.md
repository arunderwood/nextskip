---
description: Comprehensive pre-commit validation (Gradle, tests, Spring Boot, Vaadin, frontend)
argument-hint: ""
allowed-tools: Bash, Read, Grep, Glob
---

**⚠️ CRITICAL EXECUTION INSTRUCTION**: This command requires you to EXECUTE actual validation steps using the Bash tool. DO NOT just show example output or documentation. You MUST run real Gradle commands and npm commands, and report actual results.

## Purpose

This command validates the entire NextSkip stack (Gradle, Spring Boot, Vaadin, React) to ensure:
- Backend code compiles without errors
- All backend tests pass (expected: 60/60)
- All frontend tests pass (expected: 90/90)
- Quality checks complete successfully
- Build artifacts are generated
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
- Test count (X/90 passing)
- Test duration (in seconds or milliseconds)
- Any test failures with error messages

**Expected**: 90/90 tests passing in ~1 second

### Step 3: Gradle Clean Build

**Execute**: `time ./gradlew clean build`

This runs the full build including:
- Compilation (Java 25 → Java 21 bytecode)
- Backend test execution (all 60 tests)
- Quality checks (Checkstyle, PMD, SpotBugs, JaCoCo)
- JAR packaging

**Report**:
- Build result (SUCCESS or FAILURE)
- Build duration (in seconds)
- Compilation warnings/errors
- Test results (X/60 passing)
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

### Step 5: Check for Port Conflicts (Optional)

If Spring Boot startup validation is needed:

**Execute**: `lsof -ti :8080`

If port 8080 is in use, kill the process:
**Execute**: `lsof -ti :8080 | xargs kill -9`

## Summary Report Format

After executing ALL validation steps, provide this structured summary using ACTUAL results:

```
## NextSkip Build Validation Report

📊 Git Status: [Clean / X modified, Y untracked files]
🎨 Frontend Tests: [SUCCESS/FAILURE] ([actual count]/90 passing, [actual duration])
🔨 Backend Build: [SUCCESS/FAILURE] ([actual duration]s)
✅ Backend Tests: [actual count]/60 passing ([test duration]s)
📋 Quality Violations:
  - Checkstyle: [actual] main, [actual] test
  - PMD: [actual] main, [actual] test
  - SpotBugs: [actual status]
📦 Artifacts: [actual JAR name] ([actual size])

🎯 Overall Result: [✅ READY TO COMMIT / ❌ NEEDS FIXES]

[Provide specific recommendations based on actual results]
```

### Example of Actual Report (Not Documentation):

```
## NextSkip Build Validation Report

📊 Git Status: 1 modified (BentoCard.tsx)
🎨 Frontend Tests: SUCCESS (90/90 passing, 907ms)
🔨 Backend Build: SUCCESS (12.4s)
✅ Backend Tests: 60/60 passing (3.2s)
📋 Quality Violations:
  - Checkstyle: 5 warnings main, 59 warnings test
  - PMD: 62 violations main, 70 violations test
  - SpotBugs: exit code 4 (Java 25 warnings, non-blocking)
📦 Artifacts: nextskip-0.0.1-SNAPSHOT.jar (89MB)

🎯 Overall Result: ✅ READY TO COMMIT

All validation checks passed. Quality violations are within acceptable limits.
Frontend accessibility tests (WCAG 2.1 AA) passing.
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

## Key Principles for Execution

1. **Use Bash tool** - Execute commands with `Bash` tool, don't simulate
2. **Report real data** - Show actual build times, test counts, violation numbers from command output
3. **Be specific** - If failures occur, include actual error messages and stack traces
4. **Provide evidence** - Include relevant snippets from command output
5. **Give clear verdict** - State clearly: READY TO COMMIT or NEEDS FIXES
6. **Run frontend tests first** - Catch quick failures early before running longer Gradle build

## Success Criteria

- ✅ Frontend tests complete with 90/90 passing
- ✅ Backend build completes with SUCCESS status
- ✅ All 60 backend tests pass
- ✅ JAR artifact is generated (~89MB)
- ✅ Quality violations within acceptable range:
  - Checkstyle: < 100 total warnings
  - PMD: < 150 total violations
  - No critical compilation errors

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
