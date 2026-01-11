package io.nextskip.admin.internal.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for admin authentication.
 *
 * <p>Configures GitHub OAuth2 login for the admin area while keeping
 * the public dashboard accessible without authentication.
 *
 * <p>Route protection:
 * <ul>
 *   <li>{@code /admin/**} - Requires ADMIN role (OAuth2 login)</li>
 *   <li>All other routes - Publicly accessible</li>
 * </ul>
 *
 * <p>This configuration is only active when GitHub OAuth2 credentials are configured.
 * If not configured, the admin area will be inaccessible.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.github",
        name = "client-id")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected beans are managed by Spring container")
public class SecurityConfig {

    private final GitHubAdminUserService gitHubAdminUserService;

    public SecurityConfig(GitHubAdminUserService gitHubAdminUserService) {
        this.gitHubAdminUserService = gitHubAdminUserService;
    }

    /**
     * Security filter chain for admin routes.
     *
     * <p>Higher priority (Order 1) ensures admin routes are processed first.
     */
    @Bean
    @Order(1)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // Spring Security API requires throws Exception
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Only apply to /admin/** paths
                .securityMatcher("/admin/**", "/login/oauth2/**", "/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                        // Admin routes require ADMIN role
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // OAuth2 callback must be accessible
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/admin/login?error=unauthorized")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(gitHubAdminUserService)
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                // Enable CSRF for admin routes (session-based OAuth2 auth)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/connect/**")
                )
                .build();
    }

    /**
     * Security filter chain for public routes.
     *
     * <p>Lower priority (Order 2) - catches all non-admin routes.
     * Permits all requests without authentication for the public dashboard.
     */
    @Bean
    @Order(2)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // Spring Security API requires throws Exception
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Vaadin Hilla handles CSRF for its RPC endpoints internally
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/connect/**")
                )
                .build();
    }
}
