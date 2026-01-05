package io.nextskip.spots.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModeWindow} enum.
 *
 * <p>Tests cover mode-specific window durations, forMode() lookup, and baseline calculations.
 */
class ModeWindowTest {

    // =========================================================================
    // Enum Values Tests
    // =========================================================================

    @Nested
    class EnumValuesTests {

        @Test
        void testEnumValues_HasFiveModeWindows() {
            assertThat(ModeWindow.values())
                    .as("Should have 5 mode windows (FT8, FT4, CW, SSB, DEFAULT)")
                    .hasSize(5);
        }
    }

    // =========================================================================
    // FT8 Window Tests
    // =========================================================================

    @Nested
    class Ft8WindowTests {

        @Test
        void testFT8_CurrentWindowIs15Minutes() {
            assertThat(ModeWindow.FT8.getCurrentWindow())
                    .as("FT8 current window should be 15 minutes")
                    .isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        void testFT8_BaselineWindowIs1Hour() {
            assertThat(ModeWindow.FT8.getBaselineWindow())
                    .as("FT8 baseline window should be 1 hour")
                    .isEqualTo(Duration.ofHours(1));
        }

        @Test
        void testFT8_BaselineWindowCountIs4() {
            assertThat(ModeWindow.FT8.getBaselineWindowCount())
                    .as("FT8 baseline should contain 4 current windows (60min / 15min)")
                    .isEqualTo(4);
        }
    }

    // =========================================================================
    // FT4 Window Tests
    // =========================================================================

    @Nested
    class Ft4WindowTests {

        @Test
        void testFT4_HasSameConfigAsFT8() {
            assertThat(ModeWindow.FT4.getCurrentWindow())
                    .as("FT4 should have same current window as FT8")
                    .isEqualTo(ModeWindow.FT8.getCurrentWindow());

            assertThat(ModeWindow.FT4.getBaselineWindow())
                    .as("FT4 should have same baseline window as FT8")
                    .isEqualTo(ModeWindow.FT8.getBaselineWindow());
        }
    }

    // =========================================================================
    // CW Window Tests
    // =========================================================================

    @Nested
    class CwWindowTests {

        @Test
        void testCW_CurrentWindowIs30Minutes() {
            assertThat(ModeWindow.CW.getCurrentWindow())
                    .as("CW current window should be 30 minutes")
                    .isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        void testCW_BaselineWindowIs2Hours() {
            assertThat(ModeWindow.CW.getBaselineWindow())
                    .as("CW baseline window should be 2 hours")
                    .isEqualTo(Duration.ofHours(2));
        }

        @Test
        void testCW_BaselineWindowCountIs4() {
            assertThat(ModeWindow.CW.getBaselineWindowCount())
                    .as("CW baseline should contain 4 current windows (120min / 30min)")
                    .isEqualTo(4);
        }
    }

    // =========================================================================
    // SSB Window Tests
    // =========================================================================

    @Nested
    class SsbWindowTests {

        @Test
        void testSSB_CurrentWindowIs60Minutes() {
            assertThat(ModeWindow.SSB.getCurrentWindow())
                    .as("SSB current window should be 60 minutes")
                    .isEqualTo(Duration.ofMinutes(60));
        }

        @Test
        void testSSB_BaselineWindowIs3Hours() {
            assertThat(ModeWindow.SSB.getBaselineWindow())
                    .as("SSB baseline window should be 3 hours")
                    .isEqualTo(Duration.ofHours(3));
        }

        @Test
        void testSSB_BaselineWindowCountIs3() {
            assertThat(ModeWindow.SSB.getBaselineWindowCount())
                    .as("SSB baseline should contain 3 current windows (180min / 60min)")
                    .isEqualTo(3);
        }
    }

    // =========================================================================
    // DEFAULT Window Tests
    // =========================================================================

    @Nested
    class DefaultWindowTests {

