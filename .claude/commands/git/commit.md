---
description: Create conventional commits with NextSkip scopes
argument-hint: [message]
allowed-tools: Bash(git *), Read, Grep
---

## Purpose

This command helps create well-structured, semantic commit messages that:
- Follow Conventional Commits specification
- Enable automatic changelog generation
- Support semantic versioning
- Improve git history readability
- Clearly communicate intent

## When to Use

- ✅ **Every commit** - All commits should follow this format
- ✅ **After /validate-build passes** - Ensure build is healthy
- ✅ **When feature work is complete** - Logical commit points
- ✅ **After refactoring** - Document improvements

## Usage

```
/commit
/commit the api improvements
/commit fix for date parsing bug
```

Or in conversation:
```
User: Commit these changes
User: Create a commit for the refactoring
```

## Conventional Commit Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types for NextSkip

**feat**: New feature (triggers minor version bump)
- Example: `feat(propagation): add VOACAP data source`
- Example: `feat(dashboard): display K-index trend graph`

**fix**: Bug fix (triggers patch version bump)
- Example: `fix(api): handle NOAA partial date formats`
- Example: `fix(dashboard): correct band condition color coding`

**refactor**: Code change without behavior change (no version bump)
- Example: `refactor(client): extract XML parsing to utility`
- Example: `refactor(dto): convert SolarIndices to record`

**perf**: Performance improvement (triggers patch version bump)
- Example: `perf(cache): increase TTL to 30 minutes`
- Example: `perf(api): batch API requests`

**test**: Adding or modifying tests (no version bump)
- Example: `test(hamqsl): add XML DOCTYPE handling test`
- Example: `test(integration): add Spring Boot context loading test`

**docs**: Documentation only (no version bump)
- Example: `docs(readme): add API client architecture diagram`
- Example: `docs(javadoc): document SolarIndices validation`

**style**: Code formatting, no logic change (no version bump)
- Example: `style(dto): apply consistent formatting`
- Example: `style: fix Checkstyle violations`

**build**: Build system or dependencies (no version bump)
- Example: `build(gradle): upgrade Spring Boot to 3.4.1`
- Example: `build(deps): add Checkstyle plugin`

**ci**: CI/CD configuration (no version bump)
- Example: `ci(github): add test workflow`
- Example: `ci(actions): enable automatic dependency updates`

**chore**: Maintenance tasks (no version bump)
- Example: `chore(gitignore): add node_modules`
- Example: `chore: clean up TODO comments`

### Scopes for NextSkip

Scopes identify the area of the codebase affected:

**propagation**: Propagation data and band conditions
- `feat(propagation): add solar cycle prediction`

**api**: External API clients (NOAA, HamQSL)
- `fix(api): retry on timeout errors`

**dashboard**: Frontend/UI components
- `feat(dashboard): add dark mode toggle`

**config**: Spring Boot configuration
- `refactor(config): externalize API URLs`

**dto**: Data transfer objects
- `feat(dto): add validation to NoaaSolarCycleEntry`

**test**: Test infrastructure
- `test: configure mutation testing`

**common**: Shared utilities and models
- `refactor(common): extract frequency band enum`

### Breaking Changes

Indicate breaking changes that require major version bump:

**Option 1: Exclamation Mark**:
```
feat(api)!: change SolarIndices to return Optional

BREAKING CHANGE: fetchSolarIndices() now returns Optional<SolarIndices>
instead of nullable SolarIndices. Consumers must update to use .orElse()
or .orElseThrow().
```

**Option 2: Footer**:
```
feat(api): change SolarIndices return type

BREAKING CHANGE: fetchSolarIndices() now returns Optional<SolarIndices>
```

---

## Workflow

### 1. Analyze Changes

Review what will be committed:

```bash
git status
git diff --staged
```

### 2. Group by Scope

Organize changes into logical commits by feature area:

- **Don't**: Mix propagation logic changes with dashboard UI changes in one commit
- **Do**: Separate commits for each scope

Example:
```bash
# Commit 1: API improvements
git add src/main/java/io/nextskip/propagation/internal/NoaaSwpcClient.java
git commit -m "refactor(api): extract date parsing logic"

# Commit 2: DTO changes
git add src/main/java/io/nextskip/propagation/internal/NoaaSolarCycleEntry.java
git commit -m "feat(dto): add validation to NoaaSolarCycleEntry"

# Commit 3: Test updates
git add src/test/java/io/nextskip/propagation/internal/NoaaSwpcClientTest.java
git commit -m "test(api): update tests for new date parsing"
```

### 3. Draft Message

For each group, create a commit message with:

**Type**: What kind of change (feat, fix, refactor, etc.)
**Scope**: Which area (propagation, api, dashboard, etc.)
**Description**: What was done (imperative mood, <50 chars)

