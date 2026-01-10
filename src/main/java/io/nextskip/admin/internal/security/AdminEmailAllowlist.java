package io.nextskip.admin.internal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the allowlist of email addresses permitted to access the admin area.
 *
 * <p>Emails are configured via the {@code nextskip.admin.allowed-emails} property,
 * which accepts a comma-separated list of email addresses. Email comparison is
 * case-insensitive.
 *
 * <p>Example configuration:
 * <pre>
 * nextskip.admin.allowed-emails=admin@example.com,user@example.com
 * </pre>
 *
 * <p>Or via environment variable:
 * <pre>
 * NEXTSKIP_ADMIN_ALLOWED_EMAILS=admin@example.com,user@example.com
 * </pre>
 */
@Component
public class AdminEmailAllowlist {

    private static final Logger LOG = LoggerFactory.getLogger(AdminEmailAllowlist.class);

    private final Set<String> allowedEmails;

    public AdminEmailAllowlist(
            @Value("${nextskip.admin.allowed-emails:}") String allowedEmailsConfig) {

        this.allowedEmails = parseEmails(allowedEmailsConfig);

        if (allowedEmails.isEmpty()) {
            LOG.warn("Admin email allowlist is empty. No users will be able to access the admin area. "
                    + "Configure 'nextskip.admin.allowed-emails' or set NEXTSKIP_ADMIN_ALLOWED_EMAILS.");
        } else {
            LOG.info("Admin email allowlist configured with {} email(s)", allowedEmails.size());
        }
    }

    /**
     * Checks if the given email is in the allowlist.
     *
     * @param email the email address to check (case-insensitive)
     * @return true if the email is allowed to access the admin area
     */
    public boolean isAllowed(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return allowedEmails.contains(email.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the number of emails in the allowlist.
     *
     * @return count of allowed emails
     */
    public int size() {
        return allowedEmails.size();
    }

    private Set<String> parseEmails(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptySet();
        }

        return Stream.of(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
