package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class RawDateType extends BaseType<String> implements TypePrettyNameSupplier {
    
    private static final long serialVersionUID = 936566410691643144L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF * 2 + Sizer.REFERENCE;
    private static final String DATA_DICTIONARY_TYPE_NAME = "Raw Date";
    
    public RawDateType() {
        super(Normalizer.RAW_DATE_NORMALIZER);
    }
    
    public RawDateType(String dateString) {
        super(Normalizer.RAW_DATE_NORMALIZER);
        super.setDelegate(normalizer.denormalize(dateString));
    }
    
    /**
     * Two String + normalizer reference
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
