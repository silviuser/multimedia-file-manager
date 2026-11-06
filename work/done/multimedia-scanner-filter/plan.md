---
task: multimedia-scanner-filter
artifact: plan
version: 1
scope_version: 1
answers_critique_round: 0
---

# Plan: Multimedia Directory Scanner and Catalog File Filtering

## Summary

Implement `MultimediaScanner` using `Files.walkFileTree` (`SimpleFileVisitor<Path>`) to recursively discover regular `.mp3`, `.wav`, `.jpg`, and `.png` files without following symlinks and while capturing non-fatal traversal warnings. Extend `MultimediaCatalog` to maintain `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` (`EnumMap`) in memory, automatically scanning accessible directories on startup, add, remove, and `rescan()`, and expose filtering/searching by `MediaType`, extension, and filename substring.

## Context

- `work/multimedia-scanner-filter/scope.md`: confirms R1–R5, `Files.walkFileTree` visitor design, and `EnumMap` + `List` collections.
- `docs/ARCHITECTURE.md`: defines `MultimediaScanner` and `MultimediaCatalog` responsibilities (`G3`, `G4`, `S4`, `S6`, `D2`, `D3`, `D9`).
- `docs/standards/java.md`, `data.md`, `validation.md`, `testing.md`: rules `JAVA-1`, `JAVA-2`, `JAVA-4`, `JAVA-5`, `JAVA-7`, `JAVA-8`, `JAVA-9`, `DATA-2`, `DATA-5`, `VAL-1`–`VAL-5`, `TEST-1`–`TEST-6`.
- `project_ppoo/src/main/java/ro/ase/multimedia/model/MediaType.java` & `MultimediaFile.java`: already provide `MediaType.extractExtension`, `MediaType.fromExtension`, and `MultimediaFile` validation (`project-foundation`).
- `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`: manages `List<MonitoredDirectory>` and `LocationRepository` sync (`location-management`).

## Files

| # | File | Action | Purpose |
| --- | --- | --- | --- |
| 1 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaScanner.java` | create | Recursively scans an accessible directory via `Files.walkFileTree` and returns sorted `MultimediaFile` entries with non-fatal warnings (`R1`). |
| 2 | `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java` | modify | Maintains `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>`, triggers scans on lifecycle changes, and implements filter/search methods (`R2`–`R5`). |
| 3 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | modify | Adds automated unit tests `T17`–`T21` covering `R1`–`R5` while keeping `T1`–`T16` passing. |

## Steps

### Step 1. Create `MultimediaScanner` (`R1`, `JAVA-2`, `JAVA-5`, `JAVA-7`, `JAVA-9`, `VAL-4`, `VAL-5`)

Create `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaScanner.java`:
- Define public record `ScanResult(List<MultimediaFile> files, List<String> warnings)` with a compact constructor validating non-null arguments and copying both lists with `List.copyOf`.
- Implement `public ScanResult scanDirectory(Path directoryPath)`:
  - Throw `IllegalArgumentException` if `directoryPath` is `null`.
  - Normalize `Path root = directoryPath.toAbsolutePath().normalize()`. If `!Files.exists(root) || !Files.isDirectory(root) || !Files.isReadable(root)` (or on `SecurityException`), return an empty `files` list and a warning message without throwing (`S6`).
  - Traverse `root` using `Files.walkFileTree(root, new SimpleFileVisitor<>() { ... })` (which does not follow symbolic links by default):

```java
@Override
public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
    if (attrs.isRegularFile() && file.getFileName() != null) {
        String fileName = file.getFileName().toString();
        MediaType.extractExtension(fileName).ifPresent(ext -> {
            MediaType type = MediaType.fromExtension(ext).orElseThrow();
            discovered.add(new MultimediaFile(fileName, ext, type, attrs.size(), root, file));
        });
    }
    return FileVisitResult.CONTINUE;
}

