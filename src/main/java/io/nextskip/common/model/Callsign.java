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
     * Validates that this callsign matches a basic amateur radio format.
     *
     * <p>This is a basic validation checking for reasonable structure.
     * It does not validate against actual ITU allocations.
     *
     * @return true if the callsign appears valid
     */
    public boolean isValid() {
        String baseCall = getBaseCall();
        // Basic check: 3-7 characters, contains at least one letter and one digit
        if (baseCall.length() < 3 || baseCall.length() > 7) {
            return false;
        }
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
        return hasLetter && hasDigit;
    }

    @Override
    public String toString() {
        return value;
    }
}
