package io.nextskip.common.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an amateur radio callsign with prefix extraction and validation.
 *
 * <p>This model parses callsigns into their components:
 * <ul>
 *   <li><b>Prefix</b>: The country identifier (e.g., W1, JA1, VK2, G)</li>
 *   <li><b>Portable prefix</b>: Country prefix when operating abroad (e.g., M/ for England)</li>
 *   <li><b>Suffix</b>: Optional modifier (e.g., /P for portable, /M for mobile)</li>
 *   <li><b>Base call</b>: The callsign without prefix or suffix</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 * new Callsign("W1ABC").getPrefix()           // "W1"
 * new Callsign("JA1ABC/P").getSuffix()        // "P"
 * new Callsign("G3XYZ/QRP").isQrp()           // true
 * new Callsign("VK2ABC/M").isMobile()         // true
 * new Callsign("M/SQ9VR").getPortablePrefix() // "M"
 * new Callsign("M/SQ9VR").getBaseCall()       // "SQ9VR"
 * new Callsign("F/W1AW/P").getPortablePrefix()// "F"
 * new Callsign("F/W1AW/P").getBaseCall()      // "W1AW"
 * new Callsign("F/W1AW/P").getSuffix()        // "P"
 * </pre>
 *
 * <p>Provides foundation for cty.dat-based DXCC lookup in Phase 2.
 *
 * @param value the full callsign string (case-insensitive)
 */
public record Callsign(String value) {

    /** Minimum length for base callsign (supports special event 1x1 callsigns like K1A). */
    private static final int MIN_BASE_CALL_LENGTH = 2;

    /** Maximum length for base callsign per ITU allocations. */
    private static final int MAX_BASE_CALL_LENGTH = 7;

    /** Maximum length for portable prefix (e.g., M, F, VK, EA8). */
    private static final int MAX_PORTABLE_PREFIX_LENGTH = 3;

    /** Minimum length for base call after portable prefix (to distinguish from suffix). */
    private static final int MIN_BASE_AFTER_PREFIX_LENGTH = 3;

    /**
     * Reasons why a callsign validation might fail.
     *
     * <p>Note: These are logged for analysis but don't cause rejection
     * in the permissive validation mode used by the spot pipeline.
     */
    public enum ValidationFailure {
        /** Base call is less than 2 characters. */
        TOO_SHORT("Base call less than 2 characters"),

        /** Base call exceeds 7 characters. */
        TOO_LONG("Base call exceeds 7 characters"),

        /** Callsign must contain at least one letter. */
        NO_LETTER("Must contain at least one letter"),

        /** Callsign must contain at least one digit. */
        NO_DIGIT("Must contain at least one digit"),

        /** Q prefixes are reserved for Q-codes (QSL, QTH, etc.). */
        Q_PREFIX("Q prefixes are reserved for service codes");

        private final String description;

        ValidationFailure(String description) {
            this.description = description;
        }

        /**
         * Returns the human-readable description of the failure.
         *
         * @return failure description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Result of callsign validation.
     *
     * @param valid true if the callsign passes validation
     * @param failure the reason for failure, or null if valid
     */
    public record ValidationResult(boolean valid, ValidationFailure failure) {

        /** Singleton for valid callsigns. */
        public static final ValidationResult VALID = new ValidationResult(true, null);

        /**
         * Creates a failed validation result.
         *
         * @param failure the reason for failure
         * @return failed validation result
         */
        public static ValidationResult failed(ValidationFailure failure) {
            return new ValidationResult(false, failure);
        }

        /**
         * Returns whether this result represents a valid callsign.
         *
         * @return true if valid
         */
        public boolean isValid() {
            return valid;
        }
    }

    // Prefix extraction: captures the country prefix (letters + first digit)
    // Examples: W1, JA1, VK2, G3, 4X1
    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "^([A-Z0-9]*[A-Z][A-Z0-9]*[0-9])",
            Pattern.CASE_INSENSITIVE
    );

