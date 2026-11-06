package ro.ase.multimedia.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ro.ase.multimedia.exception.DuplicateLocationException;
import ro.ase.multimedia.exception.InvalidDirectoryException;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.storage.LocationRepository;

/**
 * Coordinates the in-memory catalog of monitored directories and synchronizes state with {@link LocationRepository}.
 */
public final class MultimediaCatalog {

    // TODO(TD-location-management-1): Integrate MultimediaScanner, StatisticsCalculator (1D/2D primitive arrays),
    // BinaryCatalogStorage, and ReportExporter in multimedia-scanner-filter and statistics-and-reports.
    private final LocationRepository locationRepository;
    private final List<MonitoredDirectory> monitoredDirectories = new ArrayList<>();
    private final Set<Path> monitoredPaths = new HashSet<>();

    /**
     * Constructs a catalog backed by the specified location repository.
     *
     * @param locationRepository the repository managing {@code locations.txt} (must not be {@code null})
     * @throws IllegalArgumentException if {@code locationRepository} is {@code null}
     */
    public MultimediaCatalog(LocationRepository locationRepository) {
        if (locationRepository == null) {
            throw new IllegalArgumentException("LocationRepository must not be null.");
        }
        this.locationRepository = locationRepository;
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

        return List.copyOf(warnings);
    }

    /**
     * Validates, adds, and persists a new directory to the monitored locations catalog.
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
        return entry;
    }

    /**
     * Removes a monitored directory reference from the in-memory catalog and from {@code locations.txt}
     * without deleting or modifying any files or directories on disk.
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
        return removed;
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
