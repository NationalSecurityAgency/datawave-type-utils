package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.NodeAssert.assertThat;
import static datawave.data.normalizer.regex.RegexParser.parse;

class GroupAlternationsExpanderTest {
    
    @Test
    void testExpandingNull() {
        assertThat(GroupAlternationsExpander.expand(null)).isNull();
    }
    
    @Test
    void testExpandingEmptyPattern() {
        assertNotExpanded("");
    }
    
    @Test
    void testExpandingPatternsWithoutGroups() {
        assertNotExpanded("123.*");
        assertNotExpanded("^123.?$");
        assertNotExpanded(".*1234");
        assertNotExpanded(".{3}54[3-6]");
        assertNotExpanded("343|34.*|6534.*|23\\.[34]{4}");
    }
    
    @Test
    void testExpandingGroupsWithoutAlternations() {
        assertNotExpanded("(123)");
        assertNotExpanded(".*(123).*");
        assertNotExpanded("(123)|.*(54[39]).*");
        assertNotExpanded("(234).*(453)");
    }
    
    @Test
    void testExpandingGroupsWithAlternations() {
        assertExpandedTo("(3|4)", "(3)|(4)");
        assertExpandedTo("(3|4)(5|6)", "(3)(5)|(3)(6)|(4)(5)|(4)(6)");
        assertExpandedTo("(12|65)|(45.*|99[34])|(88)", "(12)|(65)|(45.*)|(99[34])|(88)");
        assertExpandedTo(".*(123|7[3-6]).*", ".*(123).*|.*(7[3-6]).*");
        assertExpandedTo("[3-5](234|543.*)|.*(43|654){3}", "[3-5](234)|[3-5](543.*)|.*(43){3}|.*(654){3}");
        assertExpandedTo("1(3|5).*(4|5).*", "1(3).*(4).*|1(3).*(5).*|1(5).*(4).*|1(5).*(5).*");
    }
    
    private void assertNotExpanded(String pattern) {
        assertExpandedTo(pattern, pattern);
    }
    
    private void assertExpandedTo(String pattern, String expectedPattern) {
        Node actual = GroupAlternationsExpander.expand(parse(pattern));
        if (expectedPattern == null) {
            assertThat(actual).isNull();
        } else {
            Node expected = parse(expectedPattern);
            assertThat(actual).isEqualTreeTo(expected);
        }
    }
}
