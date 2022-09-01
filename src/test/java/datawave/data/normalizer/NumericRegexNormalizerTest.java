package datawave.data.normalizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NumericRegexNormalizerTest {
    
    private static final String[] letters = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w",
            "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private String regex;
    
    /**
     * Verify that an exception is thrown for any blank regex patterns.
     */
    @Test
    void testEmptyRegex() {
        givenRegex("");
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not be blank.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that cannot be compiled.
     */
    @Test
    void testRegexWithInvalidPattern() {
        givenRegex("123[]"); // Empty character lists are invalid.
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Invalid numeric regex pattern provided: 123[]", throwable.getMessage());
        
        givenRegex("123\\"); // Trailing backslashes are invalid.
        throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Invalid numeric regex pattern provided: 123\\", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains any letter other than the special case of \d.
     */
    @Test
    void testRegexWithRestrictedLetters() {
        // Verify an exception is thrown for any letter.
        for (String letter : letters) {
            givenRegex(letter);
            Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
            Assertions.assertEquals("Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.",
                            throwable.getMessage());
        }
        
        // Verify an exception is thrown for \D, which indicates a non-digit character in regex.
        givenRegex("\\D");
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.",
                        throwable.getMessage());
        
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
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain any whitespace.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex with escaped backslashes.
     */
    @Test
    void testRegexWithEscapedBackslash() {
        givenRegex("\\\\234");
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain any escaped backslashes.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains line anchors.
     */
    @Test
    void testRegexWithLineAnchors() {
        givenRegex("^123");
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain line anchors ^ or $.", throwable.getMessage());
        
        givenRegex("123$");
        throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain line anchors ^ or $.", throwable.getMessage());
    }
    
    /**
     * Verify that an exception is thrown for any regex that contains empty groups or lists.
     */
    @Test
    void testRegexWithEmptyGroupsAndLists() {
        givenRegex("123()");
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Regex pattern may not contain empty groups '()'.", throwable.getMessage());
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
        Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class, this::normalize);
        Assertions.assertEquals("Nested regex groups are not supported.", throwable.getMessage());
    }
    
    /**
     * Verify that the regex character \d is correctly expanded into the numbers 0-9.
     */
    @Test
    void testRegexDigitCharacter() {
        // The character by itself.
        givenRegex("\\d");
        assertNormalizedRegex("\\+AE0|\\+aE1|\\+aE2|\\+aE3|\\+aE4|\\+aE5|\\+aE6|\\+aE7|\\+aE8|\\+aE9");
        
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
        // The character by itself.
        givenRegex(".");
        assertNormalizedRegex("\\+AE0|\\+aE1|\\+aE2|\\+aE3|\\+aE4|\\+aE5|\\+aE6|\\+aE7|\\+aE8|\\+aE9");
        
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
    
    // todo - test cases where multiple decimal points are generated within a number
    // todo - test regex patterns that create no numbers
    // todo - test regex patterns that contain qualifiers * and +
    
    private void givenRegex(String regex) {
        this.regex = regex;
    }
    
    private void normalize() {
        NumericRegexNormalizer normalizer = NumericRegexNormalizer.of(regex);
        normalizer.normalize();
    }
    
    private void assertNormalizedRegex(String expected) {
        NumericRegexNormalizer normalizer = NumericRegexNormalizer.of(regex);
        String actual = normalizer.normalize();
        Assertions.assertEquals(expected, actual);
    }
}
