package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.NodeAssert.assertThat;
import static datawave.data.normalizer.regex.RegexParser.parse;

class ZeroValueNormalizerTest {
    
    /**
     * Test different variants of zero and negative zero. These can be handled by {@link datawave.data.type.util.NumericalEncoder#encode(String)} and do not
     * need to be changed.
     */
    @Test
    public void testSimpleNumberZeros() {
        assertNotNormalized("0");
        assertNotNormalized("0\\.00");
        assertNotNormalized("-0");
        assertNotNormalized("-0\\.00");
    }
    
    @Test
    void testPositivePatternsThatCanMatchZero() {
        assertNormalized("0.*", "0.*|0");
        assertNormalized(".*0", ".*0|0");
        assertNormalized(".+0", ".+0|0");
        assertNormalized("[0-9]", "[0-9]|0");
        assertNormalized(".*0.*", ".*0.*|0");
        assertNormalized("0.", "0.|0");
        assertNormalized("0\\d", "0\\d|0");
        assertNormalized("\\d", "\\d|0");
    }
    
    @Test
    void testPositivePatternsThatOnlyMatchZero() {
        assertNormalized("0\\.0[0]", "0");
        assertNormalized("0\\.0[0-0]", "0");
    }
    
    @Test
    void testNegativePatternsThatOnlyMatchZero() {
        assertNormalized("-0\\.0[0]", "0");
        assertNormalized("-0\\.0[0-0]", "0");
    }
    
    @Test
    void testNegativePatternsThatCanMatchZero() {
        assertNormalized("-[01234]", "-[01234]|0");
        assertNormalized("-[0-9]", "-[0-9]|0");
        assertNormalized("-\\d", "-\\d|0");
        assertNormalized("-.", "-.|0");
        assertNormalized("-.*", "-.*|0");
        assertNormalized("-.+", "-.+|0");
        assertNormalized("-0.00.*", "-0.00.*|0");
        assertNormalized("-0.00.*", "-0.00.*|0");
        assertNormalized("-0.00\\d", "-0.00\\d|0");
        assertNormalized("-00\\.0\\d.", "-00\\.0\\d.|0");
        assertNormalized("-[0-3]0\\d.", "-[0-3]0\\d.|0");
    }
    
    @Test
    void testNegativePatternsThatCannotMatchZero() {
        assertNotNormalized("-234[0-3]");
        assertNotNormalized("-.*834");
        assertNotNormalized("-0\\.00.*834");
        assertNotNormalized("-.00.001");
    }
    
    @Test
    void testAlternations() {
        assertNormalized("0\\.93|34.*|-34.*|0\\.0[0]|-0.00\\d", "0\\.93|34.*|-34.*|0|-0.00\\d|0");
    }
    
    public void assertNotNormalized(String pattern) {
        assertNormalized(pattern, pattern);
    }
    
    public void assertNormalized(String pattern, String expectedPattern) {
        Node actual = ZeroValueNormalizer.normalize(parse(pattern));
        Node expected = parse(expectedPattern);
        assertThat(actual).isEqualTreeTo(expected);
    }
}
