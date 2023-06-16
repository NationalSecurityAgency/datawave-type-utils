package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import org.junit.jupiter.api.Test;

import static datawave.data.normalizer.regex.NodeAssert.assertThat;
import static datawave.data.normalizer.regex.RegexParser.parse;

class DecimalPointPlacerTest {
    
    @Test
    void testSimpleNumbers() {
        assertAdded("234", "\\+cE2\\.34");
        assertAdded("234-454", "\\+gE2\\.34-454");
        assertAdded("234|-454", "\\+cE2\\.34|!XE5\\.46");
    }
    
    @Test
    void testSingleLengthPatterns() {
        assertAdded("[3-9]", "\\+aE[3-9]");
        assertAdded("\\d", "\\+aE\\d");
        assertAdded(".", "\\+aE.");
        
        // Negative variants.
        assertAdded("-[3-9]", "!ZE[1-7]");
        assertAdded("-\\d", "!ZE\\d");
        assertAdded("-.", "!ZE.");
    }
    
    @Test
    void testLeadingMultiWildcards() {
        // In the case of .*, allow for a possible decimal point occurring after it, and after the next character.
        assertAdded(".*454", "\\+[a-zA-Z]E.?\\.?.*4\\.?54");
        assertAdded(".*?45", "\\+[a-zA-Z]E.?\\.?.*?4\\.?5");
        
        // In the case of a leading .+, allow for a possible decimal point occurring after it, and after the next character. We must account for when .+ might
        // be a decimal point, such as for the number 0.343.
        assertAdded(".+343", "\\+[a-zA-Z]E.\\.?.*343");
        assertAdded(".+?343", "\\+[a-zA-Z]E.\\.?.*?343");
    }
    
    @Test
    void testConsolidatedZeros() {
        assertAdded(".*000004", "\\+[a-zA-Z]E.?\\.?.*(0{5})?\\.?4\\.?");
    }
    
