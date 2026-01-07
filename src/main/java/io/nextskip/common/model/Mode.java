package io.nextskip.common.model;

/**
 * Operating modes for amateur radio communications.
 *
 * <p>This enum provides a common vocabulary for modes across all modules
 * (spots, propagation, etc.). It serves as a shared abstraction that can be
 * extended as new modes are added.
 *
 * <p>Future enhancements may include:
 * <ul>
 *   <li>Bandwidth characteristics</li>
 *   <li>Typical frequency subbands</li>
 *   <li>Mode families (digital, voice, etc.)</li>
 * </ul>
 */
public enum Mode {

    /**
     * FT8 - Weak signal digital mode, 15-second transmit periods.
     */
    FT8("FT8", "Weak signal digital"),

    /**
     * FT4 - Fast weak signal digital mode, 7.5-second transmit periods.
     */
    FT4("FT4", "Fast weak signal digital"),

    /**
     * CW - Continuous wave / Morse code.
     */
    CW("CW", "Continuous wave / Morse"),

    /**
     * SSB - Single sideband voice.
     */
    SSB("SSB", "Single sideband voice"),

    /**
     * RTTY - Radioteletype.
     */
    RTTY("RTTY", "Radioteletype"),

    /**
     * PSK31 - Phase shift keying, 31.25 baud.
     */
    PSK31("PSK31", "Phase shift keying"),

    /**
     * JS8 - Keyboard-to-keyboard digital mode based on FT8.
     */
    JS8("JS8", "Keyboard-to-keyboard digital");

    private final String displayName;
    private final String description;

    Mode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the display name for this mode.
     *
     * @return display name (e.g., "FT8")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a description of this mode.
     *
     * @return description (e.g., "Weak signal digital")
     */
    public String getDescription() {
        return description;
    }
}
