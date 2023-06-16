package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.NodeAssert.assertThat;
import static datawave.data.normalizer.regex.RegexParser.parse;

class GroupFlattenerTest {
    
    @Test
    void testFlatteningNull() {
        assertThat(GroupFlattener.flatten(null)).isNull();
    }
    
    @Test
    void testFlatteningEmptyPattern() {
        assertNotFlattened("");
    }
    
    @Test
    void testFlatteningPatternsWithoutGroups() {
        assertNotFlattened("123.*");
        assertNotFlattened("123.?");
        assertNotFlattened(".*1234");
        assertNotFlattened(".{3}54[3-6]");
        assertNotFlattened("343|34.*|6534.*|23\\.[34]{4}");
    }
    
    @Test
    void testFlatteningPatternsWithGroups() {
        // Test mixed groups that can all be flattened.
        assertFlattenedTo("(3)|(4)", "3|4");
        assertFlattenedTo("(3)(5)|(3)(6)|(4)(5)|(4)(6)", "35|36|45|46");
        assertFlattenedTo("(12)|(65)|(45.*)|(99[34])|(88)", "12|65|45.*|99[34]|88");
        assertFlattenedTo(".*(123).*|.*(7[3-6]).*", ".*123.*|.*7[3-6].*");
        assertFlattenedTo("1(3).*(4).*|1(3).*(5).*|1(5).*(4).*|1(5).*(5).*", "13.*4.*|13.*5.*|15.*4.*|15.*5.*");
        
        // Test groups with immediate succeeding characters that do not prohibit flattening.
        assertFlattenedTo("(234)1", "2341");
        assertFlattenedTo("(234).*", "234.*");
        assertFlattenedTo("(234).+", "234.+");
        assertFlattenedTo("(234)[3-5]", "234[3-5]");
        assertFlattenedTo("(234)\\d", "234\\d");
        assertFlattenedTo("(234)\\.", "234\\.");
        
        // Test groups that are followed by + * ? or a repetition quantifier. In these cases, the operation applies to the entire group, and as such we cannot
        // flatten the groups.
        assertNotFlattened("(234){3}");
        assertNotFlattened("(234)?");
        assertNotFlattened("(234)+");
        assertNotFlattened("(234)*");
        
        // Allow groups followed by + * ? or a repetition quantifier to be flattened if they only contain one single character.
        assertFlattenedTo("(2){3}", "2{3}");
        assertFlattenedTo("(\\d)+", "\\d+");
        assertFlattenedTo("([36-9])*", "[36-9]*");
        assertFlattenedTo("(4)?", "4?");
        
        // The first two groups should be flattened, but the last two should not since they are immediately followed by a repetition quantifier.
        assertFlattenedTo("[3-5](234)|[3-5](543.*)|.*(43){3}|.*(654){3}", "[3-5]234|[3-5]543.*|.*(43){3}|.*(654){3}");
    }
    
    private void assertNotFlattened(String pattern) {
        assertFlattenedTo(pattern, pattern);
    }
    
    private void assertFlattenedTo(String pattern, String expectedPattern) {
        Node actual = GroupFlattener.flatten(parse(pattern));
        if (expectedPattern == null) {
            assertThat(actual).isNull();
        } else {
            Node expected = parse(expectedPattern);
            assertThat(actual).isEqualTreeTo(expected);
        }
    }
}
