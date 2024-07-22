package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class TrimLeadingZerosType extends BaseType<String> implements TypePrettyNameSupplier {
    
    private static final long serialVersionUID = -7425014359719165469L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF * 2 + Sizer.REFERENCE;
    private static final String DATA_DICTIONARY_TYPE_NAME = "Trimmed Number";
    
    public TrimLeadingZerosType() {
        super(Normalizer.TRIM_LEADING_ZEROS_NORMALIZER);
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
