package io.nextskip.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the admin module.
 *
 * <p>Bound to properties under {@code admin.*} in application.yml.
 *
 * <p>Example configuration:
 * <pre>
 * admin:
 *   allowed-emails: admin@example.com,user@example.com
 * </pre>
 *
 * @param allowedEmails list of email addresses authorized for admin access
 */
@ConfigurationProperties(prefix = "nextskip.admin")
public record AdminProperties(
        List<String> allowedEmails
) {

    /**
     * Creates AdminProperties with defaults for missing values.
     *
     * @param allowedEmails list of allowed email addresses, or null for empty list
     */
    public AdminProperties {
        if (allowedEmails == null) {
            allowedEmails = List.of();
        } else {
            allowedEmails = List.copyOf(allowedEmails);
        }
    }

    /**
     * Checks if an email is authorized for admin access.
     *
     * @param email the email to check
     * @return true if the email is in the allowed list
     */
    public boolean isEmailAllowed(String email) {
        if (email == null || allowedEmails.isEmpty()) {
            return false;
        }
        return allowedEmails.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(email));
    }
}
