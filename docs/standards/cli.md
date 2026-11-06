# Console UI and workflow standards

Scope: interactive console menu, lifecycle flow, and output formatting in `src/main/java/ro/ase/multimedia/ui/` and `Main.java`. Rule IDs (`CLI-<n>`) are cited in plans, critiques and reviews.

## CLI-1. Deterministic application lifecycle (`G2`, `G6`)

The application follows a strict, predictable flow from launch to termination (`G6`):
1. Load monitored directory paths from `locations.txt` (or initialize empty if missing);
2. Validate each directory (`ACCESSIBLE`, `MISSING`, `INACCESSIBLE`) and scan accessible directories for `.mp3`, `.wav`, `.jpg`, `.png` files;
3. Populate in-memory collections and compute 1D/2D primitive arrays;
4. Run the interactive console menu loop until the user selects `0` (Exit) or input ends;
5. Save in-memory state to `locations.txt` and `catalog.bin` on exit (`G2`, `G4`).

## CLI-2. Clear numbered menu covering all required operations (`G7`, `S5`)

`ConsoleMenu` presents clear numbered options to the user on every iteration:
- `1` - Display monitored directories (path, status, file count)
- `2` - Add a new monitored directory
- `3` - Remove a monitored directory (removes reference only; keeps disk files intact)
- `4` - Display all multimedia files from monitored directories (name, extension, media type, size, location)
- `5` - Filter or search multimedia files (by media type `AUDIO`/`IMAGE`, by extension `.mp3`/`.wav`/`.jpg`/`.png`, or by name keyword)
- `6` - View statistics (1D/2D arrays) and export report / binary snapshot
- `0` - Save state and exit application

## CLI-3. Injectable I/O streams for testability

`ConsoleMenu` receives an `InputStream` (wrapped in `Scanner` with `StandardCharsets.UTF_8`) and a `PrintStream` via its constructor instead of hardcoding `System.in` and `System.out`. `Main` passes `System.in` and `System.out`.

```java
public ConsoleMenu(MultimediaCatalog catalog, InputStream in, PrintStream out) { ... }
```

## CLI-4. Structured readable output for files and statistics (`S4`)

When listing or filtering multimedia files, each entry clearly displays: file name, extension, identified media type (`AUDIO` or `IMAGE`), human-readable & byte size, and parent directory location (`S4`). When no directories are monitored or no files match a filter, a clear informative message is printed instead of an empty screen.

## CLI-5. User-friendly error messages without raw stack traces (`G5`, `S6`)

When validation or I/O fails (invalid menu number, non-existent directory path, duplicate location, unreadable file), `ConsoleMenu` catches `MultimediaException` and standard input exceptions, prints a clear `[ERROR] <message>` line, and returns to the menu prompt without crashing (`G8`).
