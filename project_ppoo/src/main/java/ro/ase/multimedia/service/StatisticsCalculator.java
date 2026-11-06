package ro.ase.multimedia.service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;

/**
 * Pure domain service that computes 1D ({@code int[]}, {@code long[]}) and 2D ({@code long[][]})
 * primitive array statistics over monitored directories and discovered multimedia files.
 */
public final class StatisticsCalculator {

    /**
     * Constructs a new {@code StatisticsCalculator}.
     */
    public StatisticsCalculator() {
    }

    /**
     * Computes 1D extension counts and byte totals as well as the 2D directory-by-extension file count matrix
     * for the given monitored directories and multimedia files.
     *
     * @param directories the ordered list of monitored directories (must not be {@code null} or contain {@code null})
     * @param files       the list of discovered multimedia files (must not be {@code null} or contain {@code null})
     * @return an immutable {@link StatisticsSnapshot} containing defensive copies of the 1D and 2D arrays
     * @throws IllegalArgumentException if {@code directories} or {@code files} is {@code null} or contains {@code null}
     */
    public StatisticsSnapshot calculate(List<MonitoredDirectory> directories, List<MultimediaFile> files) {
        if (directories == null) {
            throw new IllegalArgumentException("Directories list must not be null.");
        }
        if (files == null) {
            throw new IllegalArgumentException("Files list must not be null.");
        }

        int extCount = MediaType.SUPPORTED_EXTENSIONS.size();
        int dirCount = directories.size();

        int[] countsByExtension = new int[extCount];
        long[] bytesByExtension = new long[extCount];
        long[][] directoryExtensionMatrix = new long[dirCount][extCount];

        Map<Path, Integer> directoryIndices = new HashMap<>();
        for (int i = 0; i < dirCount; i++) {
            MonitoredDirectory dir = directories.get(i);
            if (dir == null) {
                throw new IllegalArgumentException("MonitoredDirectory entry must not be null.");
            }
            directoryIndices.putIfAbsent(dir.getPath(), i);
        }

        for (MultimediaFile file : files) {
            if (file == null) {
                throw new IllegalArgumentException("MultimediaFile entry must not be null.");
            }
            int extIndex = MediaType.SUPPORTED_EXTENSIONS.indexOf(file.getExtension());
            if (extIndex >= 0) {
                countsByExtension[extIndex]++;
                bytesByExtension[extIndex] += file.getSizeBytes();
                Integer dirIndex = directoryIndices.get(file.getDirectoryPath());
                if (dirIndex != null) {
                    directoryExtensionMatrix[dirIndex][extIndex]++;
                }
            }
        }

        return new StatisticsSnapshot(countsByExtension, bytesByExtension, directoryExtensionMatrix);
    }

    /**
     * Immutable snapshot of 1D and 2D primitive array statistics over monitored directories and multimedia files.
     */
    public static final class StatisticsSnapshot {

        private final int[] countsByExtension;
        private final long[] bytesByExtension;
        private final long[][] directoryExtensionMatrix;
        private final int totalFiles;
        private final long totalBytes;

        /**
         * Constructs an immutable statistics snapshot by deep-cloning the provided 1D and 2D arrays.
         *
         * @param countsByExtension        1D file counts indexed by {@link MediaType#SUPPORTED_EXTENSIONS}
         * @param bytesByExtension         1D byte totals indexed by {@link MediaType#SUPPORTED_EXTENSIONS}
         * @param directoryExtensionMatrix 2D file counts indexed by {@code [dirIndex][extIndex]}
         * @throws IllegalArgumentException if any array is {@code null}, has invalid dimensions, or negative values
         */
        public StatisticsSnapshot(
                int[] countsByExtension,
                long[] bytesByExtension,
                long[][] directoryExtensionMatrix) {
            int extCount = MediaType.SUPPORTED_EXTENSIONS.size();
            if (countsByExtension == null || countsByExtension.length != extCount) {
                throw new IllegalArgumentException("countsByExtension must be non-null of length " + extCount);
            }
            if (bytesByExtension == null || bytesByExtension.length != extCount) {
                throw new IllegalArgumentException("bytesByExtension must be non-null of length " + extCount);
            }
            if (directoryExtensionMatrix == null) {
                throw new IllegalArgumentException("directoryExtensionMatrix must not be null.");
            }

            this.countsByExtension = countsByExtension.clone();
            this.bytesByExtension = bytesByExtension.clone();

            int sumFiles = 0;
            long sumBytes = 0L;
            for (int i = 0; i < extCount; i++) {
                if (this.countsByExtension[i] < 0 || this.bytesByExtension[i] < 0) {
                    throw new IllegalArgumentException("Extension counts and byte totals must not be negative.");
                }
                sumFiles += this.countsByExtension[i];
                sumBytes += this.bytesByExtension[i];
            }
            this.totalFiles = sumFiles;
            this.totalBytes = sumBytes;

            this.directoryExtensionMatrix = new long[directoryExtensionMatrix.length][];
            for (int r = 0; r < directoryExtensionMatrix.length; r++) {
                long[] row = directoryExtensionMatrix[r];
                if (row == null || row.length != extCount) {
                    throw new IllegalArgumentException("Matrix row " + r + " must be non-null of length " + extCount);
                }
                long[] clonedRow = row.clone();
                for (long val : clonedRow) {
                    if (val < 0) {
                        throw new IllegalArgumentException("Matrix values must not be negative.");
                    }
                }
                this.directoryExtensionMatrix[r] = clonedRow;
            }
        }

        /**
         * Returns a defensive copy of the 1D file counts array indexed by {@link MediaType#SUPPORTED_EXTENSIONS}.
         *
         * @return a cloned {@code int[]} of file counts per extension
         */
        public int[] getCountsByExtension() {
            return countsByExtension.clone();
        }

        /**
         * Returns a defensive copy of the 1D byte totals array indexed by {@link MediaType#SUPPORTED_EXTENSIONS}.
         *
         * @return a cloned {@code long[]} of byte sizes per extension
         */
        public long[] getBytesByExtension() {
            return bytesByExtension.clone();
        }

        /**
         * Returns a deep defensive copy of the 2D directory-by-extension file count matrix.
         *
         * @return a deep-cloned {@code long[][]} matrix indexed by {@code [dirIndex][extIndex]}
         */
        public long[][] getDirectoryExtensionMatrix() {
            long[][] copy = new long[directoryExtensionMatrix.length][];
            for (int i = 0; i < directoryExtensionMatrix.length; i++) {
                copy[i] = directoryExtensionMatrix[i].clone();
            }
            return copy;
        }

        /**
         * Returns the total number of cataloged multimedia files across all extensions.
         *
         * @return total file count
         */
        public int getTotalFiles() {
            return totalFiles;
        }

        /**
         * Returns the total size in bytes of all cataloged multimedia files across all extensions.
         *
         * @return total byte size
         */
        public long getTotalBytes() {
            return totalBytes;
        }
    }
}
