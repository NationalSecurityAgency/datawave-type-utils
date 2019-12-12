package datawave.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypeFactoryTest {
    
    @Test
    public void testWithCorrectType() throws Exception {
        Type<?> type = Type.Factory.createType("datawave.data.type.LcType");
        assertTrue(type instanceof LcType);
    }
    
    @Test
    public void testWithIncorrectType() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Type.Factory.createType("datawave.ingest.data.normalizer.LcNoDiacriticsNormalizer"));
    }
    
}
