package ro.ase.multimedia.model;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable representation of a cataloged multimedia file discovered inside a monitored directory.
 */
public final class MultimediaFile {

    private final String name;
    private final String extension;
    private final MediaType mediaType;
    private final long sizeBytes;
    private final Path directoryPath;
    private final Path fullPath;

    /**
     * Constructs a validated multimedia file entry.
     *
     * @param name          the file name (must not be {@code null} or blank)
     * @param extension     the file extension, e.g. {@code ".mp3"} (must be supported and match {@code mediaType})
     * @param mediaType     the multimedia category (must not be {@code null} and must match {@code extension})
     * @param sizeBytes     the file size in bytes (must be {@code >= 0})
     * @param directoryPath the monitored root directory path containing this file (must not be {@code null})
     * @param fullPath      the full file path on disk (must not be {@code null})
     * @throws IllegalArgumentException if any invariant is violated
     */
    public MultimediaFile(
            String name,
            String extension,
            MediaType mediaType,
            long sizeBytes,
            Path directoryPath,
            Path fullPath) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("File name must not be null or blank.");
        }
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("File extension must not be null or blank.");
        }
        String trimmedExt = extension.trim().toLowerCase(Locale.ROOT);
        String normalizedExt = trimmedExt.startsWith(".") ? trimmedExt : "." + trimmedExt;
        MediaType resolvedType = MediaType.fromExtension(normalizedExt)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported extension: " + extension));
        if (mediaType == null || resolvedType != mediaType) {
            throw new IllegalArgumentException("Extension " + normalizedExt + " does not match " + mediaType);
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("File size must not be negative: " + sizeBytes);
        }
        if (directoryPath == null) {
            throw new IllegalArgumentException("Directory path must not be null.");
        }
        if (fullPath == null) {
            throw new IllegalArgumentException("Full file path must not be null.");
        }

        this.name = name.trim();
        this.extension = normalizedExt;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.directoryPath = directoryPath.toAbsolutePath().normalize();
        this.fullPath = fullPath.toAbsolutePath().normalize();
    }

    /**
     * Returns the trimmed file name.
     *
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the normalized lowercase extension including the leading dot (e.g. {@code ".mp3"}).
     *
     * @return the normalized file extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns the multimedia category of this file.
     *
     * @return the {@link MediaType}
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Returns the file size in bytes.
     *
     * @return non-negative size in bytes
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * Returns the normalized absolute path of the monitored directory that owns this file.
     *
     * @return the monitored directory {@link Path}
     */
    public Path getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Returns the normalized absolute path of this multimedia file on disk.
     *
     * @return the full file {@link Path}
     */
    public Path getFullPath() {
        return fullPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultimediaFile that)) {
            return false;
        }
        return Objects.equals(fullPath, that.fullPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullPath);
    }

    @Override
    public String toString() {
        return "MultimediaFile{name='" + name + "', extension='" + extension + "', mediaType=" + mediaType
                + ", sizeBytes=" + sizeBytes + ", fullPath=" + fullPath + "}";
    }
}
