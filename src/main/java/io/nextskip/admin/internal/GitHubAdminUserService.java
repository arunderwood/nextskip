package io.nextskip.admin.internal;

import io.nextskip.admin.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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

    private final AdminProperties adminProperties;

    /**
     * Creates a new GitHubAdminUserService.
     *
     * @param adminProperties configuration containing the email allowlist
     */
    public GitHubAdminUserService(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        return processAndAuthorizeUser(user);
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
