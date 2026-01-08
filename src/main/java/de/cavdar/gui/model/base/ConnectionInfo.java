package de.cavdar.gui.model.base;

import java.util.Base64;
import java.util.Objects;

/**
 * Represents a database connection configuration.
 * Supports serialization for persistence in configuration files.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public class ConnectionInfo {
    private String name;
    private String driver;
    private String url;
    private String username;
    private String password;

    /**
     * Creates a new ConnectionInfo instance.
     *
     * @param name     display name for the connection
     * @param driver   JDBC driver class name
     * @param url      JDBC connection URL
     * @param username database username
     * @param password database password
     */
    public ConnectionInfo(String name, String driver, String url, String username, String password) {
        this.name = name;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Serializes this connection info to a string for storage.
     * Password is Base64 encoded.
     *
     * @return serialized connection string
     */
    public String serialize() {
        String encodedPwd = Base64.getEncoder().encodeToString(password.getBytes());
        return name + "|" + driver + "|" + url + "|" + username + "|" + encodedPwd;
    }

    /**
     * Deserializes a connection info from a stored string.
     *
     * @param data the serialized connection string
     * @return ConnectionInfo instance, or null if parsing fails
     */
    public static ConnectionInfo deserialize(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        String[] parts = data.split("\\|", 5);
        if (parts.length >= 5) {
            try {
                String pwd = new String(Base64.getDecoder().decode(parts[4]));
                return new ConnectionInfo(parts[0], parts[1], parts[2], parts[3], pwd);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionInfo that = (ConnectionInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
