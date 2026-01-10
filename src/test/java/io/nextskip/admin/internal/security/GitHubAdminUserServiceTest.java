package io.nextskip.admin.internal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.nextskip.test.TestConstants.ADMIN_AVATAR_URL;
import static io.nextskip.test.TestConstants.ADMIN_EMAIL;
import static io.nextskip.test.TestConstants.ADMIN_NAME;
import static io.nextskip.test.TestConstants.ADMIN_USERNAME;
import static io.nextskip.test.TestConstants.ATTR_AVATAR_URL;
import static io.nextskip.test.TestConstants.ATTR_EMAIL;
import static io.nextskip.test.TestConstants.ATTR_LOGIN;
import static io.nextskip.test.TestConstants.ATTR_NAME;
import static io.nextskip.test.TestConstants.NON_ADMIN_EMAIL;
import static io.nextskip.test.TestConstants.ROLE_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GitHubAdminUserService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubAdminUserService")
class GitHubAdminUserServiceTest {

    @Mock
    private AdminEmailAllowlist allowlist;

    private TestableGitHubAdminUserService userService;

    @BeforeEach
    void setUp() {
        userService = new TestableGitHubAdminUserService(allowlist);
    }

    @Nested
    @DisplayName("loadUser")
    class LoadUser {

        @Test
        @DisplayName("grants ADMIN role when email is in allowlist")
        void testLoadUser_EmailInAllowlist_GrantsAdminRole() {
            when(allowlist.isAllowed(ADMIN_EMAIL)).thenReturn(true);

            OAuth2UserRequest request = createUserRequest();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(ATTR_LOGIN, ADMIN_USERNAME);
            attributes.put(ATTR_EMAIL, ADMIN_EMAIL);
            attributes.put(ATTR_NAME, ADMIN_NAME);

            userService.setMockUser(createOAuth2User(attributes));

            OAuth2User result = userService.loadUser(request);

            assertNotNull(result);
            assertTrue(result.getAuthorities().stream()
                    .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority())));
            assertEquals(ADMIN_USERNAME, result.getName());
        }

        @Test
        @DisplayName("throws exception when email is not in allowlist")
        void testLoadUser_EmailNotInAllowlist_ThrowsException() {
            when(allowlist.isAllowed(NON_ADMIN_EMAIL)).thenReturn(false);

            OAuth2UserRequest request = createUserRequest();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(ATTR_LOGIN, "normaluser");
            attributes.put(ATTR_EMAIL, NON_ADMIN_EMAIL);

            userService.setMockUser(createOAuth2User(attributes));

            OAuth2AuthenticationException exception = assertThrows(
                    OAuth2AuthenticationException.class,
                    () -> userService.loadUser(request)
            );

            assertEquals("access_denied", exception.getError().getErrorCode());
        }

        @Test
        @DisplayName("throws exception when email is null")
        void testLoadUser_NullEmail_ThrowsException() {
            OAuth2UserRequest request = createUserRequest();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(ATTR_LOGIN, "noEmailUser");
            // No email attribute

            userService.setMockUser(createOAuth2User(attributes));

            OAuth2AuthenticationException exception = assertThrows(
                    OAuth2AuthenticationException.class,
                    () -> userService.loadUser(request)
            );

            assertEquals("email_required", exception.getError().getErrorCode());
        }

        @Test
        @DisplayName("preserves original user attributes")
        void testLoadUser_ValidUser_PreservesAttributes() {
            when(allowlist.isAllowed(ADMIN_EMAIL)).thenReturn(true);

            OAuth2UserRequest request = createUserRequest();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(ATTR_LOGIN, ADMIN_USERNAME);
            attributes.put(ATTR_EMAIL, ADMIN_EMAIL);
            attributes.put(ATTR_AVATAR_URL, ADMIN_AVATAR_URL);
            attributes.put(ATTR_NAME, ADMIN_NAME);

            userService.setMockUser(createOAuth2User(attributes));

            OAuth2User result = userService.loadUser(request);

            assertEquals(ADMIN_EMAIL, result.getAttribute(ATTR_EMAIL));
            assertEquals(ADMIN_AVATAR_URL, result.getAttribute(ATTR_AVATAR_URL));
            assertEquals(ADMIN_NAME, result.getAttribute(ATTR_NAME));
        }
    }

    private OAuth2UserRequest createUserRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("github")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName(ATTR_LOGIN)
                .clientName("GitHub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private OAuth2User createOAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("OAUTH2_USER")),
                attributes,
                ATTR_LOGIN
        );
    }

    /**
     * Test subclass that allows injecting mock OAuth2User.
     *
     * <p>Overrides the protected fetchOAuth2User method to return a mock
     * instead of calling the actual OAuth2 provider.
     */
    @SuppressWarnings("PMD.TestClassWithoutTestCases") // Helper class for tests, not a test class
    private static class TestableGitHubAdminUserService extends GitHubAdminUserService {

        private OAuth2User mockUser;

        TestableGitHubAdminUserService(AdminEmailAllowlist allowlist) {
            super(allowlist);
        }

        void setMockUser(OAuth2User mockUser) {
            this.mockUser = mockUser;
        }

        @Override
        protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
            return mockUser;
        }
    }
}
