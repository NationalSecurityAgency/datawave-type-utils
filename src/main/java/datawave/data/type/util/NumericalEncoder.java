package datawave.data.type.util;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides a one-to-one mapping between an input decimal number and a lexicographically sorted index for that number. The index is composed of two parts,
 * roughly derived from scientific notation: the two digit exponential bin and the mantissa, with 'E' as a separator. Thus, an index takes this format:
 * {@code 'bin'E'mantissa'}.
 * <p/>
 * The bins are broken into four groups:
 * <ol>
 *     <li>!A through !Z represent negative numbers with magnitude greater than one (exponents 25 through 0, respectively)</li>
 *     <li>!a through !z represent negative numbers with magnitude less than 1 (exponents -1 through -26, respectively)</li>
 *     <li>+A through +Z represent positive numbers with magnitude less than 1 (exponents -26 through -1, respectively)</li>
 *     <li>+a through +z represent positive numbers with magnitude greater than one (exponents 0 through 25, respectively)</li>
 * </ol>
 * For positive numbers, the mantissa exactly matches the mantissa of scientific notation. For negative numbers, the mantissa equals ten minus the mantissa of
 * scientific notation.
 * <p/>
 * Some example inputs and encodings:
 * <ul>
 *     <li>-12344984165 becomes !PE8.7655015835</li>
 *     <li>-500 becomes !XE5</li>
 *     <li>-0.501 becomes !aE4.99</li>
 *     <li>0 becomes +AE0</li>
 *     <li>9E-9 becomes +RE9</li>
 *     <li>0.501 becomes +ZE5.01</li>
 *     <li>10000 becomes +eE1</li>
 * </ul>
 */
public class NumericalEncoder {
    
    private static Map<String,String> positiveNumsEncodeToIntExponentsMap;
    private static Map<String,String> positiveNumsIntToEncodeExponentsMap;
    private static Map<String,String> negativeNumEncodeToIntExponentsMap;
    private static Map<String,String> negativeNumIntToEncodeExponentsMap;
    private static final NumberFormat plainFormatter = new DecimalFormat("0.#########################################################");
    private static final NumberFormat scientificFormatter = new DecimalFormat("0.#########################################################E0");
    private static final String zero = "+AE0";
    
    static {
        initNegativeExponents();
        initPositiveExponents();
    }
    
    public static String encode(String input) {
        try {
            BigDecimal decimal = new BigDecimal(input);
            String encodedExponent;
            String mantissa;
            if (decimal.compareTo(BigDecimal.ZERO) == 0) {
                return zero;
            } else if (decimal.compareTo(BigDecimal.ZERO) > 0) {
                // Positive
                String decString = scientificFormatter.format(decimal);
                String[] decParts = decString.split("E");
                mantissa = decParts[0];
                String exp = decParts[1];
                encodedExponent = positiveNumsIntToEncodeExponentsMap.get(exp);
            } else {
                // Negative
                String decString = scientificFormatter.format(decimal);
                String[] decParts = decString.split("E");
                mantissa = decParts[0];
                String exp = decParts[1];
                encodedExponent = negativeNumIntToEncodeExponentsMap.get(exp);
                BigDecimal bigDecMantissa = new BigDecimal(mantissa);
                bigDecMantissa = BigDecimal.TEN.add(bigDecMantissa);
                mantissa = plainFormatter.format(bigDecMantissa);
                
            }
            
            if (encodedExponent == null) {
                throw new NumberFormatException("Exponent exceeded allowed range.");
            }
            
            return encodedExponent + "E" + mantissa;
        } catch (Exception ex) {
            throw new NumberFormatException("Error formatting input: " + input + " . Error: " + ex);
        }
    }
    
    /**
     * This provides a quick test that will determine whether this value is possibly encoded. Provides a mechanism that is significantly faster than waiting for
     * the decode method to throw an exception.
     * 
     * @param input the value to test for encoding
     * @return true if possibly encoded, false if definitely not encoded
     */
    public static boolean isPossiblyEncoded(String input) {
        if (null == input || input.isEmpty())
            return false;
        char c = input.charAt(0);
        return (c == '+' || c == '!');
    }
    
    public static BigDecimal decode(String input) {
        BigDecimal output;
        if (input.equals(zero)) {
            return BigDecimal.ZERO;
        } else {
            try {
                String exp = input.substring(0, 2);
                String mantissa = input.substring(3);
                if (exp.contains("+")) {
                    // Positive Number
                    exp = positiveNumsEncodeToIntExponentsMap.get(exp);
                    output = new BigDecimal(mantissa + "E" + exp);
                } else if (exp.contains("!")) {
                    // Negative Number
                    exp = negativeNumEncodeToIntExponentsMap.get(exp);
                    output = new BigDecimal(mantissa).subtract(BigDecimal.TEN).movePointRight(Integer.parseInt(exp));
                } else {
                    throw new NumberFormatException("Unknown encoded exponent");
                }
                
            } catch (Exception ex) {
                throw new NumberFormatException("Error decoding output: " + input + " . Error: " + ex);
            }
        }
        return output;
    }
    
    private static Map<String,String> createExponentMap(String[] exponents) {
        Map<String,String> map = new HashMap<>();
        for (int j = 0; j < exponents.length; j++) {
            int exponent = j - 26;
            map.put(exponents[j], String.valueOf(exponent));
        }
        return map;
    }
    
    private static Map<String,String> invertMap(Map<String,String> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }
    
    private static void initPositiveExponents() {
        // The order of the encoded characters here maps directly to how their corresponding exponent value is calculated, and must not be changed.
        String[] exponents = new String[]{"+A", "+B", "+C", "+D", "+E", "+F", "+G", "+H", "+I", "+J", "+K", "+L", "+M", "+N", "+O", "+P", "+Q", "+R", "+S",
                        "+T", "+U", "+V", "+W", "+X", "+Y", "+Z", "+a", "+b", "+c", "+d", "+e", "+f", "+g", "+h", "+i", "+j", "+k", "+l", "+m", "+n", "+o",
                        "+p", "+q", "+r", "+s", "+t", "+u", "+v", "+w", "+x", "+y", "+z"};
        Map<String,String> map = createExponentMap(exponents);
        positiveNumsEncodeToIntExponentsMap = Collections.unmodifiableMap(map);
        positiveNumsIntToEncodeExponentsMap = Collections.unmodifiableMap(invertMap(map));
    }
    
    private static void initNegativeExponents() {
        // The order of the encoded characters here maps directly to how their corresponding exponent value is calculated, and must not be changed.
        String[] exponents = new String[]{"!z", "!y", "!x", "!w", "!v", "!u", "!t", "!s", "!r", "!q", "!p", "!o", "!n", "!m", "!l", "!k", "!j", "!i", "!h",
                        "!g", "!f", "!e", "!d", "!c", "!b", "!a", "!Z", "!Y", "!X", "!W", "!V", "!U", "!T", "!S", "!R", "!Q", "!P", "!O", "!N", "!M", "!L",
                        "!K", "!J", "!I", "!H", "!G", "!F", "!E", "!D", "!C", "!B", "!A"};
        Map<String,String> map = createExponentMap(exponents);
        negativeNumEncodeToIntExponentsMap = Collections.unmodifiableMap(map);
        negativeNumIntToEncodeExponentsMap = Collections.unmodifiableMap(invertMap(map));
    }
}
