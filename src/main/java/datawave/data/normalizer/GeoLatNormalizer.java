package datawave.data.normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datawave.data.normalizer.GeoNormalizer.ParseException;
import datawave.data.type.util.NumericalEncoder;

public class GeoLatNormalizer extends AbstractNormalizer<String> {
    
    private static final long serialVersionUID = -1838190858989807274L;
    private static final Logger log = LoggerFactory.getLogger(GeoLatNormalizer.class);
    
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
        if (val < -90.0 || val > 90.0) {
            String msg = "Latitude is outside of valid range [-90, 90]";
            if (null == fieldName) {
                msg += ": " + val;
                msg += " Note: fieldName was null. Consider updating call to normalize(fieldName,fieldPod)";
            } else {
                msg += " for field " + fieldName + ": " + val;
            }
            
            throw new IllegalArgumentException(msg);
        }
        try {
            return NumericalEncoder.encode(Double.toString(val));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to normalize value as a GeoLat: " + fieldValue);
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
