package io.nextskip.admin.model;

/**
 * Represents authenticated admin user information for the frontend.
 *
 * <p>This record is returned by the UserInfoService endpoint to provide
 * the frontend with information about the currently logged-in admin user.
 *
 * @param email the user's email address (used for authorization)
 * @param name the user's display name from GitHub
 * @param avatarUrl URL to the user's GitHub avatar image
 */
public record UserInfo(
        String email,
        String name,
        String avatarUrl
) {
}
