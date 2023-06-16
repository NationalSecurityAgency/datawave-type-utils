package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.visitor.NestedGroupValidator;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.RegexParser.parse;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NestedGroupValidatorTest {
    
    /**
     * Verify that validating a null node does not result in an exception.
     */
    @Test
    void testNullNode() {
        assertValid(null);
    }
    
    /**
     * Verify that validating an empty regex does not result in an exception.
     */
    @Test
    void testEmptyRegex() {
        assertValid("");
    }
    
    /**
     * Verify that validating regexes without nested groups do not result in exceptions.
     */
    @Test
    void testRegexWithoutNestedGroups() {
        assertValid("123.*");
        assertValid("123.[543]");
        assertValid("(9834{3}|234)");
        assertValid("^(9834{3}|234)|(54|99.*)$");
    }
    
    /**
     * Verify that validating regexes with nested groups results in exceptions.
     */
    @Test
    void testRegexWithNestedGroups() {
        assertInvalid("(123|(34|454))");
        assertInvalid("234|(123|(34|454))");
        assertInvalid("(34|4542)|(123|(34|454))");
    }
    
    private void assertValid(String pattern) {
        validate(parse(pattern));
    }
    
    private void assertInvalid(String pattern) {
        assertThatThrownBy(() -> validate(parse(pattern))).isInstanceOf(IllegalArgumentException.class).hasMessage("Nested groups are not supported.");
    }
    
    private void validate(Node node) {
        NestedGroupValidator.validate(node);
    }
}