    // APRS SSID suffix pattern: -0 through -15
    // Used by packet radio to identify station type (e.g., K9TRV-4, K9TRV-4/P)
    // Matches SSID at end of string OR before a slash suffix
    private static final Pattern SSID_PATTERN = Pattern.compile("-([0-9]|1[0-5])(?=/|$)");

    // SWL (Shortwave Listener) callsign pattern: 2-3 letter country prefix + 3-6 digits
    // Examples: NL9222 (Netherlands), RS123456 (UK RSGB), DE12345 (Germany)
    private static final Pattern SWL_PATTERN = Pattern.compile(
            "^([A-Z]{2,3})([0-9]{3,6})$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Creates a Callsign from the given value.
     *
     * @param value the callsign string
     * @throws IllegalArgumentException if value is null or blank
     */
    public Callsign {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Callsign cannot be null or blank");
        }
        // Normalize to uppercase
        value = value.toUpperCase(Locale.ROOT).trim();
    }

    /**
     * Extracts the country prefix from the callsign.
     *
     * <p>The prefix is the portion before and including the first digit
     * in the call area. Examples:
     * <ul>
     *   <li>W1ABC → W1</li>
     *   <li>JA1ABC → JA1</li>
     *   <li>VK2XYZ → VK2</li>
     *   <li>G3ABC → G3</li>
     *   <li>4X1ABC → 4X1</li>
     * </ul>
     *
     * @return the prefix, or the full base call if no prefix pattern matches
     */
    public String getPrefix() {
        String baseCall = getBaseCall();
        Matcher matcher = PREFIX_PATTERN.matcher(baseCall);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Fallback: return full base call if pattern doesn't match
        return baseCall;
    }

    /**
     * Returns the portable prefix if present.
     *
     * <p>When an amateur operates from a different country, they prepend
     * the visited country's prefix:
     * <ul>
     *   <li>M/SQ9VR → M (Polish operator SQ9VR operating from England)</li>
     *   <li>F/W1AW → F (US operator W1AW operating from France)</li>
     *   <li>VK/JA1ABC → VK (Japanese operator JA1ABC operating from Australia)</li>
     *   <li>EA8/G3ABC → EA8 (UK operator G3ABC operating from Canary Islands)</li>
     *   <li>F/W1AW/P → F (with additional /P suffix)</li>
     * </ul>
     *
     * <p>A portable prefix is identified when:
     * <ul>
     *   <li>The first segment before "/" is 1-3 characters</li>
     *   <li>The second segment is at least 3 characters (a valid callsign)</li>
     * </ul>
     *
     * @return the portable prefix, or null if none
     */
    public String getPortablePrefix() {
        // Strip SSID first
        String withoutSsid = SSID_PATTERN.matcher(value).replaceFirst("");

        int firstSlash = withoutSsid.indexOf('/');
        if (firstSlash <= 0) {
            return null;
        }

        String firstPart = withoutSsid.substring(0, firstSlash);
        String remainder = withoutSsid.substring(firstSlash + 1);

        // Get the second part (before any suffix slash)
        int secondSlash = remainder.indexOf('/');
        String secondPart = secondSlash > 0 ? remainder.substring(0, secondSlash) : remainder;

        // Portable prefix: first part is short (1-3 chars), second part looks like a callsign
        if (firstPart.length() <= MAX_PORTABLE_PREFIX_LENGTH
                && secondPart.length() >= MIN_BASE_AFTER_PREFIX_LENGTH) {
            return firstPart;
        }

        return null;
    }

    /**
     * Returns whether this callsign has a portable prefix.
     *
     * @return true if a portable prefix is present
     */
    public boolean hasPortablePrefix() {
        return getPortablePrefix() != null;
    }

