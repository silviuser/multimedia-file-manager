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

JAVAC="$(resolve_jdk_tool javac)"
cd "$PROJECT_DIR"

rm -rf out/main out/test
mkdir -p out/main out/test

if [ -d src/main/java ]; then
    find src/main/java -name '*.java' | sort > out/main-sources.txt
else
    : > out/main-sources.txt
fi

if [ -s out/main-sources.txt ]; then
    "$JAVAC" -encoding UTF-8 -Xlint:all -Werror -d out/main @out/main-sources.txt
fi

if [ -d src/test/java ]; then
    find src/test/java -name '*.java' | sort > out/test-sources.txt
else
    : > out/test-sources.txt
fi

if [ -s out/test-sources.txt ]; then
    "$JAVAC" -encoding UTF-8 -Xlint:all -Werror -cp "out/main" -d out/test @out/test-sources.txt
fi

rm -f out/main-sources.txt out/test-sources.txt
