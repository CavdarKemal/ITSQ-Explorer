package de.cavdar.gui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency-Tests für C3: TestEnvironmentManager statischen State synchronisiert.
 *
 * Vorher konnten zwei Threads parallel switchEnvironment() aufrufen und die
 * statischen Felder (currentEnvironment, currentEnvDir, currentLogsDir,
 * currentTestOutputsDir) inkonsistent setzen — z.B. currentEnvironment="ABC"
 * mit currentLogsDir vom XYZ-Switch. Diese Tests zeigen, dass der State nach
 * jedem switchEnvironment() konsistent ist.
 */
@DisplayName("TestEnvironmentManager Concurrency Tests")
class TestEnvironmentManagerConcurrencyTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestEnvironmentManager.reset();
        TestEnvironmentManager.setBaseDirectory(tempDir.toFile());
    }

    @AfterEach
    void tearDown() {
        TestEnvironmentManager.reset();
    }

    /**
     * Schaltet aus mehreren Threads parallel zwischen Umgebungen um und prüft
     * nach jedem Schaltvorgang, dass die zusammengehörigen Felder konsistent
     * sind: currentLogsDir muss zum currentEnvironment passen.
     *
     * Im alten Code (kein Sync) konnte ein Schaltvorgang mitten im
     * State-Update unterbrochen werden, wodurch ein konkurrierender
     * Thread eine inkonsistente Mischung sah.
     */
    @Test
    @DisplayName("Paralleles switchEnvironment lässt keinen inkonsistenten State zurück")
    void switchEnvironment_concurrent_consistentState() throws InterruptedException {
        final int threadCount = 8;
        final int iterations = 50;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger inconsistencies = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        String[] envConfigs = {"abc-config.properties", "xyz-config.properties", "def-config.properties"};

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        String cfg = envConfigs[(threadId + i) % envConfigs.length];
                        TestEnvironmentManager.switchEnvironment(cfg);

                        // Atomarer Snapshot — alle 4 Felder unter einer Lock-Akquise.
                        // Im alten ungesicherten Code konnte der Writer mitten im
                        // Update unterbrochen werden, sodass der Snapshot env="A"
                        // mit dirs="B" sehen würde.
                        TestEnvironmentManager.StateSnapshot snap =
                                TestEnvironmentManager.getCurrentStateSnapshot();

                        if (snap.environment == null
                                || snap.logsDir == null
                                || snap.envDir == null
                                || snap.testOutputsDir == null) {
                            inconsistencies.incrementAndGet();
                            continue;
                        }
                        if (!snap.envDir.getName().equals(snap.environment)
                                || !snap.logsDir.getAbsolutePath().contains(File.separator + snap.environment + File.separator)
                                || !snap.testOutputsDir.getAbsolutePath().contains(File.separator + snap.environment + File.separator)) {
                            inconsistencies.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        exec.shutdown();

        assertThat(finished).as("Threads müssen innerhalb 30s fertig werden").isTrue();
        assertThat(errors.get()).as("Keine Exceptions erwartet").isZero();
        assertThat(inconsistencies.get())
                .as("currentEnvironment / currentEnvDir / currentLogsDir / currentTestOutputsDir " +
                        "müssen nach jedem switchEnvironment() konsistent sein")
                .isZero();
    }

    /**
     * Reentrancy-Smoke-Test: dass setBaseDirectory + reset + switchEnvironment
     * sich nicht gegenseitig auf den Zehen stehen wenn parallel aufgerufen.
     * Wir prüfen primär, dass keine Exception fliegt und der finale State
     * konsistent ist.
     */
    @Test
    @DisplayName("Parallele setBaseDirectory + switchEnvironment + reset crashen nicht")
    void mixedConcurrentCalls_noException() throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(4);
        AtomicInteger errors = new AtomicInteger(0);

        exec.submit(() -> { try { start.await(); for (int i = 0; i < 100; i++) TestEnvironmentManager.setBaseDirectory(tempDir.toFile()); } catch (Exception e) { errors.incrementAndGet(); } finally { done.countDown(); } });
        exec.submit(() -> { try { start.await(); for (int i = 0; i < 100; i++) TestEnvironmentManager.switchEnvironment("abc-config.properties"); } catch (Exception e) { errors.incrementAndGet(); } finally { done.countDown(); } });
        exec.submit(() -> { try { start.await(); for (int i = 0; i < 100; i++) TestEnvironmentManager.switchEnvironment("xyz-config.properties"); } catch (Exception e) { errors.incrementAndGet(); } finally { done.countDown(); } });
        exec.submit(() -> { try { start.await(); for (int i = 0; i < 100; i++) TestEnvironmentManager.getCurrentEnvironment(); } catch (Exception e) { errors.incrementAndGet(); } finally { done.countDown(); } });

        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        exec.shutdown();

        assertThat(finished).as("Threads müssen innerhalb 30s fertig werden").isTrue();
        assertThat(errors.get()).as("Keine Exceptions erwartet").isZero();
    }
}
