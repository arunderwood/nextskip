package io.nextskip.admin.api;

/**
 * Information about the currently authenticated admin user.
 *
 * <p>This record is returned by {@link AdminAuthEndpoint#getCurrentUser()} and contains
 * user information obtained from the GitHub OAuth2 authentication flow.
 *
 * @param email User's email address (from GitHub)
 * @param name User's display name (from GitHub)
 * @param avatarUrl URL to user's avatar image (optional, from GitHub)
 */
public record AdminUserInfo(
        String email,
        String name,
        String avatarUrl
) {

    /**
     * Creates an AdminUserInfo from GitHub OAuth2 user attributes.
     *
     * @param email User's email
     * @param name User's display name (falls back to login if name is null)
     * @param avatarUrl URL to avatar image
     * @return AdminUserInfo instance
     */
    public static AdminUserInfo of(String email, String name, String avatarUrl) {
        return new AdminUserInfo(email, name, avatarUrl);
    }
}