**Optional Body** (if needed to explain why):
- Wrap at 72 characters
- Explain motivation for change
- Reference issues if applicable

**Optional Footer** (for breaking changes or issue refs):
- `BREAKING CHANGE: description`
- `Fixes #123`
- `Closes #456`

### 4. Create Commit

**Simple Commit** (description only):
```bash
git add [files]
git commit -m "feat(propagation): add K-index alerting"
```

**Commit with Body**:
```bash
git commit -m "$(cat <<'EOF'
refactor(api): extract XML parsing to utility class

The XML parsing logic in HamQslClient was duplicated and hard to test.
This extracts it to a dedicated XmlParsingUtil class for reusability
and better test coverage.

Reduces HamQslClient from 390 to 280 lines.
EOF
)"
```

**Commit with Breaking Change**:
```bash
git commit -m "$(cat <<'EOF'
feat(api)!: change SolarIndices to return Optional

BREAKING CHANGE: fetchSolarIndices() now returns Optional<SolarIndices>
instead of nullable SolarIndices. Consumers must update to use .orElse()
or .orElseThrow().

This improves null safety and makes the API more explicit about the
possibility of missing data.
EOF
)"
```

---

## Example Commits for NextSkip

### Feature Addition

```
feat(propagation): add VOACAP propagation prediction

Integrates with VOACAP API to provide HF propagation predictions
based on solar conditions. Adds new VoacapClient with circuit
breaker and caching.

Closes #42
```

### Bug Fix

```
fix(api): handle NOAA partial date formats

NOAA API sometimes returns dates as "2025-11" (year-month only)
instead of full ISO-8601. Added fallback parsing to handle this
by treating partial dates as first day of the month.

Fixes #67
```

### Refactoring

```
refactor(api): extract XML parsing logic

Moved XML parsing from HamQslClient to XmlParsingUtil for:
- Reusability across multiple clients
- Easier unit testing
- Separation of concerns

No behavior change. All 60 tests still passing.
```

### Performance Improvement

```
perf(cache): increase TTL from 5 to 30 minutes

Reduces API calls to NOAA/HamQSL by caching results longer.
NOAA data updates every 30 minutes, so 30-minute cache is safe
and reduces load on external services.

Measured 80% reduction in API calls during testing.
```

### Test Addition

```
test(api): add property-based tests for date parsing

Uses jqwik to test date parsing with randomly generated valid
ISO-8601 dates. Covers edge cases like leap years, partial dates,
and timezone offsets.

Increases confidence in date parsing robustness.
```

### Documentation

```
docs(readme): add architecture decision records

Documents key design decisions:
- Why we use reactive WebClient over RestTemplate
- Circuit breaker configuration rationale
- Caching strategy for solar data

Helps onboard new contributors and maintain context.
```

### Build System

```
build(gradle): add static analysis plugins

Adds Checkstyle, PMD, SpotBugs for automated code quality checks.
Configured with default rulesets and integrated into ./gradlew check.

Will help catch issues early in development.
```

---

## User Preference Note

**IMPORTANT**: NEVER add co-author attribution like "Co-Authored-By: Claude" to commits.

Keep commit messages clean and professional. Attribution should reflect actual human contributors only.

---

## Related Commands

- `/validate-build` - Run before committing to ensure build is healthy
- `/find-refactor-candidates` - Identify what to refactor before committing
- `/plan-tests` - Plan test coverage before test commits

## Best Practices

1. **Commit often** - Small, focused commits are easier to review and revert
2. **Use imperative mood** - "Add feature" not "Added feature" or "Adds feature"
3. **Keep description <50 chars** - Readable in git log one-liners
4. **Wrap body at 72 chars** - Readable in terminal
5. **Explain why, not what** - Code shows what changed, message explains why
6. **Reference issues** - Use `Fixes #123` or `Closes #456` in footer
7. **Group related changes** - Don't mix unrelated changes in one commit

## Benefits

**For NextSkip**:
- Clean, searchable git history
- Automatic changelog generation with semantic-release
- Clear communication of intent to teammates
- Easier code reviews (scope is obvious)
- Better revert granularity (focused commits)

**Semantic Versioning**:
- `feat` → minor version bump (0.1.0 → 0.2.0)
- `fix`/`perf` → patch version bump (0.1.0 → 0.1.1)
- `BREAKING CHANGE` → major version bump (0.1.0 → 1.0.0)
- Other types → no version bump

## Example Workflow

```
User: I've finished implementing NOAA client improvements

Agent:
1. Run git status to see changed files
2. Group by scope:
   - API client changes → refactor(api)
   - DTO changes → feat(dto)
   - Test changes → test(api)
3. Create 3 separate commits:
   - refactor(api): extract date parsing logic
   - feat(dto): add validation to NoaaSolarCycleEntry
   - test(api): update tests for new exceptions
4. Verify all commits follow conventional format
5. Recommend: Push to remote
```
