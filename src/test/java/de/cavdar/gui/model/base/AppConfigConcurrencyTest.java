package de.cavdar.gui.model.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency-Tests für AppConfig.loadFrom().
 * Reproduziert das Problem: clear() + load() ist nicht atomar →
 * concurrent reader sieht zwischenzeitlich leere Properties.
 */
class AppConfigConcurrencyTest {

    @TempDir
    Path tempDir;

    private Path configFile;

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("test-config.properties");
        StringBuilder content = new StringBuilder();
        // Große Datei → loadFrom() dauert spürbar lange → Race wird messbar
        for (int i = 0; i < 5000; i++) {
            content.append("KEY_").append(i).append("=value_with_some_content_").append(i)
                   .append("_padded_to_make_it_longer\n");
        }
        Files.writeString(configFile, content.toString());
    }

    @AfterEach
    void tearDown() {
        // Reset zu Default-Config nach Test
    }

    @Test
    @DisplayName("loadFrom – concurrent reads sehen niemals leeren Zustand")
    void loadFromAtomic_noEmptyState() throws InterruptedException {
        AppConfig config = AppConfig.getInstance();
        config.loadFrom(configFile.toString());

        ExecutorService exec = Executors.newFixedThreadPool(8);
        AtomicInteger emptyReads = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(8);

        // 4 Reader-Threads
        for (int t = 0; t < 4; t++) {
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        // Immer prüfen ob KEY_25 gesetzt ist
                        // Wenn loadFrom non-atomar ist, gibt's einen Moment
                        // wo props leer ist und KEY_25 → "" ist
                        String val = config.getProperty("KEY_25");
                        if (val.isEmpty()) {
                            emptyReads.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // 4 Reload-Threads
        for (int t = 0; t < 4; t++) {
            exec.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        config.loadFrom(configFile.toString());
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(15, TimeUnit.SECONDS);
        exec.shutdown();

        assertThat(finished).as("Threads müssen innerhalb 15s fertig werden").isTrue();
        assertThat(errors.get()).as("Keine Exceptions erwartet").isZero();
        assertThat(emptyReads.get())
                .as("Reader dürfen NIEMALS einen leeren Zustand sehen — clear+load muss atomar sein")
                .isZero();
    }
}
