package ro.ase.multimedia;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import ro.ase.multimedia.service.MultimediaCatalog;
import ro.ase.multimedia.storage.LocationRepository;
import ro.ase.multimedia.ui.ConsoleMenu;

/**
 * Application entry point for the Multimedia File Manager console application.
 */
public final class Main {

    private Main() {
    }

    /**
     * Launches the Multimedia File Manager console application using {@code locations.txt}
     * in the working directory and standard console I/O streams.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        run(Path.of("locations.txt"), System.in, System.out);
    }

    /**
     * Wires the location repository, multimedia catalog, and interactive console menu
     * and runs the application lifecycle against the specified persistence file and I/O streams.
     *
     * @param locationsFile path to the {@code locations.txt} persistence file (must not be {@code null})
     * @param in            input stream to read user commands from (must not be {@code null})
     * @param out           print stream to write console output to (must not be {@code null})
     * @throws IllegalArgumentException if {@code locationsFile}, {@code in}, or {@code out} is {@code null}
     */
    public static void run(Path locationsFile, InputStream in, PrintStream out) {
        if (locationsFile == null) {
            throw new IllegalArgumentException("Locations file path must not be null.");
        }
        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("PrintStream must not be null.");
        }
        LocationRepository repository = new LocationRepository(locationsFile);
        MultimediaCatalog catalog = new MultimediaCatalog(repository);
        ConsoleMenu menu = new ConsoleMenu(catalog, in, out);
        menu.run();
    }
}
