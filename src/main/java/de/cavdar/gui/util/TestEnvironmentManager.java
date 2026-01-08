package de.cavdar.gui.util;

import java.io.File;

/**
 * Manages test environment directories and logging configuration.
 * Creates environment-specific directories under TEST-ENVS and configures
 * logging to use the appropriate log file.
 *
 * @author TemplateGUI
 * @version 1.0
 */
public class TestEnvironmentManager {


    private static final String TEST_ENVS_DIR = "TEST-ENVS";
    private static final String LOGS_DIR = "logs";
    private static final String TEST_OUTPUTS_DIR = "TEST-OUTPUTS";

    private static String currentEnvironment = null;
    private static File currentEnvDir = null;
    private static File currentLogsDir = null;
    private static File currentTestOutputsDir = null;
    private static File baseDirectory = null;

    private TestEnvironmentManager() {
        // Utility class
    }

    /**
     * Sets the base directory for TEST-ENVS.
     * If not set, uses user.dir.
     *
     * @param baseDir the base directory
     */
    public static void setBaseDirectory(File baseDir) {
        baseDirectory = baseDir;
        TimelineLogger.debug(TestEnvironmentManager.class, "Base directory set to: {}", baseDir);
    }

    /**
     * Gets the current base directory.
     *
     * @return the base directory (user.dir if not explicitly set)
     */
    public static File getBaseDirectory() {
        if (baseDirectory != null) {
            return baseDirectory;
        }
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Resets the manager state. Useful for testing.
     * Also closes any open FileAppender to release file handles.
     */
    public static void reset() {
        closeLogging();
        EnvironmentLockManager.releaseLock();
        currentEnvironment = null;
        currentEnvDir = null;
        currentLogsDir = null;
        currentTestOutputsDir = null;
        baseDirectory = null;
    }

    /**
     * Closes all loggers to release file handles.
     * Useful for testing to allow temp directory cleanup.
     */
    public static void closeLogging() {
        TimelineLogger.close();
    }

    /**
     * Extracts the environment name from a config file name.
     * Takes the first 3 characters and converts to uppercase.
     *
     * @param configFileName the config file name (e.g., "ene-config.properties")
     * @return the environment name (e.g., "ENE")
     */
    public static String extractEnvironmentName(String configFileName) {
        if (configFileName == null || configFileName.length() < 3) {
            return "DEFAULT";
        }
        return configFileName.substring(0, 3).toUpperCase();
    }

    /**
     * Switches to a new environment based on the config file name.
     * Creates the necessary directories, acquires environment lock, and reconfigures logging.
     *
     * @param configFileName the config file name (e.g., "ene-config.properties")
     * @return true if switch was successful, false if environment is locked by another instance
     */
    public static boolean switchEnvironment(String configFileName) {
        String envName = extractEnvironmentName(configFileName);

        if (envName.equals(currentEnvironment)) {
            TimelineLogger.debug(TestEnvironmentManager.class, "Already in environment: {}", envName);
            return true;
        }

        TimelineLogger.info(TestEnvironmentManager.class, "Switching to environment: {}", envName);

        // Create directory structure
        File baseDir = getBaseDirectory();
        File testEnvsDir = new File(baseDir, TEST_ENVS_DIR);
        File envDir = new File(testEnvsDir, envName);
        File logsDir = new File(envDir, LOGS_DIR);
        File testOutputsDir = new File(envDir, TEST_OUTPUTS_DIR);

        // Create directories first (needed for lock file)
        if (!createDirectories(logsDir, testOutputsDir)) {
            return false;
        }

        // Check if new environment is locked by another instance
        if (EnvironmentLockManager.isLocked(envDir)) {
            TimelineLogger.warn(TestEnvironmentManager.class,
                    "Environment {} is locked by another instance", envName);
            return false;
        }

        // Release old lock (if any)
        EnvironmentLockManager.releaseLock();

        // Acquire lock for new environment
        if (!EnvironmentLockManager.acquireLock(envDir, envName)) {
            TimelineLogger.error(TestEnvironmentManager.class,
                    "Could not acquire lock for environment: {}", envName);
            return false;
        }

        // Configure logging (both app and timeline logs)
        String appLogFileName = envName + ".log";
        String actionLogFileName = envName + "-actions.log";
        if (!TimelineLogger.configure(logsDir, appLogFileName, actionLogFileName)) {
            TimelineLogger.warn(TestEnvironmentManager.class, "Could not configure logging for environment: {}", envName);
        }

        // Update state
        currentEnvironment = envName;
        currentEnvDir = envDir;
        currentLogsDir = logsDir;
        currentTestOutputsDir = testOutputsDir;

        TimelineLogger.info(TestEnvironmentManager.class, "Environment switched to: {} ({})", envName, envDir.getAbsolutePath());
        return true;
    }

    /**
     * Creates the required directories.
     */
    private static boolean createDirectories(File logsDir, File testOutputsDir) {
        try {
            if (!logsDir.exists() && !logsDir.mkdirs()) {
                TimelineLogger.error(TestEnvironmentManager.class, "Could not create logs directory: {}", logsDir.getAbsolutePath());
                return false;
            }
            if (!testOutputsDir.exists() && !testOutputsDir.mkdirs()) {
                TimelineLogger.error(TestEnvironmentManager.class, "Could not create test outputs directory: {}", testOutputsDir.getAbsolutePath());
                return false;
            }
            TimelineLogger.debug(TestEnvironmentManager.class, "Directories created/verified: {}, {}", logsDir.getAbsolutePath(), testOutputsDir.getAbsolutePath());
            return true;
        } catch (SecurityException e) {
            TimelineLogger.error(TestEnvironmentManager.class, "Security exception creating directories", e);
            return false;
        }
    }

    // ===== Getters =====

    /**
     * Returns the current environment name.
     */
    public static String getCurrentEnvironment() {
        return currentEnvironment;
    }

    /**
     * Returns the current environment directory.
     */
    public static File getCurrentEnvDir() {
        return currentEnvDir;
    }

    /**
     * Returns the current logs directory.
     */
    public static File getCurrentLogsDir() {
        return currentLogsDir;
    }

    /**
     * Returns the current test outputs directory.
     */
    public static File getCurrentTestOutputsDir() {
        return currentTestOutputsDir;
    }

    /**
     * Returns the current log file path.
     */
    public static String getCurrentLogFilePath() {
        if (currentLogsDir == null || currentEnvironment == null) {
            return null;
        }
        return new File(currentLogsDir, currentEnvironment + ".log").getAbsolutePath();
    }
}
