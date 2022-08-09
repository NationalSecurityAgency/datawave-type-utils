package datawave.data.normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class NumberNormalizerTest {
    
    private final NumberNormalizer normalizer = new NumberNormalizer();
    
    /**
     * Verify that the equivalent numbers 1 and 1.00000000 are normalized to the same encoding.
     */
    @Test
    public void testNormalizingEquivalentWholeAndDecimalNumber() {
        String expected = "+aE1";
        assertNormalizeResult("1", expected);
        assertNormalizeResult("1.00000000", expected);
    }
    
    /**
     * Verify that a negative number is normalized to the correct encoding.
     */
    @Test
    public void testNormalizingNegativeDecimal() {
        String expected = "!ZE9";
        assertNormalizeResult("-1.0", expected);
        
        assertComparativelyConsecutive(expected, normalizer.normalize("1.0"));
    }
    
    /**
     * Verify that three different numbers are normalized to their correct respective encodings, and also verify that their encodings evaluate to the same
     * consecutive order as the original numbers.
     */
    @Test
    public void testNormalizingNegativeToPositiveRangeInThousandths() {
        String expected1 = "!dE9";
        assertNormalizeResult("-0.0001", expected1);
        
        String expected2 = "+AE0";
        assertNormalizeResult("0", expected2);
        
        String expected3 = "+VE1";
        assertNormalizeResult("0.00001", expected3);
        
        assertComparativelyConsecutive(expected1, expected2, expected3);
    }
    
    /**
     * Verify that large numbers are correctly normalized, and that their encodings evaluate to the same consecutive order as the original numbers.
     */
    @Test
    public void testNormalizingMaxIntegerValue() {
        String expected1 = "+jE2.147483647";
        assertNormalizeResult(Integer.toString(Integer.MAX_VALUE), "+jE2.147483647");
        
        String expected2 = "+jE2.147483646";
        assertNormalizeResult(Integer.toString(Integer.MAX_VALUE - 1), "+jE2.147483646");
    
        assertComparativelyConsecutive(expected2, expected1);
    }
    
    /**
     * Verify that two numbers that are equal, but one with extra zeroes, are normalized to the same encoding.
     */
    @Test
    public void testNormalizingEqualNumbersWithExtraZeroes() {
        String expected = "!dE1";
        assertNormalizeResult("-0.0009", expected);
        assertNormalizeResult("-0.00090", expected);
    }
    
    /**
     * Verify that different forms of zero will normalize to the same encoding.
     */
    @Test
    public void testNormalizingEquivalentZeroes() {
        assertNormalizeResult("-0.0", "+AE0");
        assertNormalizeResult("0", "+AE0");
        assertNormalizeResult("0.0", "+AE0");
    }
    
    private void assertNormalizeResult(String input, String expected) {
        assertEquals(normalizer.normalize(input), expected);
    }
    
    private void assertComparativelyConsecutive(String... values) {
        for (int i = 0; i < values.length - 1; i++) {
            int compare = values[i].compareTo(values[i+1]);
            if (compare > 0) {
                Assertions.fail("Expected values to be consecutive, but encountered " + values[i] + " which is greater than " + values[i + 1]);
            }
        }
    }
    
    private void assertNormalizeRegexResult(String input, String expected) {
        assertEquals(normalizer.normalizeRegex(input), expected);
    }
    
    private void assertNormalizeDelegateTypeResult(BigDecimal input, String expected) {
        assertEquals(normalizer.normalizeDelegateType(input), expected);
    }
    
    private void assertDenormalizeResult(String input, BigDecimal expected) {
        assertEquals(normalizer.denormalize(input), expected);
    }
    
}
