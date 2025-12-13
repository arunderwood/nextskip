---
description: Identify refactoring opportunities using 5-faceted analysis
argument-hint: [package or directory]
allowed-tools: Read, Grep, Glob, Bash
---

## Purpose

This command systematically identifies refactoring opportunities by examining:
- Complexity metrics (file size, method count, cyclomatic complexity)
- Git history patterns (modification frequency, contributor diversity)
- Code smells (long lines, duplicates, technical debt markers)
- Static analysis findings (Checkstyle, PMD, SpotBugs)
- Test coverage gaps (high complexity + low coverage = risk)

## When to Use

- ✅ **Monthly code quality reviews** - Identify growing technical debt
- ✅ **Before major feature additions** - Clean up related code first
- ✅ **After team retrospectives** - Address pain points
- ✅ **Planning refactoring sprints** - Prioritize high-impact improvements

## Usage

Analyze entire codebase or specific package:

```
/find-refactor-candidates
/find-refactor-candidates in propagation package
/find-refactor-candidates in src/main/java/io/nextskip/common
```

Or in conversation:
```
User: What code needs refactoring?
User: Find files that are too complex
```

## 5-Faceted Analysis Approach

### Facet 1: Complexity Metrics

**Large Files** (>300 lines suggest Single Responsibility violation):
```bash
find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn | head -20
```

**Methods Per Class** (>15 methods suggest Extract Class needed):
```bash
grep -c "public\|private\|protected" *.java
```

**Long Methods** (>30 lines suggest Extract Method):
```bash
# Count lines between method declarations
```

**Cyclomatic Complexity** (>10 suggests Simplify Conditionals):
- Use Checkstyle or PMD to calculate

**Thresholds**:
- Files >300 lines: Review for single responsibility
- Classes with >15 methods: Consider extracting class
- Methods >30 lines: Extract method refactoring
- Cyclomatic complexity >10: Simplify conditionals

### Facet 2: Git History Analysis

**Frequently Modified Files** (last 90 days):
```bash
git log --since="90 days ago" --pretty=format: --name-only | \
  grep "\.java$" | sort | uniq -c | sort -rn | head -20
```

**Insight**: Files modified most frequently may need better design to be more stable

**Contributor Diversity** (many developers touching same file):
```bash
git log --pretty=format:"%an" --name-only | grep "\.java$" | sort | uniq -c
```

**Insight**: High contributor count may indicate unclear ownership or complex code

### Facet 3: Code Smell Detection

**Long Lines** (>100 characters indicate readability issues):
```bash
grep -rn "^.\{101,\}$" src/main/java/ | wc -l
```

**TODO/FIXME Markers** (technical debt indicators):
```bash
grep -rn "TODO\|FIXME" src/main/java/ | wc -l
```

**Deep Nesting** (>3 levels indicates complexity):
```bash
# Search for 4+ levels of indentation
grep -rn "^[[:space:]]\{16,\}" src/main/java/
```

