package ro.ase.multimedia.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a directory monitored by the application along with its availability status on disk.
 */
public final class MonitoredDirectory {

    private final Path path;
    private final DirectoryStatus status;
    private final String addedAt;

    /**
     * Constructs a monitored directory with a normalized absolute path, status, and timestamp.
     *
     * @param path    the file-system path to monitor (must not be {@code null})
     * @param status  the current availability status on disk (must not be {@code null})
     * @param addedAt the timestamp or label recording when the directory was added (must not be blank)
     * @throws IllegalArgumentException if any argument is {@code null} or if {@code addedAt} is blank
     */
    public MonitoredDirectory(Path path, DirectoryStatus status, String addedAt) {
        if (path == null) {
            throw new IllegalArgumentException("Monitored directory path must not be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Directory status must not be null.");
        }
        if (addedAt == null || addedAt.isBlank()) {
            throw new IllegalArgumentException("Added timestamp must not be null or blank.");
        }
        this.path = path.toAbsolutePath().normalize();
        this.status = status;
        this.addedAt = addedAt.trim();
    }

    /**
     * Returns the normalized absolute path of this monitored directory.
     *
     * @return the normalized directory {@link Path}
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the current disk availability status of this monitored directory.
     *
     * @return the {@link DirectoryStatus}
     */
    public DirectoryStatus getStatus() {
        return status;
    }

    /**
     * Returns the timestamp string when this directory was added to monitoring.
     *
     * @return the trimmed timestamp string
     */
    public String getAddedAt() {
        return addedAt;
    }

    /**
     * Returns a new {@link MonitoredDirectory} instance with the same path and timestamp
     * but an updated {@link DirectoryStatus}.
     *
     * @param newStatus the updated availability status (must not be {@code null})
     * @return a new {@link MonitoredDirectory} with {@code newStatus}
     * @throws IllegalArgumentException if {@code newStatus} is {@code null}
     */
    public MonitoredDirectory withStatus(DirectoryStatus newStatus) {
        return new MonitoredDirectory(this.path, newStatus, this.addedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MonitoredDirectory that)) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path);
    }

    @Override
    public String toString() {
        return "MonitoredDirectory{path=" + path + ", status=" + status + ", addedAt='" + addedAt + "'}";
    }
}
