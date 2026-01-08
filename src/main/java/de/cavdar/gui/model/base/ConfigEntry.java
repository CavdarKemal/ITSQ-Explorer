package de.cavdar.gui.model.base;

import java.util.Objects;

/**
 * Immutable record representing a configuration entry with type-safe value access.
 * Provides convenient methods to parse the value as different types.
 *
 * @param key   the configuration key
 * @param value the configuration value (may contain comments after #)
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public record ConfigEntry(String key, String value) {

    /**
     * Compact constructor with validation.
     */
    public ConfigEntry {
        Objects.requireNonNull(key, "Key cannot be null");
        if (value == null) {
            value = "";
        }
    }

    /**
     * Returns the clean value without comments.
     * Comments start with # and are removed.
     *
     * @return the clean value
     */
    public String cleanValue() {
        String[] parts = value.split("#");
        return parts[0].trim();
    }

    /**
     * Returns the value as a semicolon-separated array.
     *
     * @return the value split by semicolons, or empty array if value is empty
     */
    public String[] asArray() {
        String clean = cleanValue();
        return clean.isEmpty() ? new String[0] : clean.split(";");
    }

    /**
     * Returns the value as a boolean.
     *
     * @return true if the value is "true" (case-insensitive), false otherwise
     */
    public boolean asBoolean() {
        return Boolean.parseBoolean(cleanValue());
    }

    /**
     * Returns the value as an integer.
     *
     * @return the integer value
     * @throws NumberFormatException if the value is not a valid integer
     */
    public int asInt() throws NumberFormatException {
        return Integer.parseInt(cleanValue());
    }

    /**
     * Returns the value as an integer with a default fallback.
     *
     * @param defaultValue the default value if parsing fails
     * @return the integer value or defaultValue if parsing fails
     */
    public int asInt(int defaultValue) {
        try {
            return asInt();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value as a double.
     *
     * @return the double value
     * @throws NumberFormatException if the value is not a valid double
     */
    public double asDouble() throws NumberFormatException {
        return Double.parseDouble(cleanValue());
    }

    /**
     * Returns the value as a double with a default fallback.
     *
     * @param defaultValue the default value if parsing fails
     * @return the double value or defaultValue if parsing fails
     */
    public double asDouble(double defaultValue) {
        try {
            return asDouble();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Checks if the value is empty or blank.
     *
     * @return true if the clean value is empty or blank
     */
    public boolean isEmpty() {
        return cleanValue().isEmpty();
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
