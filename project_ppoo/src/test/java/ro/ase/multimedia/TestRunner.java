package ro.ase.multimedia;

import java.util.Optional;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MediaType;

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
}
