# Validation, exception and safety standards

Scope: input validation, custom and standard exception handling, and file-system safety across all packages. Rule IDs (`VAL-<n>`) are cited in plans, critiques and reviews.

## VAL-1. Standard and custom exception hierarchy (`G5`)

To satisfy general requirement `G5`, validation and error handling combine standard Java exceptions (`IllegalArgumentException`, `NumberFormatException`, `IOException`, `SecurityException`) with programmer-defined exceptions in `ro.ase.multimedia.exception`:
- `MultimediaException` (checked or runtime base domain exception)
- `InvalidDirectoryException` (path is blank, does not exist, is a regular file instead of a directory, or is not readable)
- `DuplicateLocationException` (directory is already in the monitored list)
- `LocationNotFoundException` (attempting to remove a directory not in the monitored list)
- `StorageOperationException` (wrapping `IOException` when `locations.txt`, `catalog.bin`, or report files cannot be read or written)

## VAL-2. Console input validation (`G5`, `S6`)

Every console input is trimmed and validated before calling service methods:
- Menu selection: parsed via `Integer.parseInt`, catching `NumberFormatException` and rejecting out-of-range options;
- Directory path on add: rejected with `InvalidDirectoryException` if null, blank, invalid path syntax (`InvalidPathException`), non-existent (`!Files.exists(path)`), not a directory (`!Files.isDirectory(path)`), or unreadable (`!Files.isReadable(path)`);
- Filter criteria: media type or extension validated against supported formats (`.mp3`, `.wav`, `.jpg`, `.png`) in a case-insensitive manner.

## VAL-3. File input validation and resilient startup (`G5`, `S6`)

When reading `locations.txt` at startup:
- If `locations.txt` does not exist, it is created empty without throwing a fatal error (`S6`);
- Blank lines are ignored; invalid path strings are logged as warnings and skipped without aborting startup;
- If a previously monitored directory was deleted or made inaccessible on disk outside the app, it is loaded with `DirectoryStatus.MISSING` or `DirectoryStatus.INACCESSIBLE` and reported to the user rather than crashing (`S6`).

## VAL-4. Case-insensitive multimedia extension recognition (`S4`)

File extension extraction handles filenames with multiple dots, no extension, and uppercase/mixed-case extensions (e.g., `TRACK.MP3`, `Photo.JPG`, `sound.wav`, `cover.png`). Only regular files (`Files.isRegularFile`) matching supported extensions are cataloged.

```java
public static Optional<MediaType> fromExtension(String extension) {
    String normalized = extension.toLowerCase(Locale.ROOT);
    return switch (normalized) {
        case ".mp3", ".wav" -> Optional.of(AUDIO);
        case ".jpg", ".png" -> Optional.of(IMAGE);
        default -> Optional.empty();
    };
}
```

## VAL-5. Read-only guarantee on monitored directories (`S3`)

Code in `MultimediaScanner` and `MultimediaCatalog` never invokes `Files.delete`, `Files.deleteIfExists`, `Files.move`, or `Files.write` inside any user-monitored directory. Only the application's own persistence files (`locations.txt`, `catalog.bin`, `multimedia-report.txt`) are written.
