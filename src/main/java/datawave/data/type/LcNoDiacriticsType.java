package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class LcNoDiacriticsType extends BaseType<String> implements TypePrettyNameSupplier {
    
    private static final long serialVersionUID = -6219894926244790742L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF * 2 + Sizer.REFERENCE;
    private static final String DATA_DICTIONARY_TYPE_NAME = "Text";
    
    public LcNoDiacriticsType() {
        super(Normalizer.LC_NO_DIACRITICS_NORMALIZER);
    }
    
    public LcNoDiacriticsType(String delegateString) {
        super(delegateString, Normalizer.LC_NO_DIACRITICS_NORMALIZER);
    }
    
    /**
     * Two strings + normalizer reference
     * 
     * @return
     */
    @Override
    public long sizeInBytes() {
        return STATIC_SIZE + (2 * normalizedValue.length()) + (2 * delegate.length());
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