    @Test
    void testLeadingQuantifiersForSingleChar() {
        assertAdded("4*11", "\\+[b-z]E4?\\.?4*1\\.?1");
        assertAdded("4+11", "\\+[c-z]E4\\.?4*11");
        assertAdded("4{3}11", "\\+eE4\\.4{2}11");
        assertAdded("4{3,5}11", "\\+[e-g]E4\\.4{2,4}11");
        assertAdded("4{1,5}11", "\\+[c-g]E4\\.4{0,4}11");
        assertAdded("4{1,}11", "\\+[c-z]E4\\.?4*11");
        assertAdded("4{2,}11", "\\+[d-z]E4\\.4+11");
        assertAdded("4{1,2}11", "\\+[c-d]E4\\.4{0,1}11");
        assertAdded("4{1}11", "\\+cE4\\.11");
        assertAdded("4{2}11", "\\+dE4\\.411");
        assertAdded("4{0,5}11", "\\+[b-g]E4?\\.4{0,4}11");
        
        assertAdded("4{1,5}.*", "\\+[a-z]E4\\.?4{0,4}.*");
        assertAdded("4{1,2}.*", "\\+[a-z]E4\\.?4{0,1}.*");
        assertAdded("4{0,5}.*", "\\+[a-z]E4?\\.?4{0,4}.*");
        
        assertAdded("4{1,5}", "\\+[a-e]E4\\.?4{0,4}");
        assertAdded("4{1,2}", "\\+[a-b]E4\\.?4{0,1}");
        assertAdded("4{0,5}", "\\+[a-e]E4?\\.?4{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForWildcard() {
        assertAdded(".*11", "\\+[a-zA-Z]E.?\\.?.*1\\.?1");
        assertAdded(".+11", "\\+[a-zA-Z]E.\\.?.*11");
        assertAdded(".{3}11", "\\+[a-eX-Z]E.\\..{2}11");
        assertAdded(".{3,5}11", "\\+[a-gV-Z]E.\\..{2,4}11");
        assertAdded(".{1,5}11", "\\+[a-gV-Z]E.\\..{0,4}11");
        assertAdded(".{1,}11", "\\+[a-zA-Z]E.\\.?.*11");
        assertAdded(".{2,}11", "\\+[a-zA-Z]E.\\..+11");
        assertAdded(".{1,2}11", "\\+[a-dY-Z]E.\\..{0,1}11");
        assertAdded(".{1}11", "\\+[a-cZ]E.\\.11");
        assertAdded(".{2}11", "\\+[a-dY-Z]E.\\..11");
        assertAdded(".{0,5}11", "\\+[a-gV-Z]E.?\\..{0,4}11");
        
        assertAdded(".{1,5}.*", "\\+[a-zA-Z]E.\\.?.{0,4}.*");
        assertAdded(".{1,2}.*", "\\+[a-zA-Z]E.\\.?.{0,1}.*");
        assertAdded(".{0,5}.*", "\\+[a-zA-Z]E.?\\.?.{0,4}.*");
        
        assertAdded(".{1,5}", "\\+[a-eV-Z]E.\\.?.{0,4}");
        assertAdded(".{1,2}", "\\+[a-bY-Z]E.\\.?.{0,1}");
        assertAdded(".{0,5}", "\\+[a-eV-Z]E.?\\.?.{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForDigitCharClass() {
        assertAdded("\\d*11", "\\+[b-z]E\\d?\\.?\\d*1\\.?1");
        assertAdded("\\d+11", "\\+[b-z]E\\d\\.?\\d*11");
        assertAdded("\\d{3}11", "\\+[b-e]E\\d\\.\\d{2}11");
        assertAdded("\\d{3,5}11", "\\+[b-g]E\\d\\.\\d{2,4}11");
        assertAdded("\\d{1,5}11", "\\+[b-g]E\\d\\.\\d{0,4}11");
        assertAdded("\\d{1,}11", "\\+[b-z]E\\d\\.?\\d*11");
        assertAdded("\\d{2,}11", "\\+[b-z]E\\d\\.\\d+11");
        assertAdded("\\d{1,2}11", "\\+[b-d]E\\d\\.\\d{0,1}11");
        assertAdded("\\d{1}11", "\\+[b-c]E\\d\\.11");
        assertAdded("\\d{2}11", "\\+[b-d]E\\d\\.\\d11");
        assertAdded("\\d{0,5}11", "\\+[b-g]E\\d?\\.\\d{0,4}11");
        
        assertAdded("\\d{1,5}.*", "\\+[a-zA-Z]E\\d\\.?\\d{0,4}.*");
        assertAdded("\\d{1,2}.*", "\\+[a-zA-Z]E\\d\\.?\\d{0,1}.*");
        assertAdded("\\d{0,5}.*", "\\+[a-zA-Z]E\\d?\\.?\\d{0,4}.*");
        
        assertAdded("\\d{1,5}", "\\+[a-e]E\\d\\.?\\d{0,4}");
        assertAdded("\\d{1,2}", "\\+[a-b]E\\d\\.?\\d{0,1}");
        assertAdded("\\d{0,5}", "\\+[a-e]E\\d?\\.?\\d{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForCharClass() {
        assertAdded("[24]*11", "\\+[b-z]E[24]?\\.?[24]*1\\.?1");
        assertAdded("[24]+11", "\\+[c-z]E[24]\\.?[24]*11");
        assertAdded("[24]{3}11", "\\+eE[24]\\.[24]{2}11");
        assertAdded("[24]{3,5}11", "\\+[e-g]E[24]\\.[24]{2,4}11");
        assertAdded("[24]{1,5}11", "\\+[c-g]E[24]\\.[24]{0,4}11");
        assertAdded("[24]{1,}11", "\\+[c-z]E[24]\\.?[24]*11");
        assertAdded("[24]{2,}11", "\\+[d-z]E[24]\\.[24]+11");
        assertAdded("[24]{1,2}11", "\\+[c-d]E[24]\\.[24]{0,1}11");
        assertAdded("[24]{1}11", "\\+cE[24]\\.11");
        assertAdded("[24]{2}11", "\\+dE[24]\\.[24]11");
        assertAdded("[24]{0,5}11", "\\+[b-g]E[24]?\\.[24]{0,4}11");
        
        assertAdded("[24]{1,5}.*", "\\+[a-z]E[24]\\.?[24]{0,4}.*");
        assertAdded("[24]{1,2}.*", "\\+[a-z]E[24]\\.?[24]{0,1}.*");
        assertAdded("[24]{0,5}.*", "\\+[a-z]E[24]?\\.?[24]{0,4}.*");
        
        assertAdded("[24]{1,5}", "\\+[a-e]E[24]\\.?[24]{0,4}");
        assertAdded("[24]{1,2}", "\\+[a-b]E[24]\\.?[24]{0,1}");
        assertAdded("[24]{0,5}", "\\+[a-e]E[24]?\\.?[24]{0,4}");
    }
    
    public void assertAdded(String pattern, String expectedPattern) {
        Node actual = SimpleNumberEncoder.encode(parse(pattern));
        actual = ExponentialBinAdder.addBins(actual);
        actual = ZeroTrimmer.trim(actual);
        actual = NegativeNumberPatternInverter.invert(actual);
        actual = DecimalPointPlacer.addDecimalPoints(actual);
        assertThat(actual).asTreeString().isEqualTo(expectedPattern);
    }
}
