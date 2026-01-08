package de.cavdar.gui.model.base;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigEntry record.
 *
 * @author TemplateGUI
 */
@DisplayName("ConfigEntry Tests")
class ConfigEntryTest {

    // ===== Constructor Tests =====

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create entry with key and value")
        void shouldCreateEntryWithKeyAndValue() {
            ConfigEntry entry = new ConfigEntry("myKey", "myValue");

            assertEquals("myKey", entry.key());
            assertEquals("myValue", entry.value());
        }

        @Test
        @DisplayName("should throw NullPointerException for null key")
        void shouldThrowForNullKey() {
            assertThrows(NullPointerException.class, () -> new ConfigEntry(null, "value"));
        }

        @Test
        @DisplayName("should convert null value to empty string")
        void shouldConvertNullValueToEmptyString() {
            ConfigEntry entry = new ConfigEntry("key", null);

            assertEquals("", entry.value());
        }

        @Test
        @DisplayName("should preserve empty string value")
        void shouldPreserveEmptyStringValue() {
            ConfigEntry entry = new ConfigEntry("key", "");

            assertEquals("", entry.value());
        }
    }

    // ===== cleanValue Tests =====

    @Nested
    @DisplayName("cleanValue()")
    class CleanValueTests {

        @Test
        @DisplayName("should return value without comments")
        void shouldReturnValueWithoutComments() {
            ConfigEntry entry = new ConfigEntry("key", "value # this is a comment");

            assertEquals("value", entry.cleanValue());
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            ConfigEntry entry = new ConfigEntry("key", "  value  ");

            assertEquals("value", entry.cleanValue());
        }

        @Test
        @DisplayName("should handle value with multiple hash symbols")
        void shouldHandleMultipleHashSymbols() {
            ConfigEntry entry = new ConfigEntry("key", "value#comment1#comment2");

            assertEquals("value", entry.cleanValue());
        }

        @Test
        @DisplayName("should handle value without comments")
        void shouldHandleValueWithoutComments() {
            ConfigEntry entry = new ConfigEntry("key", "simple value");

            assertEquals("simple value", entry.cleanValue());
        }
    }

    // ===== asArray Tests =====

    @Nested
    @DisplayName("asArray()")
    class AsArrayTests {

        @Test
        @DisplayName("should split semicolon-separated values")
        void shouldSplitSemicolonSeparatedValues() {
            ConfigEntry entry = new ConfigEntry("key", "A;B;C");

            String[] result = entry.asArray();

            assertArrayEquals(new String[]{"A", "B", "C"}, result);
        }

        @Test
        @DisplayName("should return empty array for empty value")
        void shouldReturnEmptyArrayForEmptyValue() {
            ConfigEntry entry = new ConfigEntry("key", "");

            String[] result = entry.asArray();

            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("should return single element array for value without semicolons")
        void shouldReturnSingleElementArray() {
            ConfigEntry entry = new ConfigEntry("key", "single");

            String[] result = entry.asArray();

            assertArrayEquals(new String[]{"single"}, result);
        }

        @Test
        @DisplayName("should ignore comments when splitting")
        void shouldIgnoreCommentsWhenSplitting() {
            ConfigEntry entry = new ConfigEntry("key", "A;B;C # comment");

            String[] result = entry.asArray();

            assertArrayEquals(new String[]{"A", "B", "C"}, result);
        }
    }

    // ===== asBoolean Tests =====

    @Nested
    @DisplayName("asBoolean()")
    class AsBooleanTests {

        @ParameterizedTest
        @ValueSource(strings = {"true", "TRUE", "True", "TrUe"})
        @DisplayName("should return true for 'true' variations")
        void shouldReturnTrueForTrueVariations(String value) {
            ConfigEntry entry = new ConfigEntry("key", value);

            assertTrue(entry.asBoolean());
        }

        @ParameterizedTest
        @ValueSource(strings = {"false", "FALSE", "False", "yes", "no", "1", "0", "abc", ""})
        @DisplayName("should return false for non-true values")
        void shouldReturnFalseForNonTrueValues(String value) {
            ConfigEntry entry = new ConfigEntry("key", value);

            assertFalse(entry.asBoolean());
        }

        @Test
        @DisplayName("should ignore comments when parsing boolean")
        void shouldIgnoreCommentsWhenParsingBoolean() {
            ConfigEntry entry = new ConfigEntry("key", "true # enabled");

            assertTrue(entry.asBoolean());
        }
    }

    // ===== asInt Tests =====

    @Nested
    @DisplayName("asInt()")
    class AsIntTests {

        @ParameterizedTest(name = "value ''{0}'' should parse to {1}")
        @CsvSource({
                "42, 42",
                "0, 0",
                "-1, -1",
                "1000, 1000",
                "2147483647, 2147483647"
        })
        @DisplayName("should parse valid integers")
        void shouldParseValidIntegers(String value, int expected) {
            ConfigEntry entry = new ConfigEntry("key", value);

            assertEquals(expected, entry.asInt());
        }

        @Test
        @DisplayName("should throw NumberFormatException for invalid integer")
        void shouldThrowForInvalidInteger() {
            ConfigEntry entry = new ConfigEntry("key", "not-a-number");

            assertThrows(NumberFormatException.class, entry::asInt);
        }

        @Test
        @DisplayName("should return default value for invalid integer")
        void shouldReturnDefaultForInvalidInteger() {
            ConfigEntry entry = new ConfigEntry("key", "invalid");

            assertEquals(99, entry.asInt(99));
        }

        @Test
        @DisplayName("should parse integer ignoring comments")
        void shouldParseIntegerIgnoringComments() {
            ConfigEntry entry = new ConfigEntry("key", "123 # port number");

            assertEquals(123, entry.asInt());
        }

        @Test
        @DisplayName("should return default for empty value")
        void shouldReturnDefaultForEmptyValue() {
            ConfigEntry entry = new ConfigEntry("key", "");

            assertEquals(50, entry.asInt(50));
        }
    }

    // ===== asDouble Tests =====

    @Nested
    @DisplayName("asDouble()")
    class AsDoubleTests {

        @ParameterizedTest(name = "value ''{0}'' should parse to {1}")
        @CsvSource({
                "3.14, 3.14",
                "0.0, 0.0",
                "-2.5, -2.5",
                "100, 100.0"
        })
        @DisplayName("should parse valid doubles")
        void shouldParseValidDoubles(String value, double expected) {
            ConfigEntry entry = new ConfigEntry("key", value);

            assertEquals(expected, entry.asDouble(), 0.001);
        }

        @Test
        @DisplayName("should throw NumberFormatException for invalid double")
        void shouldThrowForInvalidDouble() {
            ConfigEntry entry = new ConfigEntry("key", "not-a-double");

            assertThrows(NumberFormatException.class, entry::asDouble);
        }

        @Test
        @DisplayName("should return default value for invalid double")
        void shouldReturnDefaultForInvalidDouble() {
            ConfigEntry entry = new ConfigEntry("key", "invalid");

            assertEquals(1.5, entry.asDouble(1.5), 0.001);
        }
    }

    // ===== isEmpty Tests =====

    @Nested
    @DisplayName("isEmpty()")
    class IsEmptyTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return true for empty or blank values")
        void shouldReturnTrueForEmptyOrBlank(String value) {
            ConfigEntry entry = new ConfigEntry("key", value);

            assertTrue(entry.isEmpty());
        }

        @Test
        @DisplayName("should return false for non-empty value")
        void shouldReturnFalseForNonEmpty() {
            ConfigEntry entry = new ConfigEntry("key", "value");

            assertFalse(entry.isEmpty());
        }

        @Test
        @DisplayName("should return true if only comment exists")
        void shouldReturnTrueIfOnlyComment() {
            ConfigEntry entry = new ConfigEntry("key", "# just a comment");

            assertTrue(entry.isEmpty());
        }
    }

    // ===== toString Tests =====

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should return key=value format")
        void shouldReturnKeyValueFormat() {
            ConfigEntry entry = new ConfigEntry("myKey", "myValue");

            assertEquals("myKey=myValue", entry.toString());
        }
    }
}
