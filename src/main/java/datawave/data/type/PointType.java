package datawave.data.type;

import datawave.data.normalizer.Normalizer;
import datawave.data.type.util.Point;
import datawave.data.type.util.TypePrettyNameSupplier;

/**
 * Provides support for point geometry types. Other geometry types are not compatible with this type.
 */
public class PointType extends AbstractGeometryType<Point> implements TypePrettyNameSupplier {
    
    private static final String DATA_DICTIONARY_TYPE_NAME = "Point";
    
    public PointType() {
        super(Normalizer.POINT_NORMALIZER);
    }
    
    @Override
    public String getDataDictionaryTypeValue() {
        return DATA_DICTIONARY_TYPE_NAME;
    }
}
