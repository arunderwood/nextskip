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
 * <p>Tests callsign parsing, prefix extraction, suffix handling, and validation.
 * Includes property-based tests for invariants.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Test data uses repeated callsign values
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
    void testIsValid_TooShort_ReturnsFalse() {
        Callsign callsign = new Callsign("W1");

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
}
