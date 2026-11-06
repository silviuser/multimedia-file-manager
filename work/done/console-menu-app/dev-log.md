---
task: console-menu-app
artifact: dev-log
plan_version: 1
round: 1
status: done
---

# Development log: console-menu-app

One entry per increment, in commit order: a plan step, or a fix in a fix round. The code review reads the increments one by one, so each entry says what the reviewer needs to know about its commit.

## Increment 1: 1. Create `ConsoleMenu` lifecycle and error handling (`R1`, `R5`, `CLI-1`, `CLI-2`, `CLI-3`, `CLI-5`, `VAL-1`, `VAL-2`)

- Commit: `e42ae59` feat(ui): create ConsoleMenu lifecycle and error handling
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java`: created interactive CLI controller with constructor validation for `MultimediaCatalog`, `InputStream`, and `PrintStream`, UTF-8 `Scanner` wrapping, startup initialization with `[WARN]` output, numbered menu loop `0`–`6`, `[ERROR]` catching for `NumberFormatException`/`IllegalArgumentException`/`MultimediaException`, and exit/EOF persistence via `catalog.saveAllOnExit()`
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added `T29` verifying startup warning display and exit/EOF state persistence
- Decisions: wrapped `catalog.initialize()` and `catalog.saveAllOnExit()` in `try`/`catch (MultimediaException)` blocks so storage errors during startup or exit also print `[ERROR] <message>` without raw stack traces (`CLI-5`)
- Tests: `T29` (`consoleMenu_initializesPrintsWarningsAndSavesStateOnZeroOrEofExit`) checks header printing, startup `[WARN]` output for missing monitored directories, and binary/text persistence on both `0` and EOF exit
- Review focus: UTF-8 `Scanner` lifecycle without closing injected streams, and clean exit on `0` or EOF
- Deviation from the plan: none

## Increment 2: 2. Implement directory management actions `1`–`3` (`R2`, `R5`, `S1`, `S2`, `S3`, `CLI-4`, `VAL-2`, `VAL-5`)

- Commit: `79f0632` feat(ui): implement directory management menu actions 1-3
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java`: implemented `handleViewDirectories` (displaying 1-based index, path, status, file count from 2D matrix row sum, and added timestamp), `handleAddDirectory`, and `handleRemoveDirectory` (resolving 1-based index or path string)
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added `T30` verifying options `1`, `2`, and `3`
- Decisions: checked `scanner.hasNextLine()` before secondary prompts in `handleAddDirectory` and `handleRemoveDirectory`, setting `running = false` on unexpected EOF so `catalog.saveAllOnExit()` is still called (`critique.md` suggestion)
- Tests: `T30` (`consoleMenu_addsViewsAndRemovesDirectoriesByIndexAndPathKeepingDiskFilesIntact`) checks empty directory message, adding two directories, displaying 1-based directory table with file counts, removing by 1-based index and by path, and preserving physical files on disk (`S3`)
- Review focus: 1-based index bounds validation (`1 <= index <= dirs.size()`) vs. path fallback in `handleRemoveDirectory`
- Deviation from the plan: none

## Increment 3: 3. Implement file listing, filtering/searching `4`–`5`, and statistics/export `6` (`R3`, `R4`, `S4`, `CLI-4`, `VAL-2`)

- Commit: `ad1f005` feat(ui): implement file listing, filtering, and statistics menu actions 4-6
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java`: implemented `handleDisplayAllFiles`, `handleFilterOrSearchFiles` (sub-menu `1`–`3` for `MediaType`, extension, and keyword search), `handleViewStatisticsAndExport` (displaying summary totals, 1D extension statistics arrays, and 2D directory-by-extension matrix and invoking `exportReport()` and `saveBinarySnapshot()`), and `formatSize` helper
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: added `T31` and `T32` verifying options `4`, `5`, and `6`
- Decisions: formatted file sizes with both human-readable units (`B`/`KB`/`MB`) and exact byte counts (`CLI-4`, `S4`); checked `scanner.hasNextLine()` before every sub-prompt in `handleFilterOrSearchFiles`
- Tests: `T31` (`consoleMenu_displaysAllFilesAndFiltersByMediaTypeExtensionAndKeyword`) checks empty file state, full listing, and filtering by `MediaType`, extension, and keyword; `T32` (`consoleMenu_views1DAnd2DStatisticsAndExportsReportAndBinarySnapshot`) checks 1D/2D primitive array tables and report/binary snapshot creation
- Review focus: case-insensitive `MediaType` resolution (`AUDIO`/`IMAGE`), sub-option validation (`1`–`3`), and 1D/2D array formatting
- Deviation from the plan: none

## Increment 4: 4. Wire `Main.java` and update `TestRunner.java` (`R1`–`R5`, `CLI-1`, `DATA-7`, `TEST-2`–`TEST-6`)

- Commit: `fe42c92` feat(main): wire Main to ConsoleMenu and add CLI error and null-check tests
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/Main.java`: removed `TODO(TD-project-foundation-1)`, added `Main.run(Path, InputStream, PrintStream)` wiring `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu`, and updated `Main.main` to invoke `run(Path.of("locations.txt"), System.in, System.out)`
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: updated `T8` to run `Main.run` against a temporary `locations.txt` and `"0\n"` input stream; added `T33` (CLI error handling without stack traces) and `T34` (null-argument validation on `ConsoleMenu` and `Main.run`)
- Decisions: none
- Tests: `T8` (`main_runsFoundationEntryPointCleanly`) checks `Main.run` lifecycle and persistence; `T33` (`consoleMenu_handlesInvalidMenuOptionsPathsAndFiltersWithErrorMessagesWithoutCrashing`) checks `[ERROR]` output and loop recovery across invalid menu inputs, paths, indices, and filter options; `T34` (`consoleMenu_andMainRejectNullConstructorAndRunArguments`) checks `IllegalArgumentException` on null arguments
- Review focus: test isolation in `T8` (`TEST-2`, `TEST-6`) and absence of raw stack traces in `T33` (`CLI-5`)
- Deviation from the plan: none

## Commands

| Command | Result |
| --- | --- |
| build: `./scripts/build.sh` | 0 errors, 0 warnings |
| test: `./scripts/test.sh` | 34 passed, 0 failed |
| lint: `./scripts/lint.sh` | no problems (`javac -Xlint:all -Werror` and `javadoc` clean) |

## Notes for testing

- Start the interactive console application with `./scripts/run.sh` (or pipe scripted commands such as `printf "1\n6\n0\n" | ./scripts/run.sh`).
- Clean up any manual test runtime files (`locations.txt`, `data/`, `reports/` inside `project_ppoo/` or repo root) after exploratory runs so the working tree stays clean.
