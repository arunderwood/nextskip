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
 *   <li><b>Suffix</b>: Optional modifier (e.g., /P for portable, /M for mobile)</li>
 *   <li><b>Base call</b>: The callsign without suffix</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 * new Callsign("W1ABC").getPrefix()     // "W1"
 * new Callsign("JA1ABC/P").getSuffix()  // "P"
 * new Callsign("G3XYZ/QRP").isQrp()     // true
 * new Callsign("VK2ABC/M").isMobile()   // true
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
        Q_PREFIX("Q prefixes are reserved for service codes"),

        /** Per ITU rules, last character of suffix should be a letter (informational only). */
        LAST_CHAR_NOT_LETTER("Last character of suffix is not a letter (ITU guideline)");

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
     * Returns the callsign without any suffix.
     *
     * <p>Examples:
     * <ul>
     *   <li>W1ABC → W1ABC</li>
     *   <li>W1ABC/P → W1ABC</li>
     *   <li>G3XYZ/QRP → G3XYZ</li>
     * </ul>
     *
     * @return the base callsign
     */
    public String getBaseCall() {
        int slashIndex = value.indexOf('/');
        if (slashIndex > 0) {
            return value.substring(0, slashIndex);
        }
        return value;
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
     * @return the suffix, or null if none
     */
    public String getSuffix() {
        int slashIndex = value.indexOf('/');
        if (slashIndex > 0 && slashIndex < value.length() - 1) {
            return value.substring(slashIndex + 1);
        }
        return null;
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
     * Validates this callsign and returns detailed results.
     *
     * <p>Validation checks (in order):
     * <ol>
     *   <li>Length: 2-7 characters (supports special event 1x1 callsigns like K1A)</li>
     *   <li>Must contain at least one letter</li>
     *   <li>Must contain at least one digit</li>
     *   <li>Q prefix warning (Q codes are reserved, but logged rather than rejected)</li>
     *   <li>Last character letter check (ITU guideline, informational)</li>
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

        // ITU guideline: last character of suffix should be a letter
        // This is informational - many valid callsigns violate this (e.g., W1AW/7)
        char lastChar = baseCall.charAt(baseCall.length() - 1);
        if (Character.isDigit(lastChar)) {
            return ValidationResult.failed(ValidationFailure.LAST_CHAR_NOT_LETTER);
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
