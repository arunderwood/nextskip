---
description: Ship code: create branch, commit, push, and open PR
argument-hint: [commit message or description]
allowed-tools: Bash(git *), Skill, mcp__github__create_pull_request, mcp__github__get_me
---

## Purpose

Ship staged changes to a PR in one command: branch from `origin/main`, commit, push, and open PR.

## Usage

```
/ship
/ship add caching to API client
```

## Workflow

1. **Fetch latest**: `git fetch origin main`

2. **Create branch** from `origin/main` using conventional commit naming:
   - `feat:` → `feature/{slug}`
   - `fix:` → `fix/{slug}`
   - `refactor:` → `refactor/{slug}`
   - etc.

3. **Commit** using `/git:commit`

4. **Push**: `git push -u origin {branch}`

5. **Create PR** targeting `main`
   - Title: commit's first line
   - Body: brief summary (no Claude attribution)

6. **Output result**:
   ```
   ## PR Created
   **Title**: feat(propagation): add caching
   **Link**: https://github.com/owner/repo/pull/123
   ```

## Related Commands

- `/git:commit` - Used for creating the commit
- `/validate-build` - Run before shipping
