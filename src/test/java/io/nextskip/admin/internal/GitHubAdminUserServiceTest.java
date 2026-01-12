package io.nextskip.admin.internal;

import io.nextskip.admin.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GitHubAdminUserService}.
 */
class GitHubAdminUserServiceTest {

    private static final String ALLOWED_EMAIL = "admin@example.com";
    private static final String UNAUTHORIZED_EMAIL = "user@example.com";
    private static final String TEST_LOGIN = "testuser";

    private GitHubAdminUserService service;

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties(List.of(ALLOWED_EMAIL));
        service = new GitHubAdminUserService(adminProperties);
    }

    @Test
    void testProcessAndAuthorizeUser_AllowedEmail_ReturnsUserWithAdminRole() {
        // Given
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, ALLOWED_EMAIL);

        // When
        OAuth2User result = service.processAndAuthorizeUser(mockUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
        assertThat((Object) result.getAttribute("login")).isEqualTo(TEST_LOGIN);
        assertThat((Object) result.getAttribute("email")).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testProcessAndAuthorizeUser_UnauthorizedEmail_ThrowsException() {
        // Given
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, UNAUTHORIZED_EMAIL);

        // When/Then
        assertThatThrownBy(() -> service.processAndAuthorizeUser(mockUser))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void testProcessAndAuthorizeUser_NullEmail_ThrowsException() {
        // Given
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, null);

        // When/Then
        assertThatThrownBy(() -> service.processAndAuthorizeUser(mockUser))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("No email found");
    }

    @Test
    void testProcessAndAuthorizeUser_CaseInsensitiveEmailMatch_ReturnsUserWithAdminRole() {
        // Given - email with different case than allowlist
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, ALLOWED_EMAIL.toUpperCase(Locale.ROOT));

        // When
        OAuth2User result = service.processAndAuthorizeUser(mockUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
    }

    @Test
    void testProcessAndAuthorizeUser_PreservesOriginalAuthorities() {
        // Given
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, ALLOWED_EMAIL);

        // When
        OAuth2User result = service.processAndAuthorizeUser(mockUser);

        // Then - should have both original OAUTH2_USER and new ROLE_ADMIN
        assertThat(result.getAuthorities())
                .extracting("authority")
                .contains("OAUTH2_USER", "ROLE_ADMIN");
    }

    private OAuth2User createMockOAuth2User(String login, String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", login);
        attributes.put("id", 12345);
        if (email != null) {
            attributes.put("email", email);
        }

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                attributes,
                "login"
        );
    }
}
