package io.nextskip.admin.internal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.nextskip.test.TestConstants.ADMIN_EMAIL;
import static io.nextskip.test.TestConstants.NON_ADMIN_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AdminEmailAllowlist}.
 */
@DisplayName("AdminEmailAllowlist")
class AdminEmailAllowlistTest {

    @Nested
    @DisplayName("isAllowed")
    class IsAllowed {

        @Test
        @DisplayName("returns true for email in allowlist")
        void testIsAllowed_EmailInAllowlist_ReturnsTrue() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(ADMIN_EMAIL);

            assertTrue(allowlist.isAllowed(ADMIN_EMAIL));
        }

        @Test
        @DisplayName("returns true for email with different case")
        void testIsAllowed_EmailDifferentCase_ReturnsTrue() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(ADMIN_EMAIL);

            assertTrue(allowlist.isAllowed(ADMIN_EMAIL.toUpperCase(java.util.Locale.ROOT)));
            assertTrue(allowlist.isAllowed("Admin@Example.Com"));
        }

        @Test
        @DisplayName("returns false for email not in allowlist")
        void testIsAllowed_EmailNotInAllowlist_ReturnsFalse() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(ADMIN_EMAIL);

            assertFalse(allowlist.isAllowed("other@example.com"));
        }

        @Test
        @DisplayName("returns false for null email")
        void testIsAllowed_NullEmail_ReturnsFalse() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(ADMIN_EMAIL);

            assertFalse(allowlist.isAllowed(null));
        }

        @Test
        @DisplayName("returns false for blank email")
        void testIsAllowed_BlankEmail_ReturnsFalse() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(ADMIN_EMAIL);

            assertFalse(allowlist.isAllowed(""));
            assertFalse(allowlist.isAllowed("   "));
        }
    }

    @Nested
    @DisplayName("configuration parsing")
    class ConfigurationParsing {

        @Test
        @DisplayName("handles multiple emails separated by comma")
        void testParsing_MultipleEmails_AllRecognized() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(
                    ADMIN_EMAIL + "," + NON_ADMIN_EMAIL + ",other@test.com");

            assertTrue(allowlist.isAllowed(ADMIN_EMAIL));
            assertTrue(allowlist.isAllowed(NON_ADMIN_EMAIL));
            assertTrue(allowlist.isAllowed("other@test.com"));
            assertEquals(3, allowlist.size());
        }

        @Test
        @DisplayName("trims whitespace around emails")
        void testParsing_WhitespaceAroundEmails_Trimmed() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(
                    "  " + ADMIN_EMAIL + "  ,  " + NON_ADMIN_EMAIL + "  ");

            assertTrue(allowlist.isAllowed(ADMIN_EMAIL));
            assertTrue(allowlist.isAllowed(NON_ADMIN_EMAIL));
            assertEquals(2, allowlist.size());
        }

        @Test
        @DisplayName("handles empty configuration")
        void testParsing_EmptyConfig_EmptyAllowlist() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist("");

            assertEquals(0, allowlist.size());
            assertFalse(allowlist.isAllowed("any@example.com"));
        }

        @Test
        @DisplayName("handles null configuration")
        void testParsing_NullConfig_EmptyAllowlist() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(null);

            assertEquals(0, allowlist.size());
        }

        @Test
        @DisplayName("handles blank configuration")
        void testParsing_BlankConfig_EmptyAllowlist() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist("   ");

            assertEquals(0, allowlist.size());
        }

        @Test
        @DisplayName("ignores empty entries from extra commas")
        void testParsing_ExtraCommas_EmptyEntriesIgnored() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(
                    ADMIN_EMAIL + ",,,," + NON_ADMIN_EMAIL + ",");

            assertEquals(2, allowlist.size());
            assertTrue(allowlist.isAllowed(ADMIN_EMAIL));
            assertTrue(allowlist.isAllowed(NON_ADMIN_EMAIL));
        }

        @Test
        @DisplayName("deduplicates identical emails")
        void testParsing_DuplicateEmails_Deduplicated() {
            AdminEmailAllowlist allowlist = new AdminEmailAllowlist(
                    ADMIN_EMAIL + "," + ADMIN_EMAIL + ","
                            + ADMIN_EMAIL.toUpperCase(java.util.Locale.ROOT));

            assertEquals(1, allowlist.size());
        }
    }
}
