package io.nextskip.admin.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AdminProperties.
 */
class AdminPropertiesTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String USER_EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @Test
    void testConstructor_NullAllowedEmails_CreatesEmptyList() {
        AdminProperties props = new AdminProperties(null);

        assertThat(props.allowedEmails()).isEmpty();
    }

    @Test
    void testConstructor_WithAllowedEmails_CopiesList() {
        List<String> emails = List.of(ADMIN_EMAIL, USER_EMAIL);

        AdminProperties props = new AdminProperties(emails);

        assertThat(props.allowedEmails()).containsExactly(ADMIN_EMAIL, USER_EMAIL);
    }

    @Test
    void testIsEmailAllowed_NullEmail_ReturnsFalse() {
        AdminProperties props = new AdminProperties(List.of(ADMIN_EMAIL));

        assertThat(props.isEmailAllowed(null)).isFalse();
    }

    @Test
    void testIsEmailAllowed_EmptyAllowedList_ReturnsFalse() {
        AdminProperties props = new AdminProperties(List.of());

        assertThat(props.isEmailAllowed(ADMIN_EMAIL)).isFalse();
    }

    @Test
    void testIsEmailAllowed_EmailInList_ReturnsTrue() {
        AdminProperties props = new AdminProperties(List.of(ADMIN_EMAIL, USER_EMAIL));

        assertThat(props.isEmailAllowed(ADMIN_EMAIL)).isTrue();
    }

    @Test
    void testIsEmailAllowed_EmailNotInList_ReturnsFalse() {
        AdminProperties props = new AdminProperties(List.of(ADMIN_EMAIL));

        assertThat(props.isEmailAllowed(OTHER_EMAIL)).isFalse();
    }

    @Test
    void testIsEmailAllowed_CaseInsensitive() {
        AdminProperties props = new AdminProperties(List.of("Admin@Example.COM"));

        assertThat(props.isEmailAllowed(ADMIN_EMAIL)).isTrue();
        assertThat(props.isEmailAllowed("ADMIN@EXAMPLE.COM")).isTrue();
    }

    @Test
    void testIsEmailAllowed_MiddleOfList() {
        AdminProperties props = new AdminProperties(
                List.of("first@example.com", "middle@example.com", "last@example.com"));

        assertThat(props.isEmailAllowed("middle@example.com")).isTrue();
    }
}
