package io.nextskip.admin.api;

import io.nextskip.admin.model.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserInfoService.
 */
class UserInfoServiceTest {

    private static final String TEST_EMAIL = "admin@example.com";
    private static final String TEST_NAME = "Admin User";
    private static final String TEST_AVATAR = "https://example.com/avatar.png";
    private static final String TEST_LOGIN = "adminlogin";
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SUB = "sub";
    private static final String ATTR_SUB_VALUE = "12345";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_AVATAR_URL = "avatar_url";

    private UserInfoService service;

    @BeforeEach
    void setUp() {
        service = new UserInfoService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUser_WithOAuth2User_ReturnsUserInfo() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_EMAIL, TEST_EMAIL);
        attributes.put(ATTR_NAME, TEST_NAME);
        attributes.put(ATTR_AVATAR_URL, TEST_AVATAR);
        attributes.put(ATTR_SUB, ATTR_SUB_VALUE);

        setOAuth2Authentication(attributes);

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo(TEST_EMAIL);
        assertThat(result.name()).isEqualTo(TEST_NAME);
        assertThat(result.avatarUrl()).isEqualTo(TEST_AVATAR);
    }

    @Test
    void testGetCurrentUser_NoAuthentication_ReturnsNull() {
        SecurityContextHolder.clearContext();

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNull();
    }

    @Test
    void testGetCurrentUser_NonOAuth2Principal_ReturnsNull() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNull();
    }

    @Test
    void testGetCurrentUser_NoName_FallsBackToLogin() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_EMAIL, TEST_EMAIL);
        attributes.put(ATTR_LOGIN, TEST_LOGIN);
        attributes.put(ATTR_AVATAR_URL, TEST_AVATAR);
        attributes.put(ATTR_SUB, ATTR_SUB_VALUE);

        setOAuth2Authentication(attributes);

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(TEST_LOGIN);
    }

    @Test
    void testGetCurrentUser_BlankName_FallsBackToLogin() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_EMAIL, TEST_EMAIL);
        attributes.put(ATTR_NAME, "   ");
        attributes.put(ATTR_LOGIN, TEST_LOGIN);
        attributes.put(ATTR_AVATAR_URL, TEST_AVATAR);
        attributes.put(ATTR_SUB, ATTR_SUB_VALUE);

        setOAuth2Authentication(attributes);

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(TEST_LOGIN);
    }

    @Test
    void testGetCurrentUser_NullAvatarUrl() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ATTR_EMAIL, TEST_EMAIL);
        attributes.put(ATTR_NAME, TEST_NAME);
        attributes.put(ATTR_SUB, ATTR_SUB_VALUE);

        setOAuth2Authentication(attributes);

        UserInfo result = service.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.avatarUrl()).isNull();
    }

    private void setOAuth2Authentication(Map<String, Object> attributes) {
        OAuth2User principal = new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                ATTR_SUB
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
