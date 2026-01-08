package de.cavdar.gui.model.base;

import de.cavdar.gui.exception.ConfigurationException;
import de.cavdar.gui.util.TimelineLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Singleton configuration manager for the MDI application.
 * Manages properties from the ene-config.properties file with support for
 * various data types (String, Array, Boolean, int).
 *
 * <p>This implementation uses eager initialization for thread-safety.</p>
 *
 * @author StandardMDIGUI
 * @version 1.0
 * @since 2024-12-24
 */
public class AppConfig {
    private static final String DEFAULT_FILE_PATH = "ene-config.properties";
    private static final String INITIAL_FILE_PATH;
    private static final AppConfig INSTANCE;

    static {
        // Priority: 1. System property, 2. Environment variable, 3. Default
        String sysProp = System.getProperty("config.file");
        String envPath = System.getenv("CONFIG_FILE_PATH");

        if (sysProp != null && !sysProp.isEmpty()) {
            INITIAL_FILE_PATH = sysProp;
            TimelineLogger.info(AppConfig.class, "Using config path from system property: {}", INITIAL_FILE_PATH);
        } else if (envPath != null && !envPath.isEmpty()) {
            INITIAL_FILE_PATH = envPath;
            TimelineLogger.info(AppConfig.class, "Using config path from environment: {}", INITIAL_FILE_PATH);
        } else {
            INITIAL_FILE_PATH = DEFAULT_FILE_PATH;
        }
        INSTANCE = new AppConfig();
    }

    /**
     * The currently active config file path (changes when loadFrom is called)
     */
    private String currentFilePath = INITIAL_FILE_PATH;

    /**
     * Property groups for organized config file output.
     */
    private static final Map<String, List<String>> PROPERTY_GROUPS;

    static {
        PROPERTY_GROUPS = new LinkedHashMap<>();

        PROPERTY_GROUPS.put("WINDOW - Window position and size", List.of(
                "LAST_WINDOW_HEIGHT",
                "LAST_WINDOW_WIDTH",
                "LAST_WINDOW_X_POS",
                "LAST_WINDOW_Y_POS",
                "LAST_LEFT_SPLIT_DIVIDER",
                "LAST_MAIN_SPLIT_DIVIDER"
        ));

        PROPERTY_GROUPS.put("LATEST - Last used values and selections", List.of(
                "LAST_CFG_FILENAME",
                "LAST_CFG_FILENAMES_LIST",
                "LAST_DB_CONNECTION",
                "LAST_ITSQ_REVISION",
                "LAST_LOAD_PATH",
                "LAST_LOAD_DIRECTORY",
                "LOAD_DIRECTORIES",
                "CUSTOMER_FILE_HISTORY",
                "LAST_LOOK_AND_FEEL_CLASS",
                "LAST_TEST_SOURCE",
                "LAST_TEST_TYPE"
        ));

        PROPERTY_GROUPS.put("FLAGS - Boolean settings", List.of(
                "ADMIN_FUNCS_ENABLED",
                "CHECK-EXPORT-PROTOKOLL-ACTIVE",
                "DUMP_IN_REST_CLIENT",
                "SFTP_UPLOAD_ACTIVE",
                "LAST_UPLOAD_SYNTHETICS",
                "LAST_USE_ONLY_TEST_CLZ"
        ));

        PROPERTY_GROUPS.put("CUSTOMERS - Customer configuration", List.of(
                "AVAILABLE_CUSTOMERS",
                "CUSTOMER_CUST01",
                "CUSTOMER_CUST02",
                "CUSTOMER_CUST03",
                "CUSTOMER_CUST04"
        ));

        PROPERTY_GROUPS.put("URL - Service URLs", List.of(
                "ACTIVITI_URLS",
                "MASTERKONSOLE_URLS"
        ));

        PROPERTY_GROUPS.put("DATABASE - Database connections", List.of(
                "DB_CONNECTIONS",
                "SQL_HISTORY",
                "SQL_FAVORITES"
        ));

        PROPERTY_GROUPS.put("TESTS - Test configuration", List.of(
                "ITSQ_REVISIONS",
                "ITSQ_TAG_NAME_FORMAT",
                "MAX_CUSTOMERS_PER_TEST",
                "TEST-BASE-PATH",
                "TEST-SOURCES",
                "TEST-TYPES"
        ));

        PROPERTY_GROUPS.put("TIMINGS - Time delays and intervals", List.of(
                "TIME_BEFORE_BTLG_IMPORT",
                "TIME_BEFORE_CT_IMPORT",
                "TIME_BEFORE_EXPORT",
                "TIME_BEFORE_INSO_EXPORTS",
                "TIME_BEFORE_SFTP_COLLECT"
        ));
    }

