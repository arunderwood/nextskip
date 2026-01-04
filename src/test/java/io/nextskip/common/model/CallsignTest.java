package io.nextskip.common.model;

import io.nextskip.test.fixtures.CallsignFixtures;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

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

    @Test
    void testCallsign_NullValue_ThrowsException() {
        assertThatThrownBy(() -> new Callsign(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void testCallsign_BlankValue_ThrowsException() {
        assertThatThrownBy(() -> new Callsign("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void testCallsign_EmptyValue_ThrowsException() {
        assertThatThrownBy(() -> new Callsign(""))
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

    @Test
    void testGetPrefix_StandardUsCallsign_ReturnsW1() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("W1");
    }

    @Test
    void testGetPrefix_JapanCallsign_ReturnsJA1() {
        Callsign callsign = CallsignFixtures.japanCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("JA1");
    }

    @Test
    void testGetPrefix_AustraliaCallsign_ReturnsVK2() {
        Callsign callsign = CallsignFixtures.australiaCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("VK2");
    }

    @Test
    void testGetPrefix_GermanyCallsign_ReturnsDL1() {
        Callsign callsign = CallsignFixtures.germanyCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("DL1");
    }

    @Test
    void testGetPrefix_UkCallsign_ReturnsG3() {
        Callsign callsign = CallsignFixtures.ukCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("G3");
    }

    @Test
    void testGetPrefix_BrazilCallsign_ReturnsPY2() {
        Callsign callsign = CallsignFixtures.brazilCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("PY2");
    }

    @Test
    void testGetPrefix_NumericPrefix_Returns4X1() {
        Callsign callsign = CallsignFixtures.israelCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("4X1");
    }

    @Test
    void testGetPrefix_CanadaCallsign_ReturnsVE3() {
        Callsign callsign = CallsignFixtures.canadaCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("VE3");
    }

    @Test
    void testGetPrefix_PortableCallsign_IgnoresSuffix() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.getPrefix()).isEqualTo("W1");
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

    @Test
    void testGetSuffix_NoSuffix_ReturnsNull() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.getSuffix()).isNull();
    }

    @Test
    void testGetSuffix_PortableSuffix_ReturnsP() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.getSuffix()).isEqualTo("P");
    }

    @Test
    void testGetSuffix_MobileSuffix_ReturnsM() {
        Callsign callsign = CallsignFixtures.mobileCallsign();

        assertThat(callsign.getSuffix()).isEqualTo("M");
    }

    @Test
    void testGetSuffix_MaritimeMobileSuffix_ReturnsMM() {
        Callsign callsign = CallsignFixtures.maritimeMobileCallsign();

        assertThat(callsign.getSuffix()).isEqualTo("MM");
    }

    @Test
    void testGetSuffix_QrpSuffix_ReturnsQRP() {
        Callsign callsign = CallsignFixtures.qrpCallsign();

        assertThat(callsign.getSuffix()).isEqualTo("QRP");
    }

    @Test
    void testGetSuffix_TrailingSlash_ReturnsNull() {
        Callsign callsign = new Callsign("W1AW/");

        assertThat(callsign.getSuffix()).isNull();
    }

    @Test
    void testGetSuffix_DigitSuffix_ReturnsSeven() {
        Callsign callsign = new Callsign("W1AW/7");

        assertThat(callsign.getSuffix()).isEqualTo("7");
    }

    @Test
    void testGetSuffix_MultipleSuffixes_ReturnsFirstPart() {
        // When there are multiple slashes, suffix returns everything after first slash
        Callsign callsign = new Callsign("W1AW/P/QRP");

        assertThat(callsign.getSuffix()).isEqualTo("P/QRP");
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
    // Validation tests
    // ===========================================

    @Test
    void testIsValid_StandardCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.usCallsign();

        assertThat(callsign.isValid()).isTrue();
    }

    @Test
    void testIsValid_NumericPrefixCallsign_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.israelCallsign();

        assertThat(callsign.isValid()).isTrue();
    }

    @Test
    void testIsValid_CallsignWithSuffix_ReturnsTrue() {
        Callsign callsign = CallsignFixtures.portableCallsign();

        assertThat(callsign.isValid()).isTrue();
    }

    @Test
    void testIsValid_SpecialEvent1x1_K1A_ReturnsTrue() {
        Callsign callsign = new Callsign("K1A");

        assertThat(callsign.isValid()).isTrue();
    }

    @Test
    void testIsValid_VeryShort_G3A_ReturnsTrue() {
        Callsign callsign = new Callsign("G3A");

        assertThat(callsign.isValid()).isTrue();
    }

    @Test
    void testIsValid_TwoCharEndsInDigit_ReturnsFalse() {
        Callsign callsign = new Callsign("W1");

        // Fails because last char is not a letter (ITU guideline)
        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_SingleChar_ReturnsFalse() {
        Callsign callsign = new Callsign("W");

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_TooLong_ReturnsFalse() {
        Callsign callsign = new Callsign("W1ABCDEFG");

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_NoDigit_ReturnsFalse() {
        Callsign callsign = new Callsign("WABC");

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_NoLetter_ReturnsFalse() {
        Callsign callsign = new Callsign("12345");

        assertThat(callsign.isValid()).isFalse();
    }

    @Test
    void testIsValid_QPrefix_ReturnsFalse() {
        Callsign callsign = new Callsign("Q1ABC");

        assertThat(callsign.isValid()).isFalse();
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
    void testValidate_LastCharDigit_ReturnsLastCharNotLetterFailure() {
        Callsign callsign = new Callsign("W1AB1");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.LAST_CHAR_NOT_LETTER);
    }

    @Test
    void testValidate_TwoCharEndsInDigit_ReturnsLastCharNotLetterFailure() {
        Callsign callsign = new Callsign("W1");

        Callsign.ValidationResult result = callsign.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.failure()).isEqualTo(Callsign.ValidationFailure.LAST_CHAR_NOT_LETTER);
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
    void testValidate_InvalidSsid_ShorterCallsign_LastCharNotLetter() {
        // -16 is not a valid SSID, so it's kept as part of the callsign
        // W1A-16 = 6 chars, ends in digit
        Callsign callsign = new Callsign("W1A-16");

        assertThat(callsign.getSsid()).isNull();
        assertThat(callsign.validate().failure())
                .isEqualTo(Callsign.ValidationFailure.LAST_CHAR_NOT_LETTER);
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
