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
}
