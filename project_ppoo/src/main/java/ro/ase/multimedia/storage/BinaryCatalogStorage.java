package ro.ase.multimedia.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.DirectoryStatus;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;
import ro.ase.multimedia.service.StatisticsCalculator;

/**
 * Persists and restores binary snapshots of monitored directories, discovered multimedia files,
 * and 1D/2D primitive array statistics ({@code data/catalog.bin}) using {@link DataOutputStream}
 * and {@link DataInputStream}.
 */
public final class BinaryCatalogStorage {

    /** Magic header identifying a Multimedia File Manager binary catalog snapshot ({@code "MFMC"}). */
    public static final int MAGIC_HEADER = 0x4D464D43;

    /** Current binary catalog format version. */
    public static final int FORMAT_VERSION = 1;

    private final Path binaryFile;

    /**
     * Constructs a binary catalog storage instance targeting the specified binary file path.
     *
     * @param binaryFile the target binary file path (must not be {@code null})
     * @throws IllegalArgumentException if {@code binaryFile} is {@code null}
     */
    public BinaryCatalogStorage(Path binaryFile) {
        if (binaryFile == null) {
            throw new IllegalArgumentException("Binary catalog file path must not be null.");
        }
        this.binaryFile = binaryFile.toAbsolutePath().normalize();
    }

    /**
     * Returns the normalized absolute path of the binary catalog file managed by this storage.
     *
     * @return the normalized {@link Path} to {@code catalog.bin}
     */
    public Path getBinaryFile() {
        return binaryFile;
    }

    /**
     * Saves the monitored directories, multimedia files, and 1D/2D primitive array statistics
     * to the binary file via a sibling temporary file and atomic replacement.
     *
     * @param directories the monitored directories to persist (must not be {@code null} or contain {@code null})
     * @param files       the multimedia files to persist (must not be {@code null} or contain {@code null})
     * @param statistics  the 1D/2D primitive array statistics snapshot (must not be {@code null})
     * @throws IllegalArgumentException  if any argument or list element is {@code null}
     * @throws StorageOperationException if writing or moving the binary file fails
     */
    public void save(
            List<MonitoredDirectory> directories,
            List<MultimediaFile> files,
            StatisticsCalculator.StatisticsSnapshot statistics) {
        if (directories == null) {
            throw new IllegalArgumentException("Directories list must not be null.");
        }
        if (files == null) {
            throw new IllegalArgumentException("Files list must not be null.");
        }
        if (statistics == null) {
            throw new IllegalArgumentException("StatisticsSnapshot must not be null.");
        }
        for (MonitoredDirectory dir : directories) {
            if (dir == null) {
                throw new IllegalArgumentException("MonitoredDirectory entry must not be null.");
            }
        }
        for (MultimediaFile file : files) {
            if (file == null) {
                throw new IllegalArgumentException("MultimediaFile entry must not be null.");
            }
        }

        Path tempFile = binaryFile.resolveSibling(binaryFile.getFileName() + ".tmp");
        try {
            if (binaryFile.getParent() != null) {
                Files.createDirectories(binaryFile.getParent());
            }
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                out.writeInt(MAGIC_HEADER);
                out.writeInt(FORMAT_VERSION);

                out.writeInt(directories.size());
                for (MonitoredDirectory dir : directories) {
                    out.writeUTF(dir.getPath().toString());
                    out.writeUTF(dir.getStatus().name());
                    out.writeUTF(dir.getAddedAt());
                }

                out.writeInt(files.size());
                for (MultimediaFile file : files) {
                    out.writeUTF(file.getName());
                    out.writeUTF(file.getExtension());
                    out.writeUTF(file.getMediaType().name());
                    out.writeLong(file.getSizeBytes());
                    out.writeUTF(file.getDirectoryPath().toString());
                    out.writeUTF(file.getFullPath().toString());
                }

                int[] counts = statistics.getCountsByExtension();
                out.writeInt(counts.length);
                for (int count : counts) {
                    out.writeInt(count);
                }

                long[] bytes = statistics.getBytesByExtension();
                out.writeInt(bytes.length);
                for (long b : bytes) {
                    out.writeLong(b);
                }

                long[][] matrix = statistics.getDirectoryExtensionMatrix();
                int cols = matrix.length > 0 ? matrix[0].length : MediaType.SUPPORTED_EXTENSIONS.size();
                out.writeInt(matrix.length);
                out.writeInt(cols);
                for (long[] row : matrix) {
                    for (long val : row) {
                        out.writeLong(val);
                    }
                }
            }

            try {
                Files.move(tempFile, binaryFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, binaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw new StorageOperationException(
                    "Failed to save binary catalog to: " + binaryFile, ex);
        }
    }

    /**
     * Loads the binary catalog snapshot from disk if the file exists.
     *
     * @return an {@link Optional} containing the restored {@link CatalogSnapshot},
     *         or {@link Optional#empty()} if the binary file does not exist yet
     * @throws StorageOperationException if the binary file cannot be read or contains corrupted/invalid data
     */
    public Optional<CatalogSnapshot> load() {
        if (!Files.exists(binaryFile)) {
            return Optional.empty();
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(binaryFile)))) {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC_HEADER || version != FORMAT_VERSION) {
                throw new StorageOperationException("Unsupported or corrupted binary catalog header in: " + binaryFile);
            }

