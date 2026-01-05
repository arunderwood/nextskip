package io.nextskip.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Mode} enum.
 */
@DisplayName("Mode enum")
class ModeTest {

    @ParameterizedTest
    @EnumSource(Mode.class)
    @DisplayName("getDisplayName_AllModes_ReturnsNonEmptyString")
    void testGetDisplayName_AllModes_ReturnsNonEmptyString(Mode mode) {
        assertThat(mode.getDisplayName())
                .isNotNull()
                .isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    @DisplayName("getDescription_AllModes_ReturnsNonEmptyString")
    void testGetDescription_AllModes_ReturnsNonEmptyString(Mode mode) {
        assertThat(mode.getDescription())
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("FT8_Properties_MatchExpected")
    void testFT8_Properties_MatchExpected() {
        assertThat(Mode.FT8.getDisplayName()).isEqualTo("FT8");
        assertThat(Mode.FT8.getDescription()).isEqualTo("Weak signal digital");
    }

    @Test
    @DisplayName("CW_Properties_MatchExpected")
    void testCW_Properties_MatchExpected() {
        assertThat(Mode.CW.getDisplayName()).isEqualTo("CW");
        assertThat(Mode.CW.getDescription()).isEqualTo("Continuous wave / Morse");
    }

    @Test
    @DisplayName("values_ContainsExpectedModes")
    void testValues_ContainsExpectedModes() {
        assertThat(Mode.values())
                .contains(Mode.FT8, Mode.FT4, Mode.CW, Mode.SSB, Mode.RTTY, Mode.PSK31, Mode.JS8);
    }

    @Test
    @DisplayName("valueOf_ValidName_ReturnsMode")
    void testValueOf_ValidName_ReturnsMode() {
        assertThat(Mode.valueOf("FT8")).isEqualTo(Mode.FT8);
        assertThat(Mode.valueOf("CW")).isEqualTo(Mode.CW);
    }
}
