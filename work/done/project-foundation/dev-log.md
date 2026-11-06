---
task: project-foundation
artifact: dev-log
plan_version: 1
round: 1
status: done
---

# Development log: project-foundation

One entry per increment, in commit order: a plan step, or a fix in a fix round. The code review reads the increments one by one, so each entry says what the reviewer needs to know about its commit.

## Increment 1: 1. Create cross-platform build, lint, test, and run scripts (`R1`)

- Commit: `51cc1b8` `chore(scripts): add cross-platform build, lint, test, and run scripts`
- Changes:
  - `scripts/build.sh`: auto-detects Linux/WSL/Windows JDK 21 `javac`, compiles `src/main/java` and `src/test/java` into `project_ppoo/out/{main,test}` with `-Xlint:all -Werror`, and removes temporary source list files.
  - `scripts/lint.sh`: runs `scripts/build.sh` and `javadoc -Xdoclint:all -Werror -quiet`, cleaning up temporary `out/javadoc` and source list files.
  - `scripts/test.sh`: runs `scripts/build.sh` and executes `ro.ase.multimedia.TestRunner` with `-ea` and platform-aware classpath separator (`;` for `.exe`, `:` otherwise).
  - `scripts/run.sh`: builds if needed and runs `ro.ase.multimedia.Main`.
- Decisions: Applied critique suggestion to remove `out/main-sources.txt` and `out/test-sources.txt` inside `scripts/build.sh` as well as `scripts/lint.sh`, and guarded `javac`/`javadoc`/`TestRunner` execution when source directories are initially empty so scripts succeed at every step.
- Tests: Verified `./scripts/build.sh`, `./scripts/lint.sh`, and `./scripts/test.sh` exit with `0`.
- Review focus: WSL vs. Linux JDK path discovery and classpath separator handling.
- Deviation from the plan: None.

## Increment 2: 2. Implement `MediaType` and `DirectoryStatus` enums (`R2`, `R3`)

- Commit: `d23c7aa` `feat(model): implement MediaType and DirectoryStatus enums`
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/model/MediaType.java`: defines `AUDIO` and `IMAGE`, `SUPPORTED_EXTENSIONS = List.of(".mp3", ".wav", ".jpg", ".png")`, and `extractExtension`, `fromExtension`, and `fromFileName` using `Locale.ROOT`.
  - `project_ppoo/src/main/java/ro/ase/multimedia/model/DirectoryStatus.java`: defines `ACCESSIBLE`, `MISSING`, and `INACCESSIBLE`.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: introduces test harness and tests `T1` (`mediaType_resolvesSupportedExtensionsCaseInsensitivelyAndHandlesMultiDots`) and `T2` (`mediaType_rejectsUnsupportedBlankOrMissingExtensions`).
- Decisions: Applied critique suggestion in `MediaType.fromExtension` to accept extensions both with and without a leading dot (normalizing `"mp3"` to `".mp3"`), documented in JavaDoc.
- Tests: `T1` and `T2` in `TestRunner` verify case-insensitive extension/filename resolution, multi-dot filenames, Windows/Unix separators, and rejection of unsupported/hidden/blank inputs.
- Review focus: Edge cases in `MediaType.extractExtension` (hidden dotfiles, trailing dots, mixed path separators).
- Deviation from the plan: None.

## Increment 3: 3. Implement `MonitoredDirectory` and `MultimediaFile` models (`R3`, `R4`)

- Commit: `07baf2c` `feat(model): implement MonitoredDirectory and MultimediaFile domain classes`
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/model/MonitoredDirectory.java`: immutable value class holding normalized `Path`, `DirectoryStatus`, and trimmed `addedAt` timestamp, with `withStatus(DirectoryStatus)` and path-based `equals`/`hashCode`.
  - `project_ppoo/src/main/java/ro/ase/multimedia/model/MultimediaFile.java`: immutable value class holding file metadata, normalizing extension to lowercase with leading dot, enforcing consistency between `extension` and `mediaType`, non-negative `sizeBytes`, and normalized `directoryPath` and `fullPath`.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: adds `assertThrows` helper and tests `T3`–`T6`.
- Decisions: Normalized extensions without a leading dot in `MultimediaFile` (consistent with `MediaType.fromExtension`) before checking `resolvedType == mediaType`.
- Tests: `T3` and `T4` test `MonitoredDirectory` path normalization, `withStatus` immutability, and null/blank rejection; `T5` and `T6` test `MultimediaFile` valid construction and rejection of negative size, unsupported extension, or mismatched `MediaType`.
- Review focus: Constructor invariant checks and path normalization in `MonitoredDirectory` and `MultimediaFile`.
- Deviation from the plan: None.

## Increment 4: 4. Implement `MultimediaException` hierarchy (`R5`)

- Commit: `8bea87c` `feat(exception): implement MultimediaException hierarchy`
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/exception/MultimediaException.java`: base unchecked domain exception (`extends RuntimeException`) with `serialVersionUID = 1L` and `(String)` / `(String, Throwable)` constructors.
  - `project_ppoo/src/main/java/ro/ase/multimedia/exception/{InvalidDirectoryException,DuplicateLocationException,LocationNotFoundException,StorageOperationException}.java`: specific domain exception subclasses with `serialVersionUID = 1L` and matching constructors.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: adds `T7` (`customExceptions_preserveMessageAndCauseInHierarchy`).
- Decisions: Included `serialVersionUID = 1L` and full JavaDoc on all constructors so `-Xlint:all -Werror` and `-Xdoclint:all -Werror` pass cleanly.
- Tests: `T7` verifies message and cause propagation and `MultimediaException`/`RuntimeException` inheritance across all 5 classes.
- Review focus: Unchecked exception hierarchy and `serialVersionUID` declarations.
- Deviation from the plan: None.

## Increment 5: 5. Implement `Main` entry point and `TestRunner` suite (`R6`)

- Commit: `97ff60a` `feat(app): add Main entry point and complete foundation test suite`
- Changes:
  - `project_ppoo/src/main/java/ro/ase/multimedia/Main.java`: creates the `ro.ase.multimedia.Main` entry point printing the application banner and marking `TODO(TD-project-foundation-1)` for wiring `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu` in `console-menu-app`.
  - `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java`: adds `T8` (`main_runsFoundationEntryPointCleanly`) completing the 8-test foundation suite.
- Decisions: Kept `Main` utility-class constructor `private` and documented `main(String[] args)` for clean `-Xdoclint:all -Werror` execution.
- Tests: `T8` in `TestRunner` and `./scripts/run.sh` verify `Main.main` executes cleanly and exits with code `0`.
- Review focus: `TD-project-foundation-1` placeholder tracking and `TestRunner` coverage of `R2`–`R6`.
- Deviation from the plan: None.