**Duplicate Code** (use PMD's Copy/Paste Detector):
```bash
./gradlew cpdCheck
```

**Java-Specific Smells**:
- **God Classes**: Many responsibilities (>300 lines + >15 methods)
- **Shotgun Surgery**: Single change requires many file edits
- **Primitive Obsession**: Should use value objects/records

### Facet 4: Static Analysis

Run static analysis tools to find issues:

**Checkstyle** (code style violations):
```bash
./gradlew checkstyleMain checkstyleTest
```

**PMD** (programming errors, code smells):
```bash
./gradlew pmdMain pmdTest
```

**SpotBugs** (potential bugs, security issues):
```bash
./gradlew spotbugsMain spotbugsTest
```

**Focus On**:
- High severity findings
- Security vulnerabilities
- Performance issues
- Repeated patterns across files

### Facet 5: Test Coverage Assessment

**Generate JaCoCo Coverage Report**:
```bash
./gradlew jacocoTestReport
```

**Identify High-Complexity, Low-Coverage Files**:
- Priority = Files with complexity >10 AND coverage <60%
- These are high-risk for bugs

**Insight**: Complex code without tests is dangerous

---

## Output Format

Provide a prioritized, actionable list:

```
## Refactoring Candidates for NextSkip

**Analysis Date**: 2025-12-13
**Codebase**: src/main/java/io/nextskip

---

### High Priority (Fix Soon)

#### 1. HamQslClient.java
**Location**: `src/main/java/io/nextskip/propagation/internal/HamQslClient.java`

**Metrics**:
- Lines: 390 (threshold: 300) ⚠️
- Methods: 8 (OK)
- Cyclomatic Complexity: 12 (threshold: 10) ⚠️
- Coverage: 85% (OK)

**Git History** (90 days):
- Modified: 12 times ⚠️
- Contributors: 2

**Code Smells**:
- 15 TODO comments (technical debt) ⚠️
- 3 methods >30 lines ⚠️
- 2 duplicate code blocks (CPD)

**Static Analysis**:
- PMD: 3 high-severity findings
  - AvoidDeeplyNestedIfStmts (line 215)
  - ExcessiveMethodLength (line 186)
- SpotBugs: 1 medium-severity
  - Possible null pointer (line 245)

**Recommendation**:
1. Extract XML parsing logic to separate XmlParser class
2. Simplify nested conditionals in band mapping (lines 210-240)
3. Address null pointer issue at line 245

**Estimated Effort**: 3-4 hours
**Impact**: High (frequently modified, used by main service)

---

#### 2. NoaaSwpcClient.java
**Location**: `src/main/java/io/nextskip/propagation/internal/NoaaSwpcClient.java`

**Metrics**:
- Lines: 210 (OK)
- Methods: 6 (OK)
- Cyclomatic Complexity: 7 (OK)
- Coverage: 90% (OK)

**Git History** (90 days):
- Modified: 15 times ⚠️⚠️
- Contributors: 3

**Code Smells**:
- 8 TODO comments ⚠️
- Date parsing method has 4 nested try-catch blocks ⚠️

**Static Analysis**:
- Checkstyle: 2 warnings (line length)
- PMD: 1 medium-severity
  - AvoidCatchingGenericException (line 181)

**Recommendation**:
1. Extract date parsing to utility class
2. Replace generic Exception with specific types
3. Clean up TODO comments

**Estimated Effort**: 2 hours
**Impact**: High (frequently modified, critical path)

---

### Medium Priority (Schedule for Later)

#### 3. PropagationService.java
**Location**: `src/main/java/io/nextskip/propagation/PropagationService.java`

**Metrics**:
- Lines: 180 (OK)
- Methods: 5 (OK)
- Cyclomatic Complexity: 6 (OK)
- Coverage: 75% (threshold: 80%) ⚠️

**Git History** (90 days):
- Modified: 8 times
- Contributors: 2

**Code Smells**:
- 1 method >30 lines
- Missing null checks (3 locations)

**Static Analysis**:
- SpotBugs: 2 medium-severity
  - Possible null pointer (lines 45, 67)

**Recommendation**:
1. Add null safety checks
2. Improve test coverage from 75% to 85%

**Estimated Effort**: 1-2 hours
**Impact**: Medium (moderate complexity, needs better coverage)

---

### Low Priority (Monitor)

#### 4. BandCondition.java
**Location**: `src/main/java/io/nextskip/propagation/model/BandCondition.java`

**Metrics**:
- Lines: 25 (OK)
- Methods: 2 (OK)
- Coverage: 100% (excellent)

**Code Smells**:
- None

**Static Analysis**:
- Clean

**Recommendation**: No immediate action needed

---

## Summary

**Total Files Analyzed**: 47
**High Priority**: 2 files
**Medium Priority**: 1 file
**Low Priority**: 44 files

**Top Issues**:
1. Excessive file length (2 files >300 lines)
2. High modification frequency (3 files >10 changes/90 days)
3. Technical debt markers (35 TODO/FIXME total)
4. Cyclomatic complexity (2 files >10)

**Recommended Actions**:
1. Refactor HamQslClient (extract XML parsing) - **3-4 hours**
2. Refactor NoaaSwpcClient (extract date parsing) - **2 hours**
3. Improve PropagationService coverage - **1-2 hours**

**Total Estimated Effort**: 6-8 hours
**Expected Quality Improvement**:
- Reduce average complexity by 20%
- Eliminate high-severity static analysis findings
- Improve coverage from 85% to 90%
```

---

## Tool Setup (One-Time)

To enable full 5-faceted analysis, add these plugins to `build.gradle`:

```gradle
plugins {
    id 'checkstyle'
    id 'pmd'
    id 'com.github.spotbugs' version '6.0.0'
    id 'jacoco'
}

checkstyle {
    toolVersion = '10.12.5'
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

pmd {
    toolVersion = '6.55.0'
    ruleSets = [
        'category/java/bestpractices.xml',
        'category/java/errorprone.xml',
        'category/java/performance.xml'
    ]
}

spotbugs {
    toolVersion = '4.8.0'
    effort = 'max'
    reportLevel = 'medium'
}

jacoco {
    toolVersion = '0.8.11'
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```

Create configuration files:
- `config/checkstyle/checkstyle.xml` - Checkstyle rules
- `config/pmd/ruleset.xml` - PMD rules (optional, uses defaults)

---

## Analysis Workflow

1. **Run Static Analysis**:
   ```bash
   ./gradlew check jacocoTestReport
   ```

2. **Analyze Metrics**:
   - File sizes: `find src/main/java -name "*.java" -exec wc -l {} \; | sort -rn`
   - Git history: `git log --since="90 days ago" ...`
   - Code smells: `grep -rn "TODO\|FIXME" src/main/java/`

3. **Cross-Reference**:
   - Combine complexity + coverage data
   - Identify files with multiple red flags
   - Prioritize by impact and effort

4. **Generate Report**:
   - High Priority: Multiple issues, high impact
   - Medium Priority: Some issues, moderate impact
   - Low Priority: Minor issues, low impact

---

## Prioritization Criteria

**High Priority** if ANY of:
- File >300 lines AND >10 methods
- Cyclomatic complexity >10
- Modified >10 times in 90 days
- >10 TODO/FIXME comments
- High-severity static analysis findings
- Coverage <60% AND complexity >10

**Medium Priority** if ANY of:
- File 200-300 lines
- Cyclomatic complexity 7-10
- Modified 5-10 times in 90 days
- 5-10 TODO/FIXME comments
- Medium-severity static analysis findings
- Coverage 60-80%

**Low Priority**:
- All other files

---

## Related Commands

- `/code-refactoring` - Refactor identified candidates (delegates to agent)
- `/validate-build` - Ensure tests pass before refactoring
- `/plan-tests` - Improve coverage for complex files
- `/commit` - Commit refactorings with conventional commits

## Best Practices

1. **Run monthly** - Catch technical debt early
2. **Start with high priority** - Maximum ROI
3. **One file at a time** - Focused refactoring
4. **Measure impact** - Re-run analysis after refactoring
5. **Celebrate wins** - Track reduction in technical debt

## Example Usage

```
User: Find refactoring candidates in the propagation package

Agent:
1. Run file size analysis
2. Check git history for modification frequency
3. Search for code smells (TODOs, long lines)
4. Run static analysis tools
5. Generate coverage report
6. Cross-reference all facets
7. Produce prioritized list with HamQslClient and NoaaSwpcClient as high priority
8. Provide actionable recommendations with effort estimates
```
