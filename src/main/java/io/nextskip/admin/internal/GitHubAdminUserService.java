package io.nextskip.admin.internal;

import io.nextskip.admin.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom OAuth2 user service that adds ADMIN authority for allowlisted GitHub users.
 *
 * <p>This service intercepts the GitHub OAuth2 login flow to:
 * <ol>
 *   <li>Load the user from GitHub via the default OAuth2 flow</li>
 *   <li>Check if the user's email is in the configured allowlist</li>
 *   <li>Grant ROLE_ADMIN authority if authorized</li>
 *   <li>Reject authentication if email is not allowed</li>
 * </ol>
 *
 * <p>Email allowlist is configured via {@link AdminProperties#allowedEmails()}.
 */
@Service
public class GitHubAdminUserService extends DefaultOAuth2UserService {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAdminUserService.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String GITHUB_EMAILS_URI = "https://api.github.com/user/emails";

    private final AdminProperties adminProperties;
    private final RestTemplate restTemplate;

    /**
     * Creates a new GitHubAdminUserService.
     *
     * @param adminProperties configuration containing the email allowlist
     */
    public GitHubAdminUserService(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String email = user.getAttribute("email");

        // If email is not public, fetch from GitHub emails API
        if (email == null) {
            email = fetchPrimaryEmail(userRequest.getAccessToken().getTokenValue());
            if (email != null) {
                // Create new user with email added to attributes
                Map<String, Object> attributes = new HashMap<>(user.getAttributes());
                attributes.put("email", email);
                user = new DefaultOAuth2User(user.getAuthorities(), attributes, "login");
            }
        }

        return processAndAuthorizeUser(user);
    }

    /**
     * Fetches the primary email from GitHub's /user/emails endpoint.
     *
     * <p>This is needed when users have their email set to private.
     * The user:email scope grants access to this endpoint.
     *
     * @param accessToken the OAuth2 access token
     * @return the primary email, or null if not found
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // RestTemplate can throw various runtime exceptions
    private String fetchPrimaryEmail(String accessToken) {
        try {
            RequestEntity<Void> request = RequestEntity
                    .get(URI.create(GITHUB_EMAILS_URI))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .build();

            List<Map<String, Object>> emails = restTemplate.exchange(
                    request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() { }
            ).getBody();

            if (emails != null) {
                // Find primary email first, fall back to first verified email
                for (Map<String, Object> emailEntry : emails) {
                    if (Boolean.TRUE.equals(emailEntry.get("primary"))) {
                        return (String) emailEntry.get("email");
                    }
                }
                // If no primary, return first verified
                for (Map<String, Object> emailEntry : emails) {
                    if (Boolean.TRUE.equals(emailEntry.get("verified"))) {
                        return (String) emailEntry.get("email");
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to fetch emails from GitHub API: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Processes the OAuth2 user and authorizes based on email allowlist.
     *
     * <p>This method is extracted for testability - tests can call this directly
     * with a mock OAuth2User without making HTTP calls to GitHub.
     *
     * @param user the OAuth2 user loaded from GitHub
     * @return the user with ADMIN authority added if authorized
     * @throws OAuth2AuthenticationException if email is missing or not in allowlist
     */
    protected OAuth2User processAndAuthorizeUser(OAuth2User user) throws OAuth2AuthenticationException {
        String email = user.getAttribute("email");
        String login = user.getAttribute("login");

        LOG.debug("Processing OAuth2 login for user: {} ({})", login, email);

        if (email == null) {
            LOG.warn("GitHub user {} has no public email configured", login);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "No email found for GitHub user. Please make your email public in GitHub settings.");
        }

        if (!adminProperties.isEmailAllowed(email)) {
            LOG.warn("Access denied for user {} - email {} not in allowlist", login, email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "User not authorized for admin access: " + email);
        }

        LOG.info("Admin access granted to user: {} ({})", login, email);

        // Add ADMIN authority to existing authorities
        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));

        // Return user with enhanced authorities
        // Use "login" as the name attribute key (GitHub's username field)
        return new DefaultOAuth2User(authorities, user.getAttributes(), "login");
    }
}