        @Test
        void testDEFAULT_CurrentWindowIs30Minutes() {
            assertThat(ModeWindow.DEFAULT.getCurrentWindow())
                    .as("DEFAULT current window should be 30 minutes")
                    .isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        void testDEFAULT_BaselineWindowIs1Hour() {
            assertThat(ModeWindow.DEFAULT.getBaselineWindow())
                    .as("DEFAULT baseline window should be 1 hour")
                    .isEqualTo(Duration.ofHours(1));
        }

        @Test
        void testDEFAULT_BaselineWindowCountIs2() {
            assertThat(ModeWindow.DEFAULT.getBaselineWindowCount())
                    .as("DEFAULT baseline should contain 2 current windows (60min / 30min)")
                    .isEqualTo(2);
        }
    }

    // =========================================================================
    // forMode() Tests
    // =========================================================================

    @Nested
    class ForModeTests {

        @ParameterizedTest
        @ValueSource(strings = {"FT8", "ft8", "Ft8", "fT8"})
        void testForMode_FT8_CaseInsensitive(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return FT8", mode)
                    .isEqualTo(ModeWindow.FT8);
        }

        @ParameterizedTest
        @ValueSource(strings = {"FT4", "ft4", "Ft4"})
        void testForMode_FT4_CaseInsensitive(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return FT4", mode)
                    .isEqualTo(ModeWindow.FT4);
        }

        @ParameterizedTest
        @ValueSource(strings = {"CW", "cw", "Cw"})
        void testForMode_CW_CaseInsensitive(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return CW", mode)
                    .isEqualTo(ModeWindow.CW);
        }

        @ParameterizedTest
        @ValueSource(strings = {"SSB", "ssb", "Ssb"})
        void testForMode_SSB_CaseInsensitive(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return SSB", mode)
                    .isEqualTo(ModeWindow.SSB);
        }

