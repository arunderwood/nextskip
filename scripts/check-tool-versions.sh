#!/usr/bin/env bash
# Keeps every Java/Node version pin in agreement with .tool-versions.
#
# .tool-versions is the single source of truth. The GitHub Actions workflows read
# it directly (setup-java's `java-version-file` / setup-node's `node-version-file`),
# so they cannot drift. These three files cannot read it, so they are checked here:
#
#   Dockerfile             builder + runtime base images (Java major version)
#   .github/renovate.json  `install-tool` commands in postUpgradeTasks
#   build.gradle.kts       toolchain languageVersion
#   README.md              version badge + Tech Stack entry
#
# Usage:
#   check-tool-versions.sh          report drift and exit non-zero (CI)
#   check-tool-versions.sh --fix    rewrite the three files to match, then report
#
# Renovate runs --fix as a postUpgradeTask so that its own .tool-versions bumps
# arrive with the downstream pins already updated.
set -euo pipefail

cd "$(dirname "$0")/.."

fix=0
[[ "${1:-}" == "--fix" ]] && fix=1

fail=0
check() {
  local what=$1 expected=$2 actual=$3
  if [[ "$expected" == "$actual" ]]; then
    printf '  ok    %-46s %s\n' "$what" "$actual"
  else
    printf '  FAIL  %-46s %s (expected %s)\n' "$what" "${actual:-<not found>}" "$expected"
    fail=1
  fi
}

# Replace via a literal-safe python sub so version strings containing `+` and `.`
# are never treated as regex metacharacters.
replace() {
  local file=$1 pattern=$2 replacement=$3
  python3 - "$file" "$pattern" "$replacement" <<'PY'
import re, sys
path, pattern, replacement = sys.argv[1:4]
src = open(path).read()
out = re.sub(pattern, lambda m: m.group(0).replace(m.group(1), replacement), src, flags=re.MULTILINE)
if out != src:
    open(path, 'w').write(out)
PY
}

# --- parse .tool-versions -------------------------------------------------
# e.g. "java temurin-25.0.1+8.0.LTS" -> dist=temurin full=25.0.1+8.0.LTS major=25
java_id=$(awk '$1=="java"{print $2}' .tool-versions)
node_version=$(awk '$1=="nodejs"{print $2}' .tool-versions)
[[ -n "$java_id" && -n "$node_version" ]] || { echo "could not parse .tool-versions"; exit 1; }

java_dist=${java_id%%-*}
java_full=${java_id#*-}
java_major=${java_full%%.*}

echo ".tool-versions: java ${java_dist} ${java_full} (major ${java_major}), nodejs ${node_version}"
echo

if [[ $fix -eq 1 ]]; then
  replace Dockerfile '^FROM eclipse-temurin:([0-9]+)' "$java_major"
  replace Dockerfile 'distroless/java([0-9]+)' "$java_major"
  replace .github/renovate.json 'install-tool java ([^"]+)' "$java_full"
  replace .github/renovate.json 'install-tool node ([^"]+)' "$node_version"
  replace build.gradle.kts 'JavaLanguageVersion\.of\(([0-9]+)\)' "$java_major"
  replace README.md '!\[Java ([0-9]+)\]' "$java_major"
  replace README.md 'badge/Java-([0-9]+)-' "$java_major"
  replace README.md '^- Java ([0-9]+)$' "$java_major"
  echo "Applied --fix; re-checking."
  echo
fi

echo "Dockerfile"
check "builder base image" "$java_major" \
  "$(grep -oE '^FROM eclipse-temurin:[0-9]+' Dockerfile | grep -oE '[0-9]+$' || true)"
check "runtime base image" "$java_major" \
  "$(grep -oE 'distroless/java[0-9]+' Dockerfile | grep -oE '[0-9]+$' || true)"

echo ".github/renovate.json"
check "install-tool java" "$java_full" \
  "$(grep -oE 'install-tool java [^"]+' .github/renovate.json | awk '{print $3}' | sort -u | head -1 || true)"
check "install-tool node" "$node_version" \
  "$(grep -oE 'install-tool node [^"]+' .github/renovate.json | awk '{print $3}' | sort -u | head -1 || true)"

echo "build.gradle.kts"
check "toolchain languageVersion" "$java_major" \
  "$(grep -oE 'JavaLanguageVersion\.of\([0-9]+\)' build.gradle.kts | grep -oE '[0-9]+' || true)"

echo "README.md"
check "badge alt text" "$java_major" \
  "$(grep -oE '!\[Java [0-9]+\]' README.md | grep -oE '[0-9]+' || true)"
check "badge image URL" "$java_major" \
  "$(grep -oE 'badge/Java-[0-9]+-' README.md | grep -oE '[0-9]+' || true)"
check "Tech Stack entry" "$java_major" \
  "$(grep -oE '^- Java [0-9]+$' README.md | grep -oE '[0-9]+' || true)"

echo
if [[ $fail -ne 0 ]]; then
  echo "Version pins are out of sync with .tool-versions."
  echo "Run: ./scripts/check-tool-versions.sh --fix"
  exit 1
fi
echo "All version pins match .tool-versions."
