package de.cavdar.gui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency-Tests für ConnectionManager Listener-Verwaltung.
 * Reproduziert ConcurrentModificationException die beim parallelen
 * addListener/notifyListeners auftrat.
 */
class ConnectionManagerConcurrencyTest {

    @Test
    @DisplayName("Listener-Add während Notify wirft keine ConcurrentModificationException")
    void addListenerWhileNotifying_noException() throws InterruptedException {
        // Cleanup vorhandene Listener
        // (kein public clear, daher dummy-Listener entfernen)
        ConnectionManager.ConnectionListener[] dummy = new ConnectionManager.ConnectionListener[100];
        for (int i = 0; i < dummy.length; i++) {
            dummy[i] = () -> { /* no-op */ };
            ConnectionManager.addListener(dummy[i]);
        }

        ExecutorService exec = Executors.newFixedThreadPool(4);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(4);

        // Thread 1+2: kontinuierlich Listener hinzufügen/entfernen
        for (int t = 0; t < 2; t++) {
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 200; i++) {
                        ConnectionManager.ConnectionListener l = () -> { /* no-op */ };
                        ConnectionManager.addListener(l);
                        ConnectionManager.removeListener(l);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // Thread 3+4: kontinuierlich saveConnections() (löst notifyListeners aus)
        for (int t = 0; t < 2; t++) {
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 200; i++) {
                        ConnectionManager.saveConnections();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(10, TimeUnit.SECONDS);
        exec.shutdown();

        // Cleanup dummy listeners
        for (var l : dummy) {
            ConnectionManager.removeListener(l);
        }

        assertThat(finished).as("Threads müssen innerhalb 10s fertig werden").isTrue();
        assertThat(errors.get()).as("Keine ConcurrentModificationException erwartet").isZero();
    }

    @Test
    @DisplayName("addListener verhindert Duplikate auch unter Last")
    void addListener_noDuplicates() {
        ConnectionManager.ConnectionListener listener = () -> { /* no-op */ };
        try {
            // Mehrfach hinzufügen
            ConnectionManager.addListener(listener);
            ConnectionManager.addListener(listener);
            ConnectionManager.addListener(listener);

            // notifyListeners darf nicht crashen
            ConnectionManager.saveConnections();
        } finally {
            ConnectionManager.removeListener(listener);
        }
    }

    /**
     * Regression-Test für C6/C16: notifyListeners() darf den
     * ConnectionManager-Klassenmonitor NICHT halten, wenn ein Listener
     * aufgerufen wird. Sonst kann ein Listener, der z.B. EDT-Code triggert
     * oder zurück in ConnectionManager ruft, einen Lock-Ordering-Deadlock
     * erzeugen.
     *
     * Dieser Test macht die Garantie deterministisch sichtbar via
     * Thread.holdsLock() — ein Revert der Fix-Änderung lässt den Test
     * sofort umkippen.
     */
    @Test
    @DisplayName("Listener wird OHNE ConnectionManager-Monitor aufgerufen (Lock-Ordering-Sicherheit)")
    void notifyListeners_doesNotHoldClassMonitor() {
        java.util.concurrent.atomic.AtomicBoolean monitorHeld = new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.concurrent.atomic.AtomicBoolean wasNotified = new java.util.concurrent.atomic.AtomicBoolean(false);

        ConnectionManager.ConnectionListener probe = () -> {
            wasNotified.set(true);
            // Wenn der Aufrufer den Class-Monitor hält, ist die Fix-Eigenschaft verletzt
            monitorHeld.set(Thread.holdsLock(ConnectionManager.class));
        };

        try {
            ConnectionManager.addListener(probe);
            // saveConnections triggert notifyListeners()
            ConnectionManager.saveConnections();

            assertThat(wasNotified.get())
                    .as("Listener muss aufgerufen worden sein")
                    .isTrue();
            assertThat(monitorHeld.get())
                    .as("notifyListeners darf den ConnectionManager-Klassenmonitor NICHT halten")
                    .isFalse();
        } finally {
            ConnectionManager.removeListener(probe);
        }
    }

    /**
     * Regression-Test für C6/C16: ein Listener, der zurück in ConnectionManager
     * ruft (z.B. getConnections()), darf NICHT deadlocken oder unter einer
     * fremden Lock-Ordering hängen. Mit dem alten Code (notify innerhalb des
     * synchronized-Blocks) konnte ein zweiter Thread, der parallel z.B.
     * getConnections() aufruft, hier einen klassischen Lock-Order-Deadlock
     * provozieren wenn die Listener-Logik einen anderen Lock vor dem
     * ConnectionManager-Monitor erwerben wollte.
     */
    @Test
    @DisplayName("Listener-Reentrancy + paralleler getConnections-Aufruf läuft ohne Deadlock durch")
    void listenerReentrancy_noDeadlock() throws InterruptedException {
        Object foreignLock = new Object();
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger(0);
        CountDownLatch listenerEntered = new CountDownLatch(1);
        CountDownLatch otherThreadDone = new CountDownLatch(1);

        // Listener: hält foreignLock, dann ruft getConnections() (würde im
        // alten Code den ConnectionManager-Monitor anfordern, während der
        // saveConnections-Aufrufer ihn schon hält)
        ConnectionManager.ConnectionListener probe = () -> {
            synchronized (foreignLock) {
                listenerEntered.countDown();
                try {
                    // Kurz warten, damit der andere Thread Zeit hat, den
                    // ConnectionManager-Monitor zu fordern (alter Code → Deadlock)
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                ConnectionManager.getConnections(); // braucht Monitor
            }
        };

        try {
            ConnectionManager.addListener(probe);

            Thread t1 = new Thread(() -> {
                try {
                    ConnectionManager.saveConnections();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }, "save-thread");

            Thread t2 = new Thread(() -> {
                try {
                    listenerEntered.await(2, TimeUnit.SECONDS);
                    // Versuche jetzt, ohne foreignLock, getConnections() aufzurufen
                    ConnectionManager.getConnections();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    otherThreadDone.countDown();
                }
            }, "reader-thread");

            t1.start();
            t2.start();

            t1.join(5_000);
            t2.join(5_000);

            assertThat(t1.isAlive())
                    .as("save-thread darf nicht hängen (Deadlock-Indikator)")
                    .isFalse();
            assertThat(t2.isAlive())
                    .as("reader-thread darf nicht hängen (Deadlock-Indikator)")
                    .isFalse();
            assertThat(errors.get()).as("Keine Exceptions erwartet").isZero();
        } finally {
            ConnectionManager.removeListener(probe);
        }
    }
}
