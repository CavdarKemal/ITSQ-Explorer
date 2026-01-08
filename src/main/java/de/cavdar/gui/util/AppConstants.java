package de.cavdar.gui.util;

/**
 * Application-wide constants used across multiple View classes.
 *
 * @author TemplateGUI
 * @version 1.0
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - no instantiation
    }

    // ===== UI Labels =====
    public static final String NEW_CONNECTION = "<Neue Verbindung>";
    public static final String LOADING_NODE = "Laden...";
    public static final String FAVORITE_PREFIX = "★ ";

    // ===== Config Keys - SQL =====
    public static final String SQL_HISTORY_KEY = "SQL_HISTORY";
    public static final String SQL_FAVORITES_KEY = "SQL_FAVORITES";

    // ===== Config Keys - Customer/File Loading =====
    public static final String LAST_LOAD_DIRECTORY_KEY = "LAST_LOAD_DIRECTORY";
    public static final String LOAD_DIRECTORIES_KEY = "LOAD_DIRECTORIES";
    public static final String FILE_HISTORY_KEY = "CUSTOMER_FILE_HISTORY";

    // ===== Config Keys - ITSQ =====
    public static final String ITSQ_PATH_KEY = "ITSQ_PATH";
    public static final String DEFAULT_ITSQ_PATH = "ITSQ";

    // ===== Separators =====
    public static final String SQL_SEPARATOR = ";;";
    public static final String DIRECTORY_SEPARATOR = ";";
}
