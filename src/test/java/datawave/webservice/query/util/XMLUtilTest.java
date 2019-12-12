package datawave.webservice.query.util;

import com.sun.org.apache.xml.internal.utils.XMLChar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XMLUtilTest {
    @Test
    public void testAllCharacters() {
        for (int i = 0; i < 0x300000; ++i) {
            // Uses an internal class, but only for testing.
            boolean expectedResult = XMLChar.isValid(i);
            assertEquals(expectedResult, XMLUtil.isValidXMLChar(i), "Mismatch for 0x" + Integer.toHexString(i));
        }
    }
    
    @Test
    public void testValidXMLString() {
        assertTrue(XMLUtil.isValidXML("This is valid XML \u0009\r\n \u0021 \uD1FF"));
    }
    
    @Test
    public void testInvalidXMLString() {
        assertFalse(XMLUtil.isValidXML("This \u0002 is not valid"));
    }
}
