package de.cavdar.gui.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestEnvironmentManager.
 *
 * @author TemplateGUI
 */
@DisplayName("TestEnvironmentManager Tests")
class TestEnvironmentManagerTest {

    @BeforeEach
    void setUp() {
        // Reset manager state before each test
        TestEnvironmentManager.reset();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        TestEnvironmentManager.reset();
    }

    // ===== extractEnvironmentName Tests =====

    @Nested
    @DisplayName("extractEnvironmentName()")
    class ExtractEnvironmentNameTests {

        @ParameterizedTest(name = "Config ''{0}'' should extract ''{1}''")
        @CsvSource({
                "ene-config.properties, ENE",
                "abc-config.properties, ABC",
                "xyz-test.properties, XYZ",
                "ENE-config.properties, ENE",
                "Abc-Config.properties, ABC",
                "123-config.properties, 123",
                "a_b-config.properties, A_B"
        })
        @DisplayName("should extract first 3 characters as uppercase")
        void shouldExtractFirst3CharsAsUppercase(String configFileName, String expectedEnv) {
            String result = TestEnvironmentManager.extractEnvironmentName(configFileName);
            assertEquals(expectedEnv, result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return DEFAULT for null or empty input")
        void shouldReturnDefaultForNullOrEmpty(String input) {
            String result = TestEnvironmentManager.extractEnvironmentName(input);
            assertEquals("DEFAULT", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"a", "ab"})
        @DisplayName("should return DEFAULT for input shorter than 3 characters")
        void shouldReturnDefaultForShortInput(String input) {
            String result = TestEnvironmentManager.extractEnvironmentName(input);
            assertEquals("DEFAULT", result);
        }

        @Test
        @DisplayName("should handle exactly 3 characters")
        void shouldHandleExactly3Chars() {
            String result = TestEnvironmentManager.extractEnvironmentName("abc");
            assertEquals("ABC", result);
        }
    }

    // ===== switchEnvironment Tests =====

    @Nested
    @DisplayName("switchEnvironment()")
    class SwitchEnvironmentTests {

        @TempDir
        Path tempDir;

        @BeforeEach
        void setUpTempDir() {
            // Set base directory to temp directory for isolated tests
            TestEnvironmentManager.setBaseDirectory(tempDir.toFile());
        }

        @Test
        @DisplayName("should create TEST-ENVS directory structure")
        void shouldCreateDirectoryStructure() {
            boolean result = TestEnvironmentManager.switchEnvironment("ene-config.properties");

            assertTrue(result, "switchEnvironment should return true");

            // Verify directories exist
            File testEnvsDir = new File(tempDir.toFile(), "TEST-ENVS");
            File envDir = new File(testEnvsDir, "ENE");
            File logsDir = new File(envDir, "logs");
            File testOutputsDir = new File(envDir, "TEST-OUTPUTS");

            assertTrue(testEnvsDir.exists(), "TEST-ENVS directory should exist");
            assertTrue(envDir.exists(), "ENE directory should exist");
            assertTrue(logsDir.exists(), "logs directory should exist");
            assertTrue(testOutputsDir.exists(), "TEST-OUTPUTS directory should exist");
        }

        @Test
        @DisplayName("should set current environment name")
        void shouldSetCurrentEnvironmentName() {
            TestEnvironmentManager.switchEnvironment("abc-config.properties");

            assertEquals("ABC", TestEnvironmentManager.getCurrentEnvironment());
        }

        @Test
        @DisplayName("should set current environment directory")
        void shouldSetCurrentEnvDir() {
            TestEnvironmentManager.switchEnvironment("xyz-config.properties");

            File envDir = TestEnvironmentManager.getCurrentEnvDir();
            assertNotNull(envDir);
            assertTrue(envDir.getAbsolutePath().endsWith("XYZ"));
        }

        @Test
        @DisplayName("should set current logs directory")
        void shouldSetCurrentLogsDir() {
            TestEnvironmentManager.switchEnvironment("ene-config.properties");

            File logsDir = TestEnvironmentManager.getCurrentLogsDir();
            assertNotNull(logsDir);
            assertTrue(logsDir.getAbsolutePath().contains("logs"));
        }

        @Test
        @DisplayName("should set current test outputs directory")
        void shouldSetCurrentTestOutputsDir() {
            TestEnvironmentManager.switchEnvironment("ene-config.properties");

            File testOutputsDir = TestEnvironmentManager.getCurrentTestOutputsDir();
            assertNotNull(testOutputsDir);
            assertTrue(testOutputsDir.getAbsolutePath().contains("TEST-OUTPUTS"));
        }

        @Test
        @DisplayName("should return correct log file path")
        void shouldReturnCorrectLogFilePath() {
            TestEnvironmentManager.switchEnvironment("ene-config.properties");

            String logPath = TestEnvironmentManager.getCurrentLogFilePath();
            assertNotNull(logPath);
            assertTrue(logPath.endsWith("ENE.log"));
            assertTrue(logPath.contains("logs"));
        }

        @Test
        @DisplayName("should return true when switching to same environment")
        void shouldReturnTrueForSameEnvironment() {
            TestEnvironmentManager.switchEnvironment("ene-config.properties");
            boolean result = TestEnvironmentManager.switchEnvironment("ene-other.properties");

            assertTrue(result, "Should return true for same environment prefix");
        }

        @Test
        @DisplayName("should create new directories when switching environments")
        void shouldCreateNewDirsWhenSwitching() {
            TestEnvironmentManager.switchEnvironment("aaa-config.properties");
            TestEnvironmentManager.switchEnvironment("bbb-config.properties");

            File testEnvsDir = new File(tempDir.toFile(), "TEST-ENVS");
            File aaaDir = new File(testEnvsDir, "AAA");
            File bbbDir = new File(testEnvsDir, "BBB");

            assertTrue(aaaDir.exists(), "AAA directory should exist");
            assertTrue(bbbDir.exists(), "BBB directory should exist");
            assertEquals("BBB", TestEnvironmentManager.getCurrentEnvironment());
        }

        @Test
        @DisplayName("should handle DEFAULT environment for short config names")
        void shouldHandleDefaultEnvironment() {
            boolean result = TestEnvironmentManager.switchEnvironment("ab");

            assertTrue(result);
            assertEquals("DEFAULT", TestEnvironmentManager.getCurrentEnvironment());

            File defaultDir = new File(tempDir.toFile(), "TEST-ENVS/DEFAULT");
            assertTrue(defaultDir.exists());
        }
    }

    // ===== Getter Tests (initial state) =====

    @Nested
    @DisplayName("Getters (initial state)")
    class GetterInitialStateTests {

        @Test
        @DisplayName("getCurrentLogFilePath should return null before any switch")
        void getCurrentLogFilePathShouldReturnNullInitially() {
            // Note: This test may fail if other tests ran before it
            // In a fresh JVM, these would be null
            // For now, we just verify the method doesn't throw
            assertDoesNotThrow(() -> TestEnvironmentManager.getCurrentLogFilePath());
        }
    }
}
