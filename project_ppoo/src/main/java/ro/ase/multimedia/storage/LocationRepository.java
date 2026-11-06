package ro.ase.multimedia.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.MonitoredDirectory;

/**
 * Persists and restores monitored directory paths in a UTF-8 text file ({@code locations.txt}).
 */
public final class LocationRepository {

    private final Path locationsFile;

    /**
     * Constructs a repository targeting the specified locations file path.
     *
     * @param locationsFile the path to the locations persistence file (must not be {@code null})
     * @throws IllegalArgumentException if {@code locationsFile} is {@code null}
     */
    public LocationRepository(Path locationsFile) {
        if (locationsFile == null) {
            throw new IllegalArgumentException("Locations file path must not be null.");
        }
        this.locationsFile = locationsFile.toAbsolutePath().normalize();
    }

    /**
     * Returns the normalized absolute path of the persistence file managed by this repository.
     *
     * @return the normalized {@link Path} to {@code locations.txt}
     */
    public Path getLocationsFile() {
        return locationsFile;
    }

    /**
     * Loads monitored directory paths from the persistence file.
     * If the file does not exist yet, creates parent directories if needed and initializes an empty file.
     * Blank lines are ignored, and malformed path lines are collected as warnings without aborting startup.
     *
     * @return a {@link LoadResult} containing parsed normalized paths and any non-fatal warnings
     * @throws StorageOperationException if reading or creating the locations file fails due to an I/O error
     */
    public LoadResult load() {
        try {
            if (!Files.exists(locationsFile)) {
                if (locationsFile.getParent() != null) {
                    Files.createDirectories(locationsFile.getParent());
                }
                Files.createFile(locationsFile);
                return new LoadResult(List.of(), List.of());
            }

            List<String> rawLines = Files.readAllLines(locationsFile, StandardCharsets.UTF_8);
            List<Path> paths = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            for (int i = 0; i < rawLines.size(); i++) {
                String line = rawLines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                String trimmed = line.trim();
                try {
                    Path normalized = Path.of(trimmed).toAbsolutePath().normalize();
                    paths.add(normalized);
                } catch (InvalidPathException ex) {
                    warnings.add("Line " + (i + 1) + ": invalid directory path '" + trimmed + "' (" + ex.getMessage() + ")");
                }
            }

            return new LoadResult(paths, warnings);
        } catch (IOException ex) {
            throw new StorageOperationException(
                    "Failed to load monitored locations from: " + locationsFile, ex);
        }
    }

    /**
     * Saves the given list of monitored directories to the persistence file in UTF-8 encoding,
     * writing one normalized path per line via a sibling temporary file and atomic replacement.
     *
     * @param directories the monitored directories to persist (must not be {@code null} or contain {@code null})
     * @throws IllegalArgumentException  if {@code directories} is {@code null} or contains a {@code null} element
     * @throws StorageOperationException if writing or moving the persistence file fails
     */
    public void save(List<MonitoredDirectory> directories) {
        if (directories == null) {
            throw new IllegalArgumentException("Monitored directories list must not be null.");
        }
        List<String> lines = new ArrayList<>(directories.size());
        for (MonitoredDirectory directory : directories) {
            if (directory == null) {
                throw new IllegalArgumentException("Monitored directory entry must not be null.");
            }
            lines.add(directory.getPath().toAbsolutePath().normalize().toString());
        }

        Path tempFile = locationsFile.resolveSibling(locationsFile.getFileName() + ".tmp");
        try {
            if (locationsFile.getParent() != null) {
                Files.createDirectories(locationsFile.getParent());
            }
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, locationsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, locationsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw new StorageOperationException(
                    "Failed to save monitored locations to: " + locationsFile, ex);
        }
    }

    /**
     * Immutable result of loading monitored paths from {@code locations.txt}.
     *
     * @param paths    the list of valid, normalized directory paths read from the file
     * @param warnings the list of non-fatal warning messages for malformed lines
     */
    public record LoadResult(List<Path> paths, List<String> warnings) {

        /**
         * Constructs an immutable load result with defensive copies of paths and warnings.
         *
         * @param paths    the parsed directory paths (must not be {@code null})
         * @param warnings the warning messages (must not be {@code null})
         * @throws IllegalArgumentException if {@code paths} or {@code warnings} is {@code null}
         */
        public LoadResult {
            if (paths == null) {
                throw new IllegalArgumentException("Paths list must not be null.");
            }
            if (warnings == null) {
                throw new IllegalArgumentException("Warnings list must not be null.");
            }
            paths = List.copyOf(paths);
            warnings = List.copyOf(warnings);
        }
    }
}
