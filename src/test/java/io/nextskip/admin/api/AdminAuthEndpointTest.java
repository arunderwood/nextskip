package io.nextskip.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static io.nextskip.test.TestConstants.ADMIN_AVATAR_URL;
import static io.nextskip.test.TestConstants.ADMIN_EMAIL;
import static io.nextskip.test.TestConstants.ADMIN_NAME;
import static io.nextskip.test.TestConstants.ADMIN_USERNAME;
import static io.nextskip.test.TestConstants.ATTR_AVATAR_URL;
import static io.nextskip.test.TestConstants.ATTR_EMAIL;
import static io.nextskip.test.TestConstants.ATTR_LOGIN;
import static io.nextskip.test.TestConstants.ATTR_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminAuthEndpoint}.
 *
 * <p>Tests the Hilla endpoint layer with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthEndpoint")
class AdminAuthEndpointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private AdminAuthEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new AdminAuthEndpoint(request);
    }

    @AfterEach
    void tearDown() {
        // Clear security context to avoid leaking state between tests
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(Object principal) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUserTests {

        @Test
        @DisplayName("returns user info when authenticated with all attributes")
        void testGetCurrentUser_AllAttributes_ReturnsUserInfo() {
            // Given: OAuth2User has all attributes
            when(oAuth2User.getAttribute(ATTR_EMAIL)).thenReturn(ADMIN_EMAIL);
            when(oAuth2User.getAttribute(ATTR_NAME)).thenReturn(ADMIN_NAME);
            when(oAuth2User.getAttribute(ATTR_LOGIN)).thenReturn(ADMIN_USERNAME);
            when(oAuth2User.getAttribute(ATTR_AVATAR_URL)).thenReturn(ADMIN_AVATAR_URL);
            setUpSecurityContext(oAuth2User);

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNotNull(result);
            assertEquals(ADMIN_EMAIL, result.email());
            assertEquals(ADMIN_NAME, result.name());
            assertEquals(ADMIN_AVATAR_URL, result.avatarUrl());
        }

        @Test
        @DisplayName("uses login as fallback when name is null")
        void testGetCurrentUser_NullName_UsesLoginAsFallback() {
            // Given: OAuth2User has null name
            when(oAuth2User.getAttribute(ATTR_EMAIL)).thenReturn(ADMIN_EMAIL);
            when(oAuth2User.getAttribute(ATTR_NAME)).thenReturn(null);
            when(oAuth2User.getAttribute(ATTR_LOGIN)).thenReturn(ADMIN_USERNAME);
            when(oAuth2User.getAttribute(ATTR_AVATAR_URL)).thenReturn(ADMIN_AVATAR_URL);
            setUpSecurityContext(oAuth2User);

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNotNull(result);
            assertEquals(ADMIN_EMAIL, result.email());
            assertEquals(ADMIN_USERNAME, result.name());
            assertEquals(ADMIN_AVATAR_URL, result.avatarUrl());
        }

        @Test
        @DisplayName("handles null avatar URL")
        void testGetCurrentUser_NullAvatarUrl_ReturnsNullAvatar() {
            // Given: OAuth2User has null avatar_url
            when(oAuth2User.getAttribute(ATTR_EMAIL)).thenReturn(ADMIN_EMAIL);
            when(oAuth2User.getAttribute(ATTR_NAME)).thenReturn(ADMIN_NAME);
            when(oAuth2User.getAttribute(ATTR_LOGIN)).thenReturn(ADMIN_USERNAME);
            when(oAuth2User.getAttribute(ATTR_AVATAR_URL)).thenReturn(null);
            setUpSecurityContext(oAuth2User);

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNotNull(result);
            assertEquals(ADMIN_EMAIL, result.email());
            assertEquals(ADMIN_NAME, result.name());
            assertNull(result.avatarUrl());
        }

        @Test
        @DisplayName("returns null when authentication is null")
        void testGetCurrentUser_NullAuthentication_ReturnsNull() {
            // Given: No authentication in security context
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("returns null when principal is not OAuth2User")
        void testGetCurrentUser_NonOAuth2Principal_ReturnsNull() {
            // Given: Principal is a different type
            setUpSecurityContext("anonymous-user");

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("handles null email gracefully")
        void testGetCurrentUser_NullEmail_ReturnsNullEmail() {
            // Given: OAuth2User has null email
            when(oAuth2User.getAttribute(ATTR_EMAIL)).thenReturn(null);
            when(oAuth2User.getAttribute(ATTR_NAME)).thenReturn(ADMIN_NAME);
            when(oAuth2User.getAttribute(ATTR_LOGIN)).thenReturn(ADMIN_USERNAME);
            when(oAuth2User.getAttribute(ATTR_AVATAR_URL)).thenReturn(ADMIN_AVATAR_URL);
            setUpSecurityContext(oAuth2User);

            // When
            AdminUserInfo result = endpoint.getCurrentUser();

            // Then
            assertNotNull(result);
            assertNull(result.email());
            assertEquals(ADMIN_NAME, result.name());
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("invalidates session on logout")
        void testLogout_InvalidatesSession() {
            // Given
            when(request.getSession()).thenReturn(session);

            // When
            endpoint.logout();

            // Then
            verify(request).getSession();
            verify(session).invalidate();
        }
    }
}
