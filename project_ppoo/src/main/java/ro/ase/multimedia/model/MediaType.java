package ro.ase.multimedia.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Supported multimedia categories and extension classification utilities.
 */
public enum MediaType {
    /** Audio file formats ({@code .mp3}, {@code .wav}). */
    AUDIO,
    /** Image file formats ({@code .jpg}, {@code .png}). */
    IMAGE;

    /** Immutable list of all supported lowercase file extensions (with leading dot). */
    public static final List<String> SUPPORTED_EXTENSIONS = List.of(".mp3", ".wav", ".jpg", ".png");

    /**
     * Extracts and normalizes a supported extension (including the leading dot) from a file name or path string.
     *
     * @param fileName the file name or path to inspect
     * @return an {@link Optional} containing the lowercase extension (e.g. {@code ".mp3"}) if supported,
     *         or {@link Optional#empty()} if missing, blank, hidden without extension, or unsupported
     */
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

    /**
     * Resolves a {@link MediaType} from a file extension string in a case-insensitive manner.
     * Accepts extensions with or without a leading dot (e.g. {@code ".mp3"} or {@code "MP3"}).
     *
     * @param extension the extension string to resolve
     * @return an {@link Optional} with the matching {@link MediaType}, or {@link Optional#empty()} if unsupported
     */
    public static Optional<MediaType> fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        String trimmed = extension.trim().toLowerCase(Locale.ROOT);
        String normalized = trimmed.startsWith(".") ? trimmed : "." + trimmed;
        return switch (normalized) {
            case ".mp3", ".wav" -> Optional.of(AUDIO);
            case ".jpg", ".png" -> Optional.of(IMAGE);
            default -> Optional.empty();
        };
    }

    /**
     * Resolves a {@link MediaType} from a file name or path string by extracting its extension.
     *
     * @param fileName the file name or path to classify
     * @return an {@link Optional} with the matching {@link MediaType}, or {@link Optional#empty()} if unsupported
     */
    public static Optional<MediaType> fromFileName(String fileName) {
        return extractExtension(fileName).flatMap(MediaType::fromExtension);
    }
}
