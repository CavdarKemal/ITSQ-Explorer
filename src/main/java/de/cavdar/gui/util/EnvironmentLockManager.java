package de.cavdar.gui.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Verwaltet prozessuebergreifende Locks fuer Umgebungen.
 * Verwendet ServerSocket fuer zuverlaessige prozessuebergreifende Locks.
 * Jede Umgebung bekommt einen eigenen Port basierend auf dem Umgebungsnamen.
 */
public class EnvironmentLockManager {

    private static final String LOCK_FILE_NAME = ".env.lock";
    private static final int BASE_PORT = 47100; // Basis-Port fuer Locks

    // Port-Mapping fuer bekannte Umgebungen
    private static final Map<String, Integer> ENV_PORTS = new HashMap<>();
    static {
        ENV_PORTS.put("ENE", BASE_PORT);
        ENV_PORTS.put("ABE", BASE_PORT + 1);
        ENV_PORTS.put("GEE", BASE_PORT + 2);
    }

    private static ServerSocket lockSocket;
    private static File currentLockFile;
    private static String currentLockedEnv;
    private static boolean shutdownHookRegistered = false;

    /**
     * Ermittelt den Port fuer eine Umgebung.
     */
    private static int getPortForEnvironment(String envName) {
        return ENV_PORTS.getOrDefault(envName.toUpperCase(), BASE_PORT + Math.abs(envName.hashCode() % 100));
    }

    /**
     * Versucht einen Lock fuer die angegebene Umgebung zu erwerben.
     *
     * @param envDir  Das Umgebungsverzeichnis (z.B. TEST-ENVS/ABE)
     * @param envName Der Name der Umgebung (z.B. "ABE")
     * @return true wenn Lock erfolgreich erworben, false wenn bereits gesperrt
     */
    public static synchronized boolean acquireLock(File envDir, String envName) {
        // Verzeichnis erstellen falls nicht vorhanden
        if (!envDir.exists() && !envDir.mkdirs()) {
            TimelineLogger.error(EnvironmentLockManager.class,
                    "Konnte Umgebungsverzeichnis nicht erstellen: {}", envDir.getAbsolutePath());
            return false;
        }

        int port = getPortForEnvironment(envName);

        try {
            // Versuche ServerSocket auf dem Port zu oeffnen
            // Wenn der Port bereits belegt ist, wirft dies eine Exception
            ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());

            // Lock erfolgreich erworben
            lockSocket = socket;
            currentLockedEnv = envName;

            // Lock-Datei mit Info schreiben
            File lockFile = new File(envDir, LOCK_FILE_NAME);
            String lockInfo = "Locked by PID: " + ProcessHandle.current().pid() + "\n" +
                    "Environment: " + envName + "\n" +
                    "Port: " + port + "\n" +
                    "Time: " + java.time.LocalDateTime.now() + "\n";
            Files.writeString(lockFile.toPath(), lockInfo,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            currentLockFile = lockFile;

            TimelineLogger.info(EnvironmentLockManager.class,
                    "Lock fuer Umgebung {} erworben (Port {})", envName, port);
            return true;

        } catch (IOException e) {
            // Port ist bereits belegt - Umgebung ist gesperrt
            TimelineLogger.warn(EnvironmentLockManager.class,
                    "Umgebung {} ist bereits von einer anderen Instanz gesperrt (Port {} belegt)",
                    envName, port);
            return false;
        }
    }

    /**
     * Gibt den aktuellen Lock frei.
     */
    public static synchronized void releaseLock() {
        if (lockSocket != null) {
            try {
                String envName = currentLockedEnv;
                int port = lockSocket.getLocalPort();
                lockSocket.close();
                TimelineLogger.info(EnvironmentLockManager.class,
                        "Lock fuer Umgebung {} freigegeben (Port {})", envName, port);
            } catch (IOException e) {
                TimelineLogger.error(EnvironmentLockManager.class,
                        "Fehler beim Freigeben des Locks: {}", e.getMessage());
            }
            lockSocket = null;
        }

        // Lock-Datei loeschen
        if (currentLockFile != null && currentLockFile.exists()) {
            try {
                Files.deleteIfExists(currentLockFile.toPath());
            } catch (IOException e) {
                TimelineLogger.warn(EnvironmentLockManager.class,
                        "Lock-Datei konnte nicht geloescht werden: {}", currentLockFile.getAbsolutePath());
            }
            currentLockFile = null;
        }

        currentLockedEnv = null;
    }

    /**
     * Prueft ob die angegebene Umgebung bereits gesperrt ist.
     *
     * @param envDir Das Umgebungsverzeichnis (wird fuer Umgebungsnamen-Extraktion verwendet)
     * @return true wenn gesperrt, false wenn frei
     */
    public static boolean isLocked(File envDir) {
        String envName = envDir.getName().toUpperCase();
        int port = getPortForEnvironment(envName);

        try (ServerSocket testSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            // Port ist frei - Umgebung ist nicht gesperrt
            testSocket.close();
            return false;
        } catch (IOException e) {
            // Port ist belegt - Umgebung ist gesperrt
            return true;
        }
    }

    /**
     * Gibt den Namen der aktuell gesperrten Umgebung zurueck.
     *
     * @return Name der Umgebung oder null wenn keine gesperrt
     */
    public static String getCurrentLockedEnvironment() {
        return currentLockedEnv;
    }

    /**
     * Registriert einen Shutdown Hook um den Lock beim Beenden freizugeben.
     */
    public static synchronized void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                TimelineLogger.info(EnvironmentLockManager.class, "Shutdown Hook: Gebe Lock frei...");
                releaseLock();
            }, "EnvironmentLockManager-ShutdownHook"));
            shutdownHookRegistered = true;
            TimelineLogger.debug(EnvironmentLockManager.class, "Shutdown Hook registriert");
        }
    }
}
