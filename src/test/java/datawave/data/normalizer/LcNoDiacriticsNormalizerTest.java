package datawave.data.normalizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class LcNoDiacriticsNormalizerTest {
    @Test
    public void test1() {
        LcNoDiacriticsNormalizer norm = new LcNoDiacriticsNormalizer();
        String b = null;
        String n1 = norm.normalize(b);
        
        assertNull(n1);
        
    }
}