@Override
public FileVisitResult visitFileFailed(Path file, IOException exc) {
    warnings.add("Skipped unreadable path during scan (" + file + "): " + exc.getMessage());
    return FileVisitResult.CONTINUE;
}
```

  - Also override `postVisitDirectory(Path dir, IOException exc)` to record non-null `exc` in `warnings` and continue.
  - Sort `discovered` deterministically by `Comparator.comparing(MultimediaFile::getFullPath)` and return `new ScanResult(discovered, warnings)`.

### Step 2. Integrate `MultimediaScanner` and media collections into `MultimediaCatalog` (`R2`, `JAVA-2`, `JAVA-4`, `DATA-2`)

Modify `project_ppoo/src/main/java/ro/ase/multimedia/service/MultimediaCatalog.java`:
- Add fields `private final MultimediaScanner scanner`, `private final List<MultimediaFile> multimediaFiles = new ArrayList<>()`, and `private final Map<MediaType, List<MultimediaFile>> filesByMediaType = new EnumMap<>(MediaType.class)`.
- Keep `public MultimediaCatalog(LocationRepository locationRepository)` delegating to `this(locationRepository, new MultimediaScanner())`, and add `public MultimediaCatalog(LocationRepository locationRepository, MultimediaScanner scanner)` validating both arguments are non-null and initializing empty unmodifiable lists for every `MediaType`.
- Implement `public List<String> rescan()`: call `refreshDirectoryStatuses()`, rebuild `multimediaFiles` and `filesByMediaType` by scanning all `ACCESSIBLE` directories in `monitoredDirectories` insertion order, deduplicating files across overlapping directories via a `Set<Path> seenFullPaths`:

```java
for (MonitoredDirectory dir : monitoredDirectories) {
    if (dir.getStatus() == DirectoryStatus.ACCESSIBLE) {
        MultimediaScanner.ScanResult result = scanner.scanDirectory(dir.getPath());
        warnings.addAll(result.warnings());
        for (MultimediaFile file : result.files()) {
            if (seenFullPaths.add(file.getFullPath())) {
                multimediaFiles.add(file);
                grouped.get(file.getMediaType()).add(file);
            }
        }
    }
}
```

- Freeze each `grouped` list into `filesByMediaType` using `List.copyOf`.
- Invoke `rescan()` at the end of `initialize()` (appending any scan warnings to startup warnings), `addDirectory(...)`, and `removeDirectory(...)`.
- Add getters `getMultimediaFiles()` returning `List.copyOf(multimediaFiles)` and `getFilesByMediaType()` returning `Map.copyOf(filesByMediaType)`.

### Step 3. Add filtering and searching methods to `MultimediaCatalog` (`R3`, `R4`, `R5`, `JAVA-2`, `JAVA-7`, `VAL-1`, `VAL-2`, `VAL-4`)

In `MultimediaCatalog.java`, add pure in-memory query methods:
- `public List<MultimediaFile> filterByMediaType(MediaType mediaType)`: throw `IllegalArgumentException` if `mediaType == null`; return `List.copyOf(filesByMediaType.getOrDefault(mediaType, List.of()))`.
- `public List<MultimediaFile> filterByExtension(String extension)`: throw `IllegalArgumentException` if `extension` is `null`, blank, or not in `MediaType.fromExtension(...)`; normalize to lowercase with leading dot and filter `multimediaFiles.stream().filter(f -> f.getExtension().equals(normalized)).toList()`.
- `public List<MultimediaFile> searchByName(String query)`: throw `IllegalArgumentException` if `query` is `null` or blank; normalize `query.trim().toLowerCase(Locale.ROOT)` and filter `multimediaFiles.stream().filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)).toList()`.

### Step 4. Add unit tests `T17`–`T21` in `TestRunner` (`TEST-1`–`TEST-6`)

Update `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` to register and implement `T17`–`T21` using isolated temporary directories cleaned up in `finally` blocks (`TEST-2`).

## Interfaces and data

- `ro.ase.multimedia.service.MultimediaScanner`:
  - `public record ScanResult(List<MultimediaFile> files, List<String> warnings)`
  - `public ScanResult scanDirectory(Path directoryPath)`
- `ro.ase.multimedia.service.MultimediaCatalog` additions:
  - `public MultimediaCatalog(LocationRepository locationRepository, MultimediaScanner scanner)`
  - `public List<String> rescan()`
  - `public List<MultimediaFile> getMultimediaFiles()`
  - `public Map<MediaType, List<MultimediaFile>> getFilesByMediaType()`
  - `public List<MultimediaFile> filterByMediaType(MediaType mediaType)`
  - `public List<MultimediaFile> filterByExtension(String extension)`
  - `public List<MultimediaFile> searchByName(String query)`
- New runtime dependencies: none.

## Tests

| ID | File | Checks | Requirement | New / existing |
| --- | --- | --- | --- | --- |
| T1–T16 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | Existing domain, exception, repository, and directory management tests continue to pass | R1–R5 | existing |
| T17 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `scanner_recursivelyFindsSupportedFilesSortedAndIgnoresUnsupportedOrMissing`: scans nested directories with `.mp3`, `.wav`, `.jpg`, `.png` (mixed case), ignores unsupported files, returns sorted `MultimediaFile` list, handles missing/null paths | R1 | new |
| T18 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_scansAccessibleDirectoriesDeduplicatesOverlapsAndUpdatesOnAddRemoveRescan`: verifies `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` on `initialize`, `addDirectory`, `removeDirectory`, `rescan`, and overlapping parent/child directories | R2 | new |
| T19 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_filterByMediaTypeReturnsMatchingAudioAndImageFilesAndRejectsNull`: filters `AUDIO` and `IMAGE` files and throws `IllegalArgumentException` on `null` | R3 | new |
| T20 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_filterByExtensionMatchesCaseInsensitivelyWithOrWithoutDotAndRejectsInvalid`: filters by `MP3`, `.wav`, `.JPG`, `png` and throws `IllegalArgumentException` on `null`, blank, or unsupported extension | R4 | new |
| T21 | `project_ppoo/src/test/java/ro/ase/multimedia/TestRunner.java` | `catalog_searchByNameFindsCaseInsensitiveSubstringMatchesAndRejectsBlankQuery`: searches filename substrings case-insensitively and throws `IllegalArgumentException` on `null` or blank query | R5 | new |

Commands: `./scripts/build.sh`, `./scripts/lint.sh`, `./scripts/test.sh`.

## Placeholders and tech debt

| Placeholder | Why | Removed by |
| --- | --- | --- |
| `TODO(TD-location-management-1)` in `MultimediaCatalog.java` | 1D/2D primitive array recalculation (`StatisticsCalculator`), text report export (`ReportExporter`), and binary snapshot persistence (`BinaryCatalogStorage`) belong to the next roadmap increment | `statistics-and-reports` |

## Size

- Production code: about 230 changed lines in 2 files (`MultimediaScanner.java`, `MultimediaCatalog.java`); new runtime dependencies: none.
- Increment budget: fits.

## Risks

- Overlapping monitored directories (parent and subdirectory both monitored) could duplicate files in memory: mitigated by deduplicating discovered files by normalized `fullPath` in insertion order during `rescan()`.
- Unreadable subdirectories or broken symlinks aborting directory scan: mitigated by using `Files.walkFileTree` without `FOLLOW_LINKS` and recording errors in `visitFileFailed` / `postVisitDirectory` as warnings.
