package de.cavdar.itsq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AB30MapperUtilTest {

    @TempDir
    Path tempDir;

    private AB30MapperUtil util;

    @BeforeEach
    void setUp() {
        util = new AB30MapperUtil();
    }

    @Test
    @DisplayName("writeCrefoToCustomerMappingFile – jeder Crefo erscheint in der Datei (kein Datenverlust)")
    void writeCrefoToCustomerMappingFile_allCrefosWritten() throws IOException {
        // Vorbereiten: 3 Crefos, jeder gehört zu Customer "C1"
        Map<Long, AB30XMLProperties> map = new HashMap<>();
        map.put(1001L, propertiesFor(1001L, "C1"));
        map.put(1002L, propertiesFor(1002L, "C1"));
        map.put(1003L, propertiesFor(1003L, "C1"));

        File outputFile = tempDir.resolve("crefo_to_customer.txt").toFile();

        util.writeCrefoToCustomerMappingFile(outputFile, map);

        // Datei existiert
        assertThat(outputFile).exists();

        // Inhalt prüfen — alle 3 Crefos müssen in der Customer-C1-Zeile stehen
        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertThat(lines).hasSize(2); // 1 Customer-Zeile + 1 Crefo-Zeile

        String customerLine = lines.get(0);
        String crefosLine = lines.get(1);

        assertThat(customerLine).isEqualTo("C1");
        // Bug: aktuell fehlt die ERSTE Crefo (zufällige Reihenfolge wegen HashMap)
        // Erwartet: alle drei Crefos
        assertThat(crefosLine).contains("1001");
        assertThat(crefosLine).contains("1002");
        assertThat(crefosLine).contains("1003");
    }

    @Test
    @DisplayName("writeCrefoToCustomerMappingFile – ein Crefo zu mehreren Customern")
    void writeCrefoToCustomerMappingFile_crefoInMultipleCustomers() throws IOException {
        Map<Long, AB30XMLProperties> map = new HashMap<>();
        map.put(2001L, propertiesFor(2001L, "C1", "C2"));
        map.put(2002L, propertiesFor(2002L, "C2"));

        File outputFile = tempDir.resolve("crefo_to_customer.txt").toFile();

        util.writeCrefoToCustomerMappingFile(outputFile, map);

        List<String> lines = Files.readAllLines(outputFile.toPath());
        // 2 Customer-Zeilen + 2 Crefo-Zeilen
        assertThat(lines).hasSize(4);

        String content = String.join("\n", lines);
        // C1 muss Crefo 2001 enthalten
        assertThat(content).contains("C1");
        assertThat(content).contains("2001");
        // C2 muss BEIDE Crefos enthalten (2001 und 2002)
        assertThat(content).contains("C2");
        assertThat(content).contains("2002");
    }

    @Test
    @DisplayName("writeCrefoToCustomerMappingFile – einzelner Crefo, einzelner Customer")
    void writeCrefoToCustomerMappingFile_singleCrefoSingleCustomer() throws IOException {
        Map<Long, AB30XMLProperties> map = new HashMap<>();
        map.put(3001L, propertiesFor(3001L, "C1"));

        File outputFile = tempDir.resolve("crefo_to_customer.txt").toFile();

        util.writeCrefoToCustomerMappingFile(outputFile, map);

        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo("C1");
        assertThat(lines.get(1)).contains("3001");
    }

    @Test
    @DisplayName("writeCrefoToCustomerMappingFile – existierende Datei wird sicher gesichert")
    void writeCrefoToCustomerMappingFile_existingFileBackedUp() throws IOException {
        File outputFile = tempDir.resolve("existing.txt").toFile();
        Files.writeString(outputFile.toPath(), "OLD CONTENT");
        assertThat(outputFile).exists();

        Map<Long, AB30XMLProperties> map = new HashMap<>();
        map.put(4001L, propertiesFor(4001L, "C1"));

        util.writeCrefoToCustomerMappingFile(outputFile, map);

        // Neue Datei muss existieren mit neuem Inhalt
        assertThat(outputFile).exists();
        String content = Files.readString(outputFile.toPath());
        assertThat(content).contains("C1");
        assertThat(content).contains("4001");
        assertThat(content).doesNotContain("OLD CONTENT");

        // Alte Datei sollte als .old gesichert sein
        File oldFile = new File(tempDir.toFile(), "existing.txt.old");
        assertThat(oldFile).exists();
        assertThat(Files.readString(oldFile.toPath())).isEqualTo("OLD CONTENT");
    }

    @Test
    @DisplayName("writeAb30CrefoToPropertiesMapToFile – Backup-Logik schützt vor Datenverlust")
    void writeAb30CrefoToPropertiesMapToFile_existingFileBackedUp() throws IOException {
        File outputFile = tempDir.resolve("TestCrefosExtended.properties").toFile();
        Files.writeString(outputFile.toPath(), "OLD CONTENT");

        Map<Long, AB30XMLProperties> map = new HashMap<>();
        AB30XMLProperties props = new AB30XMLProperties(5001L);
        map.put(5001L, props);

        util.writeAb30CrefoToPropertiesMapToFile(outputFile, map);

        // Neue Datei muss existieren
        assertThat(outputFile).exists();
        assertThat(Files.readString(outputFile.toPath())).doesNotContain("OLD CONTENT");

        // Backup muss existieren mit altem Inhalt
        File backupFile = new File(tempDir.toFile(), "TestCrefosExtended.properties.old");
        assertThat(backupFile).exists();
        assertThat(Files.readString(backupFile.toPath())).isEqualTo("OLD CONTENT");
    }

    // ── Helpers ──────────────────────────────────────────────

    private AB30XMLProperties propertiesFor(Long crefoNr, String... customerKeys) {
        AB30XMLProperties props = new AB30XMLProperties(crefoNr);
        for (String key : customerKeys) {
            props.usedByCustomersList.add(key);
        }
        return props;
    }
}
