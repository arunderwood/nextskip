# Spring Security OAuth2/OIDC Research for NextSkip

> **Research Date**: 2026-01-09
> **Target Stack**: Spring Boot 3.4, Spring Security 6.x, Vaadin Hilla

## Summary

This document captures best practices for implementing OAuth2/OIDC authentication in the NextSkip application, with email-based allowlist authorization for admin routes.

---

## 1. Dependencies Required

Add to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
}
```

Spring Boot auto-configures OAuth2 login when this starter is present with valid provider configuration.

**Sources**: [Spring Boot and OAuth2 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/), [Spring Security OAuth2 Core Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)

---

## 2. OAuth2 Provider Configuration (Google + GitHub)

Spring Security includes `CommonOAuth2Provider` with pre-defined settings for Google, GitHub, Facebook, and Okta. Only `client-id` and `client-secret` are required.

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, email, profile
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
        provider:
          # Google uses OIDC, no extra config needed
          # GitHub is not OIDC, but Spring handles it automatically
```

### OAuth2 Callback URLs

Configure in provider console:
- **Google**: `http://localhost:8080/login/oauth2/code/google`
- **GitHub**: `http://localhost:8080/login/oauth2/code/github`

Pattern: `/login/oauth2/code/{registrationId}`

**Sources**: [Spring Security OAuth2 Core Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)

---

## 3. SecurityFilterChain Configuration for Mixed Routes

### Vaadin Hilla Approach (VaadinSecurityConfigurer)

