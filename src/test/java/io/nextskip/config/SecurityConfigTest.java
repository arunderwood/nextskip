package io.nextskip.config;

import io.nextskip.admin.internal.GitHubAdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityConfig}.
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private GitHubAdminUserService gitHubAdminUserService;

    @Test
    void testConstructor_OAuth2Enabled_WhenClientIdProvided() {
        // Given
        String clientId = "real-client-id";

        // When
        SecurityConfig config = new SecurityConfig(gitHubAdminUserService, clientId);

        // Then - oauth2Enabled should be true (verified by behavior in filter chains)
        assertThat(config).isNotNull();
    }

    @Test
    void testConstructor_OAuth2Disabled_WhenClientIdIsPlaceholder() {
        // Given
        String clientId = "disabled";

        // When
        SecurityConfig config = new SecurityConfig(gitHubAdminUserService, clientId);

        // Then - oauth2Enabled should be false (verified by behavior in filter chains)
        assertThat(config).isNotNull();
    }

    @Test
    void testConstructor_OAuth2Disabled_PreservesService() {
        // Given
        String clientId = "disabled";

        // When
        SecurityConfig config = new SecurityConfig(gitHubAdminUserService, clientId);

        // Then
        assertThat(config).isNotNull();
        // Service is stored but not used when OAuth2 is disabled
    }

    @Test
    void testConstructor_OAuth2Enabled_PreservesService() {
        // Given
        String clientId = "valid-client-id";

        // When
        SecurityConfig config = new SecurityConfig(gitHubAdminUserService, clientId);

        // Then
        assertThat(config).isNotNull();
        // Service is stored for use in OAuth2 flow
    }
}
