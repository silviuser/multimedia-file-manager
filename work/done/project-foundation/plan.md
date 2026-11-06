---
task: project-foundation
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Project Foundation, Domain Model, and Exception Hierarchy

## Summary

Create the cross-platform `./scripts/{build,lint,test,run}.sh` toolchain targeting `project_ppoo/` (`R1`), implement the validated domain models (`MediaType`, `DirectoryStatus`, `MonitoredDirectory`, `MultimediaFile`) (`R2`–`R4`) and unchecked custom exception hierarchy rooted at `MultimediaException` (`R5`), and add the `Main` startup entry point plus a zero-dependency automated `TestRunner` verifying all requirements (`R6`).

## Context

- `work/project-foundation/scope.md`: confirms `project_ppoo/src/{main,test}/java` layout, `./scripts/*.sh` commands, unchecked `MultimediaException`, and `TD-project-foundation-1` placeholder in `Main`.
- `docs/ARCHITECTURE.md`: defines components in `ro.ase.multimedia.{model,exception}`, supported extensions (`.mp3`, `.wav`, `.jpg`, `.png`), and domain fields.
- `docs/standards/{java,validation,testing,git}.md`: enforces `JAVA-1`, `JAVA-2`, `JAVA-8`, `JAVA-9`, `VAL-1`, `VAL-4`, `TEST-1`, `TEST-3`, `TEST-5`, and `GIT-4`.

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `scripts/build.sh` | create | Auto-detect Linux/WSL/Windows JDK 21 and compile `src/main/java` and `src/test/java` into `project_ppoo/out/{main,test}` (`R1`). |
| 2 | `scripts/lint.sh` | create | Run `javac -Xlint:all -Werror` on all sources and `javadoc -Xdoclint:all -Werror` on `src/main/java` (`R1`, `JAVA-8`, `JAVA-9`). |
| 3 | `scripts/test.sh` | create | Invoke `./scripts/build.sh` and run `ro.ase.multimedia.TestRunner` with `-ea` (`R1`, `R6`, `TEST-1`). |
| 4 | `scripts/run.sh` | create | Compile main classes if needed and launch `ro.ase.multimedia.Main` (`R1`, `R6`). |
| 5 | `project_ppoo/src/main/java/ro/ase/multimedia/model/MediaType.java` | create | Enum (`AUDIO`, `IMAGE`) with supported extension list and case-insensitive extension/filename resolution (`R2`, `VAL-4`). |
| 6 | `project_ppoo/src/main/java/ro/ase/multimedia/model/DirectoryStatus.java` | create | Enum (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) representing disk status of a monitored path (`R3`). |
| 7 | `project_ppoo/src/main/java/ro/ase/multimedia/model/MonitoredDirectory.java` | create | Domain class holding normalized `Path`, `DirectoryStatus`, and `addedAt` timestamp with validation (`R3`, `JAVA-2`). |
| 8 | `project_ppoo/src/main/java/ro/ase/multimedia/model/MultimediaFile.java` | create | Immutable domain class for a discovered media file with extension/type consistency checks (`R4`, `JAVA-2`). |
| 9 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/MultimediaException.java` | create | Base unchecked domain exception (`extends RuntimeException`) with `serialVersionUID` (`R5`, `VAL-1`). |
| 10 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/InvalidDirectoryException.java` | create | Unchecked exception for invalid, missing, non-directory, or unreadable paths (`R5`, `VAL-1`). |
| 11 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/DuplicateLocationException.java` | create | Unchecked exception when adding an already-monitored directory (`R5`, `VAL-1`). |
| 12 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/LocationNotFoundException.java` | create | Unchecked exception when removing an unmonitored directory (`R5`, `VAL-1`). |
| 13 | `project_ppoo/src/main/java/ro/ase/multimedia/exception/StorageOperationException.java` | create | Unchecked exception wrapping persistence and report I/O errors (`R5`, `VAL-1`). |
| 14 | `project_ppoo/src/main/java/ro/ase/multimedia/Main.java` | create | Application entry point with `TD-project-foundation-1` startup placeholder (`R6`). |
| 15 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | create | Self-contained test harness and unit tests for `MediaType`, `MonitoredDirectory`, `MultimediaFile`, `MultimediaException` subtypes, and `Main` (`R6`, `TEST-1`, `TEST-3`). |

## Steps

### Step 1. Create cross-platform build, lint, test, and run scripts (`R1`)

