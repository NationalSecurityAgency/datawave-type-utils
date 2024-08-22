package datawave.data.normalizer;

import java.util.Locale;

import datawave.query.parser.JavaRegexAnalyzer;
import datawave.query.parser.JavaRegexAnalyzer.JavaRegexParseException;

/**
 * 
 */
public class LcNormalizer extends AbstractNormalizer<String> {
    
    private static final long serialVersionUID = 8311875506912885780L;
    
    public String normalize(String fieldValue) {
        return normalize(null, fieldValue);
    }
    
    public String normalize(String fieldName, String fieldValue) {
        return fieldValue.toLowerCase(Locale.ENGLISH);
    }
    
    public String normalizeRegex(String fieldRegex) {
        if (null == fieldRegex) {
            return null;
        }
        try {
            JavaRegexAnalyzer regex = new JavaRegexAnalyzer(fieldRegex);
            regex.applyRegexCaseSensitivity(false);
            return regex.getRegex();
        } catch (JavaRegexParseException e) {
            throw new IllegalArgumentException("Unable to parse regex " + fieldRegex, e);
        }
    }
    
    @Override
    public String normalizeDelegateType(String delegateIn) {
        return normalize(delegateIn);
    }
    
    @Override
    public String denormalize(String in) {
        return in;
    }
}
