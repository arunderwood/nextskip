---
description: Review GitHub issue and create implementation plan following SOLID principles
argument-hint: <issue-number-or-url> [additional guidance]
allowed-tools: Read, Grep, Glob, Bash(git *), Task, AskUserQuestion, mcp__github__issue_read, mcp__github__get_file_contents
---

# Work Issue: $ARGUMENTS

## Workflow

### 1. Sync with Main

Ensure branch is up to date with `origin/main` before planning (unless explicitly told otherwise):
```bash
git fetch origin main && git rebase origin/main
```

### 2. Fetch Issue

Parse `$1` as either issue number or full GitHub URL. Use GitHub MCP tools to retrieve the issue content, labels, and comments.

### 3. Explore Codebase

Launch Explore agents to understand:
- Files/packages mentioned in the issue
- Related existing implementations
- Current patterns and conventions

Compare the issue's implementation details against current codebase state.

### 4. Research Alternatives

If `$ARGUMENTS` includes questions about alternatives or approaches, research and compare options considering project conventions.

### 5. Clarify

Ask clarifying questions about ambiguous requirements or implementation choices before planning.

### 6. Create Plan

Enter **planning mode** to create an implementation plan that:

- Follows CLAUDE.md best practices and existing patterns
- Adheres to SOLID principles
- Includes testing strategy (unit, integration, E2E per `/plan-tests`)
- Includes documentation updates

## Output

```
## Issue Review: #{number} - {title}

### Summary
{Intent and proposed implementation}

### Codebase Analysis
{Current state, alignment with issue, any gaps}

### Questions
{Clarifying questions before proceeding}

---

## Implementation Plan
{Steps with specific files, testing plan, documentation}
```
