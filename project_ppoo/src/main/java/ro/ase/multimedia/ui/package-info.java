/**
 * Interactive command-line user interface for the Multimedia File Manager.
 *
 * <p>Provides {@link ro.ase.multimedia.ui.ConsoleMenu}, which reads user
 * commands from an injectable {@link java.io.InputStream} and formats output
 * and validation messages to a {@link java.io.PrintStream} while delegating all
 * domain and persistence operations to
 * {@link ro.ase.multimedia.service.MultimediaCatalog}.
 */
package ro.ase.multimedia.ui;
