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
        assertAdded("-[3-9]", "!ZE[3-9]");
        assertAdded("-\\d", "!ZE\\d");
        assertAdded("-.", "!ZE.");
    }
    
    @Test
    void testLeadingMultiWildcards() {
        assertAdded(".*", "\\+[a-zA-Z]E.*");
        assertAdded(".*?", "\\+[a-zA-Z]E.*?");
        assertAdded(".+", "\\+[a-zA-Z]E.+");
        assertAdded(".+?", "\\+[a-zA-Z]E.+?");
        
        // In the case of .*, allow for a possible decimal point occurring after it, and after the next character.
        assertAdded(".*454", "\\+[c-zA-Z]E.*4\\.?54");
        assertAdded(".*?45", "\\+[b-zA-Z]E.*?4\\.?5");
        
        // In the case of a leading .+, allow for a possible decimal point occurring after it, and after the next character. We must account for when .+ might
        // be a decimal point, such as for the number 0.343.
        assertAdded(".+343", "\\+[c-zA-Z]E.*3\\.?43");
        assertAdded(".+?343", "\\+[c-zA-Z]E.*?3\\.?43");
        
    }
    
    @Test
    void testConsolidatedZeros() {
        assertAdded(".*000004", "\\+[a-zA-Z]E.*(0{5})?4");
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
        assertAdded(".*11", "\\+[b-zA-Z]E.*1\\.?1");
        assertAdded(".+11", "\\+[b-zA-Z]E.*1\\.?1");
        assertAdded(".{3}11", "\\+[b-eW-Z]E.?\\.?.{0,2}1\\.?1");
        assertAdded(".{3,5}11", "\\+[b-gU-Z]E(.\\.?.{2,4})?1\\.?1");
        assertAdded(".{1,5}11", "\\+[b-gU-Z]E(.\\.?.{0,4})?1\\.?1");
        assertAdded(".{1,}11", "\\+[b-zA-Z]E(.\\.?.*)?1\\.?1");
        assertAdded(".{2,}11", "\\+[b-zA-Z]E(.\\.?.+)?1\\.?1");
        assertAdded(".{1,2}11", "\\+[b-dX-Z]E(.\\.?.{0,1})?1\\.?1");
        assertAdded(".{1}11", "\\+[b-cY-Z]E.?\\.?1\\.?1");
        assertAdded(".{2}11", "\\+[b-dX-Z]E.?\\.?.{0,1}1\\.?1");
        assertAdded(".{0,5}11", "\\+[b-gU-Z]E.?\\.?.{0,4}1\\.?1");
        
        assertAdded(".{1,5}.*", "\\+[a-zA-Z]E(.\\.?.{0,4})?.*");
        assertAdded(".{1,2}.*", "\\+[a-zA-Z]E(.\\.?.{0,1})?.*");
        assertAdded(".{0,5}.*", "\\+[a-zA-Z]E.?\\.?.{0,4}.*");
        
        assertAdded(".{1,5}", "\\+[a-eU-Z]E.\\.?.{0,4}");
        assertAdded(".{1,2}", "\\+[a-bX-Z]E.\\.?.{0,1}");
        assertAdded(".{0,5}", "\\+[a-eU-Z]E.?\\.?.{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForDigitCharClass() {
        assertAdded("\\d*11", "\\+[b-z]E\\d?\\.?\\d*1\\.?1");
        assertAdded("\\d+11", "\\+[b-z]E\\d?\\.?\\d*1\\.?1");
        assertAdded("\\d{3}11", "\\+[b-e]E\\d?\\.?\\d{0,2}1\\.?1");
        assertAdded("\\d{3,5}11", "\\+[b-g]E(\\d\\.?\\d{2,4})?1\\.?1");
        assertAdded("\\d{1,5}11", "\\+[b-g]E(\\d\\.?\\d{0,4})?1\\.?1");
        assertAdded("\\d{1,}11", "\\+[b-z]E(\\d\\.?\\d*)?1\\.?1");
        assertAdded("\\d{2,}11", "\\+[b-z]E(\\d\\.?\\d+)?1\\.?1");
        assertAdded("\\d{1,2}11", "\\+[b-d]E(\\d\\.?\\d{0,1})?1\\.?1");
        assertAdded("\\d{1}11", "\\+[b-c]E\\d?\\.?1\\.?1");
        assertAdded("\\d{2}11", "\\+[b-d]E\\d?\\.?\\d{0,1}1\\.?1");
        assertAdded("\\d{0,5}11", "\\+[b-g]E\\d?\\.?\\d{0,4}1\\.?1");
        
        assertAdded("\\d{1,5}.*", "\\+[a-zA-Z]E(\\d\\.?\\d{0,4})?.*");
        assertAdded("\\d{1,2}.*", "\\+[a-zA-Z]E(\\d\\.?\\d{0,1})?.*");
        assertAdded("\\d{0,5}.*", "\\+[a-zA-Z]E\\d?\\.?\\d{0,4}.*");
        
        assertAdded("\\d{1,5}", "\\+[a-e]E\\d\\.?\\d{0,4}");
        assertAdded("\\d{1,2}", "\\+[a-b]E\\d\\.?\\d{0,1}");
        assertAdded("\\d{0,5}", "\\+[a-e]E\\d?\\.?\\d{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForCharClassContainingZero() {
        assertAdded("[012]*11", "\\+[b-z]E[012]?\\.?[012]*1\\.?1");
        assertAdded("[012]+11", "\\+[b-z]E[012]?\\.?[012]*1\\.?1");
        assertAdded("[012]{3}11", "\\+[b-e]E[012]?\\.?[012]{0,2}1\\.?1");
        assertAdded("[012]{3,5}11", "\\+[b-g]E([012]\\.?[012]{2,4})?1\\.?1");
        assertAdded("[012]{1,5}11", "\\+[b-g]E([012]\\.?[012]{0,4})?1\\.?1");
        assertAdded("[012]{1,}11", "\\+[b-z]E([012]\\.?[012]*)?1\\.?1");
        assertAdded("[012]{2,}11", "\\+[b-z]E([012]\\.?[012]+)?1\\.?1");
        assertAdded("[012]{1,2}11", "\\+[b-d]E([012]\\.?[012]{0,1})?1\\.?1");
        assertAdded("[012]{1}11", "\\+[b-c]E[012]?\\.?1\\.?1");
        assertAdded("[012]{2}11", "\\+[b-d]E[012]?\\.?[012]{0,1}1\\.?1");
        assertAdded("[012]{0,5}11", "\\+[b-g]E[012]?\\.?[012]{0,4}1\\.?1");
        
        assertAdded("[012]{1,5}.*", "\\+[a-zA-Z]E([012]\\.?[012]{0,4})?.*");
        assertAdded("[012]{1,2}.*", "\\+[a-zA-Z]E([012]\\.?[012]{0,1})?.*");
        assertAdded("[012]{0,5}.*", "\\+[a-zA-Z]E[012]?\\.?[012]{0,4}.*");
        
        assertAdded("[012]{1,5}", "\\+[a-e]E[012]\\.?[012]{0,4}");
        assertAdded("[012]{1,2}", "\\+[a-b]E[012]\\.?[012]{0,1}");
        assertAdded("[012]{0,5}", "\\+[a-e]E[012]?\\.?[012]{0,4}");
    }
    
    @Test
    void testLeadingQuantifiersForCharClassNotContainingZero() {
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
    
    @Test
    void testRandom() {
        assertAdded("[06]+\\.[01789]*[124678]*[036]{1,3}[0589][0578][3578]{1,3}[013567]*7[01459]+\\d+[379]+[01345]{1,3}[13459]*[012469]*3+",
                        "\\+[a-zA-Z]E[06]?\\.?[06]*[01789]?\\.?[01789]*[124678]?\\.?[124678]*[036]\\.?[036]{0,2}[0589][0578][3578]{1,3}[013567]*7[01459]+\\d+[379]+[01345]{1,3}[13459]*[012469]*3+");
    }
    
    /**
     * Test patterns that have multiple possible leading zero elements that must all be made optional.
     */
    @Test
    void testMultiplePossibleLeadingZeros() {
        assertAdded("[30].\\d", "\\+[a-cY-Z]E[30]?\\.?.?\\.?\\d?");
        assertAdded("[30]\\d..*", "\\+[a-zA-Z]E[30]?\\.?\\d?\\.?.?\\.?.*");
        assertAdded(".*54", "\\+[b-zA-Z]E.*5\\.?4");
        assertAdded(".+54", "\\+[b-zA-Z]E.*5\\.?4");
        assertAdded("[04]{3}[05]{2}[06]", "\\+[a-f]E[04]?\\.?[04]{0,2}[05]?\\.?[05]{0,1}[06]?");
        assertAdded("[04]{3}[05]{2}[06].*.+43", "\\+[b-zA-Z]E[04]?\\.?[04]{0,2}[05]?\\.?[05]{0,1}[06]?\\.?.*.*4\\.?3");
        assertAdded("[04]{3}0000[05]{2}[06].*.+43", "\\+[b-zA-Z]E[04]?\\.?[04]{0,2}(0{4})?[05]?\\.?[05]{0,1}[06]?\\.?.*.*4\\.?3");
        assertAdded("[04]{3}0000[05]{2}[06][08]{3,5}.*.+43", "\\+[b-zA-Z]E[04]?\\.?[04]{0,2}(0{4})?[05]?\\.?[05]{0,1}[06]?\\.?([08]\\.?[08]{2,4})?.*.*4\\.?3");
    }
    
    /**
     * Test patterns similar to those in {@link #testMultiplePossibleLeadingZeros()}, but with a non-leading zero at the beginning, and verify that none of the
     * elements after the non-leading zero are made optional.
     */
    @Test
    void testMultipleNonLeadingZeros() {
        assertAdded("3[30]\\d..*", "\\+[c-z]E3\\.?[30]?\\d?.?.*");
        assertAdded("3.*54", "\\+[a-z]E3\\..*54");
        assertAdded("3.+54", "\\+[a-z]E3\\..+54");
        assertAdded("3[04]{3}[05]{2}[06]", "\\+gE3\\.?[04]{0,3}[05]{0,2}[06]?");
        assertAdded("3[04]{3}[05]{2}[06].*.+43", "\\+[g-z]E3\\.[04]{3}[05]{2}[06].*.+43");
        assertAdded("3[04]{3}0000[05]{2}[06].*.+43", "\\+[k-z]E3\\.[04]{3}0000[05]{2}[06].*.+43");
        assertAdded("3[04]{3}0000[05]{2}[06][08]{3,5}.*.+43", "\\+[n-z]E3\\.[04]{3}0000[05]{2}[06][08]{3,5}.*.+43");
    }
    
    /**
     * Test patterns similar to those in {@link #testMultiplePossibleLeadingZeros()}, but with a non-leading zero somewhere in the middle. Verify that any
     * possible zeros before the first non-leading zeros are made optional, but any succeeding possible zeros are not made optional.
     */
    @Test
    void testMixedLeadingAndNonLeadingZeros() {
        assertAdded("[30]\\d..*34[05]{2}[04]", "\\+[e-zA-Z]E[30]?\\.?\\d?\\.?.?\\.?.*3\\.?4[05]{0,2}[04]?");
        assertAdded(".*[05]{2}5[05]{2}4", "\\+[d-zA-Z]E.*[05]?\\.?[05]{0,1}5\\.?[05]{2}4");
        assertAdded(".+[05]{2}54[05]{2}", "\\+[d-zA-Z]E.*[05]?\\.?[05]{0,1}5\\.?4[05]{0,2}");
        assertAdded("[04]{3}[05]{2}33[06][05]{2}", "\\+[e-j]E[04]?\\.?[04]{0,2}[05]?\\.?[05]{0,1}3\\.?3[06]?[05]{0,2}");
        assertAdded("[04]{3}[05]{2}33[06][05]{2}.*.+", "\\+[e-z]E[04]?\\.?[04]{0,2}[05]?\\.?[05]{0,1}3\\.?3[06]?[05]{0,2}.*.*");
        assertAdded("[04]{3}0000[05]{2}33[06].*.+", "\\+[c-z]E[04]?\\.?[04]{0,2}(0{4})?[05]?\\.?[05]{0,1}3\\.?3[06]?.*.*");
        assertAdded("[04]{3}0000[05]{2}33[06][08]{3,5}.*.+", "\\+[f-z]E[04]?\\.?[04]{0,2}(0{4})?[05]?\\.?[05]{0,1}3\\.?3[06]?([08]{3,5})?.*.*");
    }
    
    public void assertAdded(String pattern, String expectedPattern) {
        Node actual = SimpleNumberEncoder.encode(parse(pattern));
        actual = ExponentialBinAdder.addBins(actual);
        actual = ZeroTrimmer.trim(actual);
        actual = DecimalPointPlacer.addDecimalPoints(actual);
        assertThat(actual).asTreeString().isEqualTo(expectedPattern);
    }
}