Create `scripts/{build,lint,test,run}.sh` (`chmod +x`). Each script locates JDK 21 (`javac`, `java`, `javadoc`) on `PATH` or in Windows JDK folders (`/mnt/c/Program Files/Eclipse Adoptium/jdk-21*`, `/mnt/c/Users/*/.jdks/*21*`, `/mnt/c/Program Files/Java/jdk-21*`). When invoking a `.exe` tool from WSL, convert relative project paths or run inside `project_ppoo/` with `;` classpath separator (`:` on Linux) so paths work without external dependencies (`JAVA-8`, `JAVA-9`, `TEST-1`, `GIT-4`).

```bash
cd "$REPO_ROOT/project_ppoo"
mkdir -p out/main out/test
find src/main/java -name '*.java' > out/main-sources.txt
"$JAVAC" -encoding UTF-8 -Xlint:all -Werror -d out/main @out/main-sources.txt
find src/test/java -name '*.java' > out/test-sources.txt
"$JAVAC" -encoding UTF-8 -Xlint:all -Werror -cp "out/main" -d out/test @out/test-sources.txt
```

`scripts/lint.sh` runs `scripts/build.sh` and then executes `"$JAVADOC" -encoding UTF-8 -Xdoclint:all -Werror -quiet -d out/javadoc @out/main-sources.txt`, removing `out/javadoc` and `out/*-sources.txt` afterward so no temporary files remain (`GIT-4`).

### Step 2. Implement `MediaType` and `DirectoryStatus` enums (`R2`, `R3`)

Create `DirectoryStatus` (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) and `MediaType` (`AUDIO`, `IMAGE`) in `ro.ase.multimedia.model` with full JavaDoc (`JAVA-1`, `JAVA-9`). `MediaType` exposes `SUPPORTED_EXTENSIONS = List.of(".mp3", ".wav", ".jpg", ".png")`, `extractExtension(String fileName)`, `fromExtension(String extension)`, and `fromFileName(String fileName)` using `Locale.ROOT` (`VAL-4`):

```java
public static Optional<String> extractExtension(String fileName) {
    if (fileName == null || fileName.isBlank()) {
        return Optional.empty();
    }
    int lastSeparator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
    String baseName = lastSeparator >= 0 ? fileName.substring(lastSeparator + 1) : fileName;
    int dotIndex = baseName.lastIndexOf('.');
    if (dotIndex <= 0 || dotIndex == baseName.length() - 1) {
        return Optional.empty();
    }
    String ext = baseName.substring(dotIndex).toLowerCase(Locale.ROOT);
    return SUPPORTED_EXTENSIONS.contains(ext) ? Optional.of(ext) : Optional.empty();
}
```

### Step 3. Implement `MonitoredDirectory` and `MultimediaFile` models (`R3`, `R4`)

Create `MonitoredDirectory` and `MultimediaFile` in `ro.ase.multimedia.model` (`JAVA-2`, `JAVA-5`, `JAVA-9`):
- `MonitoredDirectory(Path path, DirectoryStatus status, String addedAt)`: validates non-null `path` (normalized via `path.toAbsolutePath().normalize()`), non-null `status`, and non-null/non-blank trimmed `addedAt`; provides `getPath()`, `getStatus()`, `getAddedAt()`, `withStatus(DirectoryStatus newStatus)`, `equals`/`hashCode` based on normalized `path`, and `toString()`.
- `MultimediaFile(String name, String extension, MediaType mediaType, long sizeBytes, Path directoryPath, Path fullPath)`: validates non-blank `name`, supported lowercase `extension` whose `MediaType.fromExtension(extension)` matches `mediaType`, `sizeBytes >= 0`, and non-null normalized `directoryPath` and `fullPath`.

```java
MediaType resolvedType = MediaType.fromExtension(normalizedExt)
        .orElseThrow(() -> new IllegalArgumentException("Unsupported extension: " + extension));
if (mediaType == null || resolvedType != mediaType) {
    throw new IllegalArgumentException("Extension " + normalizedExt + " does not match " + mediaType);
}
```

### Step 4. Implement `MultimediaException` hierarchy (`R5`)

Create `MultimediaException` (`extends RuntimeException`) and its four subclasses (`InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException`) in `ro.ase.multimedia.exception` (`VAL-1`, `JAVA-8`, `JAVA-9`). Each class defines `private static final long serialVersionUID = 1L;` (required by `-Xlint:serial` in `-Xlint:all`) and two constructors: `(String message)` and `(String message, Throwable cause)`.

