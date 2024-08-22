package datawave.data.normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datawave.data.normalizer.GeoNormalizer.ParseException;
import datawave.data.type.util.NumericalEncoder;

public class GeoLonNormalizer extends AbstractNormalizer<String> {
    
    private static final long serialVersionUID = 2026515023484372154L;
    private static final Logger log = LoggerFactory.getLogger(GeoLonNormalizer.class);
    
    public String normalize(String fieldValue) {
        return normalize(null, fieldValue);
    }
    
    public String normalize(String fieldName, String fieldValue) {
        double val;
        try {
            val = GeoNormalizer.parseLatOrLon(fieldValue);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        if (val < -180.0 || val > 180.0) {
            String msg = "Longitude is outside of valid range [-180, 180] ";
            if (null == fieldName) {
                msg += ": " + val;
                msg += " Note: fieldName was null. Consider updating call to normalize(fieldName,fieldPod)";
            } else {
                msg += "for field " + fieldName + ": " + val;
            }
            
            throw new IllegalArgumentException(msg);
        }
        try {
            return NumericalEncoder.encode(Double.toString(val));
        } catch (Exception e) {
            String msg = "Failed to normalize value as GeoLon";
            if (null == fieldName) {
                msg += ": " + val;
                msg += " Note: fieldName was null. Consider updating call to normalize(fieldName,fieldPod)";
            } else {
                msg += " for field " + fieldName + ": " + val;
            }
            
            throw new IllegalArgumentException(msg);
        }
    }
    
    /**
     * We cannot support regex against numbers
     */
    
    public String normalizeRegex(String fieldRegex) {
        throw new IllegalArgumentException("Cannot normalize a regex against a numeric field");
    }
    
    @Override
    public String normalizeDelegateType(String delegateIn) {
        return normalize(delegateIn);
    }
    
    @Override
    public String denormalize(String in) {
        if (NumericalEncoder.isPossiblyEncoded(in)) {
            try {
                return NumericalEncoder.decode(in).toString();
            } catch (NumberFormatException e) {
                if (log.isTraceEnabled()) {
                    log.trace("Error decoding value.", e);
                }
            }
        }
        return in;
    }
    
}
