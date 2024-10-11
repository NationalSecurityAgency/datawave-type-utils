package datawave.data.normalizer;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datawave.data.normalizer.regex.NumericRegexEncoder;
import datawave.data.type.util.NumericalEncoder;

public class NumberNormalizer extends AbstractNormalizer<BigDecimal> {
    
    private static final long serialVersionUID = -2781476072987375820L;
    private Logger log = LoggerFactory.getLogger(NumberNormalizer.class);
    
    public String normalize(String fv) {
        if (NumericalEncoder.isPossiblyEncoded(fv)) {
            try {
                NumericalEncoder.decode(fv);
                return fv;
            } catch (Exception e2) {
                // no problem here, we will simply try to encode it below
            }
        }
        try {
            return NumericalEncoder.encode(fv);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to normalize value as a number: " + fv);
        }
    }
    
    /**
     * We cannot support regex against numbers
     */
    public String normalizeRegex(String fieldRegex) {
        try {
            return NumericRegexEncoder.encode(fieldRegex);
        } catch (IllegalArgumentException e) {
            log.debug("Failed to normalize numeric field pattern '" + fieldRegex + "', returning regex as is", e);
            return fieldRegex;
        }
    }
    
    @Override
    public String normalizeDelegateType(BigDecimal delegateIn) {
        return normalize(delegateIn.toString());
    }
    
    @Override
    public BigDecimal denormalize(String in) {
        if (NumericalEncoder.isPossiblyEncoded(in)) {
            try {
                return NumericalEncoder.decode(in);
            } catch (NumberFormatException e) {
                // not encoded...
            }
        }
        return new BigDecimal(in);
    }
    
}