### Step 5. Implement `Main` entry point and `TestRunner` suite (`R6`)

Create `ro.ase.multimedia.Main` with `public static void main(String[] args)` printing the application banner and a `TODO(TD-project-foundation-1)` comment where `ConsoleMenu` and `MultimediaCatalog` will be wired in `console-menu-app`. Create `ro.ase.multimedia.TestRunner` in `src/test/java` executing deterministic unit tests for `R2`–`R6` (`TEST-1`, `TEST-3`, `TEST-5`) and exiting non-zero on any failure.

## Interfaces and data

- Package `ro.ase.multimedia.model`:
  - `MediaType`: `AUDIO`, `IMAGE`; `SUPPORTED_EXTENSIONS`; `extractExtension(String): Optional<String>`; `fromExtension(String): Optional<MediaType>`; `fromFileName(String): Optional<MediaType>`.
  - `DirectoryStatus`: `ACCESSIBLE`, `MISSING`, `INACCESSIBLE`.
  - `MonitoredDirectory`: `(Path path, DirectoryStatus status, String addedAt)`, `withStatus(DirectoryStatus)`, getters, value equality on `path`.
  - `MultimediaFile`: `(String name, String extension, MediaType mediaType, long sizeBytes, Path directoryPath, Path fullPath)`, getters, value equality on `fullPath`.
- Package `ro.ase.multimedia.exception`:
  - `MultimediaException`, `InvalidDirectoryException`, `DuplicateLocationException`, `LocationNotFoundException`, `StorageOperationException` (`(String)`, `(String, Throwable)`).
- Package `ro.ase.multimedia`:
  - `Main.main(String[] args)`.
- New runtime dependencies: none (`D1`, `JAVA-1`).

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `mediaType_resolvesSupportedExtensionsCaseInsensitivelyAndHandlesMultiDots` (`.mp3`, `.WAV`, `archive.tar.jpg`, `song.mp3` -> `AUDIO`/`IMAGE`) | R2 | new |
| T2 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `mediaType_rejectsUnsupportedBlankOrMissingExtensions` (`.txt`, `.mp4`, `noext`, `.hidden`, `null`, `""` -> `Optional.empty()`) | R2 | new |
| T3 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `monitoredDirectory_normalizesPathAndUpdatesStatusImmutably` (valid construction, `withStatus`, equality on normalized path) | R3 | new |
| T4 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `monitoredDirectory_rejectsNullOrBlankArguments` (`null` path/status or blank `addedAt` throws `IllegalArgumentException`) | R3 | new |
| T5 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `multimediaFile_storesNormalizedAttributesForValidInput` (valid `.mp3`/`.png` construction and getter verification) | R4 | new |
| T6 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `multimediaFile_rejectsNegativeSizeUnsupportedExtensionOrMismatchedMediaType` (negative size, `.txt`, or `.mp3` with `IMAGE` throws `IllegalArgumentException`) | R4 | new |
| T7 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `customExceptions_preserveMessageAndCauseInHierarchy` (all 5 classes preserve message/cause and extend `MultimediaException` & `RuntimeException`) | R5 | new |
| T8 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `main_runsFoundationEntryPointCleanly` (`Main.main(new String[0])` completes without throwing) | R6 | new |

Commands: `./scripts/build.sh`, `./scripts/lint.sh`, `./scripts/test.sh`, and `./scripts/run.sh` (each must exit with code `0` and zero compiler/Javadoc warnings for `R1` and `R6`).

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| `TODO(TD-project-foundation-1)` in `Main.main` | Full interactive lifecycle requires `LocationRepository`, `MultimediaCatalog`, and `ConsoleMenu` | `console-menu-app` |

## Size

- Production code: about 240 changed lines in 10 Java files (plus 4 shell scripts in `scripts/`); new runtime dependencies: none.
- Increment budget: fits (<= 300 lines and <= 10 production files).

## Risks

- WSL vs. Windows JDK path handling in `./scripts/*.sh`: mitigated by running `javac`/`java`/`javadoc` with relative paths from inside `project_ppoo/` and selecting `;` vs `:` classpath separator based on whether the resolved JDK binary ends with `.exe`.
- `-Xlint:all -Werror` failing on missing `serialVersionUID` in `RuntimeException` subclasses or missing default constructor JavaDoc: mitigated by declaring `serialVersionUID = 1L` on all exception classes and documenting all public constructors (`JAVA-8`, `JAVA-9`).
