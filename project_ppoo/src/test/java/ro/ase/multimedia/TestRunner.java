package ro.ase.multimedia;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import ro.ase.multimedia.exception.DuplicateLocationException;
import ro.ase.multimedia.exception.InvalidDirectoryException;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.MultimediaException;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;

/**
 * Self-contained automated unit test runner for the Multimedia File Manager.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    private TestRunner() {
    }

    /**
     * Runs all unit tests and exits with status 0 on success or 1 on any failure.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        runTest("T1 mediaType_resolvesSupportedExtensionsCaseInsensitivelyAndHandlesMultiDots",
                TestRunner::mediaType_resolvesSupportedExtensionsCaseInsensitivelyAndHandlesMultiDots);
        runTest("T2 mediaType_rejectsUnsupportedBlankOrMissingExtensions",
                TestRunner::mediaType_rejectsUnsupportedBlankOrMissingExtensions);
        runTest("T3 monitoredDirectory_normalizesPathAndUpdatesStatusImmutably",
                TestRunner::monitoredDirectory_normalizesPathAndUpdatesStatusImmutably);
        runTest("T4 monitoredDirectory_rejectsNullOrBlankArguments",
                TestRunner::monitoredDirectory_rejectsNullOrBlankArguments);
        runTest("T5 multimediaFile_storesNormalizedAttributesForValidInput",
                TestRunner::multimediaFile_storesNormalizedAttributesForValidInput);
        runTest("T6 multimediaFile_rejectsNegativeSizeUnsupportedExtensionOrMismatchedMediaType",
                TestRunner::multimediaFile_rejectsNegativeSizeUnsupportedExtensionOrMismatchedMediaType);
        runTest("T7 customExceptions_preserveMessageAndCauseInHierarchy",
                TestRunner::customExceptions_preserveMessageAndCauseInHierarchy);

        System.out.printf("Tests complete: %d passed, %d failed.%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] " + name + " -> " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Throwable> T assertThrows(
            Class<T> expectedType, Runnable executable, String message) {
        try {
            executable.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return expectedType.cast(actual);
            }
            throw new AssertionError(
                    message + " (expected " + expectedType.getSimpleName() + " but caught " + actual + ")", actual);
        }
        throw new AssertionError(message + " (expected " + expectedType.getSimpleName() + " but nothing was thrown)");
    }

    private static void mediaType_resolvesSupportedExtensionsCaseInsensitivelyAndHandlesMultiDots() {
        assertEquals(4, MediaType.SUPPORTED_EXTENSIONS.size(), "Four supported extensions");
        assertEquals(Optional.of(MediaType.AUDIO), MediaType.fromExtension(".mp3"), ".mp3 is AUDIO");
        assertEquals(Optional.of(MediaType.AUDIO), MediaType.fromExtension("WAV"), "WAV without dot is AUDIO");
        assertEquals(Optional.of(MediaType.IMAGE), MediaType.fromExtension(".JpG"), ".JpG is IMAGE");
        assertEquals(Optional.of(MediaType.IMAGE), MediaType.fromExtension(".PNG"), ".PNG is IMAGE");

        assertEquals(Optional.of(".jpg"), MediaType.extractExtension("archive.tar.JPG"), "Multi-dot extension extraction");
        assertEquals(Optional.of(MediaType.IMAGE), MediaType.fromFileName("nested/path/archive.tar.JPG"), "Multi-dot file name");
        assertEquals(Optional.of(MediaType.AUDIO), MediaType.fromFileName("C:\\Music\\song.mp3"), "Windows path separator");
        assertEquals(DirectoryStatus.ACCESSIBLE, DirectoryStatus.valueOf("ACCESSIBLE"), "DirectoryStatus enum value");
    }

    private static void mediaType_rejectsUnsupportedBlankOrMissingExtensions() {
        assertEquals(Optional.empty(), MediaType.fromExtension(".txt"), "Unsupported .txt");
        assertEquals(Optional.empty(), MediaType.fromExtension(".mp4"), "Unsupported .mp4");
        assertEquals(Optional.empty(), MediaType.fromExtension(null), "Null extension");
        assertEquals(Optional.empty(), MediaType.fromExtension("   "), "Blank extension");

        assertEquals(Optional.empty(), MediaType.extractExtension("noext"), "No extension");
        assertEquals(Optional.empty(), MediaType.extractExtension(".hidden"), "Dotfile with no extension");
        assertEquals(Optional.empty(), MediaType.extractExtension("trailing."), "Trailing dot");
        assertEquals(Optional.empty(), MediaType.extractExtension(null), "Null fileName");
        assertEquals(Optional.empty(), MediaType.extractExtension(""), "Empty fileName");
        assertTrue(MediaType.fromFileName("document.pdf").isEmpty(), "Unsupported fileName returns empty");
    }

    private static void monitoredDirectory_normalizesPathAndUpdatesStatusImmutably() {
        Path rawPath = Path.of("media", "sub", "..", "music");
        MonitoredDirectory dir = new MonitoredDirectory(rawPath, DirectoryStatus.ACCESSIBLE, " 2026-11-06T10:00:00Z ");
        assertEquals(rawPath.toAbsolutePath().normalize(), dir.getPath(), "Normalized absolute path");
        assertEquals(DirectoryStatus.ACCESSIBLE, dir.getStatus(), "Initial status");
        assertEquals("2026-11-06T10:00:00Z", dir.getAddedAt(), "Trimmed timestamp");

        MonitoredDirectory updated = dir.withStatus(DirectoryStatus.MISSING);
        assertEquals(DirectoryStatus.MISSING, updated.getStatus(), "Updated status");
        assertEquals(DirectoryStatus.ACCESSIBLE, dir.getStatus(), "Original instance remains immutable");
        assertEquals(dir, updated, "Equality based on normalized path");
        assertEquals(dir.hashCode(), updated.hashCode(), "HashCode based on normalized path");
    }

    private static void monitoredDirectory_rejectsNullOrBlankArguments() {
        Path validPath = Path.of("media");
        assertThrows(IllegalArgumentException.class,
                () -> new MonitoredDirectory(null, DirectoryStatus.ACCESSIBLE, "2026-11-06"),
                "Null path rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new MonitoredDirectory(validPath, null, "2026-11-06"),
                "Null status rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new MonitoredDirectory(validPath, DirectoryStatus.ACCESSIBLE, "   "),
                "Blank addedAt rejected");
    }

    private static void multimediaFile_storesNormalizedAttributesForValidInput() {
        Path dirPath = Path.of("media", "music");
        Path filePath = dirPath.resolve("track01.MP3");
        MultimediaFile audio = new MultimediaFile(" track01.MP3 ", ".MP3", MediaType.AUDIO, 4096L, dirPath, filePath);

        assertEquals("track01.MP3", audio.getName(), "Trimmed file name");
        assertEquals(".mp3", audio.getExtension(), "Normalized lowercase extension");
        assertEquals(MediaType.AUDIO, audio.getMediaType(), "MediaType AUDIO");
        assertEquals(4096L, audio.getSizeBytes(), "File size in bytes");
        assertEquals(dirPath.toAbsolutePath().normalize(), audio.getDirectoryPath(), "Normalized directory path");
        assertEquals(filePath.toAbsolutePath().normalize(), audio.getFullPath(), "Normalized full path");

        MultimediaFile image = new MultimediaFile("cover.png", "PNG", MediaType.IMAGE, 0L, dirPath, dirPath.resolve("cover.png"));
        assertEquals(".png", image.getExtension(), "Extension without leading dot normalized");
        assertEquals(MediaType.IMAGE, image.getMediaType(), "MediaType IMAGE");
    }

    private static void multimediaFile_rejectsNegativeSizeUnsupportedExtensionOrMismatchedMediaType() {
        Path dirPath = Path.of("media");
        Path filePath = dirPath.resolve("song.mp3");

        assertThrows(IllegalArgumentException.class,
                () -> new MultimediaFile("song.mp3", ".mp3", MediaType.AUDIO, -1L, dirPath, filePath),
                "Negative size rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new MultimediaFile("notes.txt", ".txt", MediaType.AUDIO, 100L, dirPath, dirPath.resolve("notes.txt")),
                "Unsupported extension rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new MultimediaFile("song.mp3", ".mp3", MediaType.IMAGE, 100L, dirPath, filePath),
                "Mismatched MediaType rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new MultimediaFile("  ", ".mp3", MediaType.AUDIO, 100L, dirPath, filePath),
                "Blank name rejected");
    }

    private static void customExceptions_preserveMessageAndCauseInHierarchy() {
        IOException rootCause = new IOException("Disk I/O failure");

        MultimediaException base = new MultimediaException("Base message", rootCause);
        assertEquals("Base message", base.getMessage(), "Base message preserved");
        assertEquals(rootCause, base.getCause(), "Base cause preserved");
        assertTrue(base instanceof RuntimeException, "MultimediaException extends RuntimeException");

        InvalidDirectoryException invalidDir = new InvalidDirectoryException("Invalid path");
        assertEquals("Invalid path", invalidDir.getMessage(), "InvalidDirectoryException message");
        assertTrue(invalidDir instanceof MultimediaException, "InvalidDirectoryException extends MultimediaException");

        InvalidDirectoryException invalidDirWithCause = new InvalidDirectoryException("Invalid path", rootCause);
        assertEquals(rootCause, invalidDirWithCause.getCause(), "InvalidDirectoryException cause");

        DuplicateLocationException duplicate = new DuplicateLocationException("Duplicate path", rootCause);
        assertEquals("Duplicate path", duplicate.getMessage(), "DuplicateLocationException message");
        assertEquals(rootCause, duplicate.getCause(), "DuplicateLocationException cause");

        LocationNotFoundException notFound = new LocationNotFoundException("Not found", rootCause);
        assertEquals("Not found", notFound.getMessage(), "LocationNotFoundException message");
        assertEquals(rootCause, notFound.getCause(), "LocationNotFoundException cause");

        StorageOperationException storage = new StorageOperationException("Storage failed", rootCause);
        assertEquals("Storage failed", storage.getMessage(), "StorageOperationException message");
        assertEquals(rootCause, storage.getCause(), "StorageOperationException cause");
    }
}
