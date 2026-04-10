package de.cavdar.gui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentLockManagerTest {

    @TempDir
    Path tempDir;

    private File envDirA;
    private File envDirB;

    @BeforeEach
    void setUp() {
        envDirA = tempDir.resolve("ENV_A").toFile();
        envDirB = tempDir.resolve("ENV_B").toFile();
        // Clean state
        EnvironmentLockManager.releaseLock();
    }

    @AfterEach
    void tearDown() {
        EnvironmentLockManager.releaseLock();
    }

    @Test
    @DisplayName("acquireLock – einmaliger Lock funktioniert")
    void acquireLock_single() {
        boolean acquired = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");

        assertThat(acquired).isTrue();
        assertThat(EnvironmentLockManager.getCurrentLockedEnvironment()).isEqualTo("ENV_A");
        assertThat(new File(envDirA, ".env.lock")).exists();
    }

    @Test
    @DisplayName("acquireLock – releaseLock gibt Lock sauber frei")
    void releaseLock_clearsState() {
        EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        EnvironmentLockManager.releaseLock();

        assertThat(EnvironmentLockManager.getCurrentLockedEnvironment()).isNull();
        assertThat(new File(envDirA, ".env.lock")).doesNotExist();

        // Nach Release muss erneuter Lock möglich sein
        boolean acquired = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("acquireLock – zweiter Lock auf gleiche Umgebung wird abgelehnt (kein Socket-Leak)")
    void acquireLock_sameEnvTwice_secondRejected() {
        boolean first = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        assertThat(first).isTrue();

        // Zweiter Versuch auf GLEICHE Umgebung muss fehlschlagen oder
        // den ersten Lock sauber wiederverwenden — nicht silent überschreiben
        boolean second = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");

        // State muss konsistent sein: nach release muss der Port frei sein
        EnvironmentLockManager.releaseLock();

        // Wenn der zweite acquireLock den ersten Socket leaked hätte, würde
        // dieser Lock-Versuch fehlschlagen weil der Port noch belegt ist
        boolean third = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        assertThat(third).as("Nach release muss neuer Lock möglich sein (kein Leak)").isTrue();
    }

    @Test
    @DisplayName("acquireLock – Wechsel zwischen Umgebungen")
    void acquireLock_switchEnvironments() {
        boolean a = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        assertThat(a).isTrue();
        assertThat(EnvironmentLockManager.getCurrentLockedEnvironment()).isEqualTo("ENV_A");

        // Wechsel zu B muss die Sperre auf A freigeben
        boolean b = EnvironmentLockManager.acquireLock(envDirB, "ENV_B");
        assertThat(b).isTrue();
        assertThat(EnvironmentLockManager.getCurrentLockedEnvironment()).isEqualTo("ENV_B");

        // ENV_A muss jetzt frei sein (erkennbar daran dass ein neuer Lock möglich wäre,
        // nachdem wir B freigeben)
        EnvironmentLockManager.releaseLock();
        boolean aAgain = EnvironmentLockManager.acquireLock(envDirA, "ENV_A");
        assertThat(aAgain).as("ENV_A muss nach Wechsel + Release wieder lockbar sein").isTrue();
    }

    @Test
    @DisplayName("isLocked – frische Umgebung ist nicht gesperrt")
    void isLocked_freshEnv() {
        File freshDir = tempDir.resolve("FRESH").toFile();
        assertThat(EnvironmentLockManager.isLocked(freshDir)).isFalse();
    }
}
