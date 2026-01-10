package io.nextskip.admin.internal.security;

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
 * Custom OAuth2 user service for GitHub authentication.
 *
 * <p>Extends the default OAuth2 user service to:
 * <ol>
 *   <li>Fetch user information from GitHub</li>
 *   <li>Check if user's email is in the admin allowlist</li>
 *   <li>Add ROLE_ADMIN authority if authorized</li>
 *   <li>Reject authentication if not in allowlist</li>
 * </ol>
 *
 * <p>Note: GitHub requires the {@code user:email} scope to access the user's email.
 * If the user's email is private on GitHub, this may return null and authentication
 * will fail.
 */
@Service
public class GitHubAdminUserService extends DefaultOAuth2UserService {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAdminUserService.class);
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String NAME_ATTRIBUTE = "login";

    private final AdminEmailAllowlist allowlist;

    public GitHubAdminUserService(AdminEmailAllowlist allowlist) {
        this.allowlist = allowlist;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = fetchOAuth2User(userRequest);

        String email = user.getAttribute(EMAIL_ATTRIBUTE);
        String login = user.getAttribute(NAME_ATTRIBUTE);

        LOG.debug("GitHub user authenticated: login={}, email={}", login, email);

        if (email == null) {
            LOG.warn("GitHub user {} has no public email. Authentication denied.", login);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_required",
                            "Your GitHub account email must be public or you must grant email access", null));
        }

        if (!allowlist.isAllowed(email)) {
            LOG.warn("GitHub user {} ({}) not in admin allowlist. Authentication denied.", login, email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied",
                            "You are not authorized to access the admin area", null));
        }

        LOG.info("GitHub user {} ({}) authorized as admin", login, email);

        // Add ADMIN role to the user's authorities
        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(ADMIN_ROLE));

        return new DefaultOAuth2User(
                authorities,
                user.getAttributes(),
                NAME_ATTRIBUTE
        );
    }

    /**
     * Fetches the OAuth2User from the provider.
     *
     * <p>Protected for testing - allows tests to inject mock users without reflection.
     *
     * @param userRequest the OAuth2 user request
     * @return the OAuth2 user from the provider
     */
    protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }
}
