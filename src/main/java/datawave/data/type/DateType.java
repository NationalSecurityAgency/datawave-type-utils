package datawave.data.type;

import java.util.Date;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.TypePrettyNameSupplier;

public class DateType extends BaseType<Date> implements TypePrettyNameSupplier {
    
    private static final long serialVersionUID = 936566410691643144L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF + PrecomputedSizes.DATE_STATIC_REF + Sizer.REFERENCE;
    private static final String DATA_DICTIONARY_TYPE_NAME = "Date";
    
    public DateType() {
        super(Normalizer.DATE_NORMALIZER);
    }
    
    public DateType(String dateString) {
        super(Normalizer.DATE_NORMALIZER);
        super.setDelegate(normalizer.denormalize(dateString));
    }
    
    @Override
    public String getDelegateAsString() {
        // the normalized form of the date preserves milliseconds
        return normalizer.normalizeDelegateType(getDelegate());
    }
    
    /**
     * One string, one date object, one reference to the normalizer
     * 
     * @return
     */
    @Override
    public long sizeInBytes() {
        return STATIC_SIZE + (2 * normalizedValue.length());
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
