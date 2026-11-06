---
task: javadoc-and-documentation
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Complete JavaDoc, Manual Verification Guide, and Course Project Documentation

## Summary

Add `package-info.java` files with package-level JavaDoc across all 6 application packages (`ro.ase.multimedia` and `model`, `exception`, `storage`, `service`, `ui`) (`R1`), add `./scripts/javadoc.sh` to generate UTF-8 HTML JavaDoc into git-ignored `docs/javadoc/` (`R2`), document `./scripts/javadoc.sh` and repeatable manual console verification in `README.md` (`R3`), and author the Romanian academic course documentation in `docs/PROJECT_DOCUMENTATION.md` with `.docx` conversion instructions (`R4`). Add automated verification tests `T35`–`T37` in `TestRunner.java`.

## Context

- `docs/ARCHITECTURE.md`: maps `G1`–`G9` and `S1`–`S6` to the 6 packages and 17 top-level classes/enums (`D1`–`D14`).
- `docs/standards/java.md`, `testing.md`, `git.md`: `JAVA-1` (packages and English code/comments), `JAVA-5` (layer responsibilities), `JAVA-8` (`-Xlint:all -Werror`), `JAVA-9` (`javadoc -Xdoclint:all -Werror` on all packages, classes, and methods), `TEST-1`–`TEST-7` (automated runner, requirement mapping, existing tests passing, repeatable manual verification in `README.md`), `GIT-4` (`docs/javadoc/` git-ignored).
- `scripts/lint.sh`: already compiles and runs `javadoc -encoding UTF-8 -Xdoclint:all -Werror` over all `*.java` files in `project_ppoo/src/main/java`.

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `project_ppoo/src/main/java/ro/ase/multimedia/package-info.java` | create | Root package JavaDoc for `Main` entry point and application wiring |
| 2 | `project_ppoo/src/main/java/ro/ase/multimedia/model/package-info.java` | create | Model package JavaDoc for immutable domain entities and enums |
| 3 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/package-info.java` | create | Exception package JavaDoc for unchecked domain exception hierarchy |
| 4 | `project_ppoo/src/main/java/ro/ase/multimedia/storage/package-info.java` | create | Storage package JavaDoc for text and binary persistence adapters |
| 5 | `project_ppoo/src/main/java/ro/ase/multimedia/service/package-info.java` | create | Service package JavaDoc for scanning, catalog collections, and 1D/2D array statistics |
| 6 | `project_ppoo/src/main/java/ro/ase/multimedia/ui/package-info.java` | create | UI package JavaDoc for the interactive `ConsoleMenu` boundary |
| 7 | `scripts/javadoc.sh` | create | Generates UTF-8 HTML JavaDoc into `docs/javadoc/` with `-Xdoclint:all -Werror` |
| 8 | `README.md` | modify | Documents `./scripts/javadoc.sh` and step-by-step manual console verification (`TEST-7`) |
| 9 | `docs/PROJECT_DOCUMENTATION.md` | create | Complete Romanian course project report (`Tema 4`) formatted for Word (`.docx`) export (`G9`) |
| 10 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | modify | Adds `T35`–`T37` verifying `R1`–`R4` artifacts |

## Steps

### Step 1. Create `package-info.java` in all 6 packages (`R1`, `G8`, `G9`, `JAVA-1`, `JAVA-5`, `JAVA-9`)

Create `package-info.java` in `ro.ase.multimedia`, `model`, `exception`, `storage`, `service`, and `ui`. Each file contains a concise English `/** ... */` comment describing the package's single responsibility, key types, and layer boundaries (`JAVA-5`, `JAVA-9`), followed by the `package` declaration:

```java
/**
 * Domain services for recursive directory scanning, in-memory catalog management,
 * and 1D/2D primitive array statistics calculation.
 *
 * <p>Coordinates {@link ro.ase.multimedia.service.MultimediaScanner},
 * {@link ro.ase.multimedia.service.StatisticsCalculator}, and
 * {@link ro.ase.multimedia.service.MultimediaCatalog} over in-memory collections
 * and primitive arrays without mixing console I/O concerns.
 */