    /**
     * Returns the callsign without any prefix, suffix, or SSID.
     *
     * <p>Examples:
     * <ul>
     *   <li>W1ABC → W1ABC</li>
     *   <li>W1ABC/P → W1ABC</li>
     *   <li>G3XYZ/QRP → G3XYZ</li>
     *   <li>K9TRV-4 → K9TRV (APRS SSID stripped)</li>
     *   <li>K9TRV-4/P → K9TRV (both SSID and suffix stripped)</li>
     *   <li>M/SQ9VR → SQ9VR (portable prefix stripped)</li>
     *   <li>F/W1AW/P → W1AW (both portable prefix and suffix stripped)</li>
     * </ul>
     *
     * @return the base callsign
     */
    public String getBaseCall() {
        // Strip APRS SSID suffix first (e.g., "-4" from "K9TRV-4")
        String base = SSID_PATTERN.matcher(value).replaceFirst("");

        int firstSlash = base.indexOf('/');
        if (firstSlash <= 0) {
            // No slash, entire string is the base call
            return base;
        }

        String firstPart = base.substring(0, firstSlash);
        String remainder = base.substring(firstSlash + 1);

        // Check for portable prefix pattern: short prefix / longer callsign
        int secondSlash = remainder.indexOf('/');
        String secondPart = secondSlash > 0 ? remainder.substring(0, secondSlash) : remainder;

        if (firstPart.length() <= MAX_PORTABLE_PREFIX_LENGTH
                && secondPart.length() >= MIN_BASE_AFTER_PREFIX_LENGTH) {
            // This is a portable prefix (e.g., M/SQ9VR) - base call is second part
            return secondPart;
        }

        // Not a portable prefix - first part is the base call (e.g., W1AW/P)
        return firstPart;
    }

    /**
     * Returns the suffix if present.
     *
     * <p>Common suffixes:
     * <ul>
     *   <li>P - Portable</li>
     *   <li>M - Mobile</li>
     *   <li>MM - Maritime Mobile</li>
     *   <li>AM - Aeronautical Mobile</li>
     *   <li>QRP - Low power</li>
     * </ul>
     *
     * <p>Note: APRS SSIDs (e.g., -4) are not considered suffixes.
     * For K9TRV-4/P, this returns "P", not "-4/P".
     *
     * <p>Note: Portable prefixes (e.g., M/) are not considered suffixes.
     * For F/W1AW/P, this returns "P", not "W1AW/P".
     *
     * @return the suffix, or null if none
     */
    public String getSuffix() {
        // Strip SSID first so K9TRV-4/P returns "P", not "-4/P"
        String withoutSsid = SSID_PATTERN.matcher(value).replaceFirst("");

        int firstSlash = withoutSsid.indexOf('/');
        if (firstSlash <= 0) {
            return null;
        }

        String firstPart = withoutSsid.substring(0, firstSlash);
        String remainder = withoutSsid.substring(firstSlash + 1);

        // Check for portable prefix pattern
        int secondSlash = remainder.indexOf('/');
        String secondPart = secondSlash > 0 ? remainder.substring(0, secondSlash) : remainder;

        if (firstPart.length() <= MAX_PORTABLE_PREFIX_LENGTH
                && secondPart.length() >= MIN_BASE_AFTER_PREFIX_LENGTH) {
            // This is a portable prefix - look for suffix after the base call
            if (secondSlash > 0 && secondSlash < remainder.length() - 1) {
                return remainder.substring(secondSlash + 1);
            }
            return null;
        }

        // Not a portable prefix - everything after first slash is suffix (if non-empty)
        return remainder.isEmpty() ? null : remainder;
    }

    /**
     * Returns whether this callsign has a suffix.
     *
     * @return true if the callsign has a suffix
     */
    public boolean hasSuffix() {
        return getSuffix() != null;
    }

    /**
     * Returns whether this is a portable callsign (/P).
     *
     * @return true if portable
     */
    public boolean isPortable() {
        return "P".equalsIgnoreCase(getSuffix());
    }

    /**
     * Returns whether this is a mobile callsign (/M).
     *
     * @return true if mobile
     */
    public boolean isMobile() {
        return "M".equalsIgnoreCase(getSuffix());
    }

