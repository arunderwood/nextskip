package io.nextskip.common.model;

import io.nextskip.test.fixtures.CallsignFixtures;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Callsign}.
 *
 * <p>Tests callsign parsing, prefix extraction, suffix handling, SSID handling, and validation.
 * Includes property-based tests for invariants.
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CyclomaticComplexity"})
// Test data uses repeated callsign values; comprehensive test suite has high method count
class CallsignTest {

    // ===========================================
    // Basic parsing tests
    // ===========================================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testCallsign_NullOrBlankValue_ThrowsException(String input) {
        assertThatThrownBy(() -> new Callsign(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void testCallsign_LowercaseInput_NormalizesToUppercase() {
        Callsign callsign = new Callsign("w1aw");

        assertThat(callsign.value()).isEqualTo("W1AW");
    }

    @Test
    void testCallsign_MixedCaseInput_NormalizesToUppercase() {
        Callsign callsign = new Callsign("Ja1AbC");

        assertThat(callsign.value()).isEqualTo("JA1ABC");
    }

    @Test
    void testCallsign_WithLeadingTrailingSpaces_TrimsInput() {
        Callsign callsign = new Callsign("  W1AW  ");

        assertThat(callsign.value()).isEqualTo("W1AW");
    }

    // ===========================================
    // Prefix extraction tests
    // ===========================================

    @ParameterizedTest(name = "{0} -> prefix {1}")
    @CsvSource({
        "W1AW, W1",        // US
        "JA1ABC, JA1",     // Japan
        "VK2XYZ, VK2",     // Australia
        "DL1ABC, DL1",     // Germany
        "G3ABC, G3",       // UK
        "PY2ABC, PY2",     // Brazil
        "4X1ABC, 4X1",     // Israel (numeric prefix)
        "VE3ABC, VE3",     // Canada
        "W1AW/P, W1"       // Portable suffix ignored
    })
    void testGetPrefix_VariousCallsigns_ReturnsCorrectPrefix(String callsignValue, String expectedPrefix) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.getPrefix()).isEqualTo(expectedPrefix);
    }

    // ===========================================
    // Portable prefix tests
    // ===========================================

    @Test
    void testGetPortablePrefix_NoPrefix_ReturnsNull() {
        Callsign callsign = new Callsign("W1AW");

        assertThat(callsign.getPortablePrefix()).isNull();
    }

    @Test
    void testGetPortablePrefix_WithSuffixOnly_ReturnsNull() {
        Callsign callsign = new Callsign("W1AW/P");

        assertThat(callsign.getPortablePrefix()).isNull();
    }

    @Test
    void testGetPortablePrefix_SingleCharPrefix_ReturnsPrefix() {
        // M/SQ9VR = Polish operator operating from England
        Callsign callsign = new Callsign("M/SQ9VR");

        assertThat(callsign.getPortablePrefix()).isEqualTo("M");
    }

    @Test
    void testGetPortablePrefix_TwoCharPrefix_ReturnsPrefix() {
        // VK/JA1ABC = Japanese operator operating from Australia
        Callsign callsign = new Callsign("VK/JA1ABC");

        assertThat(callsign.getPortablePrefix()).isEqualTo("VK");
    }

    @Test
    void testGetPortablePrefix_ThreeCharPrefix_ReturnsPrefix() {
        // EA8/G3ABC = UK operator operating from Canary Islands
        Callsign callsign = new Callsign("EA8/G3ABC");

        assertThat(callsign.getPortablePrefix()).isEqualTo("EA8");
    }

    @Test
    void testGetPortablePrefix_FourCharPrefix_ReturnsNull() {
        // W1AW/P is not a portable prefix - first part is too long
        Callsign callsign = new Callsign("W1AW/P");

        assertThat(callsign.getPortablePrefix()).isNull();
    }

    @Test
    void testGetPortablePrefix_WithSuffix_ReturnsPrefix() {
        // F/W1AW/P = US operator operating from France, portable
        Callsign callsign = new Callsign("F/W1AW/P");

        assertThat(callsign.getPortablePrefix()).isEqualTo("F");
    }

