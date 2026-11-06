package ro.ase.multimedia.exception;

/**
 * Thrown when attempting to add a directory that is already present in the monitored list.
 */
public class DuplicateLocationException extends MultimediaException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new duplicate location exception with the specified detail message.
     *
     * @param message the detail message identifying the duplicate directory
     */
    public DuplicateLocationException(String message) {
        super(message);
    }

    /**
     * Constructs a new duplicate location exception with the specified detail message and cause.
     *
     * @param message the detail message identifying the duplicate directory
     * @param cause   the underlying cause of this exception
     */
    public DuplicateLocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
