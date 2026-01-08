package de.cavdar.gui.exception;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 *
 * @author TemplateGUI
 */
@DisplayName("Exception Classes Tests")
class ExceptionTest {

    // ===== ConfigurationException Tests =====

    @Nested
    @DisplayName("ConfigurationException")
    class ConfigurationExceptionTests {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateWithMessage() {
            String message = "Configuration file not found";

            ConfigurationException ex = new ConfigurationException(message);

            assertEquals(message, ex.getMessage());
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            String message = "Failed to load configuration";
            Throwable cause = new RuntimeException("File read error");

            ConfigurationException ex = new ConfigurationException(message, cause);

            assertEquals(message, ex.getMessage());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            ConfigurationException ex = new ConfigurationException("test");

            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("should be throwable without declaration")
        void shouldBeThrowableWithoutDeclaration() {
            assertThrows(ConfigurationException.class, () -> {
                throw new ConfigurationException("Test exception");
            });
        }

        @Test
        @DisplayName("should preserve exception chain")
        void shouldPreserveExceptionChain() {
            Exception rootCause = new IllegalArgumentException("Invalid value");
            Exception intermediateCause = new RuntimeException("Processing failed", rootCause);
            ConfigurationException ex = new ConfigurationException("Config error", intermediateCause);

            assertEquals(intermediateCause, ex.getCause());
            assertEquals(rootCause, ex.getCause().getCause());
        }
    }

    // ===== ViewException Tests =====

    @Nested
    @DisplayName("ViewException")
    class ViewExceptionTests {

        @Test
        @DisplayName("should create exception with message")
        void shouldCreateWithMessage() {
            String message = "View initialization failed";

            ViewException ex = new ViewException(message);

            assertEquals(message, ex.getMessage());
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            String message = "Failed to render view";
            Throwable cause = new NullPointerException("Component is null");

            ViewException ex = new ViewException(message, cause);

            assertEquals(message, ex.getMessage());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("should extend RuntimeException")
        void shouldExtendRuntimeException() {
            ViewException ex = new ViewException("test");

            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("should be throwable without declaration")
        void shouldBeThrowableWithoutDeclaration() {
            assertThrows(ViewException.class, () -> {
                throw new ViewException("Test exception");
            });
        }

        @Test
        @DisplayName("should handle null message")
        void shouldHandleNullMessage() {
            ViewException ex = new ViewException(null);

            assertNull(ex.getMessage());
        }

        @Test
        @DisplayName("should handle null cause")
        void shouldHandleNullCause() {
            ViewException ex = new ViewException("message", null);

            assertEquals("message", ex.getMessage());
            assertNull(ex.getCause());
        }
    }

    // ===== Catch and Rethrow Pattern Tests =====

    @Nested
    @DisplayName("Exception Usage Patterns")
    class UsagePatternsTests {

        @Test
        @DisplayName("ConfigurationException can wrap IOException")
        void configurationExceptionCanWrapIOException() {
            Exception ioException = new java.io.IOException("File not readable");

            ConfigurationException wrapped = new ConfigurationException("Config load failed", ioException);

            assertInstanceOf(java.io.IOException.class, wrapped.getCause());
            assertTrue(wrapped.getMessage().contains("Config load failed"));
        }

        @Test
        @DisplayName("ViewException can wrap SwingWorker exceptions")
        void viewExceptionCanWrapSwingExceptions() {
            Exception swingException = new IllegalStateException("Not on EDT");

            ViewException wrapped = new ViewException("View update failed", swingException);

            assertInstanceOf(IllegalStateException.class, wrapped.getCause());
        }
    }
}
