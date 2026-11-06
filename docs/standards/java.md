# Java standards

Scope: all Java code in `src/main/java/` and `src/test/java/`. Rule IDs (`JAVA-<n>`) are cited in plans, critiques and reviews.

## JAVA-1. Standard Java 21, packages and naming

All source files live under package `ro.ase.multimedia` (`model`, `exception`, `storage`, `service`, `ui`). Class and enum names use `PascalCase`, methods and fields use `camelCase`, and constants use `UPPER_SNAKE_CASE` (`public static final`). All identifiers, comments and messages are in English.

```java
package ro.ase.multimedia.model;

public enum MediaType {
    AUDIO,
    IMAGE
}
```

## JAVA-2. Encapsulation and immutable value fields

Domain classes (`MonitoredDirectory`, `MultimediaFile`) keep fields `private` (and `final` where immutable), expose getters, and validate invariants in constructors. Collections returned by getters or services are unmodifiable views (`Collections.unmodifiableList`, `List.copyOf`, `Map.copyOf`) so external callers cannot bypass `MultimediaCatalog`.

```java
public List<MonitoredDirectory> getMonitoredDirectories() {
    return List.copyOf(monitoredDirectories);
}
```

## JAVA-3. Mandatory primitive 1D and 2D arrays (`G3`)

To satisfy general requirement `G3`, the application maintains and uses at least two primitive arrays of fundamental types (`int[]`, `long[]`, `long[][]`):
- 1D arrays: `int[] countsByExtension` and `long[] bytesByExtension` indexed by supported extension ordinal (`.mp3`, `.wav`, `.jpg`, `.png`);
- 2D matrix: `long[][] directoryExtensionMatrix` where `directoryExtensionMatrix[dirIndex][extIndex]` stores the count or byte total of files of extension `extIndex` inside monitored directory `dirIndex`.

```java
int[] countsByExtension = new int[extensions.length];
long[][] directoryExtensionMatrix = new long[directories.size()][extensions.length];
```

## JAVA-4. Mandatory collections of custom domain objects (`G3`)

To satisfy general requirement `G3`, the application maintains and uses at least two distinct Java collections of user-defined classes:
- `List<MonitoredDirectory>` (paired with a `Set<Path>` index for uniqueness) for monitored locations;
- `List<MultimediaFile>` and `Map<MediaType, List<MultimediaFile>>` for discovered multimedia files.

## JAVA-5. Small classes and methods with single responsibility

Model classes hold domain data and pure domain queries; `storage` classes handle file I/O (`locations.txt`, `catalog.bin`, `multimedia-report.txt`); `service` classes implement scanning, filtering and array statistics; `ui` classes handle console input/output. No class mixes console `Scanner`/`PrintStream` calls with file persistence.

## JAVA-6. Resource management with `try-with-resources`

Every `InputStream`, `OutputStream`, `Reader`, `Writer`, `DataInputStream`, `DataOutputStream` or `Stream<Path>` from `Files.walk`/`Files.list` is opened in a `try-with-resources` block so file descriptors are never leaked (especially important on Windows/WSL so files are not locked).

```java
try (Stream<Path> stream = Files.list(directory)) {
    return stream.filter(Files::isRegularFile).toList();
}
```

## JAVA-7. Pure domain logic, side effects at the boundaries

Filtering, searching, extension classification and primitive array computation (`StatisticsCalculator`) are pure operations over in-memory collections. Disk I/O (`LocationRepository`, `MultimediaScanner`, `ReportExporter`, `BinaryCatalogStorage`) and console I/O (`ConsoleMenu`) stay at the boundaries.

## JAVA-8. Clean compilation with zero warnings (`G8`)

All production and test code compiles with `javac -Xlint:all -Werror` with zero errors and zero warnings. No raw generic types, no unused imports, and no unchecked casts.

## JAVA-9. JavaDoc on all public classes and methods (`G9`)

Every public class, enum, constructor and method in `src/main/java/` has a concise JavaDoc comment (`/** ... */`) describing its purpose, parameters (`@param`), return value (`@return`) and thrown exceptions (`@throws`), so `javadoc` generates complete documentation without errors (`G9`).
