package io.nextskip.admin.api;

import com.vaadin.hilla.BrowserCallable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Hilla endpoint for admin authentication operations.
 *
 * <p>Provides methods to:
 * <ul>
 *   <li>Get information about the current authenticated admin user</li>
 *   <li>Log out the current user</li>
 * </ul>
 *
 * <p>All methods require the ADMIN role, which is granted during the
 * OAuth2 authentication flow when the user's email is in the allowlist.
 *
 * @see io.nextskip.admin.internal.security.GitHubAdminUserService
 */
@BrowserCallable
@RolesAllowed("ADMIN")
public class AdminAuthEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(AdminAuthEndpoint.class);

    private final HttpServletRequest request;

    public AdminAuthEndpoint(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns information about the currently authenticated admin user.
     *
     * <p>Retrieves the authenticated user from Spring Security's context.
     * The endpoint is protected by @RolesAllowed("ADMIN"), ensuring only
     * authenticated admins can call this method.
     *
     * @return AdminUserInfo containing user's email, name, and avatar
     */
    public AdminUserInfo getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User user)) {
            LOG.warn("getCurrentUser called without authenticated OAuth2 user");
            return null;
        }

        String email = user.getAttribute("email");
        String name = user.getAttribute("name");
        String login = user.getAttribute("login");
        String avatarUrl = user.getAttribute("avatar_url");

        // Use login as fallback if name is not available
        String displayName = name != null ? name : login;

        LOG.debug("Returning user info for: {} ({})", displayName, email);

        return AdminUserInfo.of(email, displayName, avatarUrl);
    }

    /**
     * Logs out the current admin user by invalidating their session.
     *
     * <p>After calling this method, the user will need to re-authenticate
     * to access admin functions.
     */
    public void logout() {
        LOG.info("Admin user logging out");
        request.getSession().invalidate();
    }
}