    @Test
    void testGetPortablePrefix_ShortSecondPart_ReturnsNull() {
        // M/AB is too short to be a callsign - not treated as portable prefix
        Callsign callsign = new Callsign("M/AB");

        assertThat(callsign.getPortablePrefix()).isNull();
    }

    @Test
    void testHasPortablePrefix_WithPrefix_ReturnsTrue() {
        Callsign callsign = new Callsign("F/W1AW");

        assertThat(callsign.hasPortablePrefix()).isTrue();
    }

    @Test
    void testHasPortablePrefix_WithoutPrefix_ReturnsFalse() {
        Callsign callsign = new Callsign("W1AW/P");

        assertThat(callsign.hasPortablePrefix()).isFalse();
    }

    // ===========================================
    // Base call tests with portable prefix
    // ===========================================

    @Test
    void testGetBaseCall_WithPortablePrefix_ReturnsBaseCall() {
        // M/SQ9VR -> base call is SQ9VR
        Callsign callsign = new Callsign("M/SQ9VR");

        assertThat(callsign.getBaseCall()).isEqualTo("SQ9VR");
    }

    @Test
    void testGetBaseCall_WithPortablePrefixAndSuffix_ReturnsBaseCall() {
        // F/W1AW/P -> base call is W1AW
        Callsign callsign = new Callsign("F/W1AW/P");

        assertThat(callsign.getBaseCall()).isEqualTo("W1AW");
    }

    @Test
    void testGetBaseCall_WithPortablePrefixAndQrpSuffix_ReturnsBaseCall() {
        // VK/JA1ABC/QRP -> base call is JA1ABC
        Callsign callsign = new Callsign("VK/JA1ABC/QRP");

        assertThat(callsign.getBaseCall()).isEqualTo("JA1ABC");
    }

    // ===========================================
    // Base call tests
    // ===========================================

