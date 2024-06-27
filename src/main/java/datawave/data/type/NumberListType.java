package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class NumberListType extends ListType implements TypePrettyNameSupplier {
    
    private static final String DATA_DICTIONARY_TYPE_NAME = "Number List";
    
    public NumberListType() {
        super(Normalizer.NUMBER_NORMALIZER);
    }
    
    @Override
    public boolean expandAtQueryTime() {
        return true;
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
