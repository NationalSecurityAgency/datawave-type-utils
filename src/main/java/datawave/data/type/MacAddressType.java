package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class MacAddressType extends BaseType<String> implements TypePrettyNameSupplier {
    
    private static final long serialVersionUID = -6743560287574389073L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF * 2 + Sizer.REFERENCE;
    private static final String DATA_DICTIONARY_TYPE_NAME = "MAC Address";
    
    public MacAddressType() {
        super(Normalizer.MAC_ADDRESS_NORMALIZER);
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
