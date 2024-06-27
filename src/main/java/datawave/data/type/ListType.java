package datawave.data.type;

import java.util.ArrayList;
import java.util.List;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;
import datawave.util.StringUtils;

public abstract class ListType extends BaseType implements OneToManyNormalizerType, TypePrettyNameSupplier {
    protected static final String delimiter = ",|;";
    List<String> normalizedValues;
    private static final String DATA_DICTIONARY_TYPE_NAME = "List";
    
    public ListType(Normalizer normalizer) {
        super(normalizer);
    }
    
    public ListType(String delegateString, Normalizer normalizer) {
        super(delegateString, normalizer);
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
    public void setDelegateFromString(String in) {
        this.normalizedValues = normalizeToMany(in);
        this.delegate = in;
        setNormalizedValue(in);
    }
    
    @Override
    public List<String> getNormalizedValues() {
        return normalizedValues;
    }
    
    @Override
    public boolean expandAtQueryTime() {
        return false;
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
