package ro.ase.multimedia.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import ro.ase.multimedia.exception.LocationNotFoundException;
import ro.ase.multimedia.exception.MultimediaException;
import ro.ase.multimedia.model.MediaType;
import ro.ase.multimedia.model.MonitoredDirectory;
import ro.ase.multimedia.model.MultimediaFile;
import ro.ase.multimedia.service.MultimediaCatalog;
import ro.ase.multimedia.service.StatisticsCalculator;

/**
 * Interactive console user interface driving the Multimedia File Manager lifecycle,
 * menu navigation, tabular output formatting, and user-friendly error presentation.
 */
public final class ConsoleMenu {

    private final MultimediaCatalog catalog;
    private final Scanner scanner;
    private final PrintStream out;
    private boolean running;

    /**
     * Constructs a console menu bound to the given catalog and I/O streams.
     *
     * @param catalog the multimedia catalog service (must not be {@code null})
     * @param in      the input stream to read user commands from (must not be {@code null})
     * @param out     the print stream to write menu output and messages to (must not be {@code null})
     * @throws IllegalArgumentException if {@code catalog}, {@code in}, or {@code out} is {@code null}
     */
    public ConsoleMenu(MultimediaCatalog catalog, InputStream in, PrintStream out) {
        if (catalog == null) {
            throw new IllegalArgumentException("MultimediaCatalog must not be null.");
        }
        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("PrintStream must not be null.");
        }
        this.catalog = catalog;
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
        this.out = out;
    }

    /**
     * Runs the application lifecycle: initializes the catalog from persistent storage,
     * displays any startup warnings, processes menu commands until {@code 0} or EOF is reached,
     * and saves state on exit.
     */
    public void run() {
        out.println("=== Multimedia File Manager ===");
        try {
            List<String> warnings = catalog.initialize();
            for (String warning : warnings) {
                out.println("[WARN] " + warning);
            }
        } catch (MultimediaException ex) {
            out.println("[ERROR] " + ex.getMessage());
        }

        running = true;
        while (running) {
            printMenu();
            if (!scanner.hasNextLine()) {
                running = false;
                break;
            }
            String trimmed = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(trimmed);
                switch (choice) {
                    case 1 -> handleViewDirectories();
                    case 2 -> handleAddDirectory();
                    case 3 -> handleRemoveDirectory();
                    case 4 -> handleDisplayAllFiles();
                    case 5 -> handleFilterOrSearchFiles();
                    case 6 -> handleViewStatisticsAndExport();
                    case 0 -> running = false;
                    default -> out.println("[ERROR] Invalid menu option: " + choice + ". Choose 0-6.");
                }
            } catch (NumberFormatException ex) {
                out.println("[ERROR] Invalid menu input: '" + trimmed + "'. Please enter a number (0-6).");
            } catch (MultimediaException | IllegalArgumentException ex) {
                out.println("[ERROR] " + ex.getMessage());
            }
        }

        try {
            catalog.saveAllOnExit();
            out.println("State saved. Exiting Multimedia File Manager.");
        } catch (MultimediaException ex) {
            out.println("[ERROR] " + ex.getMessage());
        }
    }

    private void printMenu() {
        out.println();
        out.println("--- Multimedia File Manager Menu ---");
        out.println("1 - Display monitored directories (path, status, file count)");
        out.println("2 - Add a new monitored directory");
        out.println("3 - Remove a monitored directory (keeps disk files intact)");
        out.println("4 - Display all multimedia files");
        out.println("5 - Filter or search multimedia files");
        out.println("6 - View statistics (1D/2D arrays) and export report / binary snapshot");
        out.println("0 - Save state and exit");
        out.print("Select option (0-6): ");
    }

    private void handleViewDirectories() {
        List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
        if (dirs.isEmpty()) {
            out.println("No monitored directories configured.");
            return;
        }
        long[][] matrix = catalog.getStatistics().getDirectoryExtensionMatrix();
        out.println("Monitored Directories (" + dirs.size() + "):");
        for (int i = 0; i < dirs.size(); i++) {
            MonitoredDirectory dir = dirs.get(i);
            long fileCount = 0L;
            if (i < matrix.length) {
                for (long count : matrix[i]) {
                    fileCount += count;
                }
            }
            out.println("[" + (i + 1) + "] " + dir.getPath()
                    + " | Status: " + dir.getStatus()
                    + " | Files: " + fileCount
                    + " | Added: " + dir.getAddedAt());
        }
    }

    private void handleAddDirectory() {
        out.print("Enter directory path to add: ");
        if (!scanner.hasNextLine()) {
            running = false;
            return;
        }
        String rawPath = scanner.nextLine();
        MonitoredDirectory added = catalog.addDirectory(rawPath);
        out.println("Added monitored directory: " + added.getPath());
    }

    private void handleRemoveDirectory() {
        out.print("Enter 1-based directory number or path to remove: ");
        if (!scanner.hasNextLine()) {
            running = false;
            return;
        }
        String rawInput = scanner.nextLine().trim();
        String targetPath = rawInput;
        try {
            int index = Integer.parseInt(rawInput);
            List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
            if (index < 1 || index > dirs.size()) {
                throw new LocationNotFoundException("Directory index out of range: " + index);
            }
            targetPath = dirs.get(index - 1).getPath().toString();
        } catch (NumberFormatException ignored) {
            // Input is a path string; pass directly to catalog.removeDirectory
        }
        MonitoredDirectory removed = catalog.removeDirectory(targetPath);
        out.println("Removed monitored directory reference (disk files kept intact): " + removed.getPath());
    }

    private void handleDisplayAllFiles() {
        printFileList(catalog.getMultimediaFiles(), "No multimedia files found in monitored directories.");
    }

    private void handleFilterOrSearchFiles() {
        out.println("Filter / Search Options:");
        out.println("1 - Filter by MediaType (AUDIO / IMAGE)");
        out.println("2 - Filter by extension (.mp3 / .wav / .jpg / .png)");
        out.println("3 - Search by filename keyword");
        out.print("Select filter option (1-3): ");
        if (!scanner.hasNextLine()) {
            running = false;
            return;
        }
        String subOptionRaw = scanner.nextLine().trim();
        int subOption;
        try {
            subOption = Integer.parseInt(subOptionRaw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid filter option: '" + subOptionRaw + "'. Please enter 1, 2, or 3.", ex);
        }
        if (subOption < 1 || subOption > 3) {
            throw new IllegalArgumentException("Invalid filter option: " + subOption + ". Choose 1-3.");
        }

        List<MultimediaFile> matches = switch (subOption) {
            case 1 -> {
                out.print("Enter media type (AUDIO or IMAGE): ");
                if (!scanner.hasNextLine()) {
                    running = false;
                    yield List.of();
                }
                String rawType = scanner.nextLine().trim();
                MediaType mediaType = parseMediaType(rawType);
                yield catalog.filterByMediaType(mediaType);
            }
            case 2 -> {
                out.print("Enter file extension (.mp3, .wav, .jpg, .png): ");
                if (!scanner.hasNextLine()) {
                    running = false;
                    yield List.of();
                }
                String rawExt = scanner.nextLine();
                yield catalog.filterByExtension(rawExt);
            }
            default -> {
                out.print("Enter filename keyword: ");
                if (!scanner.hasNextLine()) {
                    running = false;
                    yield List.of();
                }
                String keyword = scanner.nextLine();
                yield catalog.searchByName(keyword);
            }
        };

        if (running) {
            printFileList(matches, "No multimedia files matched the filter criteria.");
        }
    }

    private void handleViewStatisticsAndExport() {
        List<MonitoredDirectory> dirs = catalog.getMonitoredDirectories();
        StatisticsCalculator.StatisticsSnapshot stats = catalog.getStatistics();

        out.println("[Summary Totals]");
        out.println("Monitored Directories: " + dirs.size());
        out.println("Total Multimedia Files: " + stats.getTotalFiles());
        out.println("Total Size: " + formatSize(stats.getTotalBytes()));
        out.println();

        out.println("[1D Extension Statistics]");
        int[] counts = stats.getCountsByExtension();
        long[] bytes = stats.getBytesByExtension();
        for (int i = 0; i < MediaType.SUPPORTED_EXTENSIONS.size(); i++) {
            String ext = MediaType.SUPPORTED_EXTENSIONS.get(i);
            out.println(ext + " -> Count: " + counts[i] + " | Bytes: " + bytes[i]
                    + " (" + formatSize(bytes[i]) + ")");
        }
        out.println();

        out.println("[2D Directory-by-Extension Matrix]");
        StringBuilder header = new StringBuilder("Directory");
        for (String ext : MediaType.SUPPORTED_EXTENSIONS) {
            header.append(" | ").append(ext);
        }
        out.println(header);

        long[][] matrix = stats.getDirectoryExtensionMatrix();
        if (dirs.isEmpty() || matrix.length == 0) {
            out.println("(none)");
        } else {
            int rows = Math.min(dirs.size(), matrix.length);
            for (int r = 0; r < rows; r++) {
                StringBuilder row = new StringBuilder(dirs.get(r).getPath().toString());
                for (int c = 0; c < MediaType.SUPPORTED_EXTENSIONS.size(); c++) {
                    row.append(" | ").append(matrix[r][c]);
                }
                out.println(row);
            }
        }

        catalog.exportReport();
        catalog.saveBinarySnapshot();
        out.println("Exported text report and saved binary catalog snapshot.");
    }

    private void printFileList(List<MultimediaFile> files, String emptyMessage) {
        if (files.isEmpty()) {
            out.println(emptyMessage);
            return;
        }
        out.println("Multimedia Files (" + files.size() + "):");
        for (int i = 0; i < files.size(); i++) {
            MultimediaFile file = files.get(i);
            out.println("[" + (i + 1) + "] " + file.getName()
                    + " | Ext: " + file.getExtension()
                    + " | Type: " + file.getMediaType()
                    + " | Size: " + formatSize(file.getSizeBytes())
                    + " | Location: " + file.getDirectoryPath());
        }
    }

    private static MediaType parseMediaType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("Media type must not be blank. Use AUDIO or IMAGE.");
        }
        return switch (rawType.trim().toUpperCase(Locale.ROOT)) {
            case "AUDIO" -> MediaType.AUDIO;
            case "IMAGE" -> MediaType.IMAGE;
            default -> throw new IllegalArgumentException(
                    "Unsupported media type: '" + rawType + "'. Use AUDIO or IMAGE.");
        };
    }

    private static String formatSize(long sizeBytes) {
        if (sizeBytes < 1024L) {
            return sizeBytes + " B (" + sizeBytes + " bytes)";
        }
        if (sizeBytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.2f KB (%d bytes)", sizeBytes / 1024.0, sizeBytes);
        }
        return String.format(Locale.ROOT, "%.2f MB (%d bytes)", sizeBytes / (1024.0 * 1024.0), sizeBytes);
    }
}
