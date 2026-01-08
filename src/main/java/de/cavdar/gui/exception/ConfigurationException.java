package de.cavdar.gui.exception;

/**
 * Exception for configuration-related errors.
 * Thrown when configuration files cannot be loaded, saved, or contain invalid data.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructs a new ConfigurationException with the specified message.
     *
     * @param message the detail message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigurationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
