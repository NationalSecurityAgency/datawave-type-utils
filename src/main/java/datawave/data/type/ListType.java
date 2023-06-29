package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ListType extends BaseType implements OneToManyNormalizerType {
    private static final String delimiter = ",|;";
    List<String> normalizedValues;
    
    public ListType(String delegateString) {
        super(delegateString, Normalizer.LC_NO_DIACRITICS_NORMALIZER);
    }
    
    public ListType(Normalizer normalizer) {
        super(normalizer);
    }
    
    @Override
    public List<String> normalizeToMany(String in) {
        String[] splits = StringUtils.split(in, delimiter);
        List<String> strings = new ArrayList(splits.length);
        for (String s : splits) {
            
            String str = normalizer.normalize(s);
            strings.add(str);
            
        }
        
        return strings;
    }
    
    @Override
    public List<String> getNormalizedValues() {
        return normalizedValues;
    }
    
}
