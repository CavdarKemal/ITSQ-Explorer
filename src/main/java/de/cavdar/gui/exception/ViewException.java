package de.cavdar.gui.exception;

/**
 * Exception for view-related errors.
 * Thrown when view operations fail, such as layout errors or component initialization failures.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public class ViewException extends RuntimeException {

    /**
     * Constructs a new ViewException with the specified message.
     *
     * @param message the detail message
     */
    public ViewException(String message) {
        super(message);
    }

    /**
     * Constructs a new ViewException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public ViewException(String message, Throwable cause) {
        super(message, cause);
    }
}
