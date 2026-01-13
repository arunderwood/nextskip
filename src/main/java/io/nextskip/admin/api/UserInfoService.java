package io.nextskip.admin.api;

import com.vaadin.hilla.BrowserCallable;
import io.nextskip.admin.model.UserInfo;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Hilla endpoint for admin user information.
 *
 * <p>Provides the currently authenticated admin user's information to the frontend.
 * This endpoint requires ADMIN role, enforced by Spring Security.
 */
@BrowserCallable
@RolesAllowed("ADMIN")
public class UserInfoService {

    /**
     * Returns information about the currently authenticated admin user.
     *
     * @return the user info for the frontend, or null if not authenticated
     */
    public UserInfo getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User principal)) {
            return null;
        }

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String avatarUrl = principal.getAttribute("avatar_url");

        // Fall back to login name if display name is not set
        if (name == null || name.isBlank()) {
            name = principal.getAttribute("login");
        }

        return new UserInfo(email, name, avatarUrl);
    }
}
