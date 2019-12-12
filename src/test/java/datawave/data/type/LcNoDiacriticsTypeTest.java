package datawave.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 *
 * 
 */
public class LcNoDiacriticsTypeTest {
    @Test
    public void test1() {
        LcNoDiacriticsType norm = new LcNoDiacriticsType();
        String b = null;
        String n1 = norm.normalize(b);
        
        assertNull(n1);
        
    }
}
