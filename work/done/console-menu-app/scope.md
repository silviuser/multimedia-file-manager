---
task: console-menu-app
artifact: scope
version: 1
status: confirmed
branch: console-menu-app
roadmap_item: console-menu-app
---

# Scope: Interactive Console Menu and Application Lifecycle

## Goal

Users can launch `ro.ase.multimedia.Main` to automatically restore and scan monitored directories from `locations.txt`, interactively manage monitored locations (`1`–`3`), list and filter/search multimedia files (`4`–`5`), view 1D/2D primitive array statistics while exporting `reports/multimedia-report.txt` and `data/catalog.bin` (`6`), and save full application state on exit (`0` or EOF).

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | `ConsoleMenu(MultimediaCatalog catalog, InputStream in, PrintStream out)` initializes `catalog.initialize()`, prints any non-fatal startup warnings, displays the numbered menu (`1`–`6` and `0` Exit), and loops until the user selects `0` or the input stream ends, invoking `catalog.saveAllOnExit()` before terminating (`G2`, `G4`, `G6`, `G7`, `CLI-1`, `CLI-2`, `CLI-3`). | Given a `ConsoleMenu` with an `InputStream` ending with `0` (or EOF), when `run()` executes, then `catalog.initialize()` runs at startup, startup warnings (if any) are printed to `out`, the menu options `1`–`6` and `0` are displayed, and `catalog.saveAllOnExit()` persists `locations.txt` and `catalog.bin` on exit. |
| R2 | Menu options `1` (View monitored directories), `2` (Add directory), and `3` (Remove directory by 1-based list index or directory path string) display directory paths, statuses (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`), and per-directory file counts from the 2D matrix, add validated directories, and remove directory references without deleting disk files (`G4`, `S1`, `S2`, `S3`, `S5`, `S6`, `CLI-2`, `CLI-4`, `VAL-5`). | Given an active `ConsoleMenu` session, when the user selects `2` to add a directory, `1` to view directories, and `3` to remove the directory by its 1-based index or path string, then `out` shows the directory status and file count, `locations.txt` and in-memory collections update accordingly, and the physical directory and files on disk remain untouched. |
| R3 | Menu options `4` (Display all multimedia files) and `5` (Filter/search via numbered sub-menu: `1` by `MediaType` `AUDIO`/`IMAGE`, `2` by extension `.mp3`/`.wav`/`.jpg`/`.png`, `3` by filename keyword) print structured file details (name, extension, media type, human-readable + byte size, and parent directory location) or an informative empty-state message when no files match (`G3`, `S4`, `S5`, `CLI-2`, `CLI-4`, `VAL-2`). | Given cataloged `.mp3`, `.wav`, `.jpg`, and `.png` files, when the user selects `4` or `5` (filtering by media type, extension, or filename keyword), then `out` lists each matching file's name, extension, `MediaType`, size, and directory path, or prints an informative message when zero files match. |
| R4 | Menu option `6` (View statistics and export report / binary snapshot) prints summary totals, the 1D extension statistics table (`int[]`, `long[]`), and the 2D directory-by-extension matrix (`long[][]`) to `out`, and automatically exports both `reports/multimedia-report.txt` and `data/catalog.bin` (`G1`, `G3`, `G4`, `S5`, `CLI-2`, `CLI-4`). | Given a `ConsoleMenu` session with cataloged files, when the user selects `6`, then `out` displays the 1D per-extension counts/bytes and 2D directory-by-extension matrix and `catalog.exportReport()` and `catalog.saveBinarySnapshot()` write `multimedia-report.txt` and `catalog.bin` to disk. |
| R5 | `ConsoleMenu` and `Main` validate all console inputs and catch `NumberFormatException`, `IllegalArgumentException`, and `MultimediaException` (`InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`), printing `[ERROR] <message>` without raw stack traces and continuing the menu loop, while `Main.main` wires `Path.of("locations.txt")` with `System.in` and `System.out` (`G5`, `G8`, `S6`, `CLI-1`, `CLI-5`, `VAL-1`, `VAL-2`). | Given invalid menu numbers, out-of-range indices, non-existent/duplicate/unmonitored paths, or unsupported filter inputs, when entered in `ConsoleMenu`, then `[ERROR] ...` is printed to `out` without crashing and subsequent valid commands execute normally. |

## Out of scope

- Graphical user interface (GUI) or third-party CLI frameworks (`picocli`, `JLine`).
- Full course documentation (`docs/PROJECT_DOCUMENTATION.md`) and manual test walkthrough in `README.md` — covered in `javadoc-and-documentation`.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| `java.util.Scanner(in, StandardCharsets.UTF_8)` + `PrintStream out` injected into `ConsoleMenu` | Required by `CLI-3`, `D5`, and `TEST-4`; zero external dependencies; deterministic testing with `ByteArrayInputStream` and `ByteArrayOutputStream` | Line-based input without readline history | chosen |
| `java.io.Console` (`System.console()`) | Native terminal integration | Returns `null` when standard input/output is redirected or in automated tests, breaking `CLI-3` and `TEST-4` | rejected |
| External CLI libraries (`picocli` / `JLine 3`) | Rich ANSI tables and tab completion | Violates `JAVA-1` and `D1` (zero external runtime dependencies) | rejected |
| Option 3 accepts either 1-based displayed index or path string; Option 5 uses a numbered sub-menu (`1`–`3`); Option 6 prints statistics and immediately exports report & binary snapshot | Fast interactive ergonomics while preserving direct path removal and deterministic scripted CLI tests | Slightly more input parsing logic in Option 3 | chosen |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Scanner.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/PrintStream.html
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Console.html

## Constraints and assumptions

- `ConsoleMenu` wraps `InputStream` in `new Scanner(in, StandardCharsets.UTF_8)` and never closes `System.in` or `System.out` when invoked from `Main.main`.
- `Main` exposes a package-private or public helper (or uses `ConsoleMenu` directly) so `TestRunner` can test `Main` without blocking on `System.in` or polluting the working directory's `locations.txt`, `data/catalog.bin`, or `reports/multimedia-report.txt` (`DATA-7`, `TEST-2`, `TEST-6`).
- Resolved tech debt: removes `TD-project-foundation-1` in `Main.java`.

## Impact

- Code areas touched:
  - `project_ppoo/src/main/java/ro/ase/multimedia/ui/ConsoleMenu.java` (new)
  - `project_ppoo/src/main/java/ro/ase/multimedia/Main.java` (updated)
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` (updated)
- Blast radius: low (adds `ConsoleMenu` in `ui` package and wires `Main.java` without altering existing `model`, `exception`, `storage`, or `service` classes).
- Increment budget: fits (2 production files, ~260 lines of new/changed production code, 0 external dependencies).

## Split and placeholders

- None (fits within a single increment and resolves `TD-project-foundation-1`).

## Follow-ups

- `javadoc-and-documentation`: complete JavaDoc verification, repeatable manual test scenarios in `README.md`, and course documentation (`docs/PROJECT_DOCUMENTATION.md`).

## Open questions

- None
