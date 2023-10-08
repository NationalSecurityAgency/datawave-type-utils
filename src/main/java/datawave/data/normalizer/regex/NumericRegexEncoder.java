package datawave.data.normalizer.regex;

import com.google.common.base.CharMatcher;
import datawave.data.normalizer.regex.visitor.AnchorTrimmer;
import datawave.data.normalizer.regex.visitor.AlternationDeduper;
import datawave.data.normalizer.regex.visitor.DecimalPointPlacer;
import datawave.data.normalizer.regex.visitor.DecimalPointValidator;
import datawave.data.normalizer.regex.visitor.EmptyLeafTrimmer;
import datawave.data.normalizer.regex.visitor.ExponentialBinAdder;
import datawave.data.normalizer.regex.visitor.NegativeNumberPatternInverter;
import datawave.data.normalizer.regex.visitor.NegativeVariantExpander;
import datawave.data.normalizer.regex.visitor.NonEncodedNumbersChecker;
import datawave.data.normalizer.regex.visitor.NumericCharClassValidator;
import datawave.data.normalizer.regex.visitor.OptionalVariantExpander;
import datawave.data.normalizer.regex.visitor.PrintVisitor;
import datawave.data.normalizer.regex.visitor.SimpleNumberEncoder;
import datawave.data.normalizer.regex.visitor.StringVisitor;
import datawave.data.normalizer.regex.visitor.ZeroLengthRepetitionTrimmer;
import datawave.data.normalizer.regex.visitor.ZeroTrimmer;
import datawave.data.normalizer.regex.visitor.ZeroValueNormalizer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class handles provides functionality for encoding numeric regexes that are meant to match against numbers that were previously encoded via
 * {@link datawave.data.type.util.NumericalEncoder#encode(String)}. It is expected that incoming regexes are initially written to match against base ten
 * numbers. Due to the complex nature of how numbers are encoded and trimmed, accuracy is NOT guaranteed when using this class to encode numeric regexes.
 * <P>
 * <P>
 * <strong>Requirements</strong>
 * <P>
 * The following requirements apply to all incoming regexes:
 * <ul>
 * <li>Patterns may not be blank.</li>
 * <li>Patterns may not contain whitespace.</li>
 * <li>Patterns must be compilable.</li>
 * <li>Patterns may not contain any letters other than {@code "\d"}.</li>
 * <li>Patterns may not contain any escaped characters other than {@code "\."}, {@code "\-"}, or {@code "\d"}.</li>
 * <li>Patterns may not contain any groups, e.g. {@code "(45.*)"}.</li>
 * <li>Patterns may not contain any decimal points that are followed by {@code ?} {@code *} {@code +} or a repetition quantifier such as {@code {3}}.</li>
 * </ul>
 * <P>
 * <P>
 * <strong>Supported Regex Features</strong>
 * <P>
 * The following regex features are supported, with any noted caveats.
 * <ul>
 * <li>Wildcards {@code "."}.</li>
 * <li>Digit character class {@code "\d"}.</li>
 * <li>Character class lists {@code "[]"}. CAVEAT: Digit characters only. Ranges are supported.</li>
 * <li>Zero or more quantifier {@code "*"}.</li>
 * <li>One or more quantifier {@code "+"}.</li>
 * <li>Repetition quantifier {@code "{x}"}, {@code "{x,}"}, and {@code "{x,y}"}.</li>
 * <li>Anchors {@code "^"} and {@code "$"}. CAVEAT: Technically not truly supported as they are ultimately removed during the pre-optimization process. However,
 * using them will not result in an error.</li>
 * <li>Alternations {@code "|"}.</li>
 * </ul>
 * Additionally, in order to mark a regex pattern as intended to match negative numbers only, a minus sign should be placed at the beginning of the regex
 * pattern, e.g. {@code "-34.*"}, or at the beginning of each desired alternated pattern.
 * <P>
 * <P>
 * <strong>Optimizations</strong>
 * <P>
 * Before encoding the incoming regex, it will undergo the following modifications to optimize the ease of encoding:
 * <ol>
 * <li>Any empty alternations will be removed.</li>
 * <li>Any occurrences of the anchors {@code ^} or {@code $} will be removed. These will need to be added back into the returned encoded regex pattern
 * afterwards if desired.</li>
 * <li>Optional variants (characters followed by {@code ?}} will be expanded into additional alternations as seen. This will not apply to any {@code ?}
 * instances that directly follow a {@code *}, {@code +}, or {@code {x}}, as the {@code ?} in this case modifies the greediness of the matching rather than
 * whether or not a character can be present.</li>
 * <li>Any characters immediately followed by the repetition quantifier {@code "{0}"} or {@code "{0,0}"} will be removed as they are expected to occur zero
 * times. This does not apply to characters with the repetition quantifier {@code "{0,}"} or a variation of {@code "{0,x}"}.</li>
 * <li>Any patterns starting with {@code ".*"} or {@code ".+"} will result in the addition of an alternation of the same pattern with a minus sign in front of
 * it to ensure a variant for matching negative numbers is added. This does not apply to any regex patterns already starting with {@code "-.*"} or
 * {@code "-.+"}.</li>
 * <li>In some cases a pattern may match both exactly zero and another number greater than one, e.g. the pattern "[0-9].*". In this case, an alternation for the
 * character {@code "0"} will be added (i.e. {@code "[0-9].*|0"}) to ensure that the ability to match zero is not lost when enriching the pattern with the
 * required exponential bins to target the appropriate encoded numbers.</li>
 * <li>Pattern alternations will be de-duped.</li>
 * </ol>
 * <P>
 * <P>
 * A strong effort has been made to make resulting encoded patterns as accurate as possible, but there is always a chance of at least some inaccuracy, given the
 * nature of how numbers are encoded, particularly when it comes to numbers that are very similar other than the location of a decimal point, if present, in
 * them. If you find that the resulting encoded regex is not matching the desired encoding numbers, try to simplify it into a higher number of alternations with
 * simpler regexes if possible.
 * 
 * @see datawave.data.type.util.NumericalEncoder
 */
public class NumericRegexEncoder {
    
    private static final Logger log = LoggerFactory.getLogger(NumericRegexEncoder.class);
    
    /**
     * Matches against any unescaped d characters, and any other letters. If \d is present, that indicates a digit and is allowed.
     */
    private static final Pattern RESTRICTED_LETTERS_PATTERN = Pattern.compile(".*[a-ce-zA-Z].*");
    
    /**
     * Matches any escaped character that is not \. \- or \d.
     */
    private static final Pattern RESTRICTED_ESCAPED_CHARS_PATTERN = Pattern.compile(".*\\\\[^.d\\-].*");
    
    /**
     * Matches any regex that consists only of anchors, hyphens (escaped or not), escaped periods, repetitions, the quantifier *, the quantifier +, optionals,
     * alternations, and groups in any order with no alphanumeric characters that give any meaningful numeric information.
     */
    private static final Pattern NONSENSE_PATTERN = Pattern.compile("^\\^?(\\(*(\\\\\\.)*\\)*|(\\(*\\\\?[\\-*+?|])*\\)*|(\\{.*}))*\\$?$");
    
    /**
     * Matches any decimal points with ? + * or a repetition quantifier directly following them.
     */
    private static final Pattern INVALID_DECIMAL_POINTS_PATTERN = Pattern.compile(".*\\\\\\.[?+*{].*");
    
    /**
     * Matches against any variation of {@code .*}, {@code .+}, {@code .*?}, {@code .+?} that may or may not repeat, and that may or may not contain start
     * and/or end anchors.
     */
    private static final Pattern NORMALIZATION_NOT_REQUIRED_PATTERN = Pattern.compile("^\\^?(\\.[*+]\\??)+\\$?$");
    
    public static String encode(String regex) {
        return new NumericRegexEncoder(regex).encode();
    }
    
    private final String pattern;
    private Node patternTree;
    
    private NumericRegexEncoder(String pattern) {
        this.pattern = pattern;
    }
    
    private String encode() {
        if (log.isDebugEnabled()) {
            log.debug("Encoding pattern " + pattern);
        }
        
        // Check the pattern for any quick failures.
        checkPatternForQuickFailures();
        // Encode the pattern only if it requires it.
        if (isEncodingRequired()) {
            parsePatternTree();
            normalizePatternTree();
            encodePatternTree();
            
            if (log.isDebugEnabled()) {
                log.debug("Encoded pattern: " + StringVisitor.toString(this.patternTree));
            }
            
            return StringVisitor.toString(this.patternTree);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Encoding not required for " + pattern);
            }
            return this.pattern;
        }
    }
    
    /**
     * Pre-validate the regex to quickly identify any indications that the regex is not valid for numerical expansion.
     */
    private void checkPatternForQuickFailures() {
        checkForBlankPattern();
        checkForWhitespace();
        checkForCompilation();
        checkForNonsense();
        checkForRestrictedLetters();
        checkForRestrictedEscapedCharacters();
        checkForGroups();
        checkForQuantifiedDecimalPoints();
    }
    
    /**
     * Throws an exception if the regex pattern is blank.
     */
    private void checkForBlankPattern() {
        if (StringUtils.isBlank(this.pattern)) {
            throw new IllegalArgumentException("Regex pattern may not be blank.");
        }
    }
    
    /**
     * Throws an exception if the regex contains any whitespace.
     */
    private void checkForWhitespace() {
        if (CharMatcher.whitespace().matchesAnyOf(pattern)) {
            throw new IllegalArgumentException("Regex pattern may not contain any whitespace.");
        }
    }
    
    /**
     * Throws an exception if the regex cannot be compiled.
     */
    private void checkForCompilation() {
        try {
            Pattern.compile(this.pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Regex pattern will not compile.", e);
        }
    }
    
    private void checkForNonsense() {
        if (NONSENSE_PATTERN.matcher(this.pattern).matches()) {
            throw new IllegalArgumentException("A nonsense pattern has been given that cannot be normalized.");
        }
    }
    
    /**
     * Throws an exception if the regex contains any letter other than an escaped lowercase d.
     */
    private void checkForRestrictedLetters() {
        if (RESTRICTED_LETTERS_PATTERN.matcher(pattern).matches() || containsUnescapedLowercaseD()) {
            throw new IllegalArgumentException(
                            "Regex pattern may not contain any letters other than \\d to indicate a member of the digit character class 0-9.");
        }
    }
    
    /**
     * Return whether the regex contains an unescaped d.
     */
    private boolean containsUnescapedLowercaseD() {
        int pos = pattern.indexOf(RegexConstants.LOWERCASE_D);
        while (pos != -1) {
            if (pos == 0 || pattern.charAt(pos - 1) != RegexConstants.BACKSLASH) {
                return true;
            }
            pos = pattern.indexOf(RegexConstants.LOWERCASE_D, pos + 1);
        }
        return false;
    }
    
    /**
     * Throws an exception if the regex contains any escaped characters other than {@code \.}, {@code \-} or {@code \d}.
     */
    private void checkForRestrictedEscapedCharacters() {
        if (RESTRICTED_ESCAPED_CHARS_PATTERN.matcher(this.pattern).matches()) {
            throw new IllegalArgumentException("Regex pattern may not contain any escaped characters other than \\. \\- or \\d.");
        }
    }
    
    /**
     * Throws an exception if the regex contains any occurrences of '(' indicating the start of a group.
     */
    private void checkForGroups() {
        if (this.pattern.contains("(")) {
            throw new IllegalArgumentException("Regex pattern may not contain any groups.");
        }
    }
    
    /**
     * Throws an exception if the regex contains any decimal points directly followed by * + or {}.
     */
    private void checkForQuantifiedDecimalPoints() {
        if (INVALID_DECIMAL_POINTS_PATTERN.matcher(this.pattern).matches()) {
            throw new IllegalArgumentException("Regex pattern may not contain any decimal points that are directly followed by * ? or {}.");
        }
    }
    
    /**
     * Returns whether the regex requires normalization.
     * 
     * @return true if the regex requires normalization, or false otherwise.
     */
    private boolean isEncodingRequired() {
        return !NORMALIZATION_NOT_REQUIRED_PATTERN.matcher(this.pattern).matches();
    }
    
    /**
     * Parse the regex to a node tree.
     */
    private void parsePatternTree() {
        this.patternTree = RegexParser.parse(this.pattern);
        if (log.isDebugEnabled()) {
            log.debug("Parsed pattern to tree structure: \n" + PrintVisitor.printToString(this.patternTree));
        }
        
        // Verify that the regex pattern does not contain any character classes with characters other than digits or a period.
        NumericCharClassValidator.validate(this.patternTree);
        
        // Verify that the regex pattern does not contain any alternated expressions that have more than one required decimal point.
        DecimalPointValidator.validate(this.patternTree);
    }
    
    /**
     * Normalize the pattern tree.
     */
    private void normalizePatternTree() {
        // Trim the tree of any empty nodes and empty alternations, and verify if we still have a pattern to encode.
        this.patternTree = EmptyLeafTrimmer.trim(this.patternTree);
        if (this.patternTree == null) {
            throw new IllegalArgumentException("Regex pattern is empty after trimming empty alternations.");
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after trimming empty leafs: " + StringVisitor.toString(this.patternTree));
        }
        
        // Trim all anchors.
        this.patternTree = AnchorTrimmer.trim(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after trimming anchors: " + StringVisitor.toString(this.patternTree));
        }
        
        // Expand optional variants.
        this.patternTree = OptionalVariantExpander.expand(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after expanding optional variants: " + StringVisitor.toString(this.patternTree));
        }
        
        // Remove all zero-length repetitions.
        this.patternTree = ZeroLengthRepetitionTrimmer.trim(this.patternTree);
        
        // If the pattern is empty afterwards, throw an exception.
        if (this.patternTree == null) {
            throw new IllegalArgumentException("Regex pattern is empty after trimming all characters followed by {0} or {0,0}.");
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after trimming zero-length repetition characters: " + StringVisitor.toString(this.patternTree));
        }
        
        // Expand leading wildcards to include negative variants.
        this.patternTree = NegativeVariantExpander.expand(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after expanding negative variants: " + StringVisitor.toString(this.patternTree));
        }
        
        // Normalize any patterns that either only match or can match zero.
        this.patternTree = ZeroValueNormalizer.normalize(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after normalizing zero-value characters: " + StringVisitor.toString(this.patternTree));
        }
    }
    
    /**
     * Encode the pattern tree.
     */
    private void encodePatternTree() {
        // Before encoding, remove any duplicate alternations.
        this.patternTree = AlternationDeduper.dedupe(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after de-duping: " + StringVisitor.toString(this.patternTree));
        }
        
        // Encode any and all simple numbers present in the pattern.
        this.patternTree = SimpleNumberEncoder.encode(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after encoding simple numbers: " + StringVisitor.toString(this.patternTree));
        }
        
        // If there are no more unencoded sub-patterns in the tree after encoding simple numbers, no further work needs to be done.
        if (!NonEncodedNumbersChecker.check(this.patternTree)) {
            return;
        }
        
        // Add exponential bin range information, e.g. \+[a-z], ![A-Z], etc.
        this.patternTree = ExponentialBinAdder.addBins(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after adding exponential bin information: " + StringVisitor.toString(this.patternTree));
        }
        
        // Trim/consolidate any leading zeros in partially-encoded patterns.
        this.patternTree = ZeroTrimmer.trim(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after trimming leading/trailing zeros: " + StringVisitor.toString(this.patternTree));
        }
        
        // Add decimal points where required.
        this.patternTree = DecimalPointPlacer.addDecimalPoints(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after adding decimal points: " + StringVisitor.toString(this.patternTree));
        }
        
        // Invert any patterns that are meant to match negative numbers.
        this.patternTree = NegativeNumberPatternInverter.invert(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after inverting patterns for negative numbers: " + StringVisitor.toString(this.patternTree));
        }
        
        // Finally, remove any duplicate alternations that resulted during the encoding process.
        this.patternTree = AlternationDeduper.dedupe(this.patternTree);
        
        if (log.isDebugEnabled()) {
            log.debug("Pattern tree after de-duping: " + StringVisitor.toString(this.patternTree));
        }
    }
}
