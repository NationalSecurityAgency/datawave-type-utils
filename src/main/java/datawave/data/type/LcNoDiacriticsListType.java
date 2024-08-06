package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class LcNoDiacriticsListType extends ListType implements TypePrettyNameSupplier {
    
    private static final String DATA_DICTIONARY_TYPE_NAME = "Text List";
    
    public LcNoDiacriticsListType() {
        super(Normalizer.LC_NO_DIACRITICS_NORMALIZER);
    }
    
    public LcNoDiacriticsListType(String delegateString) {
        super(delegateString, Normalizer.LC_NO_DIACRITICS_NORMALIZER);
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
