package ro.ase.multimedia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import ro.ase.multimedia.exception.DuplicateLocationException;
import ro.ase.multimedia.exception.InvalidDirectoryException;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.MultimediaException;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;
import ro.ase.multimedia.service.MultimediaCatalog;
import ro.ase.multimedia.service.MultimediaScanner;
import ro.ase.multimedia.service.StatisticsCalculator;
import ro.ase.multimedia.storage.LocationRepository;

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
        runTest("T8 main_runsFoundationEntryPointCleanly",
                TestRunner::main_runsFoundationEntryPointCleanly);
        runTest("T9 locationRepository_loadCreatesMissingFileAndParsesValidLinesWithWarningsForMalformed",
                TestRunner::locationRepository_loadCreatesMissingFileAndParsesValidLinesWithWarningsForMalformed);
        runTest("T10 locationRepository_saveWritesUtf8LinesAtomicallyAndCleansUpTempFile",
                TestRunner::locationRepository_saveWritesUtf8LinesAtomicallyAndCleansUpTempFile);
        runTest("T11 catalog_initializeRestoresDeduplicatedDirectoriesAndClassifiesStatusesWithWarnings",
                TestRunner::catalog_initializeRestoresDeduplicatedDirectoriesAndClassifiesStatusesWithWarnings);
        runTest("T12 catalog_addDirectoryAddsAccessibleDirectoryAndPersistsToLocationsFile",
                TestRunner::catalog_addDirectoryAddsAccessibleDirectoryAndPersistsToLocationsFile);
        runTest("T13 catalog_addDirectoryRejectsInvalidMissingFileOrDuplicatePaths",
                TestRunner::catalog_addDirectoryRejectsInvalidMissingFileOrDuplicatePaths);
        runTest("T14 catalog_removeDirectoryDeletesReferenceOnlyAndKeepsDiskFilesIntact",
                TestRunner::catalog_removeDirectoryDeletesReferenceOnlyAndKeepsDiskFilesIntact);
        runTest("T15 catalog_removeDirectoryRejectsUnmonitoredOrBlankPath",
                TestRunner::catalog_removeDirectoryRejectsUnmonitoredOrBlankPath);
        runTest("T16 catalog_saveAllOnExitRefreshesStatusesAndFlushesCurrentListToDisk",
                TestRunner::catalog_saveAllOnExitRefreshesStatusesAndFlushesCurrentListToDisk);
        runTest("T17 scanner_recursivelyFindsSupportedFilesSortedAndIgnoresUnsupportedOrMissing",
                TestRunner::scanner_recursivelyFindsSupportedFilesSortedAndIgnoresUnsupportedOrMissing);
        runTest("T18 catalog_scansAccessibleDirectoriesDeduplicatesOverlapsAndUpdatesOnAddRemoveRescan",
                TestRunner::catalog_scansAccessibleDirectoriesDeduplicatesOverlapsAndUpdatesOnAddRemoveRescan);
        runTest("T19 catalog_filterByMediaTypeReturnsMatchingAudioAndImageFilesAndRejectsNull",
                TestRunner::catalog_filterByMediaTypeReturnsMatchingAudioAndImageFilesAndRejectsNull);
        runTest("T20 catalog_filterByExtensionMatchesCaseInsensitivelyWithOrWithoutDotAndRejectsInvalid",
                TestRunner::catalog_filterByExtensionMatchesCaseInsensitivelyWithOrWithoutDotAndRejectsInvalid);
        runTest("T21 catalog_searchByNameFindsCaseInsensitiveSubstringMatchesAndRejectsBlankQuery",
                TestRunner::catalog_searchByNameFindsCaseInsensitiveSubstringMatchesAndRejectsBlankQuery);
        runTest("T22 statisticsCalculator_computes1DAnd2DPrimitiveArraysAndEnforcesDefensiveCopies",
                TestRunner::statisticsCalculator_computes1DAnd2DPrimitiveArraysAndEnforcesDefensiveCopies);
        runTest("T23 statisticsCalculator_handlesEmptyInputsAndRejectsNullArguments",
                TestRunner::statisticsCalculator_handlesEmptyInputsAndRejectsNullArguments);

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

    private static void main_runsFoundationEntryPointCleanly() {
        Main.main(new String[0]);
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to clean up temp directory: " + root, ex);
        }
    }

    private static void locationRepository_loadCreatesMissingFileAndParsesValidLinesWithWarningsForMalformed() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t9-");
            Path locationsFile = tempRoot.resolve("nested").resolve("locations.txt");
            LocationRepository repo = new LocationRepository(locationsFile);

            assertEquals(locationsFile.toAbsolutePath().normalize(), repo.getLocationsFile(), "Normalized locationsFile path");
            LocationRepository.LoadResult initialLoad = repo.load();
            assertTrue(Files.exists(locationsFile), "Missing locations.txt is created automatically");
            assertEquals(0, initialLoad.paths().size(), "Empty paths on newly created file");
            assertEquals(0, initialLoad.warnings().size(), "No warnings on newly created file");

            Path dirA = Files.createDirectory(tempRoot.resolve("music"));
            Path dirB = Files.createDirectory(tempRoot.resolve("photos"));
            String content = "   \n  " + dirA + "  \ninvalid\0path\n" + dirB.resolve("sub").resolve("..") + "\n";
            Files.writeString(locationsFile, content, StandardCharsets.UTF_8);

            LocationRepository.LoadResult secondLoad = repo.load();
            assertEquals(2, secondLoad.paths().size(), "Two valid paths loaded");
            assertEquals(dirA.toAbsolutePath().normalize(), secondLoad.paths().get(0), "First path normalized");
            assertEquals(dirB.toAbsolutePath().normalize(), secondLoad.paths().get(1), "Second path normalized");
            assertEquals(1, secondLoad.warnings().size(), "One warning recorded for malformed line");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void locationRepository_saveWritesUtf8LinesAtomicallyAndCleansUpTempFile() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t10-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            LocationRepository repo = new LocationRepository(locationsFile);

            Path dirA = tempRoot.resolve("dirA");
            Path dirB = tempRoot.resolve("dirB").resolve("inner").resolve("..");
            List<MonitoredDirectory> dirs = List.of(
                    new MonitoredDirectory(dirA, DirectoryStatus.ACCESSIBLE, "2026-11-06T12:00:00Z"),
                    new MonitoredDirectory(dirB, DirectoryStatus.MISSING, "2026-11-06T12:01:00Z")
            );

            repo.save(dirs);
            Path tempSibling = locationsFile.resolveSibling("locations.txt.tmp");
            assertTrue(Files.exists(locationsFile), "locations.txt exists after save");
            assertTrue(!Files.exists(tempSibling), "No leftover .tmp file after save");

            List<String> lines = Files.readAllLines(locationsFile, StandardCharsets.UTF_8);
            assertEquals(2, lines.size(), "Two lines written");
            assertEquals(dirA.toAbsolutePath().normalize().toString(), lines.get(0), "First saved line matches normalized dirA");
            assertEquals(dirB.toAbsolutePath().normalize().toString(), lines.get(1), "Second saved line matches normalized dirB");

            Path directoryTarget = Files.createDirectory(tempRoot.resolve("dir-as-file"));
            LocationRepository failingRepo = new LocationRepository(directoryTarget);
            assertThrows(StorageOperationException.class,
                    () -> failingRepo.save(dirs),
                    "Saving over a directory throws StorageOperationException");
            assertTrue(!Files.exists(directoryTarget.resolveSibling("dir-as-file.tmp")),
                    "Failed save cleans up temporary sibling file");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_initializeRestoresDeduplicatedDirectoriesAndClassifiesStatusesWithWarnings() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t11-");
            Path accessibleDir = Files.createDirectory(tempRoot.resolve("accessible"));
            Path missingDir = tempRoot.resolve("missing");
            Path regularFile = Files.writeString(tempRoot.resolve("not-a-dir.mp3"), "audio", StandardCharsets.UTF_8);

            Path locationsFile = tempRoot.resolve("locations.txt");
            List<String> rawEntries = List.of(
                    accessibleDir.toString(),
                    accessibleDir.resolve("sub").resolve("..").toString(),
                    missingDir.toString(),
                    regularFile.toString()
            );
            Files.write(locationsFile, rawEntries, StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            List<String> warnings = catalog.initialize();
            List<MonitoredDirectory> restored = catalog.getMonitoredDirectories();

            assertEquals(3, restored.size(), "Three unique entries restored after deduplication");
            assertEquals(DirectoryStatus.ACCESSIBLE, restored.get(0).getStatus(), "Existing directory is ACCESSIBLE");
            assertEquals(DirectoryStatus.MISSING, restored.get(1).getStatus(), "Non-existent directory is MISSING");
            assertEquals(DirectoryStatus.INACCESSIBLE, restored.get(2).getStatus(), "Regular file is classified as INACCESSIBLE");
            assertEquals(3, warnings.size(), "Warnings emitted for duplicate, missing, and inaccessible entries");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_addDirectoryAddsAccessibleDirectoryAndPersistsToLocationsFile() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t12-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path mediaDir = Files.createDirectory(tempRoot.resolve("albums"));

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();

            MonitoredDirectory added = catalog.addDirectory("  " + mediaDir.resolve("nested").resolve("..") + "  ");
            assertEquals(mediaDir.toAbsolutePath().normalize(), added.getPath(), "Added directory path is normalized");
            assertEquals(DirectoryStatus.ACCESSIBLE, added.getStatus(), "Added directory status is ACCESSIBLE");
            assertEquals(1, catalog.getMonitoredDirectories().size(), "In-memory catalog has 1 entry");

            List<String> persistedLines = Files.readAllLines(locationsFile, StandardCharsets.UTF_8);
            assertEquals(1, persistedLines.size(), "locations.txt immediately updated with 1 line");
            assertEquals(mediaDir.toAbsolutePath().normalize().toString(), persistedLines.get(0), "Persisted path matches");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_addDirectoryRejectsInvalidMissingFileOrDuplicatePaths() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t13-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path validDir = Files.createDirectory(tempRoot.resolve("valid"));
            Path missingDir = tempRoot.resolve("does-not-exist");
            Path regularFile = Files.writeString(tempRoot.resolve("track.mp3"), "data", StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();

            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.addDirectory("   "),
                    "Blank path rejected with InvalidDirectoryException");
            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.addDirectory(null),
                    "Null path rejected with InvalidDirectoryException");
            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.addDirectory("bad\0path"),
                    "Invalid syntax rejected with InvalidDirectoryException");
            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.addDirectory(missingDir.toString()),
                    "Non-existent directory rejected with InvalidDirectoryException");
            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.addDirectory(regularFile.toString()),
                    "Regular file rejected with InvalidDirectoryException");

            catalog.addDirectory(validDir.toString());
            assertThrows(DuplicateLocationException.class,
                    () -> catalog.addDirectory(validDir.resolve("sub").resolve("..").toString()),
                    "Normalized duplicate path rejected with DuplicateLocationException");
            assertEquals(1, catalog.getMonitoredDirectories().size(), "Catalog still holds only the single valid directory");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_removeDirectoryDeletesReferenceOnlyAndKeepsDiskFilesIntact() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t14-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path mediaDir = Files.createDirectory(tempRoot.resolve("library"));
            Path sampleMp3 = Files.writeString(mediaDir.resolve("song.mp3"), "mp3-bytes", StandardCharsets.UTF_8);
            Path ephemeralDir = Files.createDirectory(tempRoot.resolve("ephemeral"));

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();
            catalog.addDirectory(mediaDir.toString());
            catalog.addDirectory(ephemeralDir.toString());

            MonitoredDirectory removed = catalog.removeDirectory(mediaDir.toString());
            assertEquals(mediaDir.toAbsolutePath().normalize(), removed.getPath(), "Removed directory matches mediaDir");
            assertEquals(1, catalog.getMonitoredDirectories().size(), "One directory remains in memory");
            assertTrue(Files.exists(mediaDir), "Physical directory on disk is untouched (S3)");
            assertTrue(Files.exists(sampleMp3), "Multimedia file inside removed directory is untouched (S3)");
            assertEquals("mp3-bytes", Files.readString(sampleMp3, StandardCharsets.UTF_8), "File content intact");

            Files.delete(ephemeralDir);
            catalog.refreshDirectoryStatuses();
            assertEquals(DirectoryStatus.MISSING, catalog.getMonitoredDirectories().get(0).getStatus(), "Deleted dir marked MISSING");

            catalog.removeDirectory(ephemeralDir.toString());
            assertEquals(0, catalog.getMonitoredDirectories().size(), "MISSING directory can also be removed cleanly");
            assertEquals(0, Files.readAllLines(locationsFile, StandardCharsets.UTF_8).size(), "locations.txt is now empty");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_removeDirectoryRejectsUnmonitoredOrBlankPath() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t15-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path unmonitoredDir = Files.createDirectory(tempRoot.resolve("unmonitored"));

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();

            assertThrows(InvalidDirectoryException.class,
                    () -> catalog.removeDirectory("  "),
                    "Blank path on remove throws InvalidDirectoryException");
            assertThrows(LocationNotFoundException.class,
                    () -> catalog.removeDirectory(unmonitoredDir.toString()),
                    "Unmonitored directory throws LocationNotFoundException");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_saveAllOnExitRefreshesStatusesAndFlushesCurrentListToDisk() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t16-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path dirOne = Files.createDirectory(tempRoot.resolve("one"));
            Path dirTwo = Files.createDirectory(tempRoot.resolve("two"));

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();
            catalog.addDirectory(dirOne.toString());
            catalog.addDirectory(dirTwo.toString());

            Files.delete(dirTwo);
            Files.writeString(locationsFile, "corrupted-external-edit\n", StandardCharsets.UTF_8);

            catalog.saveAllOnExit();

            List<MonitoredDirectory> current = catalog.getMonitoredDirectories();
            assertEquals(DirectoryStatus.ACCESSIBLE, current.get(0).getStatus(), "dirOne remains ACCESSIBLE");
            assertEquals(DirectoryStatus.MISSING, current.get(1).getStatus(), "dirTwo refreshed to MISSING on exit");

            List<String> savedLines = Files.readAllLines(locationsFile, StandardCharsets.UTF_8);
            assertEquals(2, savedLines.size(), "saveAllOnExit flushes authoritative in-memory list to locations.txt");
            assertEquals(dirOne.toAbsolutePath().normalize().toString(), savedLines.get(0), "First saved path");
            assertEquals(dirTwo.toAbsolutePath().normalize().toString(), savedLines.get(1), "Second saved path");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void scanner_recursivelyFindsSupportedFilesSortedAndIgnoresUnsupportedOrMissing() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t17-");
            Path rootDir = Files.createDirectory(tempRoot.resolve("media"));
            Path subDir = Files.createDirectories(rootDir.resolve("albums").resolve("2026"));

            Path fileMp3 = Files.writeString(rootDir.resolve("z-song.MP3"), "12345", StandardCharsets.UTF_8);
            Path fileWav = Files.writeString(subDir.resolve("a-sound.wav"), "123", StandardCharsets.UTF_8);
            Path fileJpg = Files.writeString(subDir.resolve("b.archive.JpG"), "1234567", StandardCharsets.UTF_8);
            Path filePng = Files.writeString(rootDir.resolve("c-cover.png"), "", StandardCharsets.UTF_8);

            Files.writeString(rootDir.resolve("notes.txt"), "ignored", StandardCharsets.UTF_8);
            Files.writeString(subDir.resolve(".hidden"), "ignored", StandardCharsets.UTF_8);
            Files.createDirectory(rootDir.resolve("folder.mp3"));

            MultimediaScanner scanner = new MultimediaScanner();
            MultimediaScanner.ScanResult result = scanner.scanDirectory(rootDir);

            assertEquals(0, result.warnings().size(), "No warnings on accessible tree");
            assertEquals(4, result.files().size(), "Four supported regular files discovered");

            List<Path> expectedSortedPaths = Stream.of(fileMp3, fileWav, fileJpg, filePng)
                    .map(p -> p.toAbsolutePath().normalize())
                    .sorted()
                    .toList();
            List<Path> actualPaths = result.files().stream()
                    .map(MultimediaFile::getFullPath)
                    .toList();
            assertEquals(expectedSortedPaths, actualPaths, "Discovered files sorted deterministically by fullPath");

            MultimediaFile wavEntry = result.files().stream()
                    .filter(f -> f.getName().equals("a-sound.wav"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(".wav", wavEntry.getExtension(), "Normalized .wav extension");
            assertEquals(MediaType.AUDIO, wavEntry.getMediaType(), "Classified as AUDIO");
            assertEquals(3L, wavEntry.getSizeBytes(), "Recorded byte size");
            assertEquals(rootDir.toAbsolutePath().normalize(), wavEntry.getDirectoryPath(), "Root directoryPath recorded");

            MultimediaScanner.ScanResult missingResult = scanner.scanDirectory(tempRoot.resolve("non-existent"));
            assertEquals(0, missingResult.files().size(), "Missing directory returns empty file list");
            assertEquals(1, missingResult.warnings().size(), "Missing directory records warning without throwing");

            assertThrows(IllegalArgumentException.class,
                    () -> scanner.scanDirectory(null),
                    "Null path rejected with IllegalArgumentException");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_scansAccessibleDirectoriesDeduplicatesOverlapsAndUpdatesOnAddRemoveRescan() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t18-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path parentDir = Files.createDirectory(tempRoot.resolve("parent"));
            Path childDir = Files.createDirectory(parentDir.resolve("child"));
            Path secondDir = Files.createDirectory(tempRoot.resolve("second"));

            Files.writeString(parentDir.resolve("root-track.mp3"), "mp3-data", StandardCharsets.UTF_8);
            Files.writeString(childDir.resolve("nested-photo.jpg"), "jpg-data", StandardCharsets.UTF_8);
            Files.writeString(secondDir.resolve("alert.wav"), "wav-data", StandardCharsets.UTF_8);

            Files.write(locationsFile, List.of(parentDir.toString(), childDir.toString()), StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();

            assertEquals(2, catalog.getMultimediaFiles().size(),
                    "Overlapping parent/child directories deduplicate shared files by fullPath");
            Map<MediaType, List<MultimediaFile>> byType = catalog.getFilesByMediaType();
            assertEquals(1, byType.get(MediaType.AUDIO).size(), "1 AUDIO file after initial scan");
            assertEquals(1, byType.get(MediaType.IMAGE).size(), "1 IMAGE file after initial scan");

            catalog.addDirectory(secondDir.toString());
            assertEquals(3, catalog.getMultimediaFiles().size(), "addDirectory triggers scan and adds new files");
            assertEquals(2, catalog.getFilesByMediaType().get(MediaType.AUDIO).size(), "2 AUDIO files after adding secondDir");

            Files.writeString(secondDir.resolve("banner.png"), "png-data", StandardCharsets.UTF_8);
            catalog.rescan();
            assertEquals(4, catalog.getMultimediaFiles().size(), "rescan discovers newly created file on disk");
            assertEquals(2, catalog.getFilesByMediaType().get(MediaType.IMAGE).size(), "2 IMAGE files after rescan");

            catalog.removeDirectory(secondDir.toString());
            assertEquals(2, catalog.getMultimediaFiles().size(), "removeDirectory refreshes cataloged files");
            assertTrue(Files.exists(secondDir.resolve("banner.png")), "Removed directory files remain intact on disk");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_filterByMediaTypeReturnsMatchingAudioAndImageFilesAndRejectsNull() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t19-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path mediaDir = Files.createDirectory(tempRoot.resolve("library"));

            Files.writeString(mediaDir.resolve("one.mp3"), "a", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("two.wav"), "b", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("three.jpg"), "c", StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();
            catalog.addDirectory(mediaDir.toString());

            List<MultimediaFile> audioFiles = catalog.filterByMediaType(MediaType.AUDIO);
            List<MultimediaFile> imageFiles = catalog.filterByMediaType(MediaType.IMAGE);

            assertEquals(2, audioFiles.size(), "Two AUDIO files returned");
            assertEquals(1, imageFiles.size(), "One IMAGE file returned");
            assertEquals("three.jpg", imageFiles.get(0).getName(), "IMAGE file is three.jpg");

            assertThrows(IllegalArgumentException.class,
                    () -> catalog.filterByMediaType(null),
                    "Null MediaType rejected with IllegalArgumentException");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_filterByExtensionMatchesCaseInsensitivelyWithOrWithoutDotAndRejectsInvalid() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t20-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path mediaDir = Files.createDirectory(tempRoot.resolve("library"));

            Files.writeString(mediaDir.resolve("song.MP3"), "a", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("voice.wav"), "b", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("photo.JPG"), "c", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("icon.png"), "d", StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();
            catalog.addDirectory(mediaDir.toString());

            assertEquals(1, catalog.filterByExtension("MP3").size(), "Filter by 'MP3' without dot");
            assertEquals("song.MP3", catalog.filterByExtension("MP3").get(0).getName(), "Matched song.MP3");
            assertEquals(1, catalog.filterByExtension(" .wav ").size(), "Filter by ' .wav ' with dot and spaces");
            assertEquals(1, catalog.filterByExtension(".JPG").size(), "Filter by '.JPG'");
            assertEquals(1, catalog.filterByExtension("png").size(), "Filter by 'png'");

            assertThrows(IllegalArgumentException.class,
                    () -> catalog.filterByExtension(null),
                    "Null extension rejected");
            assertThrows(IllegalArgumentException.class,
                    () -> catalog.filterByExtension("   "),
                    "Blank extension rejected");
            assertThrows(IllegalArgumentException.class,
                    () -> catalog.filterByExtension(".mp4"),
                    "Unsupported extension rejected");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void catalog_searchByNameFindsCaseInsensitiveSubstringMatchesAndRejectsBlankQuery() {
        Path tempRoot = null;
        try {
            tempRoot = Files.createTempDirectory("multimedia-test-t21-");
            Path locationsFile = tempRoot.resolve("locations.txt");
            Path mediaDir = Files.createDirectory(tempRoot.resolve("library"));

            Files.writeString(mediaDir.resolve("Summer_Vacation_01.jpg"), "a", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("summer_hits.mp3"), "b", StandardCharsets.UTF_8);
            Files.writeString(mediaDir.resolve("winter_notes.wav"), "c", StandardCharsets.UTF_8);

            MultimediaCatalog catalog = new MultimediaCatalog(new LocationRepository(locationsFile));
            catalog.initialize();
            catalog.addDirectory(mediaDir.toString());

            List<MultimediaFile> summerMatches = catalog.searchByName("  SuMmEr ");
            assertEquals(2, summerMatches.size(), "Case-insensitive substring match finds 2 files");
            assertEquals(0, catalog.searchByName("autumn").size(), "Non-matching query returns empty list");

            assertThrows(IllegalArgumentException.class,
                    () -> catalog.searchByName(null),
                    "Null search query rejected");
            assertThrows(IllegalArgumentException.class,
                    () -> catalog.searchByName("   "),
                    "Blank search query rejected");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    private static void statisticsCalculator_computes1DAnd2DPrimitiveArraysAndEnforcesDefensiveCopies() {
        Path dirA = Path.of("media", "dirA").toAbsolutePath().normalize();
        Path dirB = Path.of("media", "dirB").toAbsolutePath().normalize();
        List<MonitoredDirectory> dirs = List.of(
                new MonitoredDirectory(dirA, DirectoryStatus.ACCESSIBLE, "2026-11-06T10:00:00Z"),
                new MonitoredDirectory(dirB, DirectoryStatus.ACCESSIBLE, "2026-11-06T10:01:00Z")
        );

        List<MultimediaFile> files = List.of(
                new MultimediaFile("a.mp3", ".mp3", MediaType.AUDIO, 100L, dirA, dirA.resolve("a.mp3")),
                new MultimediaFile("b.mp3", ".mp3", MediaType.AUDIO, 250L, dirA, dirA.resolve("b.mp3")),
                new MultimediaFile("c.wav", ".wav", MediaType.AUDIO, 500L, dirB, dirB.resolve("c.wav")),
                new MultimediaFile("d.jpg", ".jpg", MediaType.IMAGE, 1200L, dirA, dirA.resolve("d.jpg")),
                new MultimediaFile("e.png", ".png", MediaType.IMAGE, 300L, dirB, dirB.resolve("e.png"))
        );

        StatisticsCalculator calculator = new StatisticsCalculator();
        StatisticsCalculator.StatisticsSnapshot snapshot = calculator.calculate(dirs, files);

        assertEquals(5, snapshot.getTotalFiles(), "Total files is 5");
        assertEquals(2350L, snapshot.getTotalBytes(), "Total bytes is 2350");

        int[] counts = snapshot.getCountsByExtension();
        assertEquals(2, counts[0], "2 .mp3 files");
        assertEquals(1, counts[1], "1 .wav file");
        assertEquals(1, counts[2], "1 .jpg file");
        assertEquals(1, counts[3], "1 .png file");

        long[] bytes = snapshot.getBytesByExtension();
        assertEquals(350L, bytes[0], "350 bytes for .mp3");
        assertEquals(500L, bytes[1], "500 bytes for .wav");
        assertEquals(1200L, bytes[2], "1200 bytes for .jpg");
        assertEquals(300L, bytes[3], "300 bytes for .png");

        long[][] matrix = snapshot.getDirectoryExtensionMatrix();
        assertEquals(2, matrix.length, "2 directory rows in 2D matrix");
        assertEquals(2L, matrix[0][0], "dirA has 2 .mp3");
        assertEquals(0L, matrix[0][1], "dirA has 0 .wav");
        assertEquals(1L, matrix[0][2], "dirA has 1 .jpg");
        assertEquals(0L, matrix[0][3], "dirA has 0 .png");
        assertEquals(0L, matrix[1][0], "dirB has 0 .mp3");
        assertEquals(1L, matrix[1][1], "dirB has 1 .wav");
        assertEquals(0L, matrix[1][2], "dirB has 0 .jpg");
        assertEquals(1L, matrix[1][3], "dirB has 1 .png");

        counts[0] = 999;
        bytes[0] = 999999L;
        matrix[0][0] = 999L;
        assertEquals(2, snapshot.getCountsByExtension()[0], "Defensive copy protects countsByExtension");
        assertEquals(350L, snapshot.getBytesByExtension()[0], "Defensive copy protects bytesByExtension");
        assertEquals(2L, snapshot.getDirectoryExtensionMatrix()[0][0], "Defensive copy protects 2D matrix");
    }

    private static void statisticsCalculator_handlesEmptyInputsAndRejectsNullArguments() {
        StatisticsCalculator calculator = new StatisticsCalculator();
        StatisticsCalculator.StatisticsSnapshot empty = calculator.calculate(List.of(), List.of());

        assertEquals(0, empty.getTotalFiles(), "0 total files when empty");
        assertEquals(0L, empty.getTotalBytes(), "0 total bytes when empty");
        assertEquals(4, empty.getCountsByExtension().length, "4 extension slots even when empty");
        assertEquals(0, empty.getDirectoryExtensionMatrix().length, "0 rows when directories list is empty");

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null, List.of()),
                "Null directories list rejected");
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(), null),
                "Null files list rejected");
        assertThrows(IllegalArgumentException.class,
                () -> new StatisticsCalculator.StatisticsSnapshot(new int[2], new long[4], new long[0][4]),
                "Invalid countsByExtension length rejected");
    }
}
