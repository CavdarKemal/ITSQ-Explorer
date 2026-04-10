package de.cavdar.gui.view.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-Tests für reine Helper-Methoden in DatabaseView, die ohne Swing-Setup
 * laufen können (insb. Credential-Masking für sicheres Logging).
 */
class DatabaseViewTest {

    @Test
    @DisplayName("URL ohne Credentials bleibt unverändert")
    void maskCredentialsInUrl_plainUrl() {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        assertThat(DatabaseView.maskCredentialsInUrl(url)).isEqualTo(url);
    }

    @Test
    @DisplayName("URL mit user:password@host wird maskiert")
    void maskCredentialsInUrl_userPasswordInline() {
        String url = "jdbc:postgresql://admin:secret123@localhost:5432/db";
        String masked = DatabaseView.maskCredentialsInUrl(url);
        assertThat(masked).doesNotContain("secret123");
        assertThat(masked).doesNotContain("admin:");
        assertThat(masked).contains("***:***@");
        assertThat(masked).contains("localhost:5432/db");
    }

    @Test
    @DisplayName("URL mit ?password=... wird maskiert")
    void maskCredentialsInUrl_queryParam() {
        String url = "jdbc:postgresql://localhost/db?user=alice&password=topsecret";
        String masked = DatabaseView.maskCredentialsInUrl(url);
        assertThat(masked).doesNotContain("topsecret");
        assertThat(masked).doesNotContain("alice");
        assertThat(masked).contains("password=***");
        assertThat(masked).contains("user=***");
    }

    @Test
    @DisplayName("Case-insensitives Password-Param wird maskiert")
    void maskCredentialsInUrl_caseInsensitive() {
        String url = "jdbc:mysql://host/db?PASSWORD=geheim";
        String masked = DatabaseView.maskCredentialsInUrl(url);
        assertThat(masked).doesNotContain("geheim");
        // Casing wird nicht erhalten — der Replacement schreibt 'password=***'
        assertThat(masked.toLowerCase()).contains("password=***");
    }

    @Test
    @DisplayName("Null und leerer String werden unverändert zurückgegeben")
    void maskCredentialsInUrl_nullAndEmpty() {
        assertThat(DatabaseView.maskCredentialsInUrl(null)).isNull();
        assertThat(DatabaseView.maskCredentialsInUrl("")).isEqualTo("");
    }
}
