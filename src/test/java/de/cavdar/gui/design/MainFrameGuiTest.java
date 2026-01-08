package de.cavdar.gui.design;

import de.cavdar.gui.view.itsq.ItsqExplorerView;
import de.cavdar.gui.design.base.MainFrame;
import de.cavdar.gui.util.TestEnvironmentManager;
import de.cavdar.gui.view.json.ItsqTreeView;
import de.cavdar.gui.view.prozess.ProzessView;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GUI tests for MainFrame using AssertJ Swing.
 * Tests basic frame functionality and environment setup.
 *
 * @author TemplateGUI
 */
@DisplayName("MainFrame GUI Tests")
class MainFrameGuiTest {

    private FrameFixture window;
    private Robot robot;
    private File tempDir;

    @BeforeEach
    void setUp() {
        // Create temp directory for test environments
        tempDir = new File(System.getProperty("java.io.tmpdir"), "gui-test-" + System.currentTimeMillis());
        tempDir.mkdirs();
        TestEnvironmentManager.setBaseDirectory(tempDir);

        // Create robot
        robot = BasicRobot.robotWithCurrentAwtHierarchy();

        // Create MainFrame on EDT and register views
        MainFrame frame = GuiActionRunner.execute(() -> {
            MainFrame f = new MainFrame();
            f.registerView(ProzessView::new);
            f.registerView(ItsqTreeView::new);
            f.registerView(ItsqExplorerView::new);
            return f;
        });
        window = new FrameFixture(robot, frame);
        window.show();
    }

    @AfterEach
    void tearDown() {
        // Close window first
        if (window != null) {
            window.cleanUp();
        }

        // Clean up TestEnvironmentManager (closes file handles)
        TestEnvironmentManager.reset();

        // Delete temp directory
        if (tempDir != null && tempDir.exists()) {
            deleteRecursively(tempDir);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    // ===== Basic Frame Tests =====

    @Test
    @DisplayName("MainFrame should be visible")
    void shouldShowMainFrame() {
        window.requireVisible();
        assertThat(window.target().getTitle()).contains("MDI Application");
    }

    @Test
    @DisplayName("Config ComboBox should exist")
    void shouldHaveConfigComboBox() {
        JComboBoxFixture configCombo = window.comboBox(new GenericTypeMatcher<JComboBox>(JComboBox.class) {
            @Override
            protected boolean isMatching(JComboBox component) {
                return "ConfigSelector".equals(component.getName());
            }
        });
        configCombo.requireVisible();
    }

    @Test
    @DisplayName("View toolbar should have all view buttons")
    void shouldHaveViewToolbar() {
        // Find buttons by text matcher
        window.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                return "Prozess".equals(button.getText());
            }
        }).requireVisible();

        window.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                return "JSON-Explorer".equals(button.getText());
            }
        }).requireVisible();

        window.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                return "ITSQ-Test-Set".equals(button.getText());
            }
        }).requireVisible();
    }

    // ===== Environment Tests =====

    @Test
    @DisplayName("Environment directory should be created on startup")
    void shouldCreateEnvironmentDirectoryOnStartup() {
        String currentEnv = TestEnvironmentManager.getCurrentEnvironment();
        assertThat(currentEnv).isNotNull();

        File envDir = TestEnvironmentManager.getCurrentEnvDir();
        assertThat(envDir).isNotNull();
        assertThat(envDir).exists();

        File logsDir = TestEnvironmentManager.getCurrentLogsDir();
        assertThat(logsDir).isNotNull();
        assertThat(logsDir).exists();

        File testOutputsDir = TestEnvironmentManager.getCurrentTestOutputsDir();
        assertThat(testOutputsDir).isNotNull();
        assertThat(testOutputsDir).exists();
    }

    @Test
    @DisplayName("TEST-ENVS directory structure should be correct")
    void shouldHaveCorrectDirectoryStructure() {
        File testEnvsDir = new File(tempDir, "TEST-ENVS");
        assertThat(testEnvsDir).exists();

        String currentEnv = TestEnvironmentManager.getCurrentEnvironment();
        File envDir = new File(testEnvsDir, currentEnv);
        assertThat(envDir).exists();

        File logsDir = new File(envDir, "logs");
        assertThat(logsDir).exists();

        File testOutputsDir = new File(envDir, "TEST-OUTPUTS");
        assertThat(testOutputsDir).exists();
    }

    @Test
    @DisplayName("Log file path should be set correctly")
    void shouldHaveCorrectLogFilePath() {
        String currentEnv = TestEnvironmentManager.getCurrentEnvironment();
        String logFilePath = TestEnvironmentManager.getCurrentLogFilePath();

        assertThat(logFilePath).isNotNull();
        assertThat(logFilePath).endsWith(currentEnv + ".log");
        assertThat(logFilePath).contains("logs");
    }

    @Test
    @DisplayName("Environment name should be extracted from config")
    void shouldExtractEnvironmentFromConfig() {
        String currentEnv = TestEnvironmentManager.getCurrentEnvironment();

        // Should be 3 uppercase characters
        assertThat(currentEnv).isNotNull();
        assertThat(currentEnv).hasSize(3);
        assertThat(currentEnv).isEqualTo(currentEnv.toUpperCase());
    }
}
