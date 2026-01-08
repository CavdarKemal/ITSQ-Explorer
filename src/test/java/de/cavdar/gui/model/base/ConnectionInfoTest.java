package de.cavdar.gui.model.base;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConnectionInfo class.
 *
 * @author TemplateGUI
 */
@DisplayName("ConnectionInfo Tests")
class ConnectionInfoTest {

    private static final String TEST_NAME = "TestDB";
    private static final String TEST_DRIVER = "org.postgresql.Driver";
    private static final String TEST_URL = "jdbc:postgresql://localhost:5432/testdb";
    private static final String TEST_USER = "admin";
    private static final String TEST_PASSWORD = "secret123";

    // ===== Constructor and Getters Tests =====

    @Nested
    @DisplayName("Constructor and Getters")
    class ConstructorAndGettersTests {

        @Test
        @DisplayName("should create ConnectionInfo with all parameters")
        void shouldCreateWithAllParameters() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertEquals(TEST_NAME, info.getName());
            assertEquals(TEST_DRIVER, info.getDriver());
            assertEquals(TEST_URL, info.getUrl());
            assertEquals(TEST_USER, info.getUsername());
            assertEquals(TEST_PASSWORD, info.getPassword());
        }

        @Test
        @DisplayName("should allow null values in constructor")
        void shouldAllowNullValues() {
            ConnectionInfo info = new ConnectionInfo(null, null, null, null, null);

            assertNull(info.getName());
            assertNull(info.getDriver());
            assertNull(info.getUrl());
            assertNull(info.getUsername());
            assertNull(info.getPassword());
        }
    }

    // ===== Setters Tests =====

    @Nested
    @DisplayName("Setters")
    class SettersTests {

        private ConnectionInfo info;

        @BeforeEach
        void setUp() {
            info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);
        }

        @Test
        @DisplayName("setName should update name")
        void setNameShouldUpdateName() {
            info.setName("NewName");
            assertEquals("NewName", info.getName());
        }

        @Test
        @DisplayName("setDriver should update driver")
        void setDriverShouldUpdateDriver() {
            info.setDriver("com.mysql.jdbc.Driver");
            assertEquals("com.mysql.jdbc.Driver", info.getDriver());
        }

        @Test
        @DisplayName("setUrl should update URL")
        void setUrlShouldUpdateUrl() {
            info.setUrl("jdbc:mysql://localhost/db");
            assertEquals("jdbc:mysql://localhost/db", info.getUrl());
        }

        @Test
        @DisplayName("setUsername should update username")
        void setUsernameShouldUpdateUsername() {
            info.setUsername("newuser");
            assertEquals("newuser", info.getUsername());
        }

        @Test
        @DisplayName("setPassword should update password")
        void setPasswordShouldUpdatePassword() {
            info.setPassword("newpassword");
            assertEquals("newpassword", info.getPassword());
        }
    }

    // ===== Serialization Tests =====

    @Nested
    @DisplayName("serialize()")
    class SerializeTests {

        @Test
        @DisplayName("should serialize to pipe-separated format with Base64 password")
        void shouldSerializeToPipeSeparatedFormat() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            String serialized = info.serialize();

            String[] parts = serialized.split("\\|");
            assertEquals(5, parts.length);
            assertEquals(TEST_NAME, parts[0]);
            assertEquals(TEST_DRIVER, parts[1]);
            assertEquals(TEST_URL, parts[2]);
            assertEquals(TEST_USER, parts[3]);

            // Verify password is Base64 encoded
            String decodedPassword = new String(Base64.getDecoder().decode(parts[4]));
            assertEquals(TEST_PASSWORD, decodedPassword);
        }

        @Test
        @DisplayName("should handle empty password")
        void shouldHandleEmptyPassword() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, "");

            String serialized = info.serialize();
            ConnectionInfo deserialized = ConnectionInfo.deserialize(serialized);

            assertNotNull(deserialized);
            assertEquals("", deserialized.getPassword());
        }

        @Test
        @DisplayName("should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String specialPassword = "p@$$w0rd!#%&*()";
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, specialPassword);

            String serialized = info.serialize();
            ConnectionInfo deserialized = ConnectionInfo.deserialize(serialized);

            assertNotNull(deserialized);
            assertEquals(specialPassword, deserialized.getPassword());
        }

        @Test
        @DisplayName("should handle Unicode characters in password")
        void shouldHandleUnicodeInPassword() {
            String unicodePassword = "密码123пароль";
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, unicodePassword);

            String serialized = info.serialize();
            ConnectionInfo deserialized = ConnectionInfo.deserialize(serialized);

            assertNotNull(deserialized);
            assertEquals(unicodePassword, deserialized.getPassword());
        }
    }

    // ===== Deserialization Tests =====

    @Nested
    @DisplayName("deserialize()")
    class DeserializeTests {

        @Test
        @DisplayName("should deserialize valid string")
        void shouldDeserializeValidString() {
            ConnectionInfo original = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);
            String serialized = original.serialize();

            ConnectionInfo deserialized = ConnectionInfo.deserialize(serialized);

            assertNotNull(deserialized);
            assertEquals(TEST_NAME, deserialized.getName());
            assertEquals(TEST_DRIVER, deserialized.getDriver());
            assertEquals(TEST_URL, deserialized.getUrl());
            assertEquals(TEST_USER, deserialized.getUsername());
            assertEquals(TEST_PASSWORD, deserialized.getPassword());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("should return null for null, empty or blank input")
        void shouldReturnNullForNullEmptyOrBlank(String input) {
            ConnectionInfo result = ConnectionInfo.deserialize(input);

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for string with insufficient parts")
        void shouldReturnNullForInsufficientParts() {
            ConnectionInfo result = ConnectionInfo.deserialize("a|b|c|d");

            assertNull(result);
        }

        @Test
        @DisplayName("should return null for invalid Base64 password")
        void shouldReturnNullForInvalidBase64() {
            String invalidData = "name|driver|url|user|not-valid-base64!!!";

            ConnectionInfo result = ConnectionInfo.deserialize(invalidData);

            assertNull(result);
        }

        @Test
        @DisplayName("should handle complex password with special characters")
        void shouldHandleComplexPassword() {
            // Password with special chars that remain valid after Base64 encoding
            String complexPassword = "p@ss!word#123$%";
            ConnectionInfo original = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, complexPassword);
            String serialized = original.serialize();

            ConnectionInfo deserialized = ConnectionInfo.deserialize(serialized);

            assertNotNull(deserialized);
            assertEquals(complexPassword, deserialized.getPassword());
        }
    }

    // ===== toString Tests =====

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should return name")
        void shouldReturnName() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertEquals(TEST_NAME, info.toString());
        }

        @Test
        @DisplayName("should return null when name is null")
        void shouldReturnNullWhenNameIsNull() {
            ConnectionInfo info = new ConnectionInfo(null, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertNull(info.toString());
        }
    }

    // ===== equals and hashCode Tests =====

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal when names match")
        void shouldBeEqualWhenNamesMatch() {
            ConnectionInfo info1 = new ConnectionInfo(TEST_NAME, "driver1", "url1", "user1", "pwd1");
            ConnectionInfo info2 = new ConnectionInfo(TEST_NAME, "driver2", "url2", "user2", "pwd2");

            assertEquals(info1, info2);
            assertEquals(info1.hashCode(), info2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            ConnectionInfo info1 = new ConnectionInfo("Name1", TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);
            ConnectionInfo info2 = new ConnectionInfo("Name2", TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertNotEquals(info1, info2);
        }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertEquals(info, info);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertNotEquals(null, info);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            ConnectionInfo info = new ConnectionInfo(TEST_NAME, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertNotEquals("TestDB", info);
        }

        @Test
        @DisplayName("should handle null names in equals")
        void shouldHandleNullNamesInEquals() {
            ConnectionInfo info1 = new ConnectionInfo(null, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);
            ConnectionInfo info2 = new ConnectionInfo(null, TEST_DRIVER, TEST_URL, TEST_USER, TEST_PASSWORD);

            assertEquals(info1, info2);
        }
    }

    // ===== Round-trip Tests =====

    @Nested
    @DisplayName("Round-trip Serialization")
    class RoundTripTests {

        @Test
        @DisplayName("should preserve all fields through serialize/deserialize")
        void shouldPreserveAllFields() {
            ConnectionInfo original = new ConnectionInfo(
                    "Production DB",
                    "org.postgresql.Driver",
                    "jdbc:postgresql://prod.example.com:5432/maindb?ssl=true",
                    "prod_user",
                    "Super$ecret!Password123"
            );

            String serialized = original.serialize();
            ConnectionInfo restored = ConnectionInfo.deserialize(serialized);

            assertNotNull(restored);
            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getDriver(), restored.getDriver());
            assertEquals(original.getUrl(), restored.getUrl());
            assertEquals(original.getUsername(), restored.getUsername());
            assertEquals(original.getPassword(), restored.getPassword());
        }
    }
}
