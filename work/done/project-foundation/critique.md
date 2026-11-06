---
task: project-foundation
artifact: critique
round: 1
plan_version: 1
verdict: approved
---

# Plan critique: project-foundation, round 1

## Checklist

| Check | Result | Notes |
| --- | --- | --- |
| Every requirement in `scope.md` is covered | pass | `R1`–`R6` are all covered across Steps 1–5 and verified by `T1`–`T8` plus `./scripts/{build,lint,test,run}.sh`. |
| One logical increment; existing behavior keeps working | pass | Greenfield foundation increment establishing toolchain, domain models, exception hierarchy, `Main` stub, and test runner. |
| Within the increment budget (files, lines, dependencies) | pass | 10 production Java files (~240 LOC), 1 test file, 4 dev scripts, 0 external runtime dependencies (`AGENTS.md`). |
| Testable: essential tests mapped to requirements, commands given | pass | 8 unit tests (`T1`–`T8`, 1–2 per requirement) mapped to `R2`–`R6`, plus script execution checks for `R1`. |
| Concise: under about 200 lines, snippets only for critical parts | pass | 142 lines with focused snippets for `javac` invocation, extension extraction, and `MultimediaFile` invariant check. |
| Follows `docs/standards/` (rule IDs) | pass | Cites and adheres to `JAVA-1`, `JAVA-2`, `JAVA-5`, `JAVA-8`, `JAVA-9`, `VAL-1`, `VAL-4`, `TEST-1`, `TEST-3`, `TEST-5`, `GIT-4`. |
| Consistent with `docs/ARCHITECTURE.md` | pass | Package structure, model attributes, supported extensions, and exception names match `docs/ARCHITECTURE.md`. |
| No over-engineering | pass | Uses standard JDK 21 tools and a lightweight `TestRunner` without unnecessary abstractions. |
| Placeholders recorded, with what removes them | pass | `TODO(TD-project-foundation-1)` in `Main.main` is documented and scheduled for removal in `console-menu-app`. |
| Snippets correct and secure | pass | `extractExtension` handles path separators, hidden files (`dotIndex <= 0`), trailing dots, and `Locale.ROOT` normalization safely. |
| Risks identified | pass | Covers WSL/Windows JDK path/classpath separator differences and `-Xlint:all -Werror` serial/JavaDoc strictness. |

## Required changes

None.

## Suggestions

- In `scripts/build.sh`, remove temporary `@out/main-sources.txt` and `@out/test-sources.txt` files immediately after compilation (not only in `scripts/lint.sh`) so `project_ppoo/out/` contains only compiled class directories.
- In `MediaType.fromExtension(String extension)`, accept both `"mp3"` and `".mp3"` (or normalize a missing leading dot) or clearly specify in JavaDoc that a leading dot is required, ensuring consistent behavior with `MultimediaFile` validation.

## Decision

approved: development can start.
