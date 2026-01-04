package io.nextskip.test.fixtures;

import io.nextskip.common.model.Callsign;

/**
 * Test fixtures for Callsign model.
 *
 * <p>Provides factory methods for creating test callsigns representing
 * various regions, formats, and modifiers.
 *
 * <p>Usage:
 * <pre>{@code
 * // Standard US callsign
 * Callsign call = CallsignFixtures.usCallsign();
 *
 * // Portable callsign
 * Callsign portable = CallsignFixtures.portableCallsign();
 *
 * // Custom callsign
 * Callsign custom = CallsignFixtures.callsign("VK2ABC/P");
 * }</pre>
 */
public final class CallsignFixtures {

    // Common test callsigns by region
    public static final String US_CALLSIGN = "W1AW";
    public static final String UK_CALLSIGN = "G3ABC";
    public static final String JAPAN_CALLSIGN = "JA1ABC";
    public static final String AUSTRALIA_CALLSIGN = "VK2XYZ";
    public static final String GERMANY_CALLSIGN = "DL1ABC";
    public static final String BRAZIL_CALLSIGN = "PY2ABC";
    public static final String ISRAEL_CALLSIGN = "4X1ABC";
    public static final String CANADA_CALLSIGN = "VE3ABC";

    // Callsigns with suffixes
    public static final String PORTABLE_CALLSIGN = "W1AW/P";
    public static final String MOBILE_CALLSIGN = "W1AW/M";
    public static final String MARITIME_MOBILE_CALLSIGN = "W1AW/MM";
    public static final String QRP_CALLSIGN = "W1AW/QRP";
    public static final String AERONAUTICAL_MOBILE_CALLSIGN = "W1AW/AM";

    // Edge cases
    public static final String SHORT_CALLSIGN = "G3A";
    public static final String LONG_CALLSIGN = "VP2EXYZ";
    public static final String NUMERIC_PREFIX_CALLSIGN = "4X1ABC";

    // SWL (Shortwave Listener) callsigns
    public static final String NETHERLANDS_SWL = "NL9222";
    public static final String UK_RSGB_SWL = "RS123456";
    public static final String GERMANY_SWL = "DE12345";

    private CallsignFixtures() {
        // Utility class
    }

    /**
     * Creates a Callsign from the given value.
     *
     * @param value the callsign string
     * @return a new Callsign
     */
    public static Callsign callsign(String value) {
        return new Callsign(value);
    }

    /**
     * Creates a standard US callsign (W1AW).
     *
     * @return US callsign
     */
    public static Callsign usCallsign() {
        return new Callsign(US_CALLSIGN);
    }

    /**
     * Creates a UK callsign (G3ABC).
     *
     * @return UK callsign
     */
    public static Callsign ukCallsign() {
        return new Callsign(UK_CALLSIGN);
    }

    /**
     * Creates a Japan callsign (JA1ABC).
     *
     * @return Japan callsign
     */
    public static Callsign japanCallsign() {
        return new Callsign(JAPAN_CALLSIGN);
    }

    /**
     * Creates an Australia callsign (VK2XYZ).
     *
     * @return Australia callsign
     */
    public static Callsign australiaCallsign() {
        return new Callsign(AUSTRALIA_CALLSIGN);
    }

    /**
     * Creates a Germany callsign (DL1ABC).
     *
     * @return Germany callsign
     */
    public static Callsign germanyCallsign() {
        return new Callsign(GERMANY_CALLSIGN);
    }

    /**
     * Creates a Brazil callsign (PY2ABC).
     *
     * @return Brazil callsign
     */
    public static Callsign brazilCallsign() {
        return new Callsign(BRAZIL_CALLSIGN);
    }

    /**
     * Creates an Israel callsign (4X1ABC) - numeric prefix.
     *
     * @return Israel callsign
     */
    public static Callsign israelCallsign() {
        return new Callsign(ISRAEL_CALLSIGN);
    }

    /**
     * Creates a Canada callsign (VE3ABC).
     *
     * @return Canada callsign
     */
    public static Callsign canadaCallsign() {
        return new Callsign(CANADA_CALLSIGN);
    }

    /**
     * Creates a portable callsign (W1AW/P).
     *
     * @return portable callsign
     */
    public static Callsign portableCallsign() {
        return new Callsign(PORTABLE_CALLSIGN);
    }

    /**
     * Creates a mobile callsign (W1AW/M).
     *
     * @return mobile callsign
     */
    public static Callsign mobileCallsign() {
        return new Callsign(MOBILE_CALLSIGN);
    }

    /**
     * Creates a maritime mobile callsign (W1AW/MM).
     *
     * @return maritime mobile callsign
     */
    public static Callsign maritimeMobileCallsign() {
        return new Callsign(MARITIME_MOBILE_CALLSIGN);
    }

    /**
     * Creates a QRP callsign (W1AW/QRP).
     *
     * @return QRP callsign
     */
    public static Callsign qrpCallsign() {
        return new Callsign(QRP_CALLSIGN);
    }

    /**
     * Creates an aeronautical mobile callsign (W1AW/AM).
     *
     * @return aeronautical mobile callsign
     */
    public static Callsign aeronauticalMobileCallsign() {
        return new Callsign(AERONAUTICAL_MOBILE_CALLSIGN);
    }

    /**
     * Creates a callsign with a numeric prefix (4X1ABC - Israel).
     *
     * @return callsign with numeric prefix
     */
    public static Callsign numericPrefixCallsign() {
        return new Callsign(NUMERIC_PREFIX_CALLSIGN);
    }

    /**
     * Creates a Netherlands SWL callsign (NL9222).
     *
     * <p>SWL (Shortwave Listener) callsigns are issued by national amateur
     * radio societies for receive-only stations. They typically consist of
     * a country prefix followed by digits.
     *
     * @return Netherlands SWL callsign
     */
    public static Callsign netherlandsSwlCallsign() {
        return new Callsign(NETHERLANDS_SWL);
    }

    /**
     * Creates a UK RSGB SWL callsign (RS123456).
     *
     * @return UK RSGB SWL callsign
     */
    public static Callsign ukSwlCallsign() {
        return new Callsign(UK_RSGB_SWL);
    }

    /**
     * Creates a Germany SWL callsign (DE12345).
     *
     * @return Germany SWL callsign
     */
    public static Callsign germanySwlCallsign() {
        return new Callsign(GERMANY_SWL);
    }
}