    /**
     * Returns whether this is a maritime mobile callsign (/MM).
     *
     * @return true if maritime mobile
     */
    public boolean isMaritimeMobile() {
        return "MM".equalsIgnoreCase(getSuffix());
    }

    /**
     * Returns whether this is a QRP (low power) callsign.
     *
     * @return true if QRP
     */
    public boolean isQrp() {
        return "QRP".equalsIgnoreCase(getSuffix());
    }

    /**
     * Returns whether this appears to be an SWL (Shortwave Listener) callsign.
     *
     * <p>SWL callsigns are issued by national amateur radio societies for
     * receive-only stations. They typically consist of a country prefix
     * followed by all digits (e.g., NL9222 for Netherlands, RS123456 for UK RSGB).
     *
     * <p><strong>Note:</strong> This is a heuristic based on common SWL formats
     * and may not be 100% accurate. There is no authoritative database of SWL
     * callsign patterns, and formats vary by country. False positives and
     * negatives are possible.
     *
     * @return true if this matches a common SWL callsign pattern
     */
    public boolean isSwl() {
        return SWL_PATTERN.matcher(getBaseCall()).matches();
    }

    /**
     * Returns the APRS SSID if present.
     *
     * <p>APRS (Automatic Packet Reporting System) uses SSID suffixes in the
     * format {@code CALLSIGN-N} where N is 0-15 to identify station types:
     * <ul>
     *   <li>0 - Primary station</li>
     *   <li>1-4 - Additional stations</li>
     *   <li>5 - Smartphone</li>
     *   <li>7 - HT (handheld)</li>
     *   <li>9 - Mobile</li>
     *   <li>10 - Internet gateway</li>
     *   <li>15 - Generic additional station</li>
     * </ul>
     *
     * @return the SSID (0-15), or null if no valid SSID present
     */
    public Integer getSsid() {
        Matcher matcher = SSID_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /**
     * Returns whether this callsign has an APRS SSID suffix.
     *
     * @return true if SSID present (0-15)
     */
    public boolean hasSsid() {
        return getSsid() != null;
    }

    /**
     * Validates this callsign and returns detailed results.
     *
     * <p>Validation checks (in order):
     * <ol>
     *   <li>Length: 2-7 characters (supports special event 1x1 callsigns like K1A)</li>
     *   <li>Must contain at least one letter</li>
     *   <li>Must contain at least one digit</li>
     *   <li>Q prefix check (Q codes are reserved for service abbreviations)</li>
     * </ol>
     *
     * @return validation result with failure reason if invalid
     */
    public ValidationResult validate() {
        String baseCall = getBaseCall();

        // Length check (supports special event 1x1 callsigns like K1A, G3A)
        if (baseCall.length() < MIN_BASE_CALL_LENGTH) {
            return ValidationResult.failed(ValidationFailure.TOO_SHORT);
        }
        if (baseCall.length() > MAX_BASE_CALL_LENGTH) {
            return ValidationResult.failed(ValidationFailure.TOO_LONG);
        }

        // Must contain at least one letter and one digit
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : baseCall.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter) {
            return ValidationResult.failed(ValidationFailure.NO_LETTER);
        }
        if (!hasDigit) {
            return ValidationResult.failed(ValidationFailure.NO_DIGIT);
        }

        // Q prefix check (Q codes are reserved for service abbreviations)
        if (baseCall.startsWith("Q")) {
            return ValidationResult.failed(ValidationFailure.Q_PREFIX);
        }

        return ValidationResult.VALID;
    }

    /**
     * Validates that this callsign matches a basic amateur radio format.
     *
     * <p>This is a basic validation checking for reasonable structure.
     * It does not validate against actual ITU allocations.
     *
     * <p>For detailed validation results including failure reasons,
     * use {@link #validate()} instead.
     *
     * @return true if the callsign appears valid
     */
    public boolean isValid() {
        return validate().isValid();
    }

    @Override
    public String toString() {
        return value;
    }
}
