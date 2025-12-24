# Dependency Management Strategy

This repository uses a hybrid approach to dependency management:

## Tools

| Tool | Ecosystems | Why |
|------|------------|-----|
| **Renovate** | Gradle, npm, Docker | Supports `postUpgradeTasks` to run gradle commands after updates |
| **Dependabot** | GitHub Actions | Simpler for Actions, no extra PAT permissions needed |

## Vaadin/Hilla Special Handling

Vaadin/Hilla controls many npm dependencies via its Gradle plugin. When any `com.vaadin*`
package is updated (plugin or BOM), Renovate automatically runs:

```bash
./gradlew vaadinPrepareFrontend
```

This task:
- Copies frontend resources from JAR dependencies to `node_modules`
- Updates `package.json` with the correct `@vaadin/*` package versions
- Regenerates `package-lock.json`

**Packages that trigger frontend regeneration:**
- `com.vaadin` Gradle plugin (in `plugins {}` block)
- `com.vaadin:vaadin-bom` (in `dependencyManagement {}`)
- `com.vaadin:vaadin-spring-boot-starter`
- `com.vaadin:hilla-spring-boot-starter`

All are grouped together so a single PR updates everything atomically.

### Ignored npm Packages

The following npm packages are controlled by Vaadin and ignored by Renovate:
- `@vaadin/*` - All Vaadin web components
- `react`, `react-dom`, `react-router`, `react-router-dom` - React ecosystem
- `lit`, `vite`, `typescript` - Build tooling
- `workbox-*` - Service worker tooling

These are updated automatically when Vaadin is updated.

## Configuration Files

- `renovate.json` - Renovate configuration
  - Dependency dashboard disabled (no GitHub issue created)
- `.github/workflows/renovate.yml` - Renovate workflow (runs 8 AM Pacific, weekdays)
- `.github/dependabot.yml` - Dependabot configuration (GitHub Actions only)

## Security

- Renovate workflow uses `schedule` and `workflow_dispatch` triggers (not `pull_request`)
- PAT is scoped to this repository only with minimal permissions
- See: [GitHub Actions Security Hardening](https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions)