    private final Properties props = new Properties();

    private AppConfig() {
        load();
    }

    /**
     * Returns the singleton instance of AppConfig.
     *
     * @return the singleton AppConfig instance
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the current config file path.
     *
     * @return the config file path
     */
    public String getFilePath() {
        return currentFilePath;
    }

    /**
     * Returns the initial config file path (from startup).
     *
     * @return the initial config file path
     */
    public static String getInitialFilePath() {
        return INITIAL_FILE_PATH;
    }

    /**
     * Loads configuration from a different file.
     * Existing properties are cleared and replaced with the new file's content.
     * Subsequent save() calls will write to this file.
     *
     * @param path the path to the config file to load
     * @return true if loaded successfully, false otherwise
     */
    public boolean loadFrom(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            TimelineLogger.error(AppConfig.class, "Config file not found: {}", path);
            return false;
        }

        try (InputStream is = new FileInputStream(file)) {
            props.clear();
            props.load(is);
            currentFilePath = file.getAbsolutePath();
            TimelineLogger.info(AppConfig.class, "Configuration loaded from {} (now active for saving)", currentFilePath);
            return true;
        } catch (IOException e) {
            TimelineLogger.error(AppConfig.class, "Failed to load configuration from {}", path, e);
            return false;
        }
    }

    /**
     * Reloads configuration from the current file.
     */
    public void reload() {
        load();
    }

    private void load() {
        try (InputStream is = new FileInputStream(currentFilePath)) {
            props.load(is);
            TimelineLogger.info(AppConfig.class, "Configuration loaded successfully from {}", currentFilePath);
        } catch (FileNotFoundException e) {
            TimelineLogger.warn(AppConfig.class, "Configuration file not found: {}. Using default values.", currentFilePath);
            initializeDefaults();
        } catch (IOException e) {
            TimelineLogger.error(AppConfig.class, "Failed to load configuration from {}", currentFilePath, e);
            initializeDefaults();
        }
    }

    private void initializeDefaults() {
        TimelineLogger.info(AppConfig.class, "Initializing default configuration values");
        props.setProperty("TEST-BASE-PATH", "/X-TESTS/ENE");
        props.setProperty("TEST-SOURCES", "ITSQ;LOCAL;REMOTE");
        props.setProperty("TEST-TYPES", "UNIT;INTEGRATION;E2E");
        props.setProperty("ITSQ_REVISIONS", "1.0;2.0;3.0");
        props.setProperty("AVAILABLE_CUSTOMERS", "CUST01,CUST02,CUST03,CUST04");
        props.setProperty("LAST_WINDOW_WIDTH", "1200");
        props.setProperty("LAST_WINDOW_HEIGHT", "800");
        props.setProperty("LAST_WINDOW_X_POS", "100");
        props.setProperty("LAST_WINDOW_Y_POS", "100");
        props.setProperty("LAST_LEFT_SPLIT_DIVIDER", "350");
        props.setProperty("LAST_MAIN_SPLIT_DIVIDER", "300");
    }

    /**
     * Sets a property value.
     *
     * @param key   the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        if (key == null || value == null) {
            TimelineLogger.warn(AppConfig.class, "Attempted to set property with null key or value");
            return;
        }
        props.setProperty(key, value);
        TimelineLogger.debug(AppConfig.class, "Property set: {} = {}", key, value);
    }

    /**
     * Gets a property value.
     *
     * @param key the property key
     * @return the property value, or empty string if not found
     */
    public String getProperty(String key) {
        return props.getProperty(key, "");
    }

    /**
     * Gets a property value as a semicolon-separated array.
     * Comments (text after #) are removed from values.
     *
     * @param key the property key
     * @return the property value as string array
     */
    public String[] getArray(String key) {
        String val = getPropertyX(key, "");
        return val.isEmpty() ? new String[0] : val.split(";");
    }

    /**
     * Gets a property value as boolean.
     *
     * @param key the property key
     * @return the boolean value, false if not found or invalid
     */
    public boolean getBool(String key) {
        return Boolean.parseBoolean(getPropertyX(key, "false"));
    }

    /**
     * Gets a property value as integer.
     *
     * @param key the property key
     * @param def the default value if property is not found or invalid
     * @return the integer value
     */
    public int getInt(String key, int def) {
        try {
            String propertyX = getPropertyX(key, String.valueOf(def));
            return Integer.parseInt(propertyX);
        } catch (NumberFormatException e) {
            TimelineLogger.warn(AppConfig.class, "Invalid integer value for key '{}', using default: {}", key, def);
            return def;
        }
    }

    private String getPropertyX(String key, String def) {
        String propValue = props.getProperty(key, def);
        String[] split = propValue.split("#");
        return split[0].trim();
    }

    /**
     * Saves the current configuration to the currently active config file.
     *
     * @throws ConfigurationException if saving fails
     */
    public void save() {
        // Ensure parent directory exists (for Docker volume)
        File configFile = new File(currentFilePath);
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                TimelineLogger.info(AppConfig.class, "Created config directory: {}", parentDir.getAbsolutePath());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(currentFilePath), StandardCharsets.ISO_8859_1))) {

            // Write header
            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("# StandardMDIGUI Configuration");
            writer.newLine();
            writer.write("# Updated: " + timestamp);
            writer.newLine();
            writer.newLine();

            // Write each group
            for (Map.Entry<String, List<String>> group : PROPERTY_GROUPS.entrySet()) {
                writer.write("# ==============================================================================");
                writer.newLine();
                writer.write("# " + group.getKey());
                writer.newLine();
                writer.write("# ==============================================================================");
                writer.newLine();

                for (String key : group.getValue()) {
                    String value = props.getProperty(key);
                    if (value != null) {
                        writer.write(escapeKey(key) + "=" + escapeValue(value));
                        writer.newLine();
                    }
                }
                writer.newLine();
            }

            // Write any remaining properties not in groups
            boolean hasUnknown = false;
            for (String key : props.stringPropertyNames()) {
                boolean found = PROPERTY_GROUPS.values().stream()
                        .anyMatch(list -> list.contains(key));
                if (!found) {
                    if (!hasUnknown) {
                        writer.write("# ==============================================================================");
                        writer.newLine();
                        writer.write("# OTHER - Uncategorized properties");
                        writer.newLine();
                        writer.write("# ==============================================================================");
                        writer.newLine();
                        hasUnknown = true;
                    }
                    writer.write(escapeKey(key) + "=" + escapeValue(props.getProperty(key)));
                    writer.newLine();
                }
            }

            TimelineLogger.info(AppConfig.class, "Configuration saved to {}", currentFilePath);
        } catch (IOException e) {
            TimelineLogger.error(AppConfig.class, "Failed to save configuration to {}", currentFilePath, e);
            throw new ConfigurationException("Failed to save configuration", e);
        }
    }

    /**
     * Escapes special characters in property keys.
     */
    private String escapeKey(String key) {
        return key.replace(":", "\\:")
                .replace("=", "\\=")
                .replace(" ", "\\ ");
    }

    /**
     * Escapes special characters in property values.
     */
    private String escapeValue(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case ':' -> sb.append("\\:");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
