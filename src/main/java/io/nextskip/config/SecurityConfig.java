package io.nextskip.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nextskip.admin.internal.GitHubAdminUserService;
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
 *   <li>Admin chain (order 1): Protects /admin/** with OAuth2</li>
 *   <li>Default chain (order 2): Permits all other requests</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-injected service is immutable singleton")
public class SecurityConfig {

    private final GitHubAdminUserService gitHubAdminUserService;

    /**
     * Creates a new SecurityConfig.
     *
     * @param gitHubAdminUserService custom OAuth2 user service for admin authorization
     */
    public SecurityConfig(GitHubAdminUserService gitHubAdminUserService) {
        this.gitHubAdminUserService = gitHubAdminUserService;
    }

    /**
     * Security filter chain for admin routes.
     *
     * <p>Requires ADMIN role for /admin/** paths and configures GitHub OAuth2 login.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) {
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
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // Disable CSRF for Hilla endpoints (they use their own protection)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
