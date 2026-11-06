package ro.ase.multimedia.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import ro.ase.multimedia.exception.DuplicateLocationException;
import ro.ase.multimedia.exception.InvalidDirectoryException;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;
import ro.ase.multimedia.storage.LocationRepository;

/**
 * Coordinates the in-memory catalog of monitored directories and discovered multimedia files,
 * synchronizing directory state with {@link LocationRepository}.
 */
public final class MultimediaCatalog {

    // TODO(TD-location-management-1): Integrate StatisticsCalculator (1D/2D primitive arrays),
    // BinaryCatalogStorage, and ReportExporter in statistics-and-reports.
    private final LocationRepository locationRepository;
    private final MultimediaScanner scanner;
    private final List<MonitoredDirectory> monitoredDirectories = new ArrayList<>();
    private final Set<Path> monitoredPaths = new HashSet<>();
    private final List<MultimediaFile> multimediaFiles = new ArrayList<>();
    private final Map<MediaType, List<MultimediaFile>> filesByMediaType = new EnumMap<>(MediaType.class);

    /**
     * Constructs a catalog backed by the specified location repository and a default {@link MultimediaScanner}.
     *
     * @param locationRepository the repository managing {@code locations.txt} (must not be {@code null})
     * @throws IllegalArgumentException if {@code locationRepository} is {@code null}
     */
    public MultimediaCatalog(LocationRepository locationRepository) {
        this(locationRepository, new MultimediaScanner());
    }

    /**
     * Constructs a catalog backed by the specified location repository and multimedia scanner.
     *
     * @param locationRepository the repository managing {@code locations.txt} (must not be {@code null})
     * @param scanner            the directory scanner used to discover multimedia files (must not be {@code null})
     * @throws IllegalArgumentException if {@code locationRepository} or {@code scanner} is {@code null}
     */
    public MultimediaCatalog(LocationRepository locationRepository, MultimediaScanner scanner) {
        if (locationRepository == null) {
            throw new IllegalArgumentException("LocationRepository must not be null.");
        }
        if (scanner == null) {
            throw new IllegalArgumentException("MultimediaScanner must not be null.");
        }
        this.locationRepository = locationRepository;
        this.scanner = scanner;
        for (MediaType type : MediaType.values()) {
            filesByMediaType.put(type, List.of());
        }
    }

