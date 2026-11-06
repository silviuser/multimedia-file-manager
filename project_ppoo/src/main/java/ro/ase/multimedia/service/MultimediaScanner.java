package ro.ase.multimedia.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MultimediaFile;

/**
 * Recursively traverses monitored directories to discover regular multimedia files
 * ({@code .mp3}, {@code .wav}, {@code .jpg}, {@code .png}) without following symbolic links.
 */
public final class MultimediaScanner {

    /**
     * Constructs a new multimedia directory scanner.
     */
    public MultimediaScanner() {
    }

    /**
     * Result of scanning a single monitored directory, containing discovered multimedia files
     * and any non-fatal warnings encountered during traversal.
     *
     * @param files    unmodifiable list of discovered {@link MultimediaFile} entries sorted by normalized full path
     * @param warnings unmodifiable list of non-fatal warning messages
     */
    public record ScanResult(List<MultimediaFile> files, List<String> warnings) {

        /**
         * Validates and defensively copies the scan result lists.
         *
         * @throws IllegalArgumentException if {@code files} or {@code warnings} is {@code null}
         */
        public ScanResult {
            if (files == null) {
                throw new IllegalArgumentException("Discovered files list must not be null.");
            }
            if (warnings == null) {
                throw new IllegalArgumentException("Warnings list must not be null.");
            }
            files = List.copyOf(files);
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * Recursively scans a directory for regular multimedia files matching supported extensions.
     * Unreadable files or subdirectories are skipped and recorded as warnings rather than aborting the scan.
     *
     * @param directoryPath the root directory path to scan (must not be {@code null})
     * @return a {@link ScanResult} containing sorted {@link MultimediaFile} instances and any traversal warnings
     * @throws IllegalArgumentException if {@code directoryPath} is {@code null}
     */
    public ScanResult scanDirectory(Path directoryPath) {
        if (directoryPath == null) {
            throw new IllegalArgumentException("Directory path must not be null.");
        }

        Path root = directoryPath.toAbsolutePath().normalize();
        List<MultimediaFile> discovered = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            if (!Files.exists(root)) {
                warnings.add("Monitored directory does not exist on disk: " + root);
                return new ScanResult(discovered, warnings);
            }
            if (!Files.isDirectory(root)) {
                warnings.add("Monitored path is not a directory: " + root);
                return new ScanResult(discovered, warnings);
            }
            if (!Files.isReadable(root)) {
                warnings.add("Monitored directory is not readable: " + root);
                return new ScanResult(discovered, warnings);
            }

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && file.getFileName() != null) {
                        String fileName = file.getFileName().toString();
                        MediaType.extractExtension(fileName).ifPresent(ext -> {
                            MediaType type = MediaType.fromExtension(ext).orElseThrow();
                            discovered.add(new MultimediaFile(fileName, ext, type, attrs.size(), root, file));
                        });
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    warnings.add("Skipped unreadable path during scan (" + file + "): " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null) {
                        warnings.add("Incomplete directory traversal (" + dir + "): " + exc.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | SecurityException ex) {
            warnings.add("Failed to scan directory (" + root + "): " + ex.getMessage());
        }

        discovered.sort(Comparator.comparing(MultimediaFile::getFullPath));
        return new ScanResult(discovered, warnings);
    }
}
