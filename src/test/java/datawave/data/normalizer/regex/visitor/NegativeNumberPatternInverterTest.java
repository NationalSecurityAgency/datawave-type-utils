package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.NodeAssert.assertThat;
import static datawave.data.normalizer.regex.RegexParser.parse;

class NegativeNumberPatternInverterTest {
    
    /**
     * Verify that patterns consisting of simple numbers are not modified by {@link NegativeNumberPatternInverter}.
     */
    @Test
    void testPatternsMadeOfSimpleNumbers() {
        // Test a single positive number.
        assertInverted("345", "\\+cE3\\.45");
        
        // Test a single negative number.
        assertInverted("-345", "!XE6\\.55");
        
        // Test alternated positive and negative number.
        assertInverted("345|-345", "\\+cE3\\.45|!XE6\\.55");
    }
    
    /**
     * Verify that patterns consisting of positive number patterns are not modified by {@link NegativeNumberPatternInverter}.
     */
    @Test
    void testPatternsMadeOfPositivePatterns() {
        // Single positive number pattern.
        assertInverted(".*345", "\\+[c-zA-Z]E.*345");
        
        // Alternated positive number patterns.
        assertInverted("45.*|0.045[3-5]", "\\+[b-z]E45.*|\\+[c-eY]E.?0?45[3-5]");
    }
    
    /**
     * Verify that patterns consisting of positive number patterns and simple numbers are not modified by {@link NegativeNumberPatternInverter}.
     */
    @Test
    void testPatternsMadeOfPositivePatternsAndSimpleNumbers() {
        // Alternated with positive simple number.
        assertInverted("345.*|456", "\\+[c-z]E345.*|\\+cE4\\.56");
        
        // Alternated with negative simple number.
        assertInverted(".*345|-456", "\\+[c-zA-Z]E.*345|!XE5\\.44");
        
        // Alternated with positive and negative simple number.
        assertInverted("45.*|0.045[3-5]|456|-456", "\\+[b-z]E45.*|\\+[c-eY]E.?0?45[3-5]|\\+cE4\\.56|!XE5\\.44");
    }
    
    @Test
    void testWildcard() {
        assertInverted("-.234", "![W-Xa]E.?766");
        assertInverted("-34.454", "![U-Y]E65.546");
        assertInverted("-34454.", "![U-V]E6554[65].?");
    }
    
    @Test
    void testMultiWildcards() {
        assertInverted("-.*234", "![A-Xa-z]E.*766");
        assertInverted("-.+234", "![A-Xa-z]E.*766");
    }
    
    @Test
    void testCharacterClasses() {
        assertInverted("-[2-4]", "!ZE[6-8]");
        assertInverted("-[1357]", "!ZE[9753]");
        assertInverted("-[46-8]", "!ZE[62-4]");
    }
    
    @Test
    void testDigitCharacterClass() {
        assertInverted("-\\d", "!ZE\\d");
    }
    
    @Test
    void testQuantifiersWithMinuendsOfNineOrTen() {
        // 4{3} needs to be subtracted from a minuend of either 9 or 10. Because it has a quantifier, the minuend variants need to be captured in a grouped
        // alternation.
        assertInverted("-454{3}.*", "![A-V]E54(6{3}|5{3}).*");
        
        // Same thing for 4*.
        assertInverted("-454*.*", "![A-Y]E5[54](6*|5*).*");
        
        // Same thing for 4+.
        assertInverted("-454+.+", "![A-X]E54(6+|5+).*");
        
        // Test [46]{3}.
        assertInverted("-45[46]{3}.*", "![A-V]E54([64]{3}|[53]{3}).*");
        
        // Test [46]*.
        assertInverted("-45[46]*.*", "![A-Y]E5[54]([64]*|[53]*).*");
        
        // Test [46]+.
        assertInverted("-45[46]+.*", "![A-X]E54([64]+|[53]+).*");
    }
    
    @Test
    void testConsolidatedLeadingZeros() {
        // The consolidated zeros in (0{3})? should not be modified for a positive expression.
        assertInverted(".*000.*3", "\\+[a-zA-Z]E.*(0{3})?.*3");
        
        // However, in a positive expression, they should get negated to a value of 9.
        assertInverted("-.*000.*3", "![A-Za-z]E.*(9{3})?.*7");
        
        // Even if there could possibly be no elements after the consolidated zeros, they should get negated to a value of 9.
        assertInverted("-3.*000.*", "![A-Z]E[76].*(9{3})?.*");
    }
    
    public void assertInverted(String pattern, String expectedPattern) {
        Node actual = SimpleNumberEncoder.encode(parse(pattern));
        actual = ExponentialBinAdder.addBins(actual);
        actual = ZeroTrimmer.trim(actual);
        actual = NegativeNumberPatternInverter.invert(actual);
        assertThat(actual).asTreeString().isEqualTo(expectedPattern);
    }
    
}
