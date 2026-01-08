package de.cavdar.gui.util;

import de.cavdar.gui.model.base.AppConfig;
import de.cavdar.gui.model.base.ConnectionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages database connections - loading, saving, and providing access to stored connections.
 * Uses AppConfig for persistence.
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public final class ConnectionManager {
    private static final String CONNECTIONS_KEY = "DB_CONNECTIONS";
    private static final String LAST_CONNECTION_KEY = "LAST_DB_CONNECTION";
    private static final String CONNECTION_SEPARATOR = ";;";

    private static final List<ConnectionInfo> connections = new ArrayList<>();
    private static final List<ConnectionListener> listeners = new ArrayList<>();
    private static boolean loaded = false;

    private ConnectionManager() {
        // Utility class
    }

    /**
     * Listener interface for connection list changes.
     */
    public interface ConnectionListener {
        void onConnectionsChanged();
    }

    /**
     * Adds a listener to be notified when connections change.
     *
     * @param listener the listener to add
     */
    public static void addListener(ConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a connection listener.
     *
     * @param listener the listener to remove
     */
    public static void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (ConnectionListener listener : listeners) {
            listener.onConnectionsChanged();
        }
    }

    /**
     * Loads connections from configuration.
     */
    public static synchronized void loadConnections() {
        connections.clear();
        AppConfig cfg = AppConfig.getInstance();
        String connectionsData = cfg.getProperty(CONNECTIONS_KEY);

        if (!connectionsData.isEmpty()) {
            String[] connStrings = connectionsData.split(CONNECTION_SEPARATOR);
            for (String connData : connStrings) {
                if (!connData.trim().isEmpty()) {
                    ConnectionInfo conn = ConnectionInfo.deserialize(connData);
                    if (conn != null) {
                        connections.add(conn);
                    }
                }
            }
        }

        loaded = true;
        TimelineLogger.debug(ConnectionManager.class, "Loaded {} database connections", connections.size());
    }

    /**
     * Forces a reload of connections from the current configuration.
     * Use this after loading a different config file.
     */
    public static synchronized void reloadConnections() {
        loaded = false;
        loadConnections();
        notifyListeners();
        TimelineLogger.info(ConnectionManager.class, "Connections reloaded from configuration");
    }

    /**
     * Saves all connections to configuration.
     */
    public static synchronized void saveConnections() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < connections.size(); i++) {
            if (i > 0) sb.append(CONNECTION_SEPARATOR);
            sb.append(connections.get(i).serialize());
        }

        AppConfig cfg = AppConfig.getInstance();
        cfg.setProperty(CONNECTIONS_KEY, sb.toString());
        cfg.save();
        TimelineLogger.debug(ConnectionManager.class, "Saved {} database connections", connections.size());
        notifyListeners();
    }

    /**
     * Returns a copy of all stored connections.
     *
     * @return list of connections
     */
    public static synchronized List<ConnectionInfo> getConnections() {
        if (!loaded) {
            loadConnections();
        }
        return new ArrayList<>(connections);
    }

    /**
     * Returns the connection names for display in ComboBoxes.
     *
     * @return array of connection names
     */
    public static synchronized String[] getConnectionNames() {
        if (!loaded) {
            loadConnections();
        }
        return connections.stream()
                .map(ConnectionInfo::getName)
                .toArray(String[]::new);
    }

    /**
     * Finds a connection by name.
     *
     * @param name the connection name
     * @return the connection, or null if not found
     */
    public static synchronized ConnectionInfo getConnection(String name) {
        if (!loaded) {
            loadConnections();
        }
        return connections.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds or updates a connection.
     *
     * @param conn the connection to save
     */
    public static synchronized void saveConnection(ConnectionInfo conn) {
        if (!loaded) {
            loadConnections();
        }

        // Remove existing with same name
        connections.removeIf(c -> c.getName().equals(conn.getName()));
        connections.add(conn);
        saveConnections();
        TimelineLogger.info(ConnectionManager.class, "Saved connection: {}", conn.getName());
    }

    /**
     * Deletes a connection by name.
     *
     * @param name the connection name to delete
     * @return true if deleted, false if not found
     */
    public static synchronized boolean deleteConnection(String name) {
        if (!loaded) {
            loadConnections();
        }

        boolean removed = connections.removeIf(c -> c.getName().equals(name));
        if (removed) {
            saveConnections();
            TimelineLogger.info(ConnectionManager.class, "Deleted connection: {}", name);
        }
        return removed;
    }

    /**
     * Gets the last used connection name.
     *
     * @return last connection name, or empty string if none
     */
    public static String getLastConnectionName() {
        return AppConfig.getInstance().getProperty(LAST_CONNECTION_KEY);
    }

    /**
     * Sets the last used connection name.
     *
     * @param name the connection name
     */
    public static void setLastConnectionName(String name) {
        AppConfig cfg = AppConfig.getInstance();
        cfg.setProperty(LAST_CONNECTION_KEY, name);
        cfg.save();
    }

    /**
     * Returns the number of stored connections.
     *
     * @return connection count
     */
    public static synchronized int getConnectionCount() {
        if (!loaded) {
            loadConnections();
        }
        return connections.size();
    }
}
