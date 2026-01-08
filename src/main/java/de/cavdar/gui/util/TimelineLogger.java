package de.cavdar.gui.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central logging utility that manages both standard logging and timeline/performance tracking.
 * All classes should use this instead of declaring their own loggers.
 *
 * <h3>Configuration:</h3>
 * <pre>
 *     // Configure logging with custom directory and filenames
 *     File logsDir = new File("path/to/logs");
 *     TimelineLogger.configure(logsDir, "myapp.log", "myapp-actions.log");
 *
 *     // Close when done
 *     TimelineLogger.close();
 * </pre>
 *
 * <h3>Standard Logging:</h3>
 * <pre>
 *     // Instead of: private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);
 *     // Use:
 *     TimelineLogger.info(MyClass.class, "Processing {} items", count);
 *     TimelineLogger.debug(MyClass.class, "Details: {}", details);
 *     TimelineLogger.warn(MyClass.class, "Warning message");
 *     TimelineLogger.error(MyClass.class, "Error occurred", exception);
 * </pre>
 *
 * <h3>Timeline/Performance Tracking:</h3>
 * <pre>
 *     // Variante 1: Start/End mit ID
 *     String actionId = TimelineLogger.start("loadData", "Loading customer data");
 *     // ... do work ...
 *     TimelineLogger.end(actionId);  // oder: TimelineLogger.end(actionId, "OK");
 *
 *     // Variante 2: Try-with-resources (empfohlen)
 *     try (TimelineLogger.Action action = TimelineLogger.action("processFile")) {
 *         // ... do work ...
 *         action.result("5 records");  // optional
 *     }
 *
 *     // Variante 3: Einzelnes Event
 *     TimelineLogger.event("userLogin", "user=admin");
 * </pre>
 *
 * @author TemplateGUI
 * @version 2.1
 */
public class TimelineLogger {

    // ===== Logger Configuration =====
    private static final String TIMELINE_LOGGER_NAME = "TIMELINE";
    private static final String TIMELINE_APPENDER_NAME = "TimelineAppender";
    private static final String APP_APPENDER_NAME = "AppFileAppender";
    private static final String APP_PACKAGE = "de.cavdar";
    private static final String TIMELINE_PATTERN = "%d{dd.MM.yyyy HH:mm:ss.SSS} | %m%n";
    private static final String APP_PATTERN = "%d{dd.MM.yyyy HH:mm:ss.SSS} [%-5p] %c - %m%n";

    // SLF4J Logger for timeline logging calls
    private static final Logger TIMELINE = LoggerFactory.getLogger(TIMELINE_LOGGER_NAME);
    // Log4j Logger for timeline appender configuration
    private static final org.apache.log4j.Logger LOG4J_TIMELINE = org.apache.log4j.Logger.getLogger(TIMELINE_LOGGER_NAME);

    // ===== Standard Logger Cache =====
    private static final Map<Class<?>, Logger> loggerCache = new ConcurrentHashMap<>();

    // ===== Timeline Action Tracking =====
    private static final Map<String, ActionInfo> activeActions = new ConcurrentHashMap<>();
    private static RollingFileAppender timelineAppender;
    private static RollingFileAppender appAppender;
    private static long actionCounter = 0;

    static {
        // Prevent timeline log propagation to root logger
        LOG4J_TIMELINE.setAdditivity(false);
    }

    private TimelineLogger() {
        // Utility class
    }

    // ==========================================================================
    // STANDARD LOGGING METHODS
    // ==========================================================================

