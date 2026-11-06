#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_DIR="$REPO_ROOT/project_ppoo"

resolve_jdk_tool() {
    local tool="$1"
    if command -v "$tool" >/dev/null 2>&1; then
        command -v "$tool"
        return 0
    fi
    if command -v "${tool}.exe" >/dev/null 2>&1; then
        command -v "${tool}.exe"
        return 0
    fi
    local candidates=(
        "/mnt/c/Program Files/Eclipse Adoptium"/jdk-21*/bin/"${tool}.exe"
        "/mnt/c/Users"/*/.jdks/*21*/bin/"${tool}.exe"
        "/mnt/c/Program Files/Java"/jdk-21*/bin/"${tool}.exe"
        "/usr/lib/jvm"/*21*/bin/"$tool"
    )
    for candidate in "${candidates[@]}"; do
        if [ -x "$candidate" ]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done
    echo "Error: Could not locate JDK 21 tool '$tool'." >&2
    return 1
}

"$REPO_ROOT/scripts/build.sh"

JAVA="$(resolve_jdk_tool java)"
cd "$PROJECT_DIR"

if [ -f out/test/ro/ase/multimedia/TestRunner.class ]; then
    if [[ "$JAVA" == *.exe ]]; then
        CP_SEP=";"
    else
        CP_SEP=":"
    fi
    "$JAVA" -ea -cp "out/main${CP_SEP}out/test" ro.ase.multimedia.TestRunner
fi