package ro.ase.multimedia.service;
```

### Step 2. Create `./scripts/javadoc.sh` (`R2`, `G9`, `JAVA-9`, `GIT-4`)

Create `scripts/javadoc.sh` (executable `chmod +x`) using the same `resolve_jdk_tool` helper as `scripts/lint.sh` (`D6`). Run from `$PROJECT_DIR` (`project_ppoo`) with relative output path `-d ../docs/javadoc` so both Linux `javadoc` and Windows/WSL `javadoc.exe` write HTML documentation cleanly into `docs/javadoc/` (which is ignored by `.gitignore` per `GIT-4`):

```bash
find src/main/java -name '*.java' | sort > out/main-sources.txt
rm -rf ../docs/javadoc
mkdir -p ../docs/javadoc
"$JAVADOC" -encoding UTF-8 -Xdoclint:all -Werror -quiet -d ../docs/javadoc @out/main-sources.txt
rm -f out/main-sources.txt
```

### Step 3. Update `README.md` with `./scripts/javadoc.sh` and repeatable manual verification (`R3`, `G6`, `G7`, `TEST-7`)

Update `README.md`:
- Add `./scripts/javadoc.sh` to the `Build, Run and Test` command block.
- Add a `Manual Console Verification` section with copy-pasteable shell commands that:
  1. Create temporary sample directories (`/tmp/mfm-demo/music`, `/tmp/mfm-demo/photos`) with sample `.mp3`, `.wav`, `.jpg`, and `.png` files.
  2. Run `./scripts/run.sh` and exercise Menu Options `1` (view empty list), `2` (add both directories), `1` (view indexed directories), `4` (display all multimedia files), `5` (filter by `MediaType` `1` -> `AUDIO`, extension `2` -> `.png`, and keyword `3` -> `song`), `6` (view 1D/2D primitive array statistics and export report/binary snapshot), `3` (remove directory by index `1` while keeping disk files intact), and `0` (save state and exit).
  3. Inspect `project_ppoo/locations.txt`, `project_ppoo/reports/multimedia-report.txt`, and `project_ppoo/data/catalog.bin` (plus verify disk files in `/tmp/mfm-demo/music` remain untouched per `S3`), then clean up demo artifacts.

### Step 4. Create `docs/PROJECT_DOCUMENTATION.md` in Romanian for Microsoft Word export (`R4`, `G9`)

Create `docs/PROJECT_DOCUMENTATION.md` in Romanian, structured with numbered academic headings and tables ready for direct conversion to `.docx` via Pandoc or Microsoft Word:
1. **Descrierea problemei și obiectivele proiectului (`Tema 4`)**.
2. **Matricea de trasabilitate a cerințelor generale (`G1`–`G9`) și specifice (`S1`–`S6`)**.
3. **Arhitectura aplicației și documentația JavaDoc pe pachete și clase** (covering all 6 packages and all 17 classes/enums: `Main`, `MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`, `MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`, `LocationRepository`, `BinaryCatalogStorage`, `ReportExporter`, `MultimediaScanner`, `StatisticsCalculator`, `MultimediaCatalog`, `ConsoleMenu`).
4. **Structuri de date în memorie (`G3`, `G4`)**: 1D primitive arrays (`int[] countsByExtension`, `long[] bytesByExtension`), 2D primitive matrix (`long[][] directoryExtensionMatrix`), and object collections (`List<MonitoredDirectory>`, `Set<Path>`, `List<MultimediaFile>`, `Map<MediaType, List<MultimediaFile>>`).
5. **Persistența datelor în fișiere text și binare (`G1`, `G2`, `S1`–`S3`)**: `locations.txt`, `reports/multimedia-report.txt`, and `data/catalog.bin` binary layout (`0x4D464D43` magic header, version `1`).
6. **Validarea datelor și ierarhia de excepții (`G5`, `S6`)**.
7. **Fluxul funcțional, meniul interactiv și scenariu de utilizare / testare (`G6`–`G8`, `S4`, `S5`)**.
8. **Generarea documentației JavaDoc (`./scripts/javadoc.sh`) și exportul în Microsoft Word (`.docx`)** (`pandoc docs/PROJECT_DOCUMENTATION.md -o Tema4_Documentatie.docx` or Word direct import).

### Step 5. Add automated verification tests `T35`–`T37` in `TestRunner.java` (`R1`–`R4`, `TEST-1`, `TEST-3`, `TEST-6`)

In `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`:
- Resolve repository root dynamically (`Files.exists(Path.of("project_ppoo")) ? Path.of(".") : Path.of("..")`) so file checks work whether `TestRunner` runs from `project_ppoo/` or repo root.
- Add `T35` (`packageInfo_existsWithJavadocCommentInAllSixPackages`), `T36` (`javadocScript_andReadmeDocumentManualVerificationForAllMenuOptionsAndPersistenceFiles`), and `T37` (`projectDocumentation_containsRomanianCourseReportWithAllRequirementsPackagesClassesAndWordExport`).

## Interfaces and data

- New CLI script: `./scripts/javadoc.sh` -> generates HTML JavaDoc in `docs/javadoc/index.html`.
- No changes to public Java class signatures, data models, or runtime dependencies.

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1–T34 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | Existing unit and integration test suite | R1–R4 | existing |
| T35 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `packageInfo_existsWithJavadocCommentInAllSixPackages` verifies all 6 `package-info.java` files and their JavaDoc comments | R1 | new |
| T36 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `javadocScript_andReadmeDocumentManualVerificationForAllMenuOptionsAndPersistenceFiles` verifies `scripts/javadoc.sh` and `README.md` manual verification steps for options `1`–`6`, `0`, and persistence files | R2, R3 | new |
| T37 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `projectDocumentation_containsRomanianCourseReportWithAllRequirementsPackagesClassesAndWordExport` verifies `docs/PROJECT_DOCUMENTATION.md` sections (`G1`–`G9`, `S1`–`S6`, 6 packages, 17 classes, arrays/collections, `.docx` export) | R4 | new |

Commands:
- `./scripts/build.sh`
- `./scripts/lint.sh` (verifies `javac -Xlint:all -Werror` and `javadoc -encoding UTF-8 -Xdoclint:all -Werror` with all 6 `package-info.java` files)
- `./scripts/javadoc.sh` (verifies `docs/javadoc/index.html` generation and clean `git status`)
- `./scripts/test.sh` (runs `T1`–`T37`)

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| None | Final roadmap increment; no deferred work | N/A |

## Size

- Production code: about 90 lines across 7 files (6 `package-info.java` files + `scripts/javadoc.sh`); documentation and tests do not count against the 300-line budget; new runtime dependencies: none.
- Increment budget: fits.

## Risks

- Windows `javadoc.exe` under WSL rejecting POSIX `/mnt/c/...` output paths: mitigated in `scripts/javadoc.sh` by running from `project_ppoo` and passing relative paths (`-d ../docs/javadoc @out/main-sources.txt`), matching `scripts/lint.sh`.
- Relative path differences when `TestRunner` executes from `project_ppoo/`: mitigated by resolving the repository root helper before reading `README.md`, `scripts/javadoc.sh`, and `docs/PROJECT_DOCUMENTATION.md`.
