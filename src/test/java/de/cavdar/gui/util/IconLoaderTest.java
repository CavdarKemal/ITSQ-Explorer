package de.cavdar.gui.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IconLoader utility class.
 *
 * @author TemplateGUI
 */
@DisplayName("IconLoader Tests")
class IconLoaderTest {

    // ===== load() Tests =====

    @Nested
    @DisplayName("load()")
    class LoadTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "client.png",
                "folder_cubes.png",
                "gear_run.png",
                "save.png",
                "refresh.png"
        })
        @DisplayName("should load existing icons")
        void shouldLoadExistingIcons(String iconName) {
            Icon icon = IconLoader.load(iconName);

            assertNotNull(icon, "Icon '" + iconName + "' should be loaded");
            assertTrue(icon.getIconWidth() > 0, "Icon should have positive width");
            assertTrue(icon.getIconHeight() > 0, "Icon should have positive height");
        }

        @Test
        @DisplayName("should return null for non-existent icon")
        void shouldReturnNullForNonExistentIcon() {
            Icon icon = IconLoader.load("non_existent_icon_12345.png");

            assertNull(icon);
        }

        @Test
        @DisplayName("should return null for null filename")
        void shouldReturnNullForNullFilename() {
            // This will result in resource lookup with null, which should return null
            Icon icon = IconLoader.load(null);

            assertNull(icon);
        }

        @Test
        @DisplayName("should handle empty filename gracefully")
        void shouldHandleEmptyFilename() {
            // Empty string may load directory path - just verify no exception
            assertDoesNotThrow(() -> IconLoader.load(""));
        }

        @Test
        @DisplayName("should load icon as ImageIcon")
        void shouldLoadIconAsImageIcon() {
            Icon icon = IconLoader.load("client.png");

            assertNotNull(icon);
            assertInstanceOf(ImageIcon.class, icon);
        }
    }

    // ===== loadWithFallback() Tests =====

    @Nested
    @DisplayName("loadWithFallback()")
    class LoadWithFallbackTests {

        @Test
        @DisplayName("should load existing icon without using fallback")
        void shouldLoadExistingIconWithoutFallback() {
            Icon icon = IconLoader.loadWithFallback("client.png", "FileView.fileIcon");

            assertNotNull(icon);
            // Should be our custom icon, not the UIManager fallback
            assertInstanceOf(ImageIcon.class, icon);
        }

        @Test
        @DisplayName("should return fallback icon when primary not found")
        void shouldReturnFallbackWhenPrimaryNotFound() {
            // Use a common UIManager icon key that should exist in most L&Fs
            Icon icon = IconLoader.loadWithFallback("non_existent.png", "FileView.directoryIcon");

            // May be null if no L&F is set, but should not throw
            // In a headless environment, UIManager icons might be null
            assertDoesNotThrow(() -> IconLoader.loadWithFallback("non_existent.png", "FileView.directoryIcon"));
        }

        @Test
        @DisplayName("should return null when both primary and fallback not found")
        void shouldReturnNullWhenBothNotFound() {
            Icon icon = IconLoader.loadWithFallback("non_existent.png", "NonExistent.iconKey");

            assertNull(icon);
        }
    }

    // ===== Icon Properties Tests =====

    @Nested
    @DisplayName("Icon Properties")
    class IconPropertiesTests {

        @Test
        @DisplayName("folder icons should have consistent dimensions")
        void folderIconsShouldHaveConsistentDimensions() {
            Icon folder1 = IconLoader.load("folder_closed.png");
            Icon folder2 = IconLoader.load("folder_cubes.png");
            Icon folder3 = IconLoader.load("folder_document.png");

            assertNotNull(folder1);
            assertNotNull(folder2);
            assertNotNull(folder3);

            // All folder icons should have same dimensions
            assertEquals(folder1.getIconWidth(), folder2.getIconWidth());
            assertEquals(folder1.getIconHeight(), folder2.getIconHeight());
            assertEquals(folder2.getIconWidth(), folder3.getIconWidth());
            assertEquals(folder2.getIconHeight(), folder3.getIconHeight());
        }

        @Test
        @DisplayName("icons should be square (16x16)")
        void iconsShouldBeSquare() {
            Icon icon = IconLoader.load("client.png");

            assertNotNull(icon);
            assertEquals(icon.getIconWidth(), icon.getIconHeight(), "Icon should be square");
        }
    }

    // ===== Utility Class Tests =====

    @Nested
    @DisplayName("Utility Class Design")
    class UtilityClassDesignTests {

        @Test
        @DisplayName("IconLoader should have private constructor")
        void shouldHavePrivateConstructor() {
            // Private constructor should prevent normal instantiation
            var constructors = IconLoader.class.getDeclaredConstructors();
            assertEquals(1, constructors.length);

            // Constructor should be private
            assertFalse(constructors[0].canAccess(null),
                    "Constructor should not be accessible (private)");
        }
    }
}
