package datawave.data.normalizer;

import datawave.data.type.util.NumericalEncoder;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NumericRegexNormalizerTest {
    
    private static final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
            "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private final List<String> data = new ArrayList<>();
    private final List<String> normalizedData = new ArrayList<>();
    private String regex;
    
    @AfterEach
    void tearDown() {
        data.clear();
        normalizedData.clear();
    }
    
    /**
     * Verify that an exception is thrown for any blank regex patterns.
     */
    @Test
    void testEmptyRegex() {
        givenRegex("");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not be blank.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that cannot be compiled.
     */
    @Test
    void testRegexWithInvalidPattern() {
        givenRegex("123[]"); // Empty character lists are invalid.
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Invalid numeric regex pattern provided: 123[]", throwable.getMessage());
        
        givenRegex("123\\"); // Trailing backslashes are invalid.
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Invalid numeric regex pattern provided: 123\\", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains any letter other than the special case of \d.
     */
    @Test
    void testRegexWithRestrictedLetters() {
        // Verify an exception is thrown for any letter.
        for (String letter : letters) {
            givenRegex(letter);
            Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
            assertEquals("Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.",
                            throwable.getMessage());
        }
        
        // Verify an exception is thrown for \D, which indicates a non-digit character in regex.
        givenRegex("\\D");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.", throwable.getMessage());
        
        // Verify an exception is not thrown for \d, which indicates a digit in regex.
        givenRegex("\\d");
        normalize();
    }
    
    /**
     * Verify that an exception is thrown for any regex with whitespace.
     */
    @Test
    void testRegexWithWhitespace() {
        givenRegex(" ");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain any whitespace.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex with escaped backslashes.
     */
    @Test
    void testRegexWithEscapedBackslash() {
        givenRegex("\\\\234");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain any escaped backslashes.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains line anchors.
     */
    @Test
    void testRegexWithLineAnchors() {
        givenRegex("^123");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain line anchors ^ or $.", throwable.getMessage());
        
        givenRegex("123$");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain line anchors ^ or $.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains empty groups or lists.
     */
    @Test
    void testRegexWithEmptyGroupsAndLists() {
        givenRegex("123()");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex pattern may not contain empty groups '()'.", throwable.getMessage());
    }
    
    /**
     * Verify that whole numbers are encoded correctly.
     */
    @Test
    void testWholeNumbers() {
        // Positive numbers.
        givenRegex("123");
        assertNormalizedRegex("\\+cE1\\.23");
        
        // Negative numbers.
        givenRegex("-32");
        assertNormalizedRegex("\\!YE6\\.8");
        
    }
    
    /**
     * Verify that decimal numbers are encoded correctly.
     */
    @Test
    void testDecimalNumbers() {
        // Positive number.
        givenRegex("3\\.4");
        assertNormalizedRegex("\\+aE3\\.4");
        
        // Negative number.
        givenRegex("-3\\.5");
        assertNormalizedRegex("\\!ZE6\\.5");
        
        // No leading zero.
        givenRegex("\\.55");
        assertNormalizedRegex("\\+ZE5\\.5");
    }
    
    /**
     * Verify that numbers separated by | are correctly encoded separately.
     */
    @Test
    void testMultipleNumbers() {
        // Whole numbers.
        givenRegex("1|-2");
        assertNormalizedRegex("\\+aE1|\\!ZE8");
        
        // Decimal numbers.
        givenRegex("1|3\\.4|7\\.999");
        assertNormalizedRegex("\\+aE1|\\+aE3\\.4|\\+aE7\\.999");
    }
    
    /**
     * Verify that character lists are correctly parsed and encoded.
     */
    @Test
    void testCharacterLists() {
        // Should expand to the numbers 6, 7, and 8.
        givenRegex("[678]");
        assertNormalizedRegex("\\+aE6|\\+aE7|\\+aE8");
        
        // Should expand to the numbers 124, 134, and 1.4.
        givenRegex("1[23.]4");
        assertNormalizedRegex("\\+aE1\\.4|\\+cE1\\.24|\\+cE1\\.34");
        
        // Should expand to the numbers 0, 1, 2, 3, 4, and 5.
        givenRegex("[0-5]");
        assertNormalizedRegex("\\+AE0|\\+aE1|\\+aE2|\\+aE3|\\+aE4|\\+aE5");
        
        // Should expand to the numbers 45.6, 46.6, and 47.6.
        givenRegex("4[5-7]\\.6");
        assertNormalizedRegex("\\+bE4\\.56|\\+bE4\\.66|\\+bE4\\.76");
    }
    
    /**
     * Verify that regex groups are correctly parsed and encoded.
     */
    @Test
    void testGroups() {
        // Simple group.
        givenRegex("(12)");
        assertNormalizedRegex("\\+bE1\\.2");
        
        // Group that contains | dividers.
        givenRegex("(-12|1\\.2|34)");
        assertNormalizedRegex("\\!YE8\\.8|\\+aE1\\.2|\\+bE3\\.4");
        
        // Should expand to numbers 11.345, 14.345, and 16.345
        givenRegex("(1[146]\\.345)");
        assertNormalizedRegex("\\+bE1\\.1345|\\+bE1\\.4345|\\+bE1\\.6345");
        
        // Should expand to numbers 40 and 434.
        givenRegex("4(0|34)");
        assertNormalizedRegex("\\+bE4|\\+cE4\\.34");
        
        // Should expand to numbers 37 and 547.
        givenRegex("(3|54)7");
        assertNormalizedRegex("\\+bE3\\.7|\\+cE5\\.47");
        
        // Multiple groups. Should expand to numbers 12, 164, 535, 1000, 453, amd 423.
        givenRegex("1(2|64)|(543|1000)|4(5|2)3");
        assertNormalizedRegex("(\\+bE1\\.2|\\+cE1\\.64)|(\\+cE5\\.43|\\+dE1)|(\\+cE4\\.53|\\+cE4\\.23)");
        
        // Nested groups should fail.
        givenRegex("(34(343|34))");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Nested regex groups are not supported.", throwable.getMessage());
    }
    
    /**
     * Verify that the regex character \d is correctly expanded into the numbers 0-9.
     */
    @Test
    void testRegexDigitCharacter() {
        // The character by itself should not require any encoding.
        givenRegex("\\d");
        assertNormalizedRegex("\\d");
        
        // Should expand to 305, 315, 325, 335, 345, 355, 365, 375, 385, and 395.
        givenRegex("3\\d5");
        assertNormalizedRegex("\\+cE3\\.05|\\+cE3\\.15|\\+cE3\\.25|\\+cE3\\.35|\\+cE3\\.45|\\+cE3\\.55|\\+cE3\\.65|\\+cE3\\.75|\\+cE3\\.85|\\+cE3\\.95");
        
        // Same as above, but with groups.
        givenRegex("(3\\d5)|(11|23)");
        assertNormalizedRegex(
                        "(\\+cE3\\.05|\\+cE3\\.15|\\+cE3\\.25|\\+cE3\\.35|\\+cE3\\.45|\\+cE3\\.55|\\+cE3\\.65|\\+cE3\\.75|\\+cE3\\.85|\\+cE3\\.95)|(\\+bE1\\.1|\\+bE2\\.3)");
    }
    
    /**
     * Verify that the wildcard . is correctly expanded into the numbers 0-9, and .
     */
    @Test
    void testDotWildcard() {
        // The character by itself should not require any encoding.
        givenRegex(".");
        assertNormalizedRegex(".");
        
        // Should expand to 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, and 19.
        givenRegex("1.");
        assertNormalizedRegex("\\+bE1|\\+bE1\\.1|\\+bE1\\.2|\\+bE1\\.3|\\+bE1\\.4|\\+bE1\\.5|\\+bE1\\.6|\\+bE1\\.7|\\+bE1\\.8|\\+bE1\\.9|\\+aE1");
        
        // Should expand to 01, 11, 21, 31, 41, 51, 61, 71, 81, 91, and 0.1
        givenRegex(".1");
        assertNormalizedRegex("\\+aE1|\\+bE1\\.1|\\+bE2\\.1|\\+bE3\\.1|\\+bE4\\.1|\\+bE5\\.1|\\+bE6\\.1|\\+bE7\\.1|\\+bE8\\.1|\\+bE9\\.1|\\+ZE1");
        
        // Should expand to 102, 112, 122, 132, 142, 152, 162, 172, 182, 192, and 1.2
        givenRegex("1.2");
        assertNormalizedRegex(
                        "\\+cE1\\.02|\\+cE1\\.12|\\+cE1\\.22|\\+cE1\\.32|\\+cE1\\.42|\\+cE1\\.52|\\+cE1\\.62|\\+cE1\\.72|\\+cE1\\.82|\\+cE1\\.92|\\+aE1\\.2");
        
        // Same as above, but with groups.
        givenRegex("(1.2)|34");
        assertNormalizedRegex(
                        "(\\+cE1\\.02|\\+cE1\\.12|\\+cE1\\.22|\\+cE1\\.32|\\+cE1\\.42|\\+cE1\\.52|\\+cE1\\.62|\\+cE1\\.72|\\+cE1\\.82|\\+cE1\\.92|\\+aE1\\.2)|\\+bE3\\.4");
    }
    
    /**
     * Verify that if any permutations with multiple decimal points are filtered out.
     */
    @Test
    void testPermutationsWithMultipleDecimalPoints() {
        // Will generate permutations 1..3, 10.3, 11.3, 12.3, 13.3, 14.3, 15.3, 16.3, 17.3, 18.3, and 19.3. The permutation 1..3 should be filtered out before
        // encoding occurs.
        givenRegex("1.\\.3");
        assertNormalizedRegex("\\+bE1\\.03|\\+bE1\\.13|\\+bE1\\.23|\\+bE1\\.33|\\+bE1\\.43|\\+bE1\\.53|\\+bE1\\.63|\\+bE1\\.73|\\+bE1\\.83|\\+bE1\\.93");
    }
    
    /**
     * Verify that the regex quantifiers * and + are handled correctly.
     */
    @Test
    void testMultiWildcards() {
        // Should return the original regex.
        givenRegex(".*");
        assertNormalizedRegex(".*");
        
        givenRegex(".+");
        assertNormalizedRegex(".+");
        
        // Should throw exceptions since we can't expand to a distinct number of permutations.
        givenRegex("1*3");
        Throwable throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *3 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.*3");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *3 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1+3");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +3 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.+3");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +3 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1\\.3.*4");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *4 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1\\.3*4");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *4 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1\\.3.+4");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +4 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1\\.3+4");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +4 that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex(".*.");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *. that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex(".+.");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +. that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex(".*\\d");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *\\d that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex(".+\\d");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +\\d that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.*[34]");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *[ that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.+[34]");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +[ that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.*(3|4)");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo *( that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        givenRegex("1.+(3|4)");
        throwable = assertThrows(IllegalArgumentException.class, this::normalize);
        assertEquals("Regex contains multi-wildcard combo +( that cannot be expanded to a finite number of permutations.", throwable.getMessage());
        
        // Should normalize the numeric value and append the quantifiers.
        givenRegex("1.*");
        assertNormalizedRegex("\\+aE1.*");
        
        givenRegex("1*");
        assertNormalizedRegex("\\+aE1*");
        
        givenRegex("1.+");
        assertNormalizedRegex("\\+aE1.+");
        
        givenRegex("1+");
        assertNormalizedRegex("\\+aE1+");
        
        givenRegex("1\\.3.*");
        assertNormalizedRegex("\\+aE1\\.3.*");
        
        givenRegex("1\\.3.+");
        assertNormalizedRegex("\\+aE1\\.3.+");
        
        givenRegex("(1|2|3).*");
        assertNormalizedRegex("(\\+aE1|\\+aE2|\\+aE3).*");
        
        givenRegex("(1|2|3).+");
        assertNormalizedRegex("(\\+aE1|\\+aE2|\\+aE3).+");
        
        givenRegex("(1|2|3\\.3).*");
        assertNormalizedRegex("(\\+aE1|\\+aE2|\\+aE3\\.3).*");
        
        givenRegex("(1|2|3\\.3).+");
        assertNormalizedRegex("(\\+aE1|\\+aE2|\\+aE3\\.3).+");
    }
    
    /**
     * Verify that both the non-encoded and encoded versions of a regex will match the same corresponding values.
     */
    @Test
    @Ignore
    void testMatchFidelity() {
        givenDataEntry("-1");
        givenDataEntry("-2");
        givenDataEntry("-3");
        givenDataEntry("-4");
        givenDataEntry("-5");
        givenDataEntry("0");
        givenDataEntry("0.0");
        givenDataEntry("1");
        givenDataEntry("2");
        givenDataEntry("3");
        givenDataEntry("4");
        givenDataEntry("5");
        givenDataEntry("1.1111");
        givenDataEntry("1.2222");
        
    }
    
    // todo - test regex patterns that contain qualifiers * and +
    // todo - test that original patterns and new patterns match the same data set
    
    private void givenRegex(String regex) {
        this.regex = regex;
    }
    
    private void givenDataEntry(String number) {
        data.add(number);
        normalizedData.add(NumericalEncoder.encode(number));
    }
    
    private void normalize() {
        NumericRegexNormalizer normalizer = NumericRegexNormalizer.of(regex);
        normalizer.normalize();
    }
    
    private void assertNormalizedRegex(String expected) {
        NumericRegexNormalizer normalizer = NumericRegexNormalizer.of(regex);
        String actual = normalizer.normalize();
        assertEquals(expected, actual);
    }
}
