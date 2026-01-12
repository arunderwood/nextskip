package io.nextskip.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.admin.internal.GitHubAdminUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for NextSkip.
 *
 * <p>Configures OAuth2 login for admin access while keeping the public dashboard
 * completely unauthenticated.
 *
 * <p>Security model:
 * <ul>
 *   <li>Public dashboard: No authentication required ({@code @AnonymousAllowed})</li>
 *   <li>Admin area: GitHub OAuth2 with email allowlist ({@code @RolesAllowed("ADMIN")})</li>
 * </ul>
 *
 * <p>Uses two filter chains:
 * <ol>
 *   <li>Admin chain (order 1): Protects /admin/** with OAuth2 (when configured)</li>
 *   <li>Default chain (order 2): Permits all other requests</li>
 * </ol>
 *
 * <p>When OAuth2 is not configured (GITHUB_CLIENT_ID not set), admin routes
 * are blocked with 403 Forbidden.
 */
@Configuration
@EnableWebSecurity
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected service is immutable singleton")
public class SecurityConfig {

    private static final String DISABLED_PLACEHOLDER = "disabled";

    private final GitHubAdminUserService gitHubAdminUserService;
    private final boolean oauth2Enabled;

    /**
     * Creates a new SecurityConfig.
     *
     * @param gitHubAdminUserService custom OAuth2 user service for admin authorization
     * @param githubClientId the GitHub OAuth2 client ID (or "disabled" placeholder)
     */
    public SecurityConfig(
            GitHubAdminUserService gitHubAdminUserService,
            @Value("${spring.security.oauth2.client.registration.github.client-id:disabled}")
            String githubClientId) {
        this.gitHubAdminUserService = gitHubAdminUserService;
        this.oauth2Enabled = !DISABLED_PLACEHOLDER.equals(githubClientId);
    }

    /**
     * Security filter chain for admin routes.
     *
     * <p>When OAuth2 is configured, requires ADMIN role for /admin/** paths
     * and configures GitHub OAuth2 login.
     *
     * <p>When OAuth2 is not configured, denies all access to /admin/** paths.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        if (oauth2Enabled) {
            http
                .securityMatcher("/admin/**", "/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                )
                .oauth2Login(oauth -> oauth
                    .loginPage("/oauth2/authorization/github")
                    .defaultSuccessUrl("/admin", true)
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(gitHubAdminUserService)
                    )
                )
                .logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
                );
        } else {
            // OAuth2 not configured - deny all admin access
            http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().denyAll()
                );
        }

        return http.build();
    }

    /**
     * Default security filter chain for public routes.
     *
     * <p>Permits all requests to the public dashboard. Hilla's {@code @AnonymousAllowed}
     * annotation handles endpoint-level security for BrowserCallable endpoints.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // Disable CSRF for Hilla endpoints (they use their own protection)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