Since Vaadin 24.x, use `VaadinSecurityConfigurer` instead of extending `VaadinWebSecurity`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/", "/login", "/login/**", "/oauth2/**").permitAll()
                .requestMatchers("/VAADIN/**", "/frontend/**").permitAll()

                // Admin routes require authentication
                .requestMatchers("/admin/**").authenticated()

                // All other routes are public (dashboard)
                .anyRequest().permitAll()
            )
            .with(VaadinSecurityConfigurer.vaadin(), configurer -> {
                configurer.oauth2LoginPage(
                    "/oauth2/authorization/google",  // Default provider
                    "{baseUrl}/session-ended"        // Post-logout redirect
                );
            })
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOidcUserService())
                )
            );

        return http.build();
    }
}
```

### Key Configuration Points

1. **Public Dashboard**: `anyRequest().permitAll()` keeps dashboard accessible
2. **Protected Admin Routes**: `/admin/**` requires authentication
3. **Vaadin Static Resources**: Always permit `/VAADIN/**`, `/frontend/**`
4. **Login Trigger**: OAuth2 login at `/oauth2/authorization/{registrationId}`

**Sources**: [Vaadin OAuth2 Configuration](https://vaadin.com/docs/latest/flow/integrations/spring/oauth2), [Spring Security Advanced Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)

---

## 4. Email-Based Allowlist Authorization

Implement custom `OidcUserService` to validate email against allowlist after OAuth authentication.

### Custom OidcUserService

```java
@Component
public class AllowlistOidcUserService extends OidcUserService {

    private final Set<String> allowedEmails;

    public AllowlistOidcUserService(
            @Value("${nextskip.security.allowed-emails}") List<String> allowedEmails) {
        this.allowedEmails = new HashSet<>(allowedEmails);
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null || !allowedEmails.contains(email.toLowerCase(Locale.ROOT))) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("access_denied",
                    "Email not in allowlist: " + email,
                    null)
            );
        }

        // Optionally add custom authorities based on email/domain
        Collection<GrantedAuthority> authorities = new ArrayList<>(oidcUser.getAuthorities());
        if (isAdmin(email)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private boolean isAdmin(String email) {
        return email.endsWith("@yourdomain.com");
    }
}
```

### For Non-OIDC Providers (GitHub)

```java
@Component
public class AllowlistOAuth2UserService extends DefaultOAuth2UserService {

    private final Set<String> allowedEmails;

    public AllowlistOAuth2UserService(
            @Value("${nextskip.security.allowed-emails}") List<String> allowedEmails) {
        this.allowedEmails = new HashSet<>(allowedEmails);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        String email = user.getAttribute("email");
        if (email == null || !allowedEmails.contains(email.toLowerCase(Locale.ROOT))) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("access_denied", "Email not authorized", null)
            );
        }

        return user;
    }
}
```

### Configuration

```yaml
nextskip:
  security:
    allowed-emails:
      - admin@example.com
      - operator@example.com
```

**Sources**: [Spring Security OAuth2 Advanced Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html), [Baeldung Spring Security OAuth2 Login](https://www.baeldung.com/spring-security-5-oauth2-login)

---

## 5. Hilla @BrowserCallable Security Integration

### Current State

NextSkip uses `@AnonymousAllowed` on all endpoints for public dashboard access:

```java
@BrowserCallable
@AnonymousAllowed
public class PropagationEndpoint {
    // Public methods
}
```

### Transitioning to Role-Based Access

#### Option A: Class-Level Annotations (Recommended)

Keep public endpoints anonymous, add role-based endpoints separately:

```java
// Public dashboard endpoint (no change)
@BrowserCallable
@AnonymousAllowed
public class PropagationEndpoint {
    public SolarIndices getSolarIndices() { ... }
}

// Admin-only endpoint
@BrowserCallable
@RolesAllowed("ADMIN")
public class AdminEndpoint {
    public void refreshAllData() { ... }

    @PermitAll  // Override class-level for specific methods
    public SystemStatus getStatus() { ... }
}
```

#### Option B: Method-Level Mixed Access

```java
@BrowserCallable
public class MixedEndpoint {

    @AnonymousAllowed
    public List<Contest> getContests() { ... }

    @RolesAllowed("ADMIN")
    public void deleteContest(long id) { ... }
}
```

### Security Annotation Hierarchy

Priority order (highest to lowest):
1. `@DenyAll` - Blocks all access
2. `@AnonymousAllowed` - Allows unauthenticated access
3. `@RolesAllowed` - Requires specific roles
4. `@PermitAll` - Allows any authenticated user

**Default behavior**: All requests denied if no annotation present.

### Using Spring Security Instead of Vaadin Annotations

To use `@PreAuthorize` instead of Vaadin annotations:

```java
@BrowserCallable
@AnonymousAllowed  // Bypass Vaadin's security layer
public class AdminEndpoint {

    @PreAuthorize("hasRole('ADMIN')")
    public void adminAction() { ... }

    @PreAuthorize("@allowlistService.isAllowed(authentication)")
    public void restrictedAction() { ... }
}
```

Requires enabling method security:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```

**Sources**: [Vaadin Hilla Security Configuration](https://vaadin.com/docs/latest/hilla/guides/security/configuring), [Protecting BrowserCallable Services](https://vaadin.com/docs/latest/building-apps/security/protect-services/hilla)

---

## 6. Complete Configuration Example

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AllowlistOidcUserService oidcUserService;
    private final AllowlistOAuth2UserService oauth2UserService;

    public SecurityConfig(
            AllowlistOidcUserService oidcUserService,
            AllowlistOAuth2UserService oauth2UserService) {
        this.oidcUserService = oidcUserService;
        this.oauth2UserService = oauth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/VAADIN/**", "/frontend/**", "/icons/**").permitAll()
                .requestMatchers("/manifest.json", "/sw.js", "/offline.html").permitAll()

                // OAuth2 endpoints
                .requestMatchers("/login/**", "/oauth2/**").permitAll()

                // Admin routes require authentication + ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Dashboard is public
                .anyRequest().permitAll()
            )

            // Vaadin security integration
            .with(VaadinSecurityConfigurer.vaadin(), configurer -> {
                configurer.oauth2LoginPage(
                    "/oauth2/authorization/google",
                    "{baseUrl}"
                );
            })

            // OAuth2 login configuration
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService)
                    .userService(oauth2UserService)
                )
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/login?error=access_denied")
            )

            // Logout configuration
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
```

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid, email, profile
          github:
            client-id: ${GITHUB_CLIENT_ID:}
            client-secret: ${GITHUB_CLIENT_SECRET:}
            scope: user:email

nextskip:
  security:
    enabled: ${NEXTSKIP_SECURITY_ENABLED:false}
    allowed-emails: ${NEXTSKIP_ALLOWED_EMAILS:}
```

---

## 7. Implementation Recommendations

### Phase 1: Add OAuth2 Infrastructure
1. Add `spring-boot-starter-oauth2-client` dependency
2. Create `SecurityConfig` with OAuth2 login
3. Create custom `OidcUserService` with email allowlist
4. Configure providers in `application.yml`

### Phase 2: Protect Admin Routes
1. Create `/admin` views for authenticated users
2. Add admin-specific `@BrowserCallable` endpoints with `@RolesAllowed("ADMIN")`
3. Keep existing endpoints as `@AnonymousAllowed`

### Phase 3: Frontend Integration
1. Add login/logout UI components
2. Handle authentication state in React
3. Conditionally render admin features

### Key Considerations

- **No Breaking Changes**: Keep dashboard public with `@AnonymousAllowed`
- **Environment-Based Toggle**: Use `nextskip.security.enabled` to disable in dev
- **Secrets Management**: Use environment variables for OAuth credentials
- **Session Management**: Consider Redis session store for production

---

## References

- [Spring Boot and OAuth2 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Spring Security OAuth2 Core Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Spring Security OAuth2 Advanced Configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
- [Vaadin OAuth2 Configuration](https://vaadin.com/docs/latest/flow/integrations/spring/oauth2)
- [Vaadin Hilla Security Configuration](https://vaadin.com/docs/latest/hilla/guides/security/configuring)
- [Protecting BrowserCallable Services](https://vaadin.com/docs/latest/building-apps/security/protect-services/hilla)
- [Baeldung Spring Security OAuth2 Login](https://www.baeldung.com/spring-security-5-oauth2-login)
- [Handle Spring Security Exceptions](https://www.baeldung.com/spring-security-exceptions)