    /**
     * Classifies the availability status of a directory path on the file system without throwing on missing
     * or unreadable paths.
     *
     * @param path the directory path to inspect (must not be {@code null})
     * @return {@link DirectoryStatus#ACCESSIBLE}, {@link DirectoryStatus#MISSING}, or {@link DirectoryStatus#INACCESSIBLE}
     * @throws IllegalArgumentException if {@code path} is {@code null}
     */
    public static DirectoryStatus classifyDirectory(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Directory path must not be null.");
        }
        try {
            if (!Files.exists(path)) {
                return DirectoryStatus.MISSING;
            }
            if (!Files.isDirectory(path) || !Files.isReadable(path)) {
                return DirectoryStatus.INACCESSIBLE;
            }
            return DirectoryStatus.ACCESSIBLE;
        } catch (SecurityException ex) {
            return DirectoryStatus.INACCESSIBLE;
        }
    }

    /**
     * Restores monitored directories from {@code locations.txt}, deduplicating normalized paths,
     * classifying each directory's current status on disk, and collecting non-fatal startup warnings.
     *
     * @return an unmodifiable list of startup warning messages
     * @throws StorageOperationException if reading or creating {@code locations.txt} fails
     */
    public List<String> initialize() {
        LocationRepository.LoadResult loadResult = locationRepository.load();
        monitoredDirectories.clear();
        monitoredPaths.clear();

        List<String> warnings = new ArrayList<>(loadResult.warnings());
        String timestamp = Instant.now().toString();

        for (Path rawPath : loadResult.paths()) {
            Path normalized = rawPath.toAbsolutePath().normalize();
            if (!monitoredPaths.add(normalized)) {
                warnings.add("Duplicate monitored location ignored: " + normalized);
                continue;
            }
            DirectoryStatus status = classifyDirectory(normalized);
            monitoredDirectories.add(new MonitoredDirectory(normalized, status, timestamp));
            if (status == DirectoryStatus.MISSING) {
                warnings.add("Monitored directory is missing on disk: " + normalized);
            } else if (status == DirectoryStatus.INACCESSIBLE) {
                warnings.add("Monitored directory is inaccessible or not a directory: " + normalized);
            }
        }

        warnings.addAll(rescan());
        return List.copyOf(warnings);
    }

    /**
     * Validates, adds, persists, and scans a new directory in the monitored locations catalog.
     *
     * @param rawPath the user-supplied directory path string (must not be {@code null} or blank)
     * @return the newly added {@link MonitoredDirectory}
     * @throws InvalidDirectoryException  if the path is null, blank, syntactically invalid, missing, not a directory, or unreadable
     * @throws DuplicateLocationException if the normalized directory is already monitored
     * @throws StorageOperationException  if persisting the updated directory list fails
     */
    public MonitoredDirectory addDirectory(String rawPath) {
        Path normalized = parseAndValidatePathSyntax(rawPath);

        try {
            if (!Files.exists(normalized)) {
                throw new InvalidDirectoryException("Directory does not exist: " + normalized);
            }
            if (!Files.isDirectory(normalized)) {
                throw new InvalidDirectoryException("Path is not a directory: " + normalized);
            }
            if (!Files.isReadable(normalized)) {
                throw new InvalidDirectoryException("Directory is not readable: " + normalized);
            }
        } catch (SecurityException ex) {
            throw new InvalidDirectoryException("Directory access denied by security manager: " + normalized, ex);
        }

        if (monitoredPaths.contains(normalized)) {
            throw new DuplicateLocationException("Directory is already monitored: " + normalized);
        }

        MonitoredDirectory entry = new MonitoredDirectory(
                normalized, DirectoryStatus.ACCESSIBLE, Instant.now().toString());
        monitoredDirectories.add(entry);
        monitoredPaths.add(normalized);
        locationRepository.save(monitoredDirectories);
        rescan();
        return entry;
    }

    /**
     * Removes a monitored directory reference from the in-memory catalog and from {@code locations.txt}
     * without deleting or modifying any files or directories on disk, and refreshes discovered files.
     *
     * @param rawPath the path of the monitored directory to remove (must not be {@code null} or blank)
     * @return the removed {@link MonitoredDirectory}
     * @throws InvalidDirectoryException if the path string is null, blank, or syntactically invalid
     * @throws LocationNotFoundException if the normalized path is not currently in the monitored list
     * @throws StorageOperationException if persisting the updated directory list fails
     */
    public MonitoredDirectory removeDirectory(String rawPath) {
        Path normalized = parseAndValidatePathSyntax(rawPath);

        int foundIndex = -1;
        for (int i = 0; i < monitoredDirectories.size(); i++) {
            if (monitoredDirectories.get(i).getPath().equals(normalized)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex < 0) {
            throw new LocationNotFoundException("Directory is not in the monitored list: " + normalized);
        }

        MonitoredDirectory removed = monitoredDirectories.remove(foundIndex);
        monitoredPaths.remove(normalized);
        locationRepository.save(monitoredDirectories);
        rescan();
        return removed;
    }

    /**
     * Refreshes monitored directory statuses and rescans all {@link DirectoryStatus#ACCESSIBLE}
     * directories on disk, rebuilding the in-memory multimedia file collections and deduplicating
     * files across overlapping directory roots.
     *
     * @return an unmodifiable list of non-fatal warning messages encountered during the scan
     */
    public List<String> rescan() {
        refreshDirectoryStatuses();
        multimediaFiles.clear();

        Map<MediaType, List<MultimediaFile>> grouped = new EnumMap<>(MediaType.class);
        for (MediaType type : MediaType.values()) {
            grouped.put(type, new ArrayList<>());
        }

        Set<Path> seenFullPaths = new HashSet<>();
        List<String> warnings = new ArrayList<>();

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

        for (MediaType type : MediaType.values()) {
            filesByMediaType.put(type, List.copyOf(grouped.get(type)));
        }

        return List.copyOf(warnings);
    }

    /**
     * Re-evaluates the disk availability status ({@code ACCESSIBLE}, {@code MISSING}, {@code INACCESSIBLE})
     * of every currently monitored directory in memory.
     */
    public void refreshDirectoryStatuses() {
        for (int i = 0; i < monitoredDirectories.size(); i++) {
            MonitoredDirectory current = monitoredDirectories.get(i);
            DirectoryStatus updatedStatus = classifyDirectory(current.getPath());
            if (current.getStatus() != updatedStatus) {
                monitoredDirectories.set(i, current.withStatus(updatedStatus));
            }
        }
    }

    /**
     * Refreshes directory statuses and saves the current in-memory monitored directory list to {@code locations.txt}.
     *
     * @throws StorageOperationException if writing {@code locations.txt} fails
     */
    public void saveAllOnExit() {
        refreshDirectoryStatuses();
        locationRepository.save(monitoredDirectories);
    }

    /**
     * Returns an unmodifiable snapshot of the currently monitored directories in insertion order.
     *
     * @return an unmodifiable {@link List} of {@link MonitoredDirectory} entries
     */
    public List<MonitoredDirectory> getMonitoredDirectories() {
        return List.copyOf(monitoredDirectories);
    }

    /**
     * Returns an unmodifiable snapshot of all discovered multimedia files across accessible directories.
     *
     * @return an unmodifiable {@link List} of {@link MultimediaFile} entries
     */
    public List<MultimediaFile> getMultimediaFiles() {
        return List.copyOf(multimediaFiles);
    }

    /**
     * Returns an unmodifiable map of discovered multimedia files grouped by {@link MediaType}.
     *
     * @return an unmodifiable {@link Map} from {@link MediaType} to unmodifiable {@link List} of {@link MultimediaFile}
     */
    public Map<MediaType, List<MultimediaFile>> getFilesByMediaType() {
        return Map.copyOf(filesByMediaType);
    }

    /**
     * Filters discovered multimedia files by their {@link MediaType} category ({@code AUDIO} or {@code IMAGE}).
     *
     * @param mediaType the target media type (must not be {@code null})
     * @return an unmodifiable {@link List} of matching {@link MultimediaFile} entries
     * @throws IllegalArgumentException if {@code mediaType} is {@code null}
     */
    public List<MultimediaFile> filterByMediaType(MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("MediaType must not be null.");
        }
        return List.copyOf(filesByMediaType.getOrDefault(mediaType, List.of()));
    }

    /**
     * Filters discovered multimedia files by a supported file extension in a case-insensitive manner,
     * accepting extensions with or without a leading dot (e.g. {@code ".mp3"} or {@code "MP3"}).
     *
     * @param extension the file extension to filter by (must be a supported extension)
     * @return an unmodifiable {@link List} of matching {@link MultimediaFile} entries
     * @throws IllegalArgumentException if {@code extension} is {@code null}, blank, or unsupported
     */
    public List<MultimediaFile> filterByExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension must not be null or blank.");
        }
        if (MediaType.fromExtension(extension).isEmpty()) {
            throw new IllegalArgumentException("Unsupported multimedia extension: " + extension);
        }
        String trimmed = extension.trim().toLowerCase(Locale.ROOT);
        String normalized = trimmed.startsWith(".") ? trimmed : "." + trimmed;
        return multimediaFiles.stream()
                .filter(f -> f.getExtension().equals(normalized))
                .toList();
    }

    /**
     * Searches discovered multimedia files by a case-insensitive filename substring.
     *
     * @param query the filename substring to search for (must not be {@code null} or blank)
     * @return an unmodifiable {@link List} of matching {@link MultimediaFile} entries
     * @throws IllegalArgumentException if {@code query} is {@code null} or blank
     */
    public List<MultimediaFile> searchByName(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be null or blank.");
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return multimediaFiles.stream()
                .filter(f -> f.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    private static Path parseAndValidatePathSyntax(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new InvalidDirectoryException("Directory path must not be null or blank.");
        }
        try {
            return Path.of(rawPath.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new InvalidDirectoryException("Invalid directory path syntax: " + rawPath, ex);
        }
    }
}