        @ParameterizedTest
        @ValueSource(strings = {"USB", "usb", "Usb"})
        void testForMode_USB_MapsToSSB(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return SSB", mode)
                    .isEqualTo(ModeWindow.SSB);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LSB", "lsb", "Lsb"})
        void testForMode_LSB_MapsToSSB(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return SSB", mode)
                    .isEqualTo(ModeWindow.SSB);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        void testForMode_NullOrBlank_ReturnsDEFAULT(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return DEFAULT", mode)
                    .isEqualTo(ModeWindow.DEFAULT);
        }

        @ParameterizedTest
        @ValueSource(strings = {"RTTY", "PSK31", "JT65", "OLIVIA", "UNKNOWN"})
        void testForMode_UnknownModes_ReturnsDEFAULT(String mode) {
            assertThat(ModeWindow.forMode(mode))
                    .as("forMode('%s') should return DEFAULT for unknown mode", mode)
                    .isEqualTo(ModeWindow.DEFAULT);
        }

        @Test
        void testForMode_WithLeadingTrailingSpaces_Trims() {
            assertThat(ModeWindow.forMode("  FT8  ")).isEqualTo(ModeWindow.FT8);
            assertThat(ModeWindow.forMode("\tCW\t")).isEqualTo(ModeWindow.CW);
        }
    }

    // =========================================================================
    // Window Duration Invariants
    // =========================================================================

    @Nested
    class WindowDurationInvariantTests {

        @Test
        void testAllModes_BaselineIsMultipleOfCurrentWindow() {
            for (ModeWindow mode : ModeWindow.values()) {
                long currentMinutes = mode.getCurrentWindow().toMinutes();
                long baselineMinutes = mode.getBaselineWindow().toMinutes();

                assertThat(baselineMinutes % currentMinutes)
                        .as("%s baseline (%d min) should be evenly divisible by current window (%d min)",
                                mode, baselineMinutes, currentMinutes)
                        .isZero();
            }
        }

        @Test
        void testAllModes_BaselineIsLongerThanCurrentWindow() {
            for (ModeWindow mode : ModeWindow.values()) {
                assertThat(mode.getBaselineWindow())
                        .as("%s baseline should be longer than current window", mode)
                        .isGreaterThan(mode.getCurrentWindow());
            }
        }

        @Test
        void testAllModes_CurrentWindowIsPositive() {
            for (ModeWindow mode : ModeWindow.values()) {
                assertThat(mode.getCurrentWindow())
                        .as("%s current window should be positive", mode)
                        .isPositive();
            }
        }

        @Test
        void testAllModes_BaselineWindowCountIsPositive() {
            for (ModeWindow mode : ModeWindow.values()) {
                assertThat(mode.getBaselineWindowCount())
                        .as("%s baseline window count should be positive", mode)
                        .isPositive();
            }
        }

        @Test
        void testFT8_HasShortestCurrentWindow() {
            Duration ft8Window = ModeWindow.FT8.getCurrentWindow();

            for (ModeWindow mode : ModeWindow.values()) {
                if (mode != ModeWindow.FT8 && mode != ModeWindow.FT4) {
                    assertThat(mode.getCurrentWindow())
                            .as("%s current window should be >= FT8", mode)
                            .isGreaterThanOrEqualTo(ft8Window);
                }
            }
        }

        @Test
        void testSSB_HasLongestCurrentWindow() {
            Duration ssbWindow = ModeWindow.SSB.getCurrentWindow();

            for (ModeWindow mode : ModeWindow.values()) {
                assertThat(mode.getCurrentWindow())
                        .as("%s current window should be <= SSB", mode)
                        .isLessThanOrEqualTo(ssbWindow);
            }
        }
    }

    // =========================================================================
    // toString() Tests
    // =========================================================================

    @Nested
    class ToStringTests {

        @Test
        void testToString_ContainsModeName() {
            assertThat(ModeWindow.FT8.toString()).contains("FT8");
            assertThat(ModeWindow.CW.toString()).contains("CW");
            assertThat(ModeWindow.SSB.toString()).contains("SSB");
        }

        @Test
        void testToString_ContainsCurrentAndBaselineInfo() {
            String result = ModeWindow.FT8.toString();

            assertThat(result)
                    .contains("current=")
                    .contains("baseline=")
                    .contains("15m")    // current window
                    .contains("60m");   // baseline window
        }

        @Test
        void testToString_FormattedCorrectly() {
            assertThat(ModeWindow.CW.toString())
                    .isEqualTo("CW[current=30m, baseline=120m]");
        }
    }

    // =========================================================================
    // Mode Window Ordering Tests
    // =========================================================================

    @Nested
    class ModeWindowOrderingTests {

        @Test
        void testDigitalModes_HaveShorterWindowsThanVoice() {
            // Digital modes (FT8, FT4) should have shorter windows than voice (SSB)
            Duration ssbWindow = ModeWindow.SSB.getCurrentWindow();

            assertThat(ModeWindow.FT8.getCurrentWindow())
                    .as("FT8 window should be shorter than SSB")
                    .isLessThan(ssbWindow);

            assertThat(ModeWindow.FT4.getCurrentWindow())
                    .as("FT4 window should be shorter than SSB")
                    .isLessThan(ssbWindow);
        }

        @Test
        void testCW_HasIntermediateWindow() {
            // CW should be between digital (FT8) and voice (SSB)
            Duration ft8Window = ModeWindow.FT8.getCurrentWindow();
            Duration ssbWindow = ModeWindow.SSB.getCurrentWindow();
            Duration cwWindow = ModeWindow.CW.getCurrentWindow();

            assertThat(cwWindow)
                    .as("CW window should be >= FT8")
                    .isGreaterThanOrEqualTo(ft8Window);

            assertThat(cwWindow)
                    .as("CW window should be <= SSB")
                    .isLessThanOrEqualTo(ssbWindow);
        }
    }
}