    /**
     * Gets or creates a logger for the specified class.
     * Use this if you need the Logger instance directly.
     *
     * @param clazz the class to get logger for
     * @return the SLF4J logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return loggerCache.computeIfAbsent(clazz, LoggerFactory::getLogger);
    }

    /**
     * Logs a TRACE message.
     */
    public static void trace(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).trace(message, args);
    }

    /**
     * Logs a DEBUG message.
     */
    public static void debug(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).debug(message, args);
    }

    /**
     * Logs an INFO message.
     */
    public static void info(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).info(message, args);
    }

    /**
     * Logs a WARN message.
     */
    public static void warn(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).warn(message, args);
    }

    /**
     * Logs a WARN message with exception.
     */
    public static void warn(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).warn(message, t);
    }

    /**
     * Logs an ERROR message.
     */
    public static void error(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).error(message, args);
    }

    /**
     * Logs an ERROR message with exception.
     */
    public static void error(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).error(message, t);
    }

    /**
     * Checks if DEBUG level is enabled for the class.
     */
    public static boolean isDebugEnabled(Class<?> clazz) {
        return getLogger(clazz).isDebugEnabled();
    }

    /**
     * Checks if TRACE level is enabled for the class.
     */
    public static boolean isTraceEnabled(Class<?> clazz) {
        return getLogger(clazz).isTraceEnabled();
    }

    // ==========================================================================
    // LOGGER CONFIGURATION
    // ==========================================================================

    /**
     * Configures both the application logger and the timeline/action logger.
     * This is the central configuration point for all file-based logging.
     *
     * @param logOutputDir    the directory for log files
     * @param appLogFileName  the filename for application logs (e.g., "app.log")
     * @param actionLogFileName the filename for timeline/action logs (e.g., "actions.log")
     * @return true if configuration was successful
     */
    public static boolean configure(File logOutputDir, String appLogFileName, String actionLogFileName) {
        try {
            // Ensure directory exists
            if (!logOutputDir.exists() && !logOutputDir.mkdirs()) {
                System.err.println("[TimelineLogger] Could not create log directory: " + logOutputDir.getAbsolutePath());
                return false;
            }

            File appLogFile = new File(logOutputDir, appLogFileName);
            File actionLogFile = new File(logOutputDir, actionLogFileName);

            // Rotate existing log files
            rotateExistingLogFile(appLogFile);
            rotateExistingLogFile(actionLogFile);

            // Configure application logger (only for de.cavdar.* packages)
            appAppender = configureAppender(appAppender, APP_APPENDER_NAME, APP_PATTERN, appLogFile, true);
            if (appAppender == null) return false;
            org.apache.log4j.Logger appPackageLogger = org.apache.log4j.Logger.getLogger(APP_PACKAGE);
            appPackageLogger.setAdditivity(false);  // Don't propagate to root logger
            if (!appPackageLogger.isAttached(appAppender)) {
                appPackageLogger.addAppender(appAppender);
            }

            // Configure timeline logger
            timelineAppender = configureAppender(timelineAppender, TIMELINE_APPENDER_NAME, TIMELINE_PATTERN, actionLogFile, false);
            if (timelineAppender == null) return false;
            if (!LOG4J_TIMELINE.isAttached(timelineAppender)) {
                LOG4J_TIMELINE.addAppender(timelineAppender);
            }

            return true;
        } catch (Exception e) {
            System.err.println("[TimelineLogger] Error configuring: " + e.getMessage());
            return false;
        }
    }

    /**
     * Rotates an existing log file by renaming it to .001, .002, etc.
     */
    private static void rotateExistingLogFile(File logFile) {
        if (!logFile.exists()) {
            return;
        }

        // Find next available number
        int number = 1;
        File rotatedFile;
        do {
            rotatedFile = new File(logFile.getAbsolutePath() + String.format(".%03d", number));
            number++;
        } while (rotatedFile.exists() && number < 1000);

        if (number >= 1000) {
            System.err.println("[TimelineLogger] Too many rotated log files for: " + logFile.getName());
            return;
        }

        if (logFile.renameTo(rotatedFile)) {
            System.out.println("[TimelineLogger] Rotated " + logFile.getName() + " -> " + rotatedFile.getName());
        } else {
            System.err.println("[TimelineLogger] Could not rotate: " + logFile.getName());
        }
    }

    private static RollingFileAppender configureAppender(RollingFileAppender existingAppender,
                                                         String appenderName, String pattern, File logFile,
                                                         boolean shortenPackageNames) {
        try {
            if (existingAppender != null) {
                // Update existing appender
                rotateExistingLogFile(logFile);
                existingAppender.setFile(logFile.getAbsolutePath());
                existingAppender.activateOptions();
                System.out.println("[TimelineLogger] " + appenderName + " reconfigured: " + logFile.getAbsolutePath());
                return existingAppender;
            }

            // Create new appender
            RollingFileAppender appender = new RollingFileAppender();
            appender.setName(appenderName);
            appender.setFile(logFile.getAbsolutePath());
            appender.setMaxFileSize("10MB");
            appender.setMaxBackupIndex(5);
            appender.setLayout(shortenPackageNames ? new ShortenedPackageLayout(pattern) : new PatternLayout(pattern));
            appender.setAppend(true);
            appender.activateOptions();
            System.out.println("[TimelineLogger] " + appenderName + " initialized: " + logFile.getAbsolutePath());
            return appender;
        } catch (Exception e) {
            System.err.println("[TimelineLogger] Error configuring " + appenderName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Custom PatternLayout that shortens package names.
     * de.cavdar.gui -> de.c.g
     */
    private static class ShortenedPackageLayout extends PatternLayout {
        ShortenedPackageLayout(String pattern) {
            super(pattern);
        }

        @Override
        public String format(LoggingEvent event) {
            String original = super.format(event);
            return original.replace("de.cavdar.gui", "de.c.g");
        }
    }

    /**
     * Closes all loggers and releases file handles.
     */
    public static void close() {
        // Close timeline appender
        if (timelineAppender != null) {
            timelineAppender.close();
            LOG4J_TIMELINE.removeAppender(timelineAppender);
            timelineAppender = null;
        }
        // Close app appender
        if (appAppender != null) {
            appAppender.close();
            org.apache.log4j.Logger.getLogger(APP_PACKAGE).removeAppender(appAppender);
            appAppender = null;
        }
        activeActions.clear();
    }

    // ==========================================================================
    // TIMELINE ACTION TRACKING
    // ==========================================================================

    /**
     * Starts tracking an action.
     *
     * @param actionName the name of the action
     * @return the action ID (use this to call end())
     */
    public static String start(String actionName) {
        return start(actionName, null);
    }

    /**
     * Starts tracking an action with a description.
     *
     * @param actionName  the name of the action
     * @param description optional description
     * @return the action ID (use this to call end())
     */
    public static String start(String actionName, String description) {
        String actionId = generateActionId(actionName);
        Instant startTime = Instant.now();

        ActionInfo info = new ActionInfo(actionName, description, startTime);
        activeActions.put(actionId, info);

        StringBuilder sb = new StringBuilder();
        sb.append("START | action=").append(actionName);
        if (description != null && !description.isEmpty()) {
            sb.append(" | desc=").append(description);
        }
        sb.append(" | id=").append(actionId);

        TIMELINE.info(sb.toString());
        return actionId;
    }

    /**
     * Ends tracking an action and logs the duration.
     *
     * @param actionId the action ID returned by start()
     */
    public static void end(String actionId) {
        end(actionId, null);
    }

    /**
     * Ends tracking an action with a result message.
     *
     * @param actionId the action ID returned by start()
     * @param result   optional result message (e.g., "OK", "FAILED", "5 records")
     */
    public static void end(String actionId, String result) {
        ActionInfo info = activeActions.remove(actionId);
        if (info == null) {
            TIMELINE.warn("END   | action=UNKNOWN | id={} | error=No matching start", actionId);
            return;
        }

        Instant endTime = Instant.now();
        Duration duration = Duration.between(info.startTime, endTime);

        StringBuilder sb = new StringBuilder();
        sb.append("END   | action=").append(info.actionName);
        sb.append(" | duration=").append(formatDuration(duration));
        if (result != null && !result.isEmpty()) {
            sb.append(" | result=").append(result);
        }
        sb.append(" | id=").append(actionId);

        TIMELINE.info(sb.toString());
    }

    /**
     * Logs a single event (no start/end tracking).
     *
     * @param eventName the name of the event
     */
    public static void event(String eventName) {
        event(eventName, null);
    }

    /**
     * Logs a single event with details.
     *
     * @param eventName the name of the event
     * @param details   optional details
     */
    public static void event(String eventName, String details) {
        StringBuilder sb = new StringBuilder();
        sb.append("EVENT | name=").append(eventName);
        if (details != null && !details.isEmpty()) {
            sb.append(" | details=").append(details);
        }
        TIMELINE.info(sb.toString());
    }

    /**
     * Creates an auto-closeable action for use with try-with-resources.
     *
     * @param actionName the name of the action
     * @return an Action that will log end() when closed
     */
    public static Action action(String actionName) {
        return new Action(actionName, null);
    }

    /**
     * Creates an auto-closeable action with description.
     *
     * @param actionName  the name of the action
     * @param description optional description
     * @return an Action that will log end() when closed
     */
    public static Action action(String actionName, String description) {
        return new Action(actionName, description);
    }

    // ==========================================================================
    // HELPER METHODS AND INNER CLASSES
    // ==========================================================================

    private static synchronized String generateActionId(String actionName) {
        return actionName + "-" + (++actionCounter);
    }

    private static String formatDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            long minutes = duration.toMinutes();
            long secs = duration.toSecondsPart();
            return String.format("%dm %ds", minutes, secs);
        }
    }

    /**
     * Holds information about an active action.
     */
    private static class ActionInfo {
        final String actionName;
        final String description;
        final Instant startTime;

        ActionInfo(String actionName, String description, Instant startTime) {
            this.actionName = actionName;
            this.description = description;
            this.startTime = startTime;
        }
    }

    /**
     * Auto-closeable action for try-with-resources pattern.
     */
    public static class Action implements AutoCloseable {
        private final String actionId;
        private String result;

        Action(String actionName, String description) {
            this.actionId = TimelineLogger.start(actionName, description);
        }

        /**
         * Sets the result to be logged when the action ends.
         *
         * @param result the result message
         * @return this action for chaining
         */
        public Action result(String result) {
            this.result = result;
            return this;
        }

        @Override
        public void close() {
            TimelineLogger.end(actionId, result);
        }
    }
}
