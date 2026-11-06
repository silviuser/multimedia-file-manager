---
task: console-menu-app
artifact: pr
branch: console-menu-app
base: main
updated_commit: none
pr: https://github.com/silviuser/multimedia-file-manager/pull/5
---

# feat(ui): interactive console menu and application lifecycle

## Summary

Adds `ConsoleMenu` in `ro.ase.multimedia.ui` and wires `ro.ase.multimedia.Main` to provide the complete interactive console lifecycle: initializing and scanning monitored directories from `locations.txt`, managing directories (`1`–`3`), listing and filtering/searching multimedia files (`4`–`5`), displaying 1D/2D primitive array statistics while exporting `reports/multimedia-report.txt` and `data/catalog.bin` (`6`), and saving state cleanly on `0` or EOF exit.

## Changes

- Created `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java` with UTF-8 `Scanner`/`PrintStream` stream injection, startup initialization and `[WARN]` display, numbered menu dispatch (`1`–`6` and `0`), 1-based index or path resolution for non-destructive directory removal, `MediaType`/extension/keyword sub-menu filtering, 1D/2D array statistics formatting with report/binary export, `[ERROR]` exception recovery without stack traces, and exit/EOF state persistence.
- Updated `project_ppoo/src/main/java/ro/ase/multimedia/Main.java` to expose `Main.run(Path, InputStream, PrintStream)` and wire `Main.main` to `Path.of("locations.txt")`, `System.in`, and `System.out`, resolving `TD-project-foundation-1`.
- Updated `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` (`T8` and new tests `T29`–`T34`, 34 total automated tests) covering startup/exit persistence, directory management, file listing/filtering, 1D/2D statistics and exports, invalid input handling, and null-argument validation.

## How to test

```bash
./scripts/build.sh
./scripts/lint.sh
./scripts/test.sh
```

Manual checks from `test-report.md`:

- Run `./scripts/run.sh` to interactively add monitored directories (`2`), view directory statuses and 2D matrix file counts (`1`), display all multimedia files (`4`), filter/search by `MediaType` (`AUDIO`/`IMAGE`), extension (`.mp3`/`.wav`/`.jpg`/`.png`), or filename keyword (`5`), view 1D/2D statistics and export `reports/multimedia-report.txt` and `data/catalog.bin` (`6`), remove directories by 1-based index or path string while keeping physical disk files intact (`3`), and exit cleanly saving state (`0` or EOF).
- Verify that invalid menu numbers, out-of-range directory indices, non-existent/duplicate paths, and unsupported filter options print `[ERROR] <message>` without raw Java stack traces and keep the menu loop active.

## Quality gates

| Gate | Task file | Result |
| --- | --- | --- |
| Plan critique | `critique.md`, round 1 | approved |
| Code review | `review.md`, round 1 | approved |
| Tests | `test-report.md`, round 1 | pass |

## Tech debt

- None (resolves `TD-project-foundation-1` in `Main.java`)

## Follow-ups

- `javadoc-and-documentation`: complete JavaDoc verification, repeatable manual test scenarios in `README.md`, and course documentation (`docs/PROJECT_DOCUMENTATION.md`).
