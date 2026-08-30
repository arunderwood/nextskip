package io.nextskip.admin.internal;

import io.nextskip.admin.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GitHubAdminUserService}.
 */
@ExtendWith(MockitoExtension.class)
class GitHubAdminUserServiceTest {

    private static final String ALLOWED_EMAIL = "admin@example.com";
    private static final String UNAUTHORIZED_EMAIL = "user@example.com";
    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_ACCESS_TOKEN = "test-token-12345";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String AUTHORITY_FIELD = "authority";
    private static final long TOKEN_VALIDITY_SECONDS = 3600L;

    // GitHub API response field names
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_PRIMARY = "primary";
    private static final String ATTR_VERIFIED = "verified";

    @Mock
    private RestTemplate mockRestTemplate;

    private GitHubAdminUserService service;
    private GitHubAdminUserService serviceWithMockRestTemplate;

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties(List.of(ALLOWED_EMAIL));
        service = new GitHubAdminUserService(adminProperties);
        serviceWithMockRestTemplate = new GitHubAdminUserService(adminProperties, mockRestTemplate);
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
                .extracting(AUTHORITY_FIELD)
                .contains(ROLE_ADMIN);
        assertThat((Object) result.getAttribute(ATTR_LOGIN)).isEqualTo(TEST_LOGIN);
        assertThat((Object) result.getAttribute(ATTR_EMAIL)).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testLoadUser_PublicEmail_DelegatesToUserInfoAndAuthorizes() {
        // Given - stub the user info response that DefaultOAuth2UserService fetches
        RestOperations userInfoRestOperations = mock(RestOperations.class);
        Map<String, Object> userAttributes = Map.of(ATTR_LOGIN, TEST_LOGIN, ATTR_EMAIL, ALLOWED_EMAIL);
        when(userInfoRestOperations.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(userAttributes, HttpStatus.OK));
        service.setRestOperations(userInfoRestOperations);

        // When
        OAuth2User result = service.loadUser(new OAuth2UserRequest(githubClientRegistration(), testAccessToken()));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting(AUTHORITY_FIELD)
                .contains(ROLE_ADMIN);
        assertThat((Object) result.getAttribute(ATTR_EMAIL)).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testResolveEmailAndAuthorize_NullUser_ThrowsException() {
        // Given - OAuth2UserService.loadUser declares a nullable return type
        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);

        // When/Then
        assertThatThrownBy(() -> service.resolveEmailAndAuthorize(null, userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("No user information returned by GitHub");
    }

    @Test
    void testResolveEmailAndAuthorize_PublicEmail_ReturnsUserWithAdminRole() {
        // Given - email is public, so no call to the GitHub emails API is needed
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, ALLOWED_EMAIL);
        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);

        // When
        OAuth2User result = service.resolveEmailAndAuthorize(mockUser, userRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting(AUTHORITY_FIELD)
                .contains(ROLE_ADMIN);
        assertThat((Object) result.getAttribute(ATTR_EMAIL)).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testResolveEmailAndAuthorize_PrivateEmail_FetchesEmailAndAuthorizes() {
        // Given - email is private, so it must be fetched from the GitHub emails API
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, null);
        OAuth2UserRequest userRequest = mockUserRequestWithToken();
        ResponseEntity<List<Map<String, Object>>> response =
                new ResponseEntity<>(List.of(createEmailEntry(ALLOWED_EMAIL, true, true)), HttpStatus.OK);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        OAuth2User result = serviceWithMockRestTemplate.resolveEmailAndAuthorize(mockUser, userRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities())
                .extracting(AUTHORITY_FIELD)
                .contains(ROLE_ADMIN);
        assertThat((Object) result.getAttribute(ATTR_EMAIL)).isEqualTo(ALLOWED_EMAIL);
        assertThat((Object) result.getAttribute(ATTR_LOGIN)).isEqualTo(TEST_LOGIN);
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
                .extracting(AUTHORITY_FIELD)
                .contains(ROLE_ADMIN);
    }

    @Test
    void testProcessAndAuthorizeUser_PreservesOriginalAuthorities() {
        // Given
        OAuth2User mockUser = createMockOAuth2User(TEST_LOGIN, ALLOWED_EMAIL);

        // When
        OAuth2User result = service.processAndAuthorizeUser(mockUser);

        // Then - should have both original OAUTH2_USER and new ROLE_ADMIN
        assertThat(result.getAuthorities())
                .extracting(AUTHORITY_FIELD)
                .contains("OAUTH2_USER", ROLE_ADMIN);
    }

    @Test
    void testFetchPrimaryEmail_PrimaryEmailFound_ReturnsPrimaryEmail() {
        // Given
        List<Map<String, Object>> emailList = List.of(
                createEmailEntry("secondary@example.com", false, true),
                createEmailEntry(ALLOWED_EMAIL, true, true)
        );
        ResponseEntity<List<Map<String, Object>>> response = new ResponseEntity<>(emailList, HttpStatus.OK);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testFetchPrimaryEmail_NoPrimary_ReturnsFirstVerified() {
        // Given - no primary email, but verified emails exist
        List<Map<String, Object>> emailList = List.of(
                createEmailEntry("unverified@example.com", false, false),
                createEmailEntry(ALLOWED_EMAIL, false, true)
        );
        ResponseEntity<List<Map<String, Object>>> response = new ResponseEntity<>(emailList, HttpStatus.OK);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isEqualTo(ALLOWED_EMAIL);
    }

    @Test
    void testFetchPrimaryEmail_EmptyList_ReturnsNull() {
        // Given
        ResponseEntity<List<Map<String, Object>>> response = new ResponseEntity<>(List.of(), HttpStatus.OK);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testFetchPrimaryEmail_NullResponse_ReturnsNull() {
        // Given
        ResponseEntity<List<Map<String, Object>>> response = ResponseEntity.ok(null);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testFetchPrimaryEmail_ApiError_ReturnsNull() {
        // Given - RestTemplate throws exception
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("API error"));

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void testFetchPrimaryEmail_NoVerifiedEmails_ReturnsNull() {
        // Given - emails exist but none are primary or verified
        List<Map<String, Object>> emailList = List.of(
                createEmailEntry("unverified1@example.com", false, false),
                createEmailEntry("unverified2@example.com", false, false)
        );
        ResponseEntity<List<Map<String, Object>>> response = new ResponseEntity<>(emailList, HttpStatus.OK);
        when(mockRestTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        // When
        String result = serviceWithMockRestTemplate.fetchPrimaryEmail(TEST_ACCESS_TOKEN);

        // Then
        assertThat(result).isNull();
    }

    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName(ATTR_LOGIN)
                .build();
    }

    private OAuth2AccessToken testAccessToken() {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                TEST_ACCESS_TOKEN,
                Instant.now(),
                Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS));
    }

    private OAuth2UserRequest mockUserRequestWithToken() {
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn(TEST_ACCESS_TOKEN);
        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        when(userRequest.getAccessToken()).thenReturn(accessToken);
        return userRequest;
    }

    private Map<String, Object> createEmailEntry(String email, boolean primary, boolean verified) {
        return Map.of(ATTR_EMAIL, email, ATTR_PRIMARY, primary, ATTR_VERIFIED, verified);
    }

    private OAuth2User createMockOAuth2User(String login, String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_LOGIN, login);
        attributes.put("id", 12345);
        if (email != null) {
            attributes.put(ATTR_EMAIL, email);
        }

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                attributes,
                ATTR_LOGIN
        );
    }
}