            int dirCount = in.readInt();
            if (dirCount < 0) {
                throw new StorageOperationException("Corrupted directory count in binary catalog: " + dirCount);
            }
            List<MonitoredDirectory> directories = new ArrayList<>(dirCount);
            for (int i = 0; i < dirCount; i++) {
                Path path = Path.of(in.readUTF());
                DirectoryStatus status = DirectoryStatus.valueOf(in.readUTF());
                String addedAt = in.readUTF();
                directories.add(new MonitoredDirectory(path, status, addedAt));
            }

            int fileCount = in.readInt();
            if (fileCount < 0) {
                throw new StorageOperationException("Corrupted file count in binary catalog: " + fileCount);
            }
            List<MultimediaFile> files = new ArrayList<>(fileCount);
            for (int i = 0; i < fileCount; i++) {
                String name = in.readUTF();
                String extension = in.readUTF();
                MediaType mediaType = MediaType.valueOf(in.readUTF());
                long sizeBytes = in.readLong();
                Path directoryPath = Path.of(in.readUTF());
                Path fullPath = Path.of(in.readUTF());
                files.add(new MultimediaFile(name, extension, mediaType, sizeBytes, directoryPath, fullPath));
            }

            int extCount = MediaType.SUPPORTED_EXTENSIONS.size();
            int countsLen = in.readInt();
            if (countsLen != extCount) {
                throw new StorageOperationException("Corrupted extension counts length: " + countsLen);
            }
            int[] countsByExtension = new int[countsLen];
            for (int i = 0; i < countsLen; i++) {
                countsByExtension[i] = in.readInt();
            }

            int bytesLen = in.readInt();
            if (bytesLen != extCount) {
                throw new StorageOperationException("Corrupted extension bytes length: " + bytesLen);
            }
            long[] bytesByExtension = new long[bytesLen];
            for (int i = 0; i < bytesLen; i++) {
                bytesByExtension[i] = in.readLong();
            }

            int rows = in.readInt();
            int cols = in.readInt();
            if (rows < 0 || cols != extCount) {
                throw new StorageOperationException("Corrupted 2D matrix dimensions: " + rows + "x" + cols);
            }
            long[][] matrix = new long[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    matrix[r][c] = in.readLong();
                }
            }

            StatisticsCalculator.StatisticsSnapshot statistics =
                    new StatisticsCalculator.StatisticsSnapshot(countsByExtension, bytesByExtension, matrix);
            return Optional.of(new CatalogSnapshot(directories, files, statistics));
        } catch (StorageOperationException ex) {
            throw ex;
        } catch (IOException | IllegalArgumentException ex) {
            throw new StorageOperationException("Failed to load binary catalog from: " + binaryFile, ex);
        }
    }

    /**
     * Immutable snapshot of monitored directories, discovered multimedia files, and primitive array statistics
     * restored from {@code catalog.bin}.
     *
     * @param directories the restored list of monitored directories
     * @param files       the restored list of multimedia files
     * @param statistics  the restored 1D/2D primitive array statistics snapshot
     */
    public record CatalogSnapshot(
            List<MonitoredDirectory> directories,
            List<MultimediaFile> files,
            StatisticsCalculator.StatisticsSnapshot statistics) {

        /**
         * Constructs an immutable catalog snapshot with defensive copies of lists.
         *
         * @param directories the monitored directories (must not be {@code null})
         * @param files       the multimedia files (must not be {@code null})
         * @param statistics  the statistics snapshot (must not be {@code null})
         * @throws IllegalArgumentException if any argument is {@code null}
         */
        public CatalogSnapshot {
            if (directories == null) {
                throw new IllegalArgumentException("Directories list must not be null.");
            }
            if (files == null) {
                throw new IllegalArgumentException("Files list must not be null.");
            }
            if (statistics == null) {
                throw new IllegalArgumentException("StatisticsSnapshot must not be null.");
            }
            directories = List.copyOf(directories);
            files = List.copyOf(files);
        }
    }
}
