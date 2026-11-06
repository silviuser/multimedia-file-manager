---
task: project-foundation
artifact: scope
version: 1
status: confirmed
branch: project-foundation
roadmap_item: project-foundation
---

# Scope: Project Foundation, Domain Model, and Exception Hierarchy

## Goal

Developers and automated checks can build, lint, test, and run the Java 21 project inside `project_ppoo/` via `./scripts/`, using validated immutable domain models (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`) and custom domain exceptions (`MultimediaException` hierarchy).

## Requirements

| ID | Requirement | Acceptance criterion |
| --- | --- | --- |
| R1 | Cross-platform `./scripts/build.sh`, `./scripts/lint.sh`, `./scripts/test.sh`, and `./scripts/run.sh` compile, lint (`-Xlint:all -Werror` + `javadoc`), test, and run Java 21 sources in `project_ppoo/` across Linux/WSL/Windows JDK 21 installations (`G8`, `JAVA-8`, `JAVA-9`, `TEST-1`). | Given a Linux, WSL, or Windows JDK 21 installation, when `./scripts/build.sh`, `./scripts/lint.sh`, `./scripts/test.sh`, and `./scripts/run.sh` are executed from the repository root, then each command succeeds with exit code `0` and zero warnings. |
| R2 | `MediaType` (`AUDIO`, `IMAGE`) classifies `.mp3`, `.wav`, `.jpg`, and `.png` extensions case-insensitively and extracts normalized extensions from filenames (`S4`, `JAVA-1`, `VAL-4`). | Given filenames or extensions with varied casing and dots (e.g., `TRACK.MP3`, `archive.tar.gz`, `noext`), when `MediaType.fromExtension` or `MediaType.fromFileName` is called, then supported extensions resolve to `Optional.of(AUDIO)` or `Optional.of(IMAGE)` and unsupported ones return `Optional.empty()`. |
| R3 | `DirectoryStatus` (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) and `MonitoredDirectory` encapsulate a normalized `Path`, status, and timestamp with constructor invariant validation (`S1`, `S6`, `JAVA-2`). | Given a valid `Path`, `DirectoryStatus`, and `addedAt` string, when a `MonitoredDirectory` is constructed or updated, then getters return normalized values and null or blank arguments throw `IllegalArgumentException`. |
| R4 | `MultimediaFile` encapsulates file name, normalized lowercase extension, `MediaType`, non-negative byte size, parent directory `Path`, and full `Path` with invariant validation (`S4`, `JAVA-2`, `VAL-4`). | Given valid multimedia file attributes, when `MultimediaFile` is instantiated, then all fields are exposed via getters and invalid inputs (nulls, negative size, mismatched extension/type) throw `IllegalArgumentException`. |
| R5 | Custom unchecked exception hierarchy rooted at `MultimediaException` (`InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) supports message and cause chaining (`G5`, `VAL-1`). | Given each custom exception class in `ro.ase.multimedia.exception`, when instantiated with a message or cause, then it is assignable to `MultimediaException` and `RuntimeException` and preserves its message and cause. |
| R6 | Entry point `ro.ase.multimedia.Main` provides a runnable foundation stub and self-contained test runner executes automated assertions for `R1`–`R5` (`G8`, `TEST-1`, `TEST-3`). | Given compiled main and test classes in `project_ppoo/out/`, when `./scripts/test.sh` and `./scripts/run.sh` run, then all unit tests for `R2`–`R5` pass and `Main` exits cleanly with code `0`. |

## Out of scope

- File-system persistence (`LocationRepository`, `BinaryCatalogStorage`, `ReportExporter`) — covered in `location-management` and `statistics-and-reports`.
- Directory scanning, filtering, and 1D/2D primitive array calculation — covered in `multimedia-scanner-filter` and `statistics-and-reports`.
- Interactive `ConsoleMenu` loop — covered in `console-menu-app`.

## Options considered

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| Standard JDK 21 CLI tools (`javac`, `java`, `javadoc`) via `./scripts/*.sh` targeting `project_ppoo/` | Zero external runtime/build dependencies (`JAVA-1`), works offline in WSL with Windows/Linux JDK 21, matches IntelliJ `project_ppoo` layout | Requires lightweight reflection/assertion `TestRunner` class in `src/test/java` | chosen |
| Maven or Gradle build wrapper | Built-in dependency management and JUnit plugin | Requires internet access to download plugins/dependencies in sandboxed environments; unnecessary for zero-dependency requirement `D1` | rejected |
| Unchecked `MultimediaException extends RuntimeException` | Clean method and stream signatures (`JAVA-6`, `JAVA-7`) while preserving `@throws` JavaDoc and centralized catch in `ConsoleMenu` (`CLI-5`) | Callers are not forced by `javac` to declare `throws` | chosen |
| Checked `MultimediaException extends Exception` | Compiler-enforced `throws` declarations | Clutters functional stream pipelines and pure domain methods | rejected |

Sources:

- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Path.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html
- https://docs.junit.org/current/user-guide/#running-tests-console-launcher

## Constraints and assumptions

- All Java source and test files reside under `project_ppoo/src/main/java` and `project_ppoo/src/test/java`, compiling into `project_ppoo/out/` (`main` and `test`), ignored by `.gitignore` (`GIT-4`).
- `./scripts/*.sh` at the repository root auto-detect JDK 21 across Linux `PATH` and Windows JDK locations mounted under `/mnt/c/` in WSL, converting file paths as needed.
- Code must compile with `javac -Xlint:all -Werror` and pass `javadoc` generation with zero warnings (`JAVA-8`, `JAVA-9`).

## Impact

- Code areas touched: `./scripts/{build,lint,test,run}.sh`, `project_ppoo/src/main/java/ro/ase/multimedia/{Main.java,model/*,exception/*}`, `project_ppoo/src/test/java/ro/ase/multimedia/*`.
- Blast radius: low (greenfield foundation increment; no existing production classes modified).
- Increment budget: fits (10 production Java files, ~240 lines of production code, 4 dev tool scripts, 0 external runtime dependencies).

## Split and placeholders

- `ro.ase.multimedia.Main` includes a minimal startup placeholder marked with `TODO(TD-project-foundation-1)` until `console-menu-app` wires `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu`.

## Follow-ups

- `location-management`: `LocationRepository` (`locations.txt`) and directory management in `MultimediaCatalog`.
- `multimedia-scanner-filter`: `MultimediaScanner` and media collection filtering.
- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle removing `TD-project-foundation-1`.

## Open questions

- None