    @Test
    void testGetBaseCall_NoSuffix_ReturnsFullCallsign() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.getBaseCall()).isEqualTo("W1AW");
    }

    @Test
    void testGetBaseCall_WithPortableSuffix_ReturnsWithoutSuffix() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.getBaseCall()).isEqualTo("W1AW");
    }

    @Test
    void testGetBaseCall_WithMobileSuffix_ReturnsWithoutSuffix() {
        Callsign callsign = CallsignFixtures.mobileCallsign();

        assertThat(callsign.getBaseCall()).isEqualTo("W1AW");
    }

    @Test
    void testGetBaseCall_WithQrpSuffix_ReturnsWithoutSuffix() {
        Callsign callsign = CallsignFixtures.qrpCallsign();

        assertThat(callsign.getBaseCall()).isEqualTo("W1AW");
    }

    // ===========================================
    // Suffix tests
    // ===========================================

    @ParameterizedTest(name = "{0} -> suffix {1}")
    @CsvSource({
        "W1AW/P, P",           // Portable
        "W1AW/M, M",           // Mobile
        "W1AW/MM, MM",         // Maritime mobile
        "W1AW/QRP, QRP",       // QRP
        "W1AW/7, 7",           // Digit suffix
        "W1AW/P/QRP, P/QRP",   // Multiple suffixes
        "F/W1AW/P, P",         // Portable prefix with suffix
        "VK/JA1ABC/QRP, QRP"   // Portable prefix with QRP
    })
    void testGetSuffix_WithSuffix_ReturnsSuffix(String callsignValue, String expectedSuffix) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.getSuffix()).isEqualTo(expectedSuffix);
    }

    @ParameterizedTest(name = "{0} has null suffix")
    @ValueSource(strings = {"W1AW", "W1AW/", "M/SQ9VR"})
    void testGetSuffix_NoSuffix_ReturnsNull(String callsignValue) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.getSuffix()).isNull();
    }

    // ===========================================
    // Suffix type tests (with portable prefix)
    // ===========================================

    @Test
    void testIsPortable_PortablePrefixWithPSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("F/W1AW/P");

        assertThat(callsign.isPortable()).isTrue();
    }

    @Test
    void testIsMobile_PortablePrefixWithMSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("M/SQ9VR/M");

        assertThat(callsign.isMobile()).isTrue();
    }

    @Test
    void testIsQrp_PortablePrefixWithQrpSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("VK/JA1ABC/QRP");

        assertThat(callsign.isQrp()).isTrue();
    }

    @Test
    void testHasSuffix_PortablePrefixNoSuffix_ReturnsFalse() {
        Callsign callsign = new Callsign("M/SQ9VR");

        assertThat(callsign.hasSuffix()).isFalse();
    }

    @Test
    void testHasSuffix_PortablePrefixWithSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("F/W1AW/P");

        assertThat(callsign.hasSuffix()).isTrue();
    }

    // ===========================================
    // Has suffix tests
    // ===========================================

    @Test
    void testHasSuffix_NoSuffix_ReturnsFalse() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.hasSuffix()).isFalse();
    }

    @Test
    void testHasSuffix_WithSuffix_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.hasSuffix()).isTrue();
    }

    // ===========================================
    // Suffix type tests
    // ===========================================

    @Test
    void testIsPortable_PortableCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.isPortable()).isTrue();
    }

    @Test
    void testIsPortable_NonPortableCallsign_ReturnsFalse() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.isPortable()).isFalse();
    }

    @Test
    void testIsMobile_MobileCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.mobileCallsign();

        assertThat(callsign.isMobile()).isTrue();
    }

    @Test
    void testIsMobile_NonMobileCallsign_ReturnsFalse() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.isMobile()).isFalse();
    }

    @Test
    void testIsMaritimeMobile_MaritimeMobileCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.maritimeMobileCallsign();

        assertThat(callsign.isMaritimeMobile()).isTrue();
    }

    @Test
    void testIsMaritimeMobile_NonMaritimeMobileCallsign_ReturnsFalse() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.isMaritimeMobile()).isFalse();
    }

    @Test
    void testIsQrp_QrpCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.qrpCallsign();

        assertThat(callsign.isQrp()).isTrue();
    }

    @Test
    void testIsQrp_NonQrpCallsign_ReturnsFalse() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.isQrp()).isFalse();
    }

    // ===========================================
    // SWL (Shortwave Listener) tests
    // ===========================================

    @ParameterizedTest(name = "{0} is SWL")
    @ValueSource(strings = {"NL9222", "RS123456", "DE12345", "nl9222"})
    void testIsSwl_ValidSwlFormats_ReturnsTrue(String callsignValue) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.isSwl()).isTrue();
    }

    @ParameterizedTest(name = "{0} is not SWL")
    @ValueSource(strings = {"W1AW", "NL12", "NL1234567", "N12345"})
    void testIsSwl_InvalidSwlFormats_ReturnsFalse(String callsignValue) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.isSwl()).isFalse();
    }

    // ===========================================
    // Validation tests
    // ===========================================

    @ParameterizedTest(name = "{0} is valid")
    @ValueSource(strings = {
        "W1AW",       // Standard US
        "4X1ABC",     // Numeric prefix
        "W1AW/P",     // With suffix
        "K1A",        // Special event 1x1
        "G3A",        // Very short
        "W1",         // Two char ends in digit
        "M/SQ9VR",    // Portable prefix
        "F/W1AW/P"    // Portable prefix with suffix
    })
    void testIsValid_ValidCallsigns_ReturnsTrue(String callsignValue) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.isValid()).isTrue();
    }

    @ParameterizedTest(name = "{0} is invalid")
    @ValueSource(strings = {
        "W",          // Single char - too short
        "W1ABCDEFG",  // Too long
        "WABC",       // No digit
        "12345",      // No letter
        "Q1ABC"       // Q prefix reserved
    })
    void testIsValid_InvalidCallsigns_ReturnsFalse(String callsignValue) {
        Callsign callsign = new Callsign(callsignValue);

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_PortablePrefixInvalidBaseCall_ReturnsFalse() {
        // F/Q1ABC - portable prefix with Q-prefix base call (invalid)
        Callsign callsign = new Callsign("F/Q1ABC");

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testValidate_PortablePrefix_ValidatesBaseCall() {
        // M/SQ9VR - validates the base call SQ9VR, not M
        Callsign callsign = new Callsign("M/SQ9VR");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidate_PortablePrefixQInBase_ReturnsQPrefixFailure() {
        // VK/Q1ABC - base call starts with Q
        Callsign callsign = new Callsign("VK/Q1ABC");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.Q_PREFIX);
    }

    // ===========================================
    // Validate method tests (detailed results)
    // ===========================================

    @Test
    void testValidate_ValidCallsign_ReturnsValid() {
        Callsign callsign = CallsignFixtures.usCallsign();

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isTrue();
        assertThat(result.failure()).isNull();
    }

    @Test
    void testValidate_TooShort_ReturnsTooShortFailure() {
        Callsign callsign = new Callsign("W");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.TOO_SHORT);
    }

    @Test
    void testValidate_TooLong_ReturnsTooLongFailure() {
        Callsign callsign = new Callsign("W1ABCDEFG");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.TOO_LONG);
    }

    @Test
    void testValidate_NoLetter_ReturnsNoLetterFailure() {
        Callsign callsign = new Callsign("12345");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.NO_LETTER);
    }

    @Test
    void testValidate_NoDigit_ReturnsNoDigitFailure() {
        Callsign callsign = new Callsign("WABC");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.NO_DIGIT);
    }

    @Test
    void testValidate_QPrefix_ReturnsQPrefixFailure() {
        Callsign callsign = new Callsign("Q1ABC");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.Q_PREFIX);
    }

    @Test
    void testValidate_LastCharDigit_ReturnsValid() {
        // Callsigns ending in digits are now valid (supports SWL callsigns)
        Callsign callsign = new Callsign("W1AB1");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidate_TwoCharEndsInDigit_ReturnsValid() {
        // Two-character callsigns ending in digits are now valid
        Callsign callsign = new Callsign("W1");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidationFailure_GetDescription_ReturnsDescription() {
        Callsign.ValidationFailure failure = Callsign.ValidationFailure.TOO_SHORT;

        assertThat(failure.getDescription()).isEqualTo("Base call less than 2 characters");
    }

    // ===========================================
    // toString tests
    // ===========================================

    @Test
    void testToString_ReturnsValue() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.toString()).isEqualTo("W1AW");
    }

    @Test
    void testToString_WithSuffix_ReturnsFullValue() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.toString()).isEqualTo("W1AW/P");
    }

    // ===========================================
    // Property-based tests using jqwik
    // ===========================================

    @Property
    void valueIsAlwaysUppercase(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign(input);

        assertThat(callsign.value()).isEqualTo(callsign.value().toUpperCase(Locale.ROOT));
    }

    @Property
    void valueIsAlwaysTrimmed(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign("  " + input + "  ");

        assertThat(callsign.value()).doesNotStartWith(" ");
        assertThat(callsign.value()).doesNotEndWith(" ");
    }

    @Property
    void baseCallNeverContainsSlash(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign(input);

        assertThat(callsign.getBaseCall()).doesNotContain("/");
    }

    @Property
    void prefixIsSubsetOfBaseCall(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign(input);

        String prefix = callsign.getPrefix();
        String baseCall = callsign.getBaseCall();

        assertThat(baseCall).startsWith(prefix);
    }

    @Property
    void suffixOnlyPresentWhenSlashInValue(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign(input);

        if (!callsign.value().contains("/")) {
            assertThat(callsign.getSuffix()).isNull();
            assertThat(callsign.hasSuffix()).isFalse();
        }
    }

    @Property
    void validCallsignsHaveLettersAndDigits(@ForAll("validCallsigns") String input) {
        Callsign callsign = new Callsign(input);

        if (callsign.isValid()) {
            String baseCall = callsign.getBaseCall();
            boolean hasLetter = baseCall.chars().anyMatch(Character::isLetter);
            boolean hasDigit = baseCall.chars().anyMatch(Character::isDigit);
            assertThat(hasLetter && hasDigit).isTrue();
        }
    }

    @Provide
    Arbitrary<String> validCallsigns() {
        return Arbitraries.of(
                "W1AW", "JA1ABC", "VK2XYZ", "DL1ABC", "G3ABC",
                "PY2ABC", "4X1ABC", "VE3ABC", "W1AW/P", "W1AW/M",
                "W1AW/MM", "W1AW/QRP", "K2DEF", "N3XYZ", "AA1AA"
        );
    }

    // ===========================================
    // APRS SSID tests
    // ===========================================

    @Test
    void testGetSsid_SsidZero_ReturnsZero() {
        Callsign callsign = new Callsign("K9TRV-0");

        assertThat(callsign.getSsid()).isEqualTo(0);
    }

    @Test
    void testGetSsid_SsidFifteen_ReturnsFifteen() {
        Callsign callsign = new Callsign("K9TRV-15");

        assertThat(callsign.getSsid()).isEqualTo(15);
    }

    @Test
    void testGetSsid_ValidSsid_ReturnsValue() {
        assertThat(new Callsign("K9TRV-4").getSsid()).isEqualTo(4);
        assertThat(new Callsign("W1AW-9").getSsid()).isEqualTo(9);
    }

    @Test
    void testGetSsid_NoSsid_ReturnsNull() {
        assertThat(new Callsign("W1AW").getSsid()).isNull();
        assertThat(new Callsign("W1AW/P").getSsid()).isNull();
    }

    @Test
    void testGetSsid_InvalidSsid_ReturnsNull() {
        // -16 and above are not valid SSIDs
        assertThat(new Callsign("K9TRV-16").getSsid()).isNull();
        assertThat(new Callsign("K9TRV-100").getSsid()).isNull();
    }

    @Test
    void testGetSsid_LettersAfterHyphen_ReturnsNull() {
        // K9TRV-A is not an SSID (could be a suffix notation)
        assertThat(new Callsign("K9TRV-A").getSsid()).isNull();
    }

    @Test
    void testHasSsid_WithSsid_ReturnsTrue() {
        assertThat(new Callsign("K9TRV-4").hasSsid()).isTrue();
    }

    @Test
    void testHasSsid_WithoutSsid_ReturnsFalse() {
        assertThat(new Callsign("W1AW").hasSsid()).isFalse();
        assertThat(new Callsign("W1AW/P").hasSsid()).isFalse();
    }

    // ===========================================
    // Base call extraction with SSID
    // ===========================================

    @Test
    void testGetBaseCall_WithSsid_StripsSsid() {
        assertThat(new Callsign("K9TRV-4").getBaseCall()).isEqualTo("K9TRV");
        assertThat(new Callsign("W1AW-15").getBaseCall()).isEqualTo("W1AW");
    }

    @Test
    void testGetBaseCall_WithSsidAndSuffix_StripsBoth() {
        // Edge case: SSID + suffix (rare but possible)
        assertThat(new Callsign("K9TRV-4/P").getBaseCall()).isEqualTo("K9TRV");
    }

    @Test
    void testGetPrefix_WithSsid_IgnoresSsid() {
        Callsign callsign = new Callsign("K9TRV-4");

        assertThat(callsign.getPrefix()).isEqualTo("K9");
    }

    // ===========================================
    // SSID + suffix interaction tests
    // ===========================================

    @Test
    void testIsPortable_WithSsidAndPortableSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("K9TRV-4/P");

        assertThat(callsign.isPortable()).isTrue();
    }

    @Test
    void testIsPortable_WithSsidOnly_ReturnsFalse() {
        Callsign callsign = new Callsign("K9TRV-4");

        assertThat(callsign.isPortable()).isFalse();
    }

    @Test
    void testIsMobile_WithSsidAndMobileSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("K9TRV-4/M");

        assertThat(callsign.isMobile()).isTrue();
    }

    @Test
    void testGetSuffix_WithSsidOnly_ReturnsNull() {
        Callsign callsign = new Callsign("K9TRV-4");

        assertThat(callsign.getSuffix()).isNull();
    }

    @Test
    void testGetSuffix_WithSsidAndSuffix_ReturnsSlashSuffix() {
        Callsign callsign = new Callsign("K9TRV-4/P");

        assertThat(callsign.getSuffix()).isEqualTo("P");
    }

    @Test
    void testHasSuffix_WithSsidOnly_ReturnsFalse() {
        Callsign callsign = new Callsign("K9TRV-4");

        assertThat(callsign.hasSuffix()).isFalse();
    }

    @Test
    void testHasSuffix_WithSsidAndSuffix_ReturnsTrue() {
        Callsign callsign = new Callsign("K9TRV-4/P");

        assertThat(callsign.hasSuffix()).isTrue();
    }

    @Test
    void testGetSsid_WithSsidAndSuffix_ReturnsSsid() {
        Callsign callsign = new Callsign("K9TRV-4/P");

        assertThat(callsign.getSsid()).isEqualTo(4);
    }

    // ===========================================
    // Validation with SSID
    // ===========================================

    @Test
    void testValidate_WithSsid_ValidatesBaseCall() {
        assertThat(new Callsign("K9TRV-4").validate().isValid()).isTrue();
        assertThat(new Callsign("W1AW-0").validate().isValid()).isTrue();
    }

    @Test
    void testValidate_InvalidBaseWithValidSsid_ReturnsFailure() {
        // Q prefix is invalid, even with valid SSID
        Callsign callsign = new Callsign("Q1ABC-4");

        assertThat(callsign.validate().isValid()).isFalse();
        assertThat(callsign.validate().failure())
                .isEqualTo(Callsign.ValidationFailure.Q_PREFIX);
    }

    @Test
    void testValidate_InvalidSsid_ValidatesAsNormal() {
        // -16 is not a valid SSID, so it's kept as part of the callsign
        // K9TRV-16 = 8 chars which exceeds the 7-char max
        Callsign callsign = new Callsign("K9TRV-16");

        assertThat(callsign.getSsid()).isNull();
        assertThat(callsign.validate().failure())
                .isEqualTo(Callsign.ValidationFailure.TOO_LONG);
    }

    @Test
    void testValidate_InvalidSsid_ShorterCallsign_IsValid() {
        // -16 is not a valid SSID, so it's kept as part of the callsign
        // W1A-16 = 6 chars, ends in digit - now valid since LAST_CHAR_NOT_LETTER removed
        Callsign callsign = new Callsign("W1A-16");

        assertThat(callsign.getSsid()).isNull();
        assertThat(callsign.validate().isValid()).isTrue();
    }

    // ===========================================
    // SSID property-based tests
    // ===========================================

    @Property
    void ssidInValidRange_AlwaysReturnsCorrectValue(
            @ForAll("validSsids") int ssid) {
        Callsign callsign = new Callsign("K9TRV-" + ssid);

        assertThat(callsign.getSsid()).isEqualTo(ssid);
        assertThat(callsign.hasSsid()).isTrue();
    }

    @Property
    void ssidOutsideValidRange_AlwaysReturnsNull(
            @ForAll("invalidSsids") int invalidSsid) {
        Callsign callsign = new Callsign("K9TRV-" + invalidSsid);

        assertThat(callsign.getSsid()).isNull();
        assertThat(callsign.hasSsid()).isFalse();
    }

    @Property
    void baseCallNeverContainsSsid_ForValidSsids(
            @ForAll("validSsids") int ssid) {
        Callsign callsign = new Callsign("K9TRV-" + ssid);

        assertThat(callsign.getBaseCall()).isEqualTo("K9TRV");
        assertThat(callsign.getBaseCall()).doesNotContain("-");
    }

    @Provide
    Arbitrary<Integer> validSsids() {
        return Arbitraries.integers().between(0, 15);
    }

    @Provide
    Arbitrary<Integer> invalidSsids() {
        return Arbitraries.integers().between(16, 999);
    }
}
