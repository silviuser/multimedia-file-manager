package ro.ase.multimedia.exception;

/**
 * Thrown when attempting to remove or look up a directory that is not in the monitored list.
 */
public class LocationNotFoundException extends MultimediaException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new location not found exception with the specified detail message.
     *
     * @param message the detail message identifying the missing monitored directory
     */
    public LocationNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new location not found exception with the specified detail message and cause.
     *
     * @param message the detail message identifying the missing monitored directory
     * @param cause   the underlying cause of this exception
     */
    public LocationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
