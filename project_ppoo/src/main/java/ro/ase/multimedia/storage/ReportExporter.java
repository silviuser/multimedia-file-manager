package ro.ase.multimedia.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import ro.ase.multimedia.exception.StorageOperationException;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;
import ro.ase.multimedia.service.StatisticsCalculator;

/**
 * Exports formatted UTF-8 text reports ({@code multimedia-report.txt}) summarizing monitored directories,
 * 1D/2D primitive array statistics, and discovered multimedia files.
 */
public final class ReportExporter {

    private final Path reportFile;

    /**
     * Constructs a report exporter targeting the specified report file path.
     *
     * @param reportFile the target report file path (must not be {@code null})
     * @throws IllegalArgumentException if {@code reportFile} is {@code null}
     */
    public ReportExporter(Path reportFile) {
        if (reportFile == null) {
            throw new IllegalArgumentException("Report file path must not be null.");
        }
        this.reportFile = reportFile.toAbsolutePath().normalize();
    }

    /**
     * Returns the normalized absolute path of the report file managed by this exporter.
     *
     * @return the normalized {@link Path} to the text report file
     */
    public Path getReportFile() {
        return reportFile;
    }

    /**
     * Formats and writes a UTF-8 multimedia catalog report atomically via a sibling temporary file.
     *
     * @param directories the monitored directories (must not be {@code null} or contain {@code null})
     * @param files       the discovered multimedia files (must not be {@code null} or contain {@code null})
     * @param statistics  the 1D/2D primitive array statistics snapshot (must not be {@code null})
     * @throws IllegalArgumentException  if any argument or list element is {@code null}
     * @throws StorageOperationException if writing or moving the report file fails
     */
    public void export(
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

        List<String> lines = formatReportLines(directories, files, statistics);
        Path tempFile = reportFile.resolveSibling(reportFile.getFileName() + ".tmp");
        try {
            if (reportFile.getParent() != null) {
                Files.createDirectories(reportFile.getParent());
            }
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, reportFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, reportFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw new StorageOperationException(
                    "Failed to export multimedia report to: " + reportFile, ex);
        }
    }

    private static List<String> formatReportLines(
            List<MonitoredDirectory> directories,
            List<MultimediaFile> files,
            StatisticsCalculator.StatisticsSnapshot statistics) {
        List<String> lines = new ArrayList<>();
        lines.add("=== MULTIMEDIA CATALOG REPORT ===");
        lines.add("");
        lines.add("[Summary Totals]");
        lines.add("Monitored Directories: " + directories.size());
        lines.add("Total Multimedia Files: " + statistics.getTotalFiles());
        lines.add("Total Size (Bytes): " + statistics.getTotalBytes());
        lines.add("");

        lines.add("[Monitored Directories]");
        if (directories.isEmpty()) {
            lines.add("(none)");
        } else {
            for (int i = 0; i < directories.size(); i++) {
                MonitoredDirectory dir = directories.get(i);
                lines.add("[" + i + "] " + dir.getPath() + " | Status: " + dir.getStatus()
                        + " | Added: " + dir.getAddedAt());
            }
        }
        lines.add("");

        lines.add("[1D Extension Statistics]");
        int[] counts = statistics.getCountsByExtension();
        long[] bytes = statistics.getBytesByExtension();
        for (int i = 0; i < MediaType.SUPPORTED_EXTENSIONS.size(); i++) {
            String ext = MediaType.SUPPORTED_EXTENSIONS.get(i);
            lines.add(ext + " -> Count: " + counts[i] + " | Bytes: " + bytes[i]);
        }
        lines.add("");

        lines.add("[2D Directory-by-Extension Matrix]");
        StringBuilder header = new StringBuilder("Directory");
        for (String ext : MediaType.SUPPORTED_EXTENSIONS) {
            header.append(" | ").append(ext);
        }
        lines.add(header.toString());

        long[][] matrix = statistics.getDirectoryExtensionMatrix();
        if (directories.isEmpty() || matrix.length == 0) {
            lines.add("(none)");
        } else {
            int rows = Math.min(directories.size(), matrix.length);
            for (int r = 0; r < rows; r++) {
                StringBuilder rowBuilder = new StringBuilder(directories.get(r).getPath().toString());
                for (int c = 0; c < MediaType.SUPPORTED_EXTENSIONS.size(); c++) {
                    rowBuilder.append(" | ").append(matrix[r][c]);
                }
                lines.add(rowBuilder.toString());
            }
        }
        lines.add("");

        lines.add("[Multimedia File Inventory]");
        if (files.isEmpty()) {
            lines.add("(none)");
        } else {
            for (MultimediaFile file : files) {
                lines.add(file.getName() + " | Type: " + file.getMediaType()
                        + " | Ext: " + file.getExtension()
                        + " | Size: " + file.getSizeBytes() + " bytes"
                        + " | Path: " + file.getFullPath());
            }
        }
        return lines;
    }
}
