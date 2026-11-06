---
task: project-foundation
artifact: pr
branch: project-foundation
base: main
updated_commit: none
pr: not created
---

# feat(foundation): add build toolchain, domain models, and exception hierarchy

## Summary

Establish the Java 21 build, lint, test, and run toolchain (`./scripts/*.sh`) targeting `project_ppoo/`, implement the validated immutable domain models (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`), and add the unchecked custom exception hierarchy rooted at `MultimediaException` along with the `Main` entry point and automated `TestRunner`.

## Changes

- Add cross-platform build, lint (`-Xlint:all -Werror` and `javadoc -Xdoclint:all -Werror`), test, and run shell scripts in `scripts/{build,lint,test,run}.sh` (`R1`).
- Implement `MediaType` (`AUDIO`, `IMAGE`) with case-insensitive extension/filename resolution and `DirectoryStatus` (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) in `project_ppoo/src/main/java/ro/ase/multimedia/model/{MediaType,DirectoryStatus}.java` (`R2`, `R3`).
- Implement immutable `MonitoredDirectory` and `MultimediaFile` value classes with path normalization and constructor invariant validation in `project_ppoo/src/main/java/ro/ase/multimedia/model/{MonitoredDirectory,MultimediaFile}.java` (`R3`, `R4`).
- Implement unchecked `MultimediaException` and its subclasses (`InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) in `project_ppoo/src/main/java/ro/ase/multimedia/exception/*.java` (`R5`).
- Add `Main` entry point (`project_ppoo/src/main/java/ro/ase/multimedia/Main.java`) and self-contained unit test suite (`project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`) covering `T1`–`T8` (`R6`).

## How to test

```bash
./scripts/build.sh
./scripts/lint.sh
./scripts/test.sh
./scripts/run.sh
```

- Clean build, lint, and test check (`rm -rf project_ppoo/out && ./scripts/build.sh && ./scripts/lint.sh && ./scripts/test.sh`): verifies zero compiler/Javadoc warnings, 8 passing unit tests, and no leftover temporary files in `project_ppoo/out/`.
- Cold run check (`rm -rf project_ppoo/out && ./scripts/run.sh`): verifies `run.sh` builds classes automatically, prints `=== Multimedia File Manager ===`, and exits with code `0`.
- Exploratory checks `E3`–`E5` in `work/project-foundation/test-report.md`: verified locale-independent extension extraction (`tr-TR`), `MonitoredDirectory` path normalization and null rejection, and `MultimediaFile` path-based equality and invariant enforcement.

## Quality gates

| Gate | Task file | Result |
| --- | --- | --- |
| Plan critique | `critique.md`, round 1 | approved |
| Code review | `review.md`, round 1 | approved |
| Tests | `test-report.md`, round 1 | pass |

## Tech debt

- `TD-project-foundation-1`: `Main.main` (`project_ppoo/src/main/java/ro/ase/multimedia/Main.java:18`) prints the startup banner only; full interactive lifecycle wiring `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu` is deferred to `console-menu-app`.

## Follow-ups

- `location-management`: `LocationRepository` (`locations.txt`) and directory management in `MultimediaCatalog`.
- `multimedia-scanner-filter`: `MultimediaScanner` and media collection filtering.
- `statistics-and-reports`: `StatisticsCalculator` (1D/2D primitive arrays), `ReportExporter`, and `BinaryCatalogStorage`.
- `console-menu-app`: interactive `ConsoleMenu` and full `Main` lifecycle removing `TD-project-foundation-1`.
