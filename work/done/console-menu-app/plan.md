---
task: console-menu-app
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Interactive Console Menu and Application Lifecycle

## Summary

Implement `ConsoleMenu` in `ro.ase.multimedia.ui` to drive the interactive console lifecycle: initializing the catalog and printing startup warnings, handling menu options `1`–`6` and `0` (Exit/EOF) over injectable `InputStream`/`PrintStream` streams, formatting file listings and 1D/2D primitive array statistics, and catching domain/input exceptions without stack traces. Wire `Main` to `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu`, removing `TD-project-foundation-1`.

## Context

- `docs/ARCHITECTURE.md`: defines `ConsoleMenu` and `Main` responsibilities and the 4-stage main flow (`G1`–`G8`, `S1`–`S6`, `D5`, `D7`).
- `docs/standards/cli.md`, `validation.md`, `java.md`, `testing.md`: `CLI-1`–`CLI-5` (deterministic lifecycle, numbered menu `1`–`6` and `0`, injectable streams, structured output, `[ERROR]` formatting), `VAL-1`–`VAL-5` (input trimming/parsing, read-only guarantee on monitored directories), `JAVA-5`–`JAVA-9` (single responsibility, zero warnings, full JavaDoc), `TEST-2`–`TEST-6` (isolated temp directories and in-memory streams).
- `MultimediaCatalog.java`: already provides `initialize()`, `addDirectory()`, `removeDirectory()`, `getMonitoredDirectories()`, `getMultimediaFiles()`, `filterByMediaType()`, `filterByExtension()`, `searchByName()`, `getStatistics()`, `exportReport()`, `saveBinarySnapshot()`, and `saveAllOnExit()`.

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java` | create | Interactive CLI menu loop, input parsing, tabular display, and error presentation |
| 2 | `project_ppoo/src/main/java/ro/ase/multimedia/Main.java` | modify | Wires `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu`; removes `TD-project-foundation-1` |
| 3 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | modify | Updates `T8` for full `Main.run` lifecycle and adds tests `T29`–`T34` for `R1`–`R5` |

## Steps

### Step 1. Create `ConsoleMenu` lifecycle and error handling (`R1`, `R5`, `CLI-1`, `CLI-2`, `CLI-3`, `CLI-5`, `VAL-1`, `VAL-2`)

Create `ro.ase.multimedia.ui.ConsoleMenu`:
- Validate non-null `MultimediaCatalog catalog`, `InputStream in`, and `PrintStream out` in the constructor (`IllegalArgumentException`), wrapping `in` in `new Scanner(in, StandardCharsets.UTF_8)` without closing `in` or `out` on exit.
- In `run()`, print the application header, call `catalog.initialize()`, and print any returned startup warnings as `[WARN] <warning>`.
- Loop displaying options `1`–`6` and `0` (Exit), reading lines from `scanner`:
  - Exit cleanly when `0` is entered or `!scanner.hasNextLine()` (EOF), invoking `catalog.saveAllOnExit()` before returning.
  - Parse menu selection via `Integer.parseInt(line.trim())`; catch `NumberFormatException`, `IllegalArgumentException`, and `MultimediaException`, printing `[ERROR] <message>` to `out` and continuing the loop.

```java
try {
    int choice = Integer.parseInt(trimmed);
    switch (choice) {
        case 1 -> handleViewDirectories();
        case 2 -> handleAddDirectory();
        case 3 -> handleRemoveDirectory();
        case 4 -> handleDisplayAllFiles();
        case 5 -> handleFilterOrSearchFiles();
        case 6 -> handleViewStatisticsAndExport();
        case 0 -> running = false;
        default -> out.println("[ERROR] Invalid menu option: " + choice + ". Choose 0-6.");
    }
} catch (NumberFormatException ex) {
    out.println("[ERROR] Invalid menu input: '" + trimmed + "'. Please enter a number (0-6).");
} catch (MultimediaException | IllegalArgumentException ex) {
    out.println("[ERROR] " + ex.getMessage());
}
```

### Step 2. Implement directory management actions `1`–`3` (`R2`, `R5`, `S1`, `S2`, `S3`, `CLI-4`, `VAL-2`, `VAL-5`)

In `ConsoleMenu`:
- **Option 1 (`handleViewDirectories`)**: retrieve `catalog.getMonitoredDirectories()` and `catalog.getStatistics().getDirectoryExtensionMatrix()`. If empty, print `"No monitored directories configured."`. Otherwise, for each directory `i`, sum row `matrix[i]` for total files and print `[i + 1] <path> | Status: <status> | Files: <count> | Added: <addedAt>`.
- **Option 2 (`handleAddDirectory`)**: prompt for path, read line, call `catalog.addDirectory(rawPath)`, and print confirmation with the added normalized path.
- **Option 3 (`handleRemoveDirectory`)**: prompt for 1-based directory number or path string. If the trimmed input parses as an `int`, validate `1 <= index <= dirs.size()` (throwing `LocationNotFoundException` if out of range) and resolve `dirs.get(index - 1).getPath().toString()`; otherwise pass the trimmed path string directly to `catalog.removeDirectory(...)`. Print confirmation noting disk files were kept intact.

```java
String targetPath = rawInput;
try {
    int index = Integer.parseInt(rawInput);
    List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
    if (index < 1 || index > dirs.size()) {
        throw new LocationNotFoundException("Directory index out of range: " + index);
    }
    targetPath = dirs.get(index - 1).getPath().toString();
} catch (NumberFormatException ignored) {
    // Input is a path string; pass directly to catalog.removeDirectory
}
MonitoredDirectory removed = catalog.removeDirectory(targetPath);
```

### Step 3. Implement file listing, filtering/searching `4`–`5`, and statistics/export `6` (`R3`, `R4`, `S4`, `CLI-4`, `VAL-2`)

In `ConsoleMenu`:
- **Option 4 (`handleDisplayAllFiles`)**: format `catalog.getMultimediaFiles()` showing index, file name, extension, `MediaType`, human-readable + byte size, and parent directory path, or print `"No multimedia files found in monitored directories."` when empty.
- **Option 5 (`handleFilterOrSearchFiles`)**: display sub-menu (`1` by `MediaType` `AUDIO`/`IMAGE`, `2` by extension `.mp3`/`.wav`/`.jpg`/`.png`, `3` by filename keyword), read sub-option and criterion, resolve `MediaType` case-insensitively for `1` (throwing `IllegalArgumentException` on invalid type), invoke the corresponding `MultimediaCatalog` filter/search method, and print matching files or `"No multimedia files matched the filter criteria."`.
- **Option 6 (`handleViewStatisticsAndExport`)**: print summary totals, the `[1D Extension Statistics]` table (`int[] countsByExtension`, `long[] bytesByExtension`), and the `[2D Directory-by-Extension Matrix]` (`long[][] directoryExtensionMatrix`), then call `catalog.exportReport()` and `catalog.saveBinarySnapshot()` and print export confirmation.

### Step 4. Wire `Main.java` and update `TestRunner.java` (`R1`–`R5`, `CLI-1`, `DATA-7`, `TEST-2`–`TEST-6`)

- Update `ro.ase.multimedia.Main`:
  - Remove `TODO(TD-project-foundation-1)`.
  - Add `public static void run(Path locationsFile, InputStream in, PrintStream out)` validating non-null arguments, creating `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu`, and calling `menu.run()`.
  - Update `Main.main(String[] args)` to call `run(Path.of("locations.txt"), System.in, System.out)`.
- Update `TestRunner.java`:
  - Update `T8` (`main_runsFoundationEntryPointCleanly`) because `Main`'s requirement changed from placeholder banner (`TD-project-foundation-1`) to interactive lifecycle (`R1`, `R5`): invoke `Main.run` with an isolated temp `locations.txt` and `"0\n"` input stream so the test suite never blocks on `System.in` or writes to the repository root (`TEST-2`, `TEST-6`).
  - Add `T29`–`T34` covering `R1`–`R5`.

## Interfaces and data

- `ro.ase.multimedia.ui.ConsoleMenu`:
  - `public ConsoleMenu(MultimediaCatalog catalog, InputStream in, PrintStream out)`
  - `public void run()`
- `ro.ase.multimedia.Main`:
  - `public static void main(String[] args)`
  - `public static void run(Path locationsFile, InputStream in, PrintStream out)`

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1–T7, T9–T28 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | Existing model, exception, storage, scanner, statistics, and catalog tests | R1–R5 | existing |
| T8 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `main_runsFoundationEntryPointCleanly` updated to call `Main.run` with temp `locations.txt` and `"0\n"` input stream (`TD-project-foundation-1` resolved) | R1, R5 | existing (updated) |
| T29 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_initializesPrintsWarningsAndSavesStateOnZeroOrEofExit` | R1 | new |
| T30 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_addsViewsAndRemovesDirectoriesByIndexAndPathKeepingDiskFilesIntact` | R2 | new |
| T31 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_displaysAllFilesAndFiltersByMediaTypeExtensionAndKeyword` | R3 | new |
| T32 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_views1DAnd2DStatisticsAndExportsReportAndBinarySnapshot` | R4 | new |
| T33 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_handlesInvalidMenuOptionsPathsAndFiltersWithErrorMessagesWithoutCrashing` | R5 | new |
| T34 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `consoleMenu_andMainRejectNullConstructorAndRunArguments` | R1, R5 | new |

Commands: `./scripts/build.sh`, `./scripts/test.sh`, `./scripts/lint.sh`.

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| None (removes `TD-project-foundation-1`) | Full console UI and application lifecycle are implemented in this increment | N/A |

## Size

- Production code: about 260 changed lines in 2 files; new runtime dependencies: none.
- Increment budget: fits.

## Risks

- Unexpected EOF mid-command (e.g., input stream ends after selecting `2`, `3`, or `5`): mitigate by checking `scanner.hasNextLine()` before reading secondary prompts and exiting the loop cleanly via `saveAllOnExit()`.
- Calling `Main.main` directly in automated tests would block on `System.in` or create `locations.txt` in the working directory: mitigate by exposing `Main.run(Path, InputStream, PrintStream)` and testing against a temporary directory (`DATA-7`, `TEST-2`).
