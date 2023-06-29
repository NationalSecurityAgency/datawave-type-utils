package datawave.data.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;

import java.util.Arrays;
import java.util.List;

public class ListTypeTest {
    
    @Test
    public void test() {
        String str = "1,2,3;a;b;c";
        
        ListType t = new ListType(str);
        Assert.equals(6, t.normalizeToMany(str).size());
    }
    
    @Test
    public void testNumberList() {
        String str = "1,2,3,5.5";
        List<String> expected = Arrays.asList(new String[] {"+aE1", "+aE2", "+aE3", "+aE5.5"});
        
        NumberListType nt = new NumberListType();
        Assert.equals(4, nt.normalizeToMany(str).size());
        Assert.equals(expected, nt.normalizeToMany(str));
    }
    
    @Test
    public void testBadNumberList() {
        String str = "3,2,1,banana";
        
        NumberListType nt = new NumberListType();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            nt.normalizeToMany(str);
        });
        
    }
    
}
